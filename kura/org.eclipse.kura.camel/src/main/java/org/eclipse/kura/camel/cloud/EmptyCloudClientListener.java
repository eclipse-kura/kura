/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.kura.camel.cloud;

import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmptyCloudClientListener implements CloudClientListener {

    private final static Logger LOG = LoggerFactory.getLogger(EmptyCloudClientListener.class);

    @Override
    public void onControlMessageArrived(String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain) {
        LOG.debug("Executed empty onControlMessageArrived callback with deviceId {}, topic {}, payload {}, QOS {} and retain {}. ",
                new Object[]{deviceId, topic, kuraPayload, qos, retain});
    }

    @Override
    public void onMessageArrived(String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain) {

    }

    @Override
    public void onConnectionLost() {

    }

    @Override
    public void onConnectionEstablished() {

    }

    @Override
    public void onMessageConfirmed(int i, String s) {

    }

    @Override
    public void onMessagePublished(int i, String s) {

    }

}
