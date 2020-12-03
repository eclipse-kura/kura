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
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message;

public enum MessageType {

    TELEMETRY_QOS_0("telemetryQos0", 0, 7, "t"),
    TELEMETRY_QOS_1("telemetryQos1", 1, 7, "t"),
    EVENT("event", 1, 5, "e"),
    ALERT("alert", 1, 2, "a"),
    CONTROL("control", 0, 2, "c");

    private String type;
    private int qos;
    private int priority;
    private String topicPrefix;

    private MessageType(String type, int qos, int priority, String topicPrefix) {
        this.type = type;
        this.qos = qos;
        this.priority = priority;
        this.topicPrefix = topicPrefix;
    }

    public String getType() {
        return this.type;
    }

    public int getQos() {
        return this.qos;
    }

    public int getPriority() {
        return this.priority;
    }

    public String getTopicPrefix() {
        return this.topicPrefix;
    }

    public static MessageType fromValue(String v) {
        for (MessageType mt : MessageType.values()) {
            if (mt.type.equals(v)) {
                return mt;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
