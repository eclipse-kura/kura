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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.subscriber;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.subscriber.SparkplugSubscriber", name = "SparkplugSubscriber", description = "Component that serves as a CloudSubscriber for this Sparkplug Cloud Connection.")
public @interface SparkplugSubscriberOptions {

    @AttributeDefinition(name = "Topic Filter", description = "The MQTT subscription topic filter. For example foo/bar/baz, foo/+/bar, #, foo/#.")
    String topic_filter() default "A/B/C";

    @AttributeDefinition(name = "QoS", options = { @Option(label = "QoS 0 - at most once", value = "0"), @Option(label = "QoS 1 - at least once", value = "1"), @Option(label = "QoS 2 - exactly once", value = "2") }, description = "The maximum desired quality of service for the subscription messages.")
    int qos() default 0;

}


