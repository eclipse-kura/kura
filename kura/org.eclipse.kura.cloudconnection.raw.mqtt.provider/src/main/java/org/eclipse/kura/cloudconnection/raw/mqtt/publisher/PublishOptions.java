/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.raw.mqtt.publisher;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.raw.mqtt.cloud.Qos;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.Property;

public class PublishOptions {

    public static final Property<String> TOPIC_PROP = new Property<>("topic", String.class);
    public static final Property<Qos> QOS_PROP = new Property<>("qos", 0).map(Qos.class, Qos::valueOf);
    public static final Property<Boolean> RETAIN_PROP = new Property<>("retain", false);
    public static final Property<Integer> PRIORITY_PROP = new Property<>("priority", 4);

    private final String topic;
    private final Qos qos;
    private final boolean retain;
    private final int priority;

    public PublishOptions(final Map<String, Object> properties) throws KuraException {
        this.topic = TOPIC_PROP.get(properties);
        this.qos = QOS_PROP.getOrDefault(properties);
        this.retain = RETAIN_PROP.getOrDefault(properties);
        this.priority = PRIORITY_PROP.getOrDefault(properties);
    }

    public String getTopic() {
        return this.topic;
    }

    public Qos getQos() {
        return this.qos;
    }

    public boolean getRetain() {
        return this.retain;
    }

    public int getPriority() {
        return this.priority;
    }

}
