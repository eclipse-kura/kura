/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.raspberrypi.sensehat.sensors;

import java.io.IOException;
import java.nio.ByteBuffer;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class KuraI2CDevice {

    private I2CDevice device;

    public KuraI2CDevice(int controllerNumber, int address, int addressSize, int clockFrequency) throws IOException {

        I2CDeviceConfig config = new I2CDeviceConfig(controllerNumber, address, addressSize, clockFrequency);
        try {
            this.device = DeviceManager.open(I2CDevice.class, config);
        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

    public int read() throws IOException {
        return this.device.read();
    }

    public void write(int value) throws IOException {
        this.device.write(value);
    }

    public void write(int register, int size, ByteBuffer value) throws IOException {
        this.device.write(register, size, value);
    }

    public void beginTransaction() throws IOException {
        this.device.begin();
    }

    public void endTransaction() throws IOException {
        this.device.end();
    }

    public void close() throws IOException {
        this.device.close();
    }

    public boolean isOpen() {
        return this.device.isOpen();
    }

}
