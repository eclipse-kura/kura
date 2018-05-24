/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
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
    private static final String DEFAULT_CLOUD_PUBLISHER_PID = "";
    private static final String DEFAULT_CLOUD_SUBSCRIBER_PID = "";
    private static final int DEFAULT_PUBLISH_RATE = 1000;
    private static final float DEFAULT_TEMPERATURE_INITIAL = 10;
    private static final float DEFAULT_TEMPERATURE_INCREMENT = 0.1f;

    // Publishing Property Names
    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
    private static final String TEMP_INITIAL_PROP_NAME = "metric.temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "metric.temperature.increment";
    private static final String[] METRIC_PROP_NAMES = { "metric.string", "metric.string.oneof", "metric.long",
            "metric.integer", "metric.integer.fixed", "metric.short", "metric.double", "metric.float", "metric.char",
            "metric.byte", "metric.boolean", "metric.password" };
    private static final String CLOUD_PUBLISHER_PROP_NAME = "CloudPublisher.target";
    private static final String CLOUD_SUBSCRIBER_PROP_NAME = "CloudSubscriber.target";

    private final Map<String, Object> properties;

    ExamplePublisherOptions(final Map<String, Object> properties) {
        requireNonNull(properties);
        this.properties = properties;
    }

    String getCloudPublisherPid() {
        String cloudPublisherPid = DEFAULT_CLOUD_PUBLISHER_PID;
        Object configCloudPublisherPid = this.properties.get(CLOUD_PUBLISHER_PROP_NAME);
        if (nonNull(configCloudPublisherPid) && configCloudPublisherPid instanceof String) {
            cloudPublisherPid = (String) configCloudPublisherPid;
        }
        return cloudPublisherPid;
    }
    
    String getCloudSubscriberPid() {
        String cloudSubscriberPid = DEFAULT_CLOUD_SUBSCRIBER_PID;
        Object configCloudSubscriberPid = this.properties.get(CLOUD_SUBSCRIBER_PROP_NAME);
        if (nonNull(configCloudSubscriberPid) && configCloudSubscriberPid instanceof String) {
            cloudSubscriberPid = (String) configCloudSubscriberPid;
        }
        return cloudSubscriberPid;
    }

    int getPublishRate() {
        int publishRate = DEFAULT_PUBLISH_RATE;
        Object rate = this.properties.get(PUBLISH_RATE_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            publishRate = (int) rate;
        }
        return publishRate;
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