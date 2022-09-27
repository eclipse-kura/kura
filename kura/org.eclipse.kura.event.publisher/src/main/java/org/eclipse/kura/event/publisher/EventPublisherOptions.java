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
package org.eclipse.kura.event.publisher;

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.util.configuration.Property;

public class EventPublisherOptions {
    
    public static final String TOPIC_PREFIX_PROP_NAME = "topic.prefix";
    public static final String TOPIC_PROP_NAME = "topic";
    public static final String QOS_PROP_NAME = "qos";
    public static final String RETAIN_PROP_NAME = "retain";
    public static final String PRIORITY_PROP_NAME = "priority";

    public static final String DEFAULT_TOPIC = "EVENT_TOPIC";
    public static final int DEFAULT_QOS = 0;
    public static final boolean DEFAULT_RETAIN = false;
    public static final int DEFAULT_PRIORITY = 7;
    public static final String DEFAULT_ENDPOINT_PID = "org.eclipse.kura.cloud.CloudService";

    private static final Property<String> PROPERTY_CLOUD_SERVICE_PID = new Property<>(
            CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), DEFAULT_ENDPOINT_PID);
    private static final Property<String> PROPERTY_TOPIC_PREFIX = new Property<>(TOPIC_PREFIX_PROP_NAME, String.class);
    private static final Property<String> PROPERTY_TOPIC = new Property<>(TOPIC_PROP_NAME, DEFAULT_TOPIC);
    private static final Property<Integer> PROPERTY_QOS = new Property<>(QOS_PROP_NAME, DEFAULT_QOS);
    private static final Property<Boolean> PROPERTY_RETAIN = new Property<>(RETAIN_PROP_NAME, DEFAULT_RETAIN);
    private static final Property<Integer> PROPERTY_PRIORITY = new Property<>(PRIORITY_PROP_NAME, DEFAULT_PRIORITY);

    private String cloudEndpointPid;
    private Optional<String> topicPrefix;
    private String topic;
    private int qos;
    private boolean retain;
    private int priority;

    public EventPublisherOptions(final Map<String, Object> properties) {
        this.cloudEndpointPid = PROPERTY_CLOUD_SERVICE_PID.get(properties);
        this.topicPrefix = PROPERTY_TOPIC_PREFIX.getOptional(properties);
        this.topic = PROPERTY_TOPIC.get(properties);
        this.qos = PROPERTY_QOS.get(properties);
        this.retain = PROPERTY_RETAIN.get(properties);
        this.priority = PROPERTY_PRIORITY.get(properties);
    }

    public String getCloudEndpointPid() {
        return this.cloudEndpointPid;
    }

    public Optional<String> getTopicPrefix() {

        if (this.topicPrefix.isPresent()) {
            if (this.topicPrefix.get().length() == 0) {
                return Optional.empty();
            }
            return Optional.of(removeTopicSeparatorsFromExtremes(this.topicPrefix.get()));
        }

        return this.topicPrefix;
    }

    public String getTopic() {
        return removeTopicSeparatorsFromExtremes(this.topic);
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

    private String removeTopicSeparatorsFromExtremes(String input) {
        String result = new String(input);
        if (result.startsWith(EventPublisherConstants.TOPIC_SEPARATOR)) {
            result = result.substring(1);
        }

        if (result.endsWith(EventPublisherConstants.TOPIC_SEPARATOR)) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}
