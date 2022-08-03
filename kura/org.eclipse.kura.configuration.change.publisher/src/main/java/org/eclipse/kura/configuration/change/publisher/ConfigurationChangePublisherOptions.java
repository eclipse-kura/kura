/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.configuration.change.publisher;

import java.util.Map;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.core.cloud.CloudServiceOptions;

public class ConfigurationChangePublisherOptions {
    
    protected static final String TOPIC_PREFIX = "$EVT";

    protected static final String TOPIC_PROP_NAME = "topic";
    protected static final String QOS_PROP_NAME = "qos";
    protected static final String RETAIN_PROP_NAME = "retain";
    protected static final String PRIORITY_PROP_NAME = "priority";
    protected static final String DEFAULT_TOPIC = "$EVT/$ACCOUNT_NAME/$CLIENT_ID/CONF/V1";
    protected static final int DEFAULT_QOS = 0;
    protected static final boolean DEFAULT_RETAIN = false;
    protected static final int DEFAULT_PRIORITY = 7;

    private static final Property<String> PROPERTY_CLOUD_SERVICE_PID = new Property<>(
            CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(),
            "org.eclipse.kura.cloud.CloudService");
    private static final Property<String> PROPERTY_TOPIC = new Property<>(TOPIC_PROP_NAME, DEFAULT_TOPIC);
    private static final Property<Integer> PROPERTY_QOS = new Property<>(QOS_PROP_NAME, DEFAULT_QOS);
    private static final Property<Boolean> PROPERTY_RETAIN = new Property<>(RETAIN_PROP_NAME, DEFAULT_RETAIN);
    private static final Property<Integer> PROPERTY_PRIORITY = new Property<>(PRIORITY_PROP_NAME, DEFAULT_PRIORITY);

    private String cloudEndpointPid;
    private String topic;
    private int qos;
    private boolean retain;
    private int priority;

    public ConfigurationChangePublisherOptions(final Map<String, Object> properties) {
        this.cloudEndpointPid = PROPERTY_CLOUD_SERVICE_PID.get(properties);
        this.topic = PROPERTY_TOPIC.get(properties);
        this.qos = PROPERTY_QOS.get(properties);
        this.retain = PROPERTY_RETAIN.get(properties);
        this.priority = PROPERTY_PRIORITY.get(properties);
    }

    public String getCloudEndpointPid() {
        return this.cloudEndpointPid;
    }

    public String getTopic() {
        String noSeparatorOnExtremes = this.topic;
        if (noSeparatorOnExtremes.startsWith(CloudServiceOptions.getTopicSeparator())) {
            noSeparatorOnExtremes = noSeparatorOnExtremes.substring(1);
        }

        if (noSeparatorOnExtremes.endsWith(CloudServiceOptions.getTopicSeparator())) {
            noSeparatorOnExtremes = noSeparatorOnExtremes.substring(0, noSeparatorOnExtremes.length() - 1);
        }

        return noSeparatorOnExtremes;
    }

    public int getQos() {
        return this.qos;
    }

    public boolean isRetain() {
        return this.retain;
    }

    public int getPriority() {
        return this.priority;
    }

    public String getTopicPrefix() {
        return TOPIC_PREFIX;
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
