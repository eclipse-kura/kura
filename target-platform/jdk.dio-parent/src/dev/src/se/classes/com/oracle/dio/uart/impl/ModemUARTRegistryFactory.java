/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.dio.uart.impl;

import jdk.dio.uart.UART;
import jdk.dio.uart.ModemUART;
import jdk.dio.uart.UARTConfig;
import jdk.dio.DeviceDescriptor;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.registry.RegistryData;
import com.oracle.dio.registry.DeviceRegistryFactory;
import com.oracle.dio.utils.Constants;
import java.io.IOException;

public final class ModemUARTRegistryFactory extends DeviceRegistryFactory {

    private static final String TYPE_VALUE = "uart.ModemUART";

    public ModemUARTRegistryFactory() {}

    public DeviceDescriptor<UART> createDeviceDescriptor(int id, RegistryData data) throws IOException {
        String name = data.getCharacterProperty(Constants.NAME);
        String[] properties = super.getProperties(data);
        return new PeripheralDescriptorImpl(id, name, UARTRegistryFactory.createConfig(data), ModemUART.class, properties);
    }

    public RegistryData createRegistryData(DeviceDescriptor descriptor) {
        UARTConfig cfg = (UARTConfig)descriptor.getConfiguration();
        String name = descriptor.getName();

        RegistryData data = UARTRegistryFactory.createData(cfg);
        data.putCharacterProperty(DEVICE_TYPE, TYPE_VALUE);

        if (name != null) {
            data.putCharacterProperty(Constants.NAME, name);
        }

        super.putProperties(data, descriptor.getProperties());
        return data;
    }
}
