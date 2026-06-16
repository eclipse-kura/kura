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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.device;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.device.SparkplugDevice", name = "SparkplugDevice", description = "Sparkplug Device configuration. This Cloud Publisher sends a device birth message (DBIRTH message type)                       when the first publish occurs or when the set of published metrics is changed.                       After a DBIRTH message, this Cloud Publisher will send device data messages (DDATA message type).")
public @interface SparkplugDeviceOptions {

    @AttributeDefinition(name = "Sparkplug Device ID", description = "Sparkplug Device identifier, needs to be unique under the same Sparkplug Edge Node ID.")
    String device_id() default "device";

}


