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
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager", name = "Eclipse ConnectionManager", description = "The Eclipse IoT ConnectionManager allows for setting a user friendly name for the current device. It also provides the option to compress message payloads to reduce network traffic.")
public @interface ConnectionManagerOptions {

    @AttributeDefinition(name = "Device Display-Name", options = { @Option(label = "Set display name as device name", value = "device-name"), @Option(label = "Set display name from hostname", value = "hostname"), @Option(label = "Custom", value = "custom"), @Option(label = "Server defined", value = "server") }, description = "Friendly name of the device. Device name is the common name of the device (eg: Reliagate 20-25, Raspberry Pi, etc.). Hostname will use the linux hostname utility.                  Custom allows for defining a unique string. Server defined relies on the remote management server to define a name.")
    String device_display$_$name() default "device-name";

    @AttributeDefinition(name = "Device Custom-Name", required = false, description = "Custom name for the device. This value is applied ONLY if device.display-name is set to \"Custom\"")
    String device_custom$_$name() default "";

    @AttributeDefinition(name = "Encode gzip", required = false, description = "Compress message payloads before sending them to the remote server to reduce the network traffic.")
    boolean encode_gzip() default true;

    @AttributeDefinition(name = "Republish Mqtt Birth Cert On Gps Lock", description = "Whether or not to republish the MQTT Birth Certificate on GPS lock event")
    boolean republish_mqtt_birth_cert_on_gps_lock() default false;

    @AttributeDefinition(name = "Payload Encoding", options = { @Option(label = "Kura Protobuf", value = "kura-protobuf"), @Option(label = "Simple JSON", value = "simple-json") }, description = "Specify the message payload encoding.")
    String payload_encoding() default "kura-protobuf";

}


