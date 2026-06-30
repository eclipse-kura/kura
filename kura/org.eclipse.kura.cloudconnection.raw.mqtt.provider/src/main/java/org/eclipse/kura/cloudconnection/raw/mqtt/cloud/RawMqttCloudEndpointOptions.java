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
package org.eclipse.kura.cloudconnection.raw.mqtt.cloud;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint", name = "RawMqttCloudEndpoint", description = "A CloudEndpoint that allows to publish MQTT messages without restrictions or assumptions on payload format.", icon = @Icon(resource = "CloudService", size = 32))
public @interface RawMqttCloudEndpointOptions {

}


