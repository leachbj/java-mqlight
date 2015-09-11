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
package com.ibm.mqlight.api.timer;

import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;

/**
 * Plug point for timer implementations.  The implementation used for an
 * instance of the client can be specified using the
 * {@link NonBlockingClient#create(com.ibm.mqlight.api.endpoint.EndpointService, com.ibm.mqlight.api.callback.CallbackService, com.ibm.mqlight.api.network.NetworkService, TimerService, com.google.gson.GsonBuilder, com.ibm.mqlight.api.ClientOptions, com.ibm.mqlight.api.NonBlockingClientListener, Object)}
 * method.
 */
public interface TimerService {

    /**
     * Schedules a timer that will "pop" at some point in the future.  When the
     * timer "pops" the promise, specified as a parameter, must be completed successfully
     * by calling the {@link Promise#setSuccess(Object)} method.  Timers are "single shot"
     * as the promise can only be completed once.
     * <p>
     * The implementation cannot block the calling thread - and so must employ some scheme
     * that uses another thread to complete the promise.
     *
     * @param delay a delay in milliseconds
     * @param promise a promise object to be completed after the delay period
     */
    void schedule(long delay, Promise<Void> promise);

    /**
     * Cancels a previously scheduled promise (e.g. one that has previously been passed to
     * the {@link TimerService#schedule(long, Promise)} method.  If the cancel operation is
     * successful then the promise's {@link Promise#setFailure(Exception)} method will be
     * invoked.
     * <p>
     * Once a promise has been scheduled, using the {@link TimerService#schedule(long, Promise)}
     * it will always be completed - even if it is cancelled as a result of this method
     * being invoked.
     * <p>
     * If this method is invoked on a promise which has already completed, it should have no
     * effect.
     *
     * @param promise the promise to cancel
     */
    void cancel(Promise<Void> promise);
}
