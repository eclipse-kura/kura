/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.raspberrypi.sensehat.sensors;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.DummyDevice;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.HTS221DummyDevice;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.LPS25HDummyDevice;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.LSM9DS1TAccDummyyDevice;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.LSM9DS1TMagDummyyDevice;

public class KuraI2CDevice {

    private int bus;
    private int address;
    private int addressSize;
    private int frequency;

    private DummyDevice device;

    public KuraI2CDevice(int bus, int address, int addressSize, int frequency) {
        this.bus = bus;
        this.address = address;
        this.addressSize = addressSize;
        this.frequency = frequency;
        switch (address) {
        case LPS25HDummyDevice.ID:
            device = new LPS25HDummyDevice();
            break;
        case HTS221DummyDevice.ID:
            device = new HTS221DummyDevice();
            break;
        case LSM9DS1TMagDummyyDevice.ID:
            device = new LSM9DS1TMagDummyyDevice();
            break;
        case LSM9DS1TAccDummyyDevice.ID:
            device = new LSM9DS1TAccDummyyDevice();
            break;
        }
    }

    public int read() throws IOException {
        return device.read();
    }

    public void write(int value) throws IOException {
        device.write(value);
    }

    public void write(int register, int size, ByteBuffer value) throws IOException {
        device.write(register, size, value);
    }

    public void beginTransaction() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void endTransaction() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        device.close();
    }

    public boolean isOpen() {
        return device.isOpen();
    }

}
