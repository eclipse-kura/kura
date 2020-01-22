/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.raw.mqtt.subscriber;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.raw.mqtt.cloud.Qos;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.Property;
import org.eclipse.kura.core.util.MqttTopicUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribeOptions {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeOptions.class);

    public static final Property<String> TOPIC_FILTER_PROP = new Property<>("topic.filter", String.class)
            .validate(SubscribeOptions::validateTopicFilter);
    public static final Property<Qos> QOS_PROP = new Property<>("qos", 0).map(Qos.class, Qos::valueOf);

    private final String topicFilter;
    private final Qos qos;

    public SubscribeOptions(final Map<String, Object> properties) throws KuraException {
        this.topicFilter = TOPIC_FILTER_PROP.get(properties);
        this.qos = QOS_PROP.get(properties);
    }

    public String getTopicFilter() {
        return this.topicFilter;
    }

    public Qos getQos() {
        return this.qos;
    }

    private static boolean validateTopicFilter(final String filter) {
        try {
            MqttTopicUtil.validate(filter, true);
            return true;
        } catch (final Exception e) {
            logger.warn("invalid topic filter", e);
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.qos == null ? 0 : this.qos.hashCode());
        result = prime * result + (this.topicFilter == null ? 0 : this.topicFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SubscribeOptions other = (SubscribeOptions) obj;
        if (this.qos != other.qos) {
            return false;
        }
        if (this.topicFilter == null) {
            if (other.topicFilter != null) {
                return false;
            }
        } else if (!this.topicFilter.equals(other.topicFilter)) {
            return false;
        }
        return true;
    }

}
