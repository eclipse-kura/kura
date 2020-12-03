/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.misc.cloudcat;

public class CloudCatSubscription {

    private final String topic;
    private final int qos;

    public CloudCatSubscription(String topic, int qos) {
        this.topic = topic;
        this.qos = qos;
    }

    public String getTopic() {
        return this.topic;
    }

    public int getQos() {
        return this.qos;
    }

    static CloudCatSubscription parseSubscription(String subscription) {
        int index = subscription.lastIndexOf(';');
        if (index == -1) {
            throw new IllegalArgumentException("QoS separator missing in: '" + subscription + "'");
        } else if (index == 0) {
            throw new IllegalArgumentException("topic token missing in: '" + subscription + "'");
        } else if (index == subscription.length() - 1) {
            throw new IllegalArgumentException("QoS token missing in: '" + subscription + "'");
        }
        String topic = subscription.substring(0, index).trim();
        int qos = Integer.parseInt(subscription.substring(index + 1).trim());
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException("Invalid QoS in '" + subscription + "'");
        }
        return new CloudCatSubscription(topic, qos);
    }
}
