/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.cloudconnection.raw.mqtt.subscriber;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.raw.mqtt.subscriber.RawMqttSubscriber", name = "RawMqttSubscriber", description = "The RawMqttSubscriber allows to define the subscribtion topic and notify the associated applications when a subscription event happens.")
public @interface RawMqttSubscriberOptions {

    @AttributeDefinition(name = "Topic Filter", description = "The MQTT subscription topic filter. For example foo/bar/baz, foo/+/bar, #, foo/#")
    String topic_filter() default "";

    @AttributeDefinition(name = "Qos", options = { @Option(label = "0", value = "0"), @Option(label = "1", value = "1"), @Option(label = "2", value = "2") }, description = "The desired quality of service for the subscription messages.")
    int qos() default 0;

}


