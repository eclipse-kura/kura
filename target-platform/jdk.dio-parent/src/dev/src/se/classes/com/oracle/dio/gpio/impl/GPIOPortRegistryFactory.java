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
import jdk.dio.gpio.GPIOPort;
import jdk.dio.gpio.GPIOPortConfig;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.registry.RegistryData;
import com.oracle.dio.registry.RegistryList;
import com.oracle.dio.registry.DeviceRegistryFactory;
import com.oracle.dio.utils.Constants;
import java.io.IOException;
import java.util.Enumeration;

public final class GPIOPortRegistryFactory extends DeviceRegistryFactory {

    private static final String PINS = "pins";
    private static final String TYPE_VALUE = "gpio.GPIOPort";

    public GPIOPortRegistryFactory() {}

    public DeviceDescriptor<GPIOPort> createDeviceDescriptor(int id, RegistryData data) throws IOException {
        String name = data.getCharacterProperty(Constants.NAME);
        int dir = data.getIntegerProperty(Constants.DIRECTION, DeviceConfig.DEFAULT);
        int initValue = data.getIntegerProperty(Constants.INIT_VALUE, 0);

        RegistryList pinList = data.getListProperty(PINS);
        if (pinList == null) {
            return null;
        }
        GPIOPinConfig parray[] = new GPIOPinConfig[pinList.size()];
        Enumeration<?> pins = pinList.elements();
        for (int i = 0; pins.hasMoreElements(); i++) {
            RegistryData pin = (RegistryData)pins.nextElement();
            pin = (RegistryData)pin.clone();
            pin.putIntegerProperty(Constants.DIRECTION, dir);
            parray[i] = GPIOPinRegistryFactory.createConfig(pin);
        }

        GPIOPortConfig cfg = new GPIOPortConfig(dir, initValue, parray);
        String[] properties = super.getProperties(data);
        return new PeripheralDescriptorImpl(id, name, cfg, GPIOPort.class, properties);
    }

    public RegistryData createRegistryData(DeviceDescriptor descriptor) {
        GPIOPortConfig cfg = (GPIOPortConfig)descriptor.getConfiguration();
        String name = descriptor.getName();

        RegistryData data = new RegistryData();
        data.putCharacterProperty(DEVICE_TYPE, TYPE_VALUE);

        if (name != null) {
            data.putCharacterProperty(Constants.NAME, name);
        }

        int dir = cfg.getDirection();
        if (dir != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(Constants.DIRECTION, dir);
        }

        data.putIntegerProperty(Constants.INIT_VALUE, cfg.getInitValue());

        GPIOPinConfig[] parray = cfg.getPinConfigs();
        if (parray.length > 0) {
            RegistryList pinList = new RegistryList();
            for (int i = 0; i < parray.length; i++) {
                RegistryData pinData = GPIOPinRegistryFactory.createData(parray[i]);
                pinList.add(pinData);
            }
            data.putListProperty(PINS, pinList);
        }

        super.putProperties(data, descriptor.getProperties());
        return data;
    }
}
