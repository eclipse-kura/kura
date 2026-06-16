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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport", name = "SparkplugDataTransport", description = "Data Transport layer configuration.", icon = @Icon(resource = "MqttDataTransport", size = 32))
public @interface SparkplugDataTransportMetatype {

    @AttributeDefinition(name = "Sparkplug Group ID", description = "Sparkplug Group identifier to which this Sparkplug Edge Node belongs.")
    String group_id() default "group";

    @AttributeDefinition(name = "Sparkplug Edge Node ID", description = "Sparkplug Edge Node identifier to use for this Cloud Connection.")
    String node_id() default "node";

    @AttributeDefinition(name = "Sparkplug Primary Host Application ID", required = false, description = "Sparkplug Primary Host Application to associate with this Sparkplug Edge Node.")
    String primary_host_application_id() default "";

    @AttributeDefinition(name = "Server URIs", description = "List of space-separated URIs of the MQTT brokers to connect to.                           Supported types of connection are tcp: and ssl:. URIs must not end with /.                           If a primary.host.application.id has been set, the client will cycle                           over the list until a Primary Host Application becomes online.")
    String server_uris() default "tcp://broker1-url:1883";

    @AttributeDefinition(name = "Client ID", description = "Client identifier to be used when connecting to the MQTT broker.")
    String client_id() default "client";

    @AttributeDefinition(name = "Username", required = false, description = "Username to be used when connecting to the MQTT broker.")
    String username() default "";

    @AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false, description = "Password to be used when connecting to the MQTT broker.")
    String password() default "";

    @AttributeDefinition(name = "Keep Alive Interval", description = "Frequency in seconds for the periodic MQTT PING message.")
    int keep_alive() default 60;

    @AttributeDefinition(name = "Connection Timeout", description = "Timeout used for all interactions with the MQTT broker.")
    int connection_timeout() default 30;

    @AttributeDefinition(name = "SslManagerService Target Filter", description = "Specifies, as an OSGi target filter, the pid of the SslManagerService used to create SSL connections.")
    String SslManagerService_target() default "(kura.service.pid=org.eclipse.kura.ssl.SslManagerService)";

}


