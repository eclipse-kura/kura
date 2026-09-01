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
package org.eclipse.kura.cloudconnection.raw.mqtt.publisher;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.raw.mqtt.publisher.RawMqttPublisher", name = "CloudPublisher", description = "The CloudPublisher allows to define publishing parameters and provide a simple endpoint where the applications can attach to publish their messages.")
public @interface RawMqttPublisherOptions {

    @AttributeDefinition(name = "Topic", description = "The MQTT topic to publish messages on.")
    String topic() default "";

    @AttributeDefinition(name = "Qos", options = { @Option(label = "0", value = "0"), @Option(label = "1", value = "1"), @Option(label = "2", value = "2") }, description = "The desired quality of service for the messages that have to be published. If Qos is 0, the message is delivered at most once, or it is not delivered at all. If Qos is set to 1, the message is always delivered at least once. If set to 2, the message will be delivered exactly once.")
    int qos() default 0;

    @AttributeDefinition(name = "Retain", description = "Retain flag for the published messages.")
    boolean retain() default false;

    @AttributeDefinition(name = "Priority", min = "0", description = "The priority of the messages. 0 is highest priority. This parameter is related to the DataService component of the cloud stack.")
    int priority() default 7;

}


