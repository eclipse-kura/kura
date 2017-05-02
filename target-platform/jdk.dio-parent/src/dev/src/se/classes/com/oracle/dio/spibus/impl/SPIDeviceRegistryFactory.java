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

package com.oracle.dio.spibus.impl;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.spibus.SPIDevice;
import jdk.dio.spibus.SPIDeviceConfig;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.registry.RegistryData;
import com.oracle.dio.registry.DeviceRegistryFactory;
import com.oracle.dio.utils.Constants;
import java.io.IOException;

public final class SPIDeviceRegistryFactory extends DeviceRegistryFactory {

    private static final String CLOCK_MODE = "clockMode";
    private static final String WORD_LENGTH = "wordLength";
    private static final String BIT_ORDERING = "bitOrdering";
    private static final String CS_ACTIVE = "csActive";
    private static final String TYPE_VALUE = "spibus.SPIDevice";

    public SPIDeviceRegistryFactory() {}

    public DeviceDescriptor<SPIDevice> createDeviceDescriptor(int id, RegistryData data) throws IOException {
        String name = data.getCharacterProperty(Constants.NAME);
        String deviceName = data.getCharacterProperty(Constants.PATH);
        int address = data.getIntegerProperty(Constants.ADDRESS, DeviceConfig.DEFAULT);
        int clockFrequency = data.getIntegerProperty(Constants.CLOCK_FREQUENCY, DeviceConfig.DEFAULT);
        int clockMode = data.getIntegerProperty(CLOCK_MODE, DeviceConfig.DEFAULT);
        int wordLength = data.getIntegerProperty(WORD_LENGTH, DeviceConfig.DEFAULT);
        int bitOrdering = data.getIntegerProperty(BIT_ORDERING, DeviceConfig.DEFAULT);
        int csActive = data.getIntegerProperty(CS_ACTIVE, DeviceConfig.DEFAULT);

        SPIDeviceConfig cfg;
        if (null == deviceName) {
            int deviceNumber = data.getIntegerProperty(Constants.DEVICE_NUMBER, DeviceConfig.DEFAULT);
            cfg = new SPIDeviceConfig(deviceNumber, address, csActive, clockFrequency, clockMode, wordLength, bitOrdering);
        } else {
            cfg = new SPIDeviceConfig(deviceName, address, csActive, clockFrequency, clockMode, wordLength, bitOrdering);
        }

        String[] properties = super.getProperties(data);
        return new PeripheralDescriptorImpl(id, name, cfg, SPIDevice.class, properties);
    }

    public RegistryData createRegistryData(DeviceDescriptor descriptor) {
        SPIDeviceConfig cfg = (SPIDeviceConfig)descriptor.getConfiguration();
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
            data.putIntegerProperty(Constants.ADDRESS, address);
        }
        int clockFrequency = cfg.getClockFrequency();
        if (clockFrequency != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(Constants.CLOCK_FREQUENCY, clockFrequency);
        }
        int clockMode = cfg.getClockMode();
        if (clockMode != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(CLOCK_MODE, clockMode);
        }
        int wordLength = cfg.getWordLength();
        if (wordLength != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(WORD_LENGTH, wordLength);
        }
        int bitOrdering = cfg.getBitOrdering();
        if (bitOrdering != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(BIT_ORDERING, bitOrdering);
        }
        int csActive = cfg.getCSActiveLevel();
        if (csActive != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(CS_ACTIVE, csActive);
        }

        super.putProperties(data, descriptor.getProperties());
        return data;
    }
}
