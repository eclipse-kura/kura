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

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.registry.RegistryData;
import com.oracle.dio.registry.DeviceRegistryFactory;
import com.oracle.dio.utils.Constants;
import java.io.IOException;

public final class UARTRegistryFactory extends DeviceRegistryFactory {

    private static final String BAUD_RATE = "baudRate";
    private static final String DATA_BITS = "dataBits";
    private static final String FLOW_CONTROL = "flowControl";
    private static final String PARITY = "parity";
    private static final String STOP_BITS = "stopBits";
    private static final String TYPE_VALUE = "uart.UART";

    public UARTRegistryFactory() {}

    public DeviceDescriptor<UART> createDeviceDescriptor(int id, RegistryData data) throws IOException {
        String name = data.getCharacterProperty(Constants.NAME);
        String[] properties = super.getProperties(data);
        return new PeripheralDescriptorImpl(id, name, createConfig(data), UART.class, properties);
    }

    public RegistryData createRegistryData(DeviceDescriptor descriptor) {
        UARTConfig cfg = (UARTConfig)descriptor.getConfiguration();
        String name = descriptor.getName();

        RegistryData data = createData(cfg);
        data.putCharacterProperty(DEVICE_TYPE, TYPE_VALUE);

        if (name != null) {
            data.putCharacterProperty(Constants.NAME, name);
        }

        super.putProperties(data, descriptor.getProperties());
        return data;
    }

    static UARTConfig createConfig(RegistryData data) {
        String deviceName = data.getCharacterProperty(Constants.PATH);
        int baudRate = data.getIntegerProperty(BAUD_RATE, DeviceConfig.DEFAULT);
        int dataBits = data.getIntegerProperty(DATA_BITS, DeviceConfig.DEFAULT);
        int flowControl = data.getIntegerProperty(FLOW_CONTROL, DeviceConfig.DEFAULT);
        int parity = data.getIntegerProperty(PARITY, DeviceConfig.DEFAULT);
        int stopBits = data.getIntegerProperty(STOP_BITS, DeviceConfig.DEFAULT);
        int channelNumber = data.getIntegerProperty(Constants.CHANNEL_NUMBER, DeviceConfig.DEFAULT);

        UARTConfig cfg;
        if (null == deviceName) {
            int deviceNumber = data.getIntegerProperty(Constants.DEVICE_NUMBER, DeviceConfig.DEFAULT);
            cfg = new UARTConfig(deviceNumber, channelNumber, baudRate, dataBits, parity, stopBits, flowControl);
        } else {
            cfg = new UARTConfig(deviceName, channelNumber, baudRate, dataBits, parity, stopBits, flowControl);
        }

        return cfg;
    }

    static RegistryData createData(UARTConfig cfg) {
        RegistryData data = new RegistryData();

        String deviceName = cfg.getControllerName();
        if (deviceName != null) {
            data.putCharacterProperty(Constants.PATH, deviceName);
        }
        int deviceNumber = cfg.getControllerNumber();
        if (deviceNumber != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(Constants.DEVICE_NUMBER, deviceNumber);
        }
        int baudRate = cfg.getBaudRate();
        if (baudRate != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(BAUD_RATE, baudRate);
        }
        int dataBits = cfg.getDataBits();
        if (dataBits != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(DATA_BITS, dataBits);
        }
        int flowControl = cfg.getFlowControlMode();
        if (flowControl != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(FLOW_CONTROL, flowControl);
        }
        int parity = cfg.getParity();
        if (parity != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(PARITY, parity);
        }
        int stopBits = cfg.getStopBits();
        if (stopBits != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(STOP_BITS, stopBits);
        }
        int channelNumber = cfg.getChannelNumber();
        if (channelNumber != DeviceConfig.DEFAULT) {
            data.putIntegerProperty(Constants.CHANNEL_NUMBER, channelNumber);
        }

        return data;
    }
}
