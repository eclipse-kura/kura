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
package org.eclipse.kura.core.cloud.subscriber;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloud.subscriber.CloudSubscriber", name = "CloudSubscriber", description = "The CloudSubscriber allows to define the subscribe topic and notify the associated applications when a subscription event happens.")
public @interface CloudSubscriberMetatype {

    @AttributeDefinition(name = "Application Id", description = "The application id used to receive messages.")
    String appId() default "W1";

    @AttributeDefinition(name = "Application Topic", required = false, description = "Follows the application Id and specifies the rest of the subscription topic.")
    String app_topic() default "A1/#";

    @AttributeDefinition(name = "Qos", options = { @Option(label = "0", value = "0"), @Option(label = "1", value = "1") }, description = "The desired quality of service for the subscription messages.")
    int qos() default 0;

    @AttributeDefinition(name = "Kind of Message", options = { @Option(label = "Data", value = "data"), @Option(label = "Control", value = "control") }, description = "Type of message to be received.")
    String message_type() default "data";

}


