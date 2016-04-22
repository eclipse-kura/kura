/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
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
