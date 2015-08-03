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
package com.ibm.mqlight.api.impl.engine;

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.Message;
import com.ibm.mqlight.api.impl.SubscriptionTopic;

public class SubscribeRequest extends Message {
    public final EngineConnection connection;
    public final SubscriptionTopic topic;
    public final QOS qos;
    public final int initialCredit;
    public final long ttl;
    
    public SubscribeRequest(EngineConnection connection, SubscriptionTopic topic, QOS qos, int initialCredit, long ttl) {
        this.connection = connection;
        this.topic = topic;
        this.qos = qos;
        this.initialCredit = initialCredit;
        this.ttl = ttl;
    }
}
