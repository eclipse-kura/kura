/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import java.util.Map;

/**
 * The Class CloudSubscriberOptions is responsible to provide all the required
 * options for the Cloud Subscriber Wire Component
 */
final class CloudSubscriberOptions {

    /** The Constant denoting QoS. */
    private static final String CONF_QOS = "subscribe.qos";

    /** The Constant denoting MQTT topic. */
    private static final String CONF_TOPIC = "subscribe.topic";

    /** The Constant denoting default QoS. */
    private static final int DEFAULT_QOS = 0;

    /** The Constant denoting default MQTT topic. */
    private static final String DEFAULT_TOPIC = "EVENT";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new cloud subscriber options.
     *
     * @param properties
     *            the properties
     */
    CloudSubscriberOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the QoS to be used for message subscription.
     *
     * @return the subscribing QoS
     */
    int getSubscribingQos() {
        int subscribingQos = DEFAULT_QOS;
        final Object qos = this.properties.get(CONF_QOS);
        if ((this.properties != null) && this.properties.containsKey(CONF_QOS) && (qos != null)
                && (qos instanceof Integer)) {
            subscribingQos = (Integer) qos;
        }
        return subscribingQos;
    }

    /**
     * Returns the topic to be used for message subscription.
     *
     * @return the subscribing topic
     */
    String getSubscribingTopic() {
        String subscribingTopic = DEFAULT_TOPIC;
        final Object topic = this.properties.get(CONF_TOPIC);
        if ((this.properties != null) && this.properties.containsKey(CONF_TOPIC) && (topic != null)
                && (topic instanceof String)) {
            subscribingTopic = String.valueOf(topic);
        }
        return subscribingTopic;
    }

}