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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class ArrivedMessage {

    private String topic;
    private MqttMessage message;

    public ArrivedMessage(String topic, MqttMessage message) {
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return this.topic;
    }

    public MqttMessage getMessage() {
        return this.message;
    }

}
