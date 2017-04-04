/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.example.publisher;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

final class ExamplePublisherOptions {

    // Cloud Application identifier
    private static final String DEFAULT_APP_ID = "EXAMPLE_PUBLISHER";
    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final int DEFAULT_PUBLISH_RATE = 1000;
    private static final int DEFAULT_PUBLISH_QOS = 0;
    private static final boolean DEFAULT_PUBLISH_RETAIN = false;
    private static final String DEFAULT_APP_TOPIC = "data/metrics";
    private static final String DEFAULT_SUBSCRIBE_TOPIC = "inbound/#";
    private static final float DEFAULT_TEMPERATURE_INITIAL = 10;
    private static final float DEFAULT_TEMPERATURE_INCREMENT = 0.1f;

    // Publishing Property Names
    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.appTopic";
    private static final String SUBSCRIBE_TOPIC_PROP_NAME = "subscribe.topic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    private static final String TEMP_INITIAL_PROP_NAME = "metric.temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "metric.temperature.increment";
    private static final String[] METRIC_PROP_NAMES = { "metric.string", "metric.string.oneof", "metric.long",
            "metric.integer", "metric.integer.fixed", "metric.short", "metric.double", "metric.float", "metric.char",
            "metric.byte", "metric.boolean", "metric.password" };
    private static final String CLOUD_SERVICE_PROP_NAME = "cloud.service.pid";
    private static final String APP_ID_PROP_NAME = "app.id";

    private final Map<String, Object> properties;

    ExamplePublisherOptions(final Map<String, Object> properties) {
        requireNonNull(properties);
        this.properties = properties;
    }

    String getCloudServicePid() {
        String cloudServicePid = DEFAULT_CLOUD_SERVICE_PID;
        Object configCloudServicePid = this.properties.get(CLOUD_SERVICE_PROP_NAME);
        if (nonNull(configCloudServicePid) && configCloudServicePid instanceof String) {
            cloudServicePid = (String) configCloudServicePid;
        }
        return cloudServicePid;
    }

    String getAppId() {
        String appId = DEFAULT_APP_ID;
        Object app = this.properties.get(APP_ID_PROP_NAME);
        if (nonNull(app) && app instanceof String) {
            appId = String.valueOf(app);
        }
        return appId;
    }

    int getPublishRate() {
        int publishRate = DEFAULT_PUBLISH_RATE;
        Object rate = this.properties.get(PUBLISH_RATE_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            publishRate = (int) rate;
        }
        return publishRate;
    }

    int getPublishQos() {
        int publishQos = DEFAULT_PUBLISH_QOS;
        Object qos = this.properties.get(PUBLISH_QOS_PROP_NAME);
        if (nonNull(qos) && qos instanceof Integer) {
            publishQos = (int) qos;
        }
        return publishQos;
    }

    boolean getPublishRetain() {
        boolean publishRetain = DEFAULT_PUBLISH_RETAIN;
        Object retain = this.properties.get(PUBLISH_RETAIN_PROP_NAME);
        if (nonNull(retain) && retain instanceof Boolean) {
            publishRetain = (boolean) retain;
        }
        return publishRetain;
    }

    String getAppTopic() {
        String appTopic = DEFAULT_APP_TOPIC;
        Object app = this.properties.get(PUBLISH_TOPIC_PROP_NAME);
        if (nonNull(app) && app instanceof String) {
            appTopic = String.valueOf(app);
        }
        return appTopic;
    }

    String getSubscribeTopic() {
        String subscribeTopic = DEFAULT_SUBSCRIBE_TOPIC;
        Object topic = this.properties.get(SUBSCRIBE_TOPIC_PROP_NAME);
        if (nonNull(topic) && topic instanceof String) {
            subscribeTopic = String.valueOf(topic);
        }
        return subscribeTopic;
    }

    float getTempInitial() {
        float tempInitial = DEFAULT_TEMPERATURE_INITIAL;
        Object temp = this.properties.get(TEMP_INITIAL_PROP_NAME);
        if (nonNull(temp) && temp instanceof Float) {
            tempInitial = (float) temp;
        }
        return tempInitial;
    }

    float getTempIncrement() {
        float tempIncrement = DEFAULT_TEMPERATURE_INCREMENT;
        Object temp = this.properties.get(TEMP_INCREMENT_PROP_NAME);
        if (nonNull(temp) && temp instanceof Float) {
            tempIncrement = (float) temp;
        }
        return tempIncrement;
    }

    String[] getMetricsPropertiesNames() {
        return METRIC_PROP_NAMES;
    }
}