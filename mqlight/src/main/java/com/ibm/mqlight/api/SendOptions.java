/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ibm.mqlight.api;

import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * A set of options that can be used to configure the behaviour of the <code>NonBlockingClient</code>
 * {@link NonBlockingClient#send(String, java.nio.ByteBuffer, Map, SendOptions, CompletionListener, Object)} and
 * {@link NonBlockingClient#send(String, String, Map, SendOptions, CompletionListener, Object)} methods. For example:
 * <pre>
 * SendOptions opts = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).setTtl(5000).build();
 * client.send("/tadpoles", "Hello baby frogs!", opts, listener, null);
 * </pre>
 */
public class SendOptions {

    private static final Logger logger = LoggerFactory.getLogger(SendOptions.class);

    private final QOS qos;
    private final long ttl;

    private SendOptions(QOS qos, long ttl) {
        final String methodName = "<init>";
        logger.entry(this, methodName, qos, ttl);

        this.qos = qos;
        this.ttl = ttl;

        logger.exit(this, methodName);
    }

    public final QOS getQos() {
        return qos;
    }

    public final long getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return super.toString()
                + " [qos=" + qos
                + ", ttl=" + ttl + "]";
    }

    /**
     * @return a new instance of <code>SendOptionsBuilder</code> that can be used to build
     *         (immutable) instances of <code>SendOptions</code>.
     */
    public static SendOptionsBuilder builder() {
        return new SendOptionsBuilder();
    }

    /**
     * A builder for <code>SendOptions</code> objects.
     */
    public static class SendOptionsBuilder {
        private QOS qos = QOS.AT_MOST_ONCE;
        private long ttl = 0;

        private SendOptionsBuilder() {}

        /**
         * Sets the quality of service that will be used to send messages to the MQ Light
         * server.
         * @param qos The required quality of service. Cannot be null.
         * @return the instance of <code>SendOptionsBuilder</code> that this method was
         *         called on.
         * @throws IllegalArgumentException if an invalid <code>qos</code> value is specified.
         */
        public SendOptionsBuilder setQos(QOS qos) throws IllegalArgumentException {
            final String methodName = "setQos";
            logger.entry(this, methodName, qos);

            if (qos == null) {
              final IllegalArgumentException exception = new IllegalArgumentException("qos argument cannot be null");
              logger.throwing(this,  methodName, exception);
              throw exception;
            }
            this.qos = qos;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Sets the time to live that will be used for messages sent to the MQ Light server.
         * @param ttl time to live in milliseconds. This must be a positive value, and a maximum of 4294967295 (0xFFFFFFFF)
         * @return the instance of <code>SendOptionsBuilder</code> that this method was
         *         called on.
         * @throws IllegalArgumentException if an invalid <code>ttl</code> value is specified.
         */
        public SendOptionsBuilder setTtl(long ttl) throws IllegalArgumentException {
            final String methodName = "setTtl";
            logger.entry(this, methodName, ttl);

            if (ttl < 1 || ttl > 4294967295L) {
              final IllegalArgumentException exception = new IllegalArgumentException("ttl value '" + ttl + "' is invalid, must be an unsigned non-zero integer number");
              logger.throwing(this,  methodName, exception);
              throw exception;
            }
            this.ttl = ttl;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * @return an instance of SendOptions based on the current settings of
         *         this builder.
         */
        public SendOptions build() {
            return new SendOptions(qos, ttl);
        }
    }


}
