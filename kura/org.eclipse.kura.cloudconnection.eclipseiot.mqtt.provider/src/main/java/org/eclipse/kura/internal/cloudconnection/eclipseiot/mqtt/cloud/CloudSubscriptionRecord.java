/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;

public class CloudSubscriptionRecord {

    private final String topic;
    private final int qos;
    private final CloudSubscriberListener subscriber;

    public CloudSubscriptionRecord(String topic, int qos, CloudSubscriberListener subscriber) {
        this.topic = topic;
        this.qos = qos;
        this.subscriber = subscriber;
    }

    public String getTopic() {
        return this.topic;
    }

    public int getQos() {
        return this.qos;
    }

    public CloudSubscriberListener getSubscriber() {
        return this.subscriber;
    }

}
