/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint;

import org.eclipse.paho.client.mqttv3.MqttTopic;

public class SubscriptionRecord {

    private final String topicFilter;
    private final Integer qos;

    public SubscriptionRecord(String topicFilter, int qos) {
        this.topicFilter = topicFilter;
        this.qos = qos;
    }

    public String getTopicFilter() {
        return this.topicFilter;
    }

    public Integer getQos() {
        return this.qos;
    }

    public boolean matches(String topic, int qos) {
        return this.qos <= qos && MqttTopic.isMatched(this.topicFilter, topic);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof SubscriptionRecord)) {
            return false;
        }

        SubscriptionRecord otherRecord = (SubscriptionRecord) other;

        return otherRecord.getTopicFilter().equals(this.topicFilter) && otherRecord.getQos().equals(this.qos);
    }

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + this.topicFilter.hashCode();
        result = prime * result + this.qos.hashCode();
        return result;
    }

}
