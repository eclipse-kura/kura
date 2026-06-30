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
package org.eclipse.kura.core.cloud.publisher;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloud.publisher.CloudPublisher", name = "CloudPublisher", description = "The CloudPublisher allows to define publishing parameters and provide a simple endpoint where the applications can attach to publish their messages.")
public @interface CloudPublisherMetatype {

    @AttributeDefinition(name = "Application Id", description = "The application id used to publish messages.")
    String appId() default "W1";

    @AttributeDefinition(name = "Application Topic", required = false, description = "Follows the application Id and specifies the rest of the publishing topic. Wildcards can be defined in the topic by specifing a $value in the field. The publisher will try to match \"value\" with a corresponding property in the received KuraMessage. If possible, the $value placeholder will be substituted with the real value specified in the KuraMessage received from the user application.")
    String app_topic() default "A1/$assetName";

    @AttributeDefinition(name = "Qos", options = { @Option(label = "0", value = "0"), @Option(label = "1", value = "1") }, description = "The desired quality of service for the messages that have to be published. If Qos is 0, the message is delivered at most once, or it is not delivered at all. If Qos is set to 1, the message is always delivered at least once.")
    int qos() default 0;

    @AttributeDefinition(name = "Retain", description = "Default retaing flag for the published messages.")
    boolean retain() default false;

    @AttributeDefinition(name = "Kind of Message", options = { @Option(label = "Data", value = "data"), @Option(label = "Control", value = "control") }, description = "Type of message to be published.")
    String message_type() default "data";

    @AttributeDefinition(name = "Priority", min = "0", description = "Message priority. Priority level 0 (highest) should be used sparingly and reserved for messages that should be sent with the minimum latency. Default is set to 7.")
    int priority() default 7;

}


