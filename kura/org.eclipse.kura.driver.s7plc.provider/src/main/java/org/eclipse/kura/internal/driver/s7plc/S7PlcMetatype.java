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
package org.eclipse.kura.internal.driver.s7plc;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.driver.s7plc", name = "S7PlcDriver", description = "S7PLC Driver")
public @interface S7PlcMetatype {

    @AttributeDefinition(name = "host.ip", description = "S7 PLC Host IP Address")
    String host_ip() default "0";

    @AttributeDefinition(name = "rack", description = "S7 PLC Rack")
    int rack() default 0;

    @AttributeDefinition(name = "slot", description = "S7 PLC Slot")
    int slot() default 2;

    @AttributeDefinition(name = "authenticate", description = "If set to true the driver will send to the PLC the session password provided in the configuration.")
    boolean authenticate() default false;

    @AttributeDefinition(name = "password", type = AttributeType.PASSWORD, required = false, description = "The session password.")
    String password() default "";

    @AttributeDefinition(name = "read.minimum.gap.size", description = "Defines the minimum gap size for read requests in bytes, if set to a non zero value the driver will aggregate read requests for non consecutive addresses if their distance is lesser than this parameter.")
    int read_minimum_gap_size() default 0;

}


