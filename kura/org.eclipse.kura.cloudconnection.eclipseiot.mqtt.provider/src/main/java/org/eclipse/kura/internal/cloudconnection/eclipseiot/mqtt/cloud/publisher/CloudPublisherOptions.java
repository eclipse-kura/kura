/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.publisher;

import java.util.Map;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageType;

public class CloudPublisherOptions {

    private static final Property<String> PROPERTY_CLOUD_SERVICE_PID = new Property<>(
            CloudConnectionConstants.CLOUD_CONNECTION_SERVICE_PID_PROP_NAME.value(),
            "org.eclipse.kura.cloud.mqtt.eclipseiot.CloudService");
    private static final Property<String> PROPERTY_SEMANTIC_TOPIC = new Property<>("semantic.topic",
            "W1/A1/$assetName");
    private static final Property<Integer> PROPERTY_QOS = new Property<>("qos", 0);
    private static final Property<String> PROPERTY_MESSAGE_TYPE = new Property<>("message.type", "telemetryQos0");

    private final String cloudServicePid;
    private final String semanticTopic;
    private final int qos;
    private final String messageType;

    public CloudPublisherOptions(final Map<String, Object> properties) {
        this.cloudServicePid = PROPERTY_CLOUD_SERVICE_PID.get(properties);
        this.semanticTopic = PROPERTY_SEMANTIC_TOPIC.get(properties);
        this.qos = PROPERTY_QOS.get(properties);
        this.messageType = PROPERTY_MESSAGE_TYPE.get(properties);
    }

    public String getCloudServicePid() {
        return this.cloudServicePid;
    }

    public String getSemanticTopic() {
        return this.semanticTopic;
    }

    public int getQos() {
        return this.qos;
    }

    public MessageType getMessageType() {
        return MessageType.fromValue(this.messageType);
    }

    private static final class Property<T> {

        private final String key;
        private final T defaultValue;

        public Property(final String key, final T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(final Map<String, Object> properties) {
            final Object value = properties.get(this.key);

            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return this.defaultValue;
        }
    }
}
