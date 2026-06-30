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
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.publisher;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher", name = "CloudPublisher", description = "The Eclipse IoT Cloud Publisher provides a service to publish messages to a cloud platform compatible with the Eclipse IoT MQTT topic namespace.")
public @interface CloudPublisherMetatype {

    @AttributeDefinition(name = "Semantic Topic", required = false, description = "The MQTT topic suffix providing the message interpretation. Wildcards can be defined in the topic by specifing a $something in this field. The publisher will try to match \"something\" with a corresponding property in the received KuraMessage. If possible, the $something placeholder will be substituted with the value specified in the KuraMessage received from the user application.")
    String semantic_topic() default "W1/A1/$assetName";

    @AttributeDefinition(name = "Kind of Message", options = { @Option(label = "Telemetry QoS 0", value = "telemetryQos0"), @Option(label = "Telemetry QoS 1", value = "telemetryQos1"), @Option(label = "Event", value = "event"), @Option(label = "Alert", value = "alert") }, description = "Type of message to be published.")
    String message_type() default "telemetryQos0";

}


