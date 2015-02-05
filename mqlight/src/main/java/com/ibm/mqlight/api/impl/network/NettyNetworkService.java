/*
 *   <copyright
 *   notice="oco-source"
 *   pids="5725-P60"
 *   years="2015"
 *   crc="1438874957" >
 *   IBM Confidential
 *
 *   OCO Source Materials
 *
 *   5724-H72
 *
 *   (C) Copyright IBM Corp. 2015
 *
 *   The source code for the program is not published
 *   or otherwise divested of its trade secrets,
 *   irrespective of what has been deposited with the
 *   U.S. Copyright Office.
 *   </copyright>
 */

package com.ibm.mqlight.api.impl.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;
import com.ibm.mqlight.api.network.NetworkService;

public class NettyNetworkService implements NetworkService {

    static {
        LogbackLogging.setup();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static Bootstrap insecureBootstrap;
    private static Bootstrap secureBootstrap;

    static class NettyInboundHandler extends ChannelInboundHandlerAdapter implements NetworkChannel {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final SocketChannel channel;
        private NetworkListener listener = null;
        private AtomicBoolean closed = new AtomicBoolean(false);

        protected NettyInboundHandler(SocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            logger.debug("channel read");
            // Make a copy of the buffer
            // TODO: this is inefficient - support for pooling should be integrated into
            //       the interfaces that define a network service...
            if (listener != null) {
                ByteBuf buf = ((ByteBuf)msg);
                ByteBuffer nioBuf = ByteBuffer.allocate(buf.readableBytes());
                buf.readBytes(nioBuf);
                nioBuf.flip();
                listener.onRead(this, nioBuf);
                buf.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.debug("exception caught: {}", cause);
            ctx.close();
            Exception exception;
            if (cause instanceof Exception) {
                exception = (Exception)cause;
            } else {
                exception = new Exception(cause);   // TODO: wrap in a better exception...
            }
            if (listener != null) {
                listener.onError(this, exception);
            }
            decrementUseCount();
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx)
                throws Exception {
            logger.debug("channelWritabilityChanged");
            doWrite(null);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("channelInactive");
            boolean alreadyClosed = closed.getAndSet(true);
            if (!alreadyClosed) {
                if (listener != null) {
                    listener.onClose(this);
                }
                decrementUseCount();
            }
        }

        protected void setListener(NetworkListener listener) {
            logger.debug("setting listener");
            this.listener = listener;
        }

        @Override
        public void close(final Promise<Void> nwfuture) {
            logger.debug("close");
            boolean alreadyClosed = closed.getAndSet(true);
            if (!alreadyClosed) {
                final ChannelFuture f = channel.disconnect();
                if (nwfuture != null) {
                    f.addListener(new GenericFutureListener<ChannelFuture>() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            nwfuture.setSuccess(null);
                            decrementUseCount();
                        }
                    });
                }
            } else if (nwfuture != null) {
                nwfuture.setSuccess(null);
            }
        }

        private static class WriteRequest {
            protected final ByteBuf buffer;
            protected final Promise<Boolean> promise;
            protected WriteRequest(ByteBuf buffer, Promise<Boolean> promise) {
                this.buffer = buffer;
                this.promise = promise;
            }
        }

        @Override
        public void write(ByteBuffer buffer, Promise<Boolean> promise) {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
            doWrite(new WriteRequest(byteBuf, promise));
        }


        LinkedList<WriteRequest> pendingWrites = new LinkedList<>();
        boolean writeInProgress = false;

        private void doWrite(final WriteRequest writeRequest) {
            logger.debug("doWrite {}" + writeRequest);
            WriteRequest toProcess = null;
            synchronized(pendingWrites) {
                if (writeRequest != null) {
                    pendingWrites.addLast(writeRequest);
                }
                if (!writeInProgress && channel.isWritable() && !pendingWrites.isEmpty()) {
                    toProcess = pendingWrites.removeFirst();
                    writeInProgress = true;
                }
            }

            if (toProcess != null) {
                final Promise<Boolean> writeCompletePromise = toProcess.promise;
                logger.debug("writeAndFlush {}", toProcess);
                final ChannelFuture f = channel.pipeline().writeAndFlush(toProcess.buffer);
                f.addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        boolean havePendingWrites = false;
                        synchronized(pendingWrites) {
                            writeInProgress = false;
                            havePendingWrites = !pendingWrites.isEmpty();
                        }
                        logger.debug("doWrite (complete)");
                        writeCompletePromise.setSuccess(!havePendingWrites);
                        doWrite(null);
                    }
                });
            }
        }

        private Object context;

        @Override
        public synchronized void setContext(Object context) {
            this.context = context;
        }

        @Override
        public synchronized Object getContext() {
            return context;
        }
    }

    protected class ConnectListener implements GenericFutureListener<ChannelFuture> {
        private final Promise<NetworkChannel> promise;
        private final NetworkListener listener;
        protected ConnectListener(ChannelFuture cFuture, Promise<NetworkChannel> promise, NetworkListener listener) {
            this.promise = promise;
            this.listener = listener;

        }
        @Override
        public void operationComplete(ChannelFuture cFuture) throws Exception {
            logger.debug("connect complete {}", cFuture);
            if (cFuture.isSuccess()) {
                NettyInboundHandler handler = (NettyInboundHandler)cFuture.channel().pipeline().last();
                handler.setListener(listener);
                promise.setSuccess(handler);
            } else {
                ClientException cause = new ClientException("Could not connect to server: " + cFuture.cause().getMessage(), cFuture.cause());
                promise.setFailure(cause);
                decrementUseCount();
            }
        }

    }

    @Override
    public void connect(Endpoint endpoint, NetworkListener listener, Promise<NetworkChannel> promise) {
        logger.debug("> connect {} {}", endpoint.getHost(), endpoint.getPort());
        final Bootstrap bootstrap = getBootstrap(endpoint.useSsl());
        final ChannelFuture f = bootstrap.connect(endpoint.getHost(), endpoint.getPort());
        f.addListener(new ConnectListener(f, promise, listener));
        logger.debug("< connect");
    }

    private static int useCount = 0;

    /**
     * Request a {@link Bootstrap} for obtaining a {@link Channel} and track
     * that the workerGroup is being used.
     *
     * @param secure
     *            a {@code boolean} indicating whether or not a secure channel
     *            will be required
     * @return a netty {@link Bootstrap} object suitable for obtaining a
     *         {@link Channel} for the
     */
    private static synchronized Bootstrap getBootstrap(final boolean secure) {
        ++useCount;
        if (useCount == 1) {
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            secureBootstrap = new Bootstrap();
            secureBootstrap.group(workerGroup);
            secureBootstrap.channel(NioSocketChannel.class);
            secureBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            secureBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
            secureBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    SslContext sslCtx = SslContext.newClientContext();
                    SSLEngine sslEngine = sslCtx.newEngine(ch.alloc());
                    sslEngine.setUseClientMode(true);
                    ch.pipeline().addFirst(new SslHandler(sslEngine));
                    ch.pipeline().addLast(new NettyInboundHandler(ch));
                }
            });
            insecureBootstrap = secureBootstrap.clone();
            insecureBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new NettyInboundHandler(ch));
                }
            });
        }
        return (secure) ? secureBootstrap : insecureBootstrap;
    }

    /**
     * Decrement the use count of the workerGroup and request a graceful
     * shutdown once it is no longer being used by anyone.
     */
    private static synchronized void decrementUseCount() {
        --useCount;
        if (useCount <= 0) {
            /*
             * NB: workerGroup is shared between both secure and insecure, so
             * we only need to call shutdown via the group on one of them
             */
            if (secureBootstrap != null) {
                secureBootstrap.group().shutdownGracefully(0, 500, TimeUnit.MILLISECONDS);
            }
            secureBootstrap = null;
            insecureBootstrap = null;
        }
    }
}
