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
package org.eclipse.kura.event.publisher;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.event.publisher.EventPublisher", name = "EventPublisher", description = "")
public @interface EventPublisherMetatype {

    @AttributeDefinition(name = "Topic prefix", required = false, description = "The topic prefix to use for events publishing. Can be left empty.")
    String topic_prefix() default "$EVT";

    @AttributeDefinition(name = "Topic", description = "The message topic to use for publishing events.")
    String topic() default "";

    @AttributeDefinition(name = "Qos", options = { @Option(label = "0", value = "0"), @Option(label = "1", value = "1") }, description = "The desired quality of service for the log messages that have to be published. If Qos is 0,             the log message is delivered at most once, or it is not delivered at all. If Qos is set to 1, the message             is always delivered at least once.")
    int qos() default 0;

    @AttributeDefinition(name = "Retain", description = "Default retaing flag for the published messages.")
    boolean retain() default false;

    @AttributeDefinition(name = "Priority", description = "Message priority. Priority level 0 (highest) should be used sparingly and reserved for             messages that should be sent with the minimum latency. Default is set to 7.")
    int priority() default 7;

}


