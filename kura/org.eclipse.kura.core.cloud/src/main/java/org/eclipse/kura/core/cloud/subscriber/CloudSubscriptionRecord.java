/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.cloud.subscriber;

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
