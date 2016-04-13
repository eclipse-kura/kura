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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.message.KuraPayload;

import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.*;

public class KuraCloudProducer extends DefaultProducer {

    // Visible for testing
    CloudClient cloudClient;

    public KuraCloudProducer(KuraCloudEndpoint endpoint, CloudClient cloudClient) {
        super(endpoint);
        this.cloudClient = cloudClient;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        String topic = firstNotNull(in.getHeader(CAMEL_KURA_CLOUD_TOPIC, String.class), getEndpoint().getTopic());
        int qos = firstNotNull(in.getHeader(CAMEL_KURA_CLOUD_QOS, Integer.class), getEndpoint().getQos());
        int priority = firstNotNull(in.getHeader(CAMEL_KURA_CLOUD_PRIORITY, Integer.class), getEndpoint().getPriority());
        boolean retain = firstNotNull(in.getHeader(CAMEL_KURA_CLOUD_RETAIN, Boolean.class), getEndpoint().isRetain());
        boolean control = firstNotNull(in.getHeader(CAMEL_KURA_CLOUD_CONTROL, Boolean.class), getEndpoint().isControl());
        String deviceId = firstNotNull(in.getHeader(CAMEL_KURA_CLOUD_DEVICEID, String.class), getEndpoint().getDeviceId());

        Object body = in.getBody();
        if(body == null) {
            throw new RuntimeException("Cannot produce null payload.");
        }

        if(!(body instanceof KuraPayload)) {
            KuraPayload payload = new KuraPayload();
            if(body instanceof byte[]) {
                payload.setBody((byte[]) body);
            } else {
                byte[] payloadBytes = in.getBody(byte[].class);
                if(payloadBytes != null) {
                    payload.setBody(in.getBody(byte[].class));
                } else {
                    payload.setBody(in.getBody(String.class).getBytes());
                }
            }
            body = payload;
        }

        if (control) {
            if (deviceId != null) {
                cloudClient.controlPublish(deviceId, topic, (KuraPayload) body, qos, retain, priority);
            } else {
                cloudClient.controlPublish(topic, (KuraPayload) body, qos, retain, priority);
            }
        } else {
                cloudClient.publish(topic, (KuraPayload) body, qos, retain, priority);
        }
    }

    // Getters

    @Override
    public KuraCloudEndpoint getEndpoint() {
        return (KuraCloudEndpoint) super.getEndpoint();
    }

    // Helpers

    private <T> T firstNotNull(T first, T second) {
        return first != null ? first : second;
    }

}
