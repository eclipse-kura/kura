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

package com.oracle.dio.gpio.impl;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.registry.RegistryData;
import com.oracle.dio.registry.DeviceRegistryFactory;
import com.oracle.dio.utils.Constants;
import java.io.IOException;

public final class GPIOPinRegistryFactory extends DeviceRegistryFactory {

    private static final String PIN_NUMBER = "pinNumber";
    private static final String MODE = "mode";
    private static final String TRIGGER = "trigger";
    private static final String TYPE_VALUE = "gpio.GPIOPin";

    public GPIOPinRegistryFactory() {}

    public DeviceDescriptor<GPIOPin> createDeviceDescriptor(int id, RegistryData data) throws IOException {
        String name = data.getCharacterProperty(Constants.NAME);
        GPIOPinConfig cfg = createConfig(data);
        String[] properties = super.getProperties(data);
        return new PeripheralDescriptorImpl(id, name, cfg, GPIOPin.class, properties);
    }

    public RegistryData createRegistryData(DeviceDescriptor descriptor) {
        GPIOPinConfig cfg = (GPIOPinConfig)descriptor.getConfiguration();
        String name = descriptor.getName();

        RegistryData data = createData(cfg);
        data.putCharacterProperty(DEVICE_TYPE, TYPE_VALUE);

        if (name != null) {
            data.putCharacterProperty(Constants.NAME, name);
        }

        super.putProperties(data, descriptor.getProperties());
        return data;
    }

    static GPIOPinConfig createConfig(RegistryData data) {
        String deviceName = data.getCharacterProperty(Constants.PATH);
        int pin = data.getIntegerProperty(PIN_NUMBER, DeviceConfig.DEFAULT);
        int dir = data.getIntegerProperty(Constants.DIRECTION, GPIOPinConfig.DIR_INPUT_ONLY);
        int mode = data.getIntegerProperty(MODE, DeviceConfig.DEFAULT);
        int trigger = data.getIntegerProperty(TRIGGER, GPIOPinConfig.TRIGGER_NONE);
        boolean initValue = data.getBooleanProperty(Constants.INIT_VALUE, false);

        GPIOPinConfig cfg;
        if (null == deviceName) {
            int deviceNumber = data.getIntegerProperty(Constants.DEVICE_NUMBER, DeviceConfig.DEFAULT);
            cfg = new GPIOPinConfig(deviceNumber, pin, dir, mode, trigger, initValue);
        } else {
            cfg = new GPIOPinConfig(deviceName, pin, dir, mode, trigger, initValue);
        }
        return cfg;
    }

    static RegistryData createData(GPIOPinConfig cfg) {
        RegistryData data = new RegistryData();
        String deviceName = cfg.getControllerName();
        if (deviceName != null) {
            data.putCharacterProperty(Constants.PATH, deviceName);
        }
        int deviceNumber = cfg.getControllerNumber();
        if (deviceNumber != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(Constants.DEVICE_NUMBER, deviceNumber);
        }
        int pin = cfg.getPinNumber();
        if (pin != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(PIN_NUMBER, pin);
        }
        int mode = cfg.getDriveMode();
        if (mode != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(MODE, mode);
        }
        data.putIntegerProperty(Constants.DIRECTION, cfg.getDirection());
        data.putIntegerProperty(TRIGGER, cfg.getTrigger());
        data.putBooleanProperty(Constants.INIT_VALUE, cfg.getInitValue());
        return data;
    }
}
