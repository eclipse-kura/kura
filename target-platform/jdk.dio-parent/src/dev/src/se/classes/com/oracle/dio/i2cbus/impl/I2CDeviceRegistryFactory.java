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

package com.oracle.dio.i2cbus.impl;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.registry.RegistryData;
import com.oracle.dio.registry.DeviceRegistryFactory;
import com.oracle.dio.utils.Constants;
import java.io.IOException;

public final class I2CDeviceRegistryFactory extends DeviceRegistryFactory {

    private static final String ADDRESS = "address";
    private static final String ADDRESS_SIZE = "addressSize";
    private static final String CLOCK_FREQUENCY = "clockFrequency";
    private static final String TYPE_VALUE = "i2cbus.I2CDevice";

    public I2CDeviceRegistryFactory() {}

    public DeviceDescriptor<I2CDevice> createDeviceDescriptor(int id, RegistryData data) throws IOException {
        String name = data.getCharacterProperty(Constants.NAME);
        String deviceName = data.getCharacterProperty(Constants.PATH);
        int address = data.getIntegerProperty(ADDRESS, DeviceConfig.DEFAULT);
        int addressSize = data.getIntegerProperty(ADDRESS_SIZE, DeviceConfig.DEFAULT);
        int clockFrequency = data.getIntegerProperty(CLOCK_FREQUENCY, DeviceConfig.DEFAULT);

        I2CDeviceConfig cfg;
        if (null == deviceName) {
            int deviceNumber = data.getIntegerProperty(Constants.DEVICE_NUMBER, DeviceConfig.DEFAULT);
            cfg = new I2CDeviceConfig(deviceNumber, address, addressSize, clockFrequency);
        } else {
            cfg = new I2CDeviceConfig(deviceName, address, addressSize, clockFrequency);
        }

        String[] properties = super.getProperties(data);
        return new PeripheralDescriptorImpl(id, name, cfg, I2CDevice.class, properties);
    }

    public RegistryData createRegistryData(DeviceDescriptor descriptor) {
        I2CDeviceConfig cfg = (I2CDeviceConfig)descriptor.getConfiguration();
        String name = descriptor.getName();

        RegistryData data = new RegistryData();
        data.putCharacterProperty(DEVICE_TYPE, TYPE_VALUE);

        if (name != null) {
            data.putCharacterProperty(Constants.NAME, name);
        }

        String deviceName = cfg.getControllerName();
        if (deviceName != null) {
            data.putCharacterProperty(Constants.PATH, deviceName);
        }
        int deviceNumber = cfg.getControllerNumber();
        if (deviceNumber != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(Constants.DEVICE_NUMBER, deviceNumber);
        }
        int address = cfg.getAddress();
        if (address != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(ADDRESS, address);
        }
        int addressSize = cfg.getAddressSize();
        if (addressSize != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(ADDRESS_SIZE, addressSize);
        }
        int clockFrequency = cfg.getClockFrequency();
        if (clockFrequency != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(CLOCK_FREQUENCY, clockFrequency);
        }

        super.putProperties(data, descriptor.getProperties());
        return data;
    }
}
