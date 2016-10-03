/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.raspberrypi.sensehat.sensors;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LPS25H {

    private static final Logger s_logger = LoggerFactory.getLogger(LPS25H.class);

    public static final int REF_P_XL = 0x08;
    public static final int REF_P_L = 0x09;
    public static final int REF_P_H = 0x0A;
    public static final int WHO_AM_I = 0x0F;
    public static final int RES_CONF = 0x10;
    public static final int CTRL_REG1 = 0x20;
    public static final int CTRL_REG2 = 0x21;
    public static final int CTRL_REG3 = 0x22;
    public static final int CTRL_REG4 = 0x23;
    public static final int INT_CFG = 0x24;
    public static final int INT_SOURCE = 0x25;
    public static final int STATUS_REG = 0x27;
    public static final int PRESS_OUT_XL = 0x28;
    public static final int PRESS_OUT_L = 0x29;
    public static final int PRESS_OUT_H = 0x2A;
    public static final int TEMP_OUT_L = 0x2B;
    public static final int TEMP_OUT_H = 0x2C;
    public static final int FIFO_CTRL = 0x2E;
    public static final int FIFO_STATUS = 0x2F;
    public static final int THS_P_L = 0x30;
    public static final int THS_P_H = 0x31;
    public static final int RPDS_L = 0x39;
    public static final int RPDS_H = 0x3A;

    public static final int WHO_AM_I_ID = 0xBD;

    private static LPS25H pressureSensor = null;
    private static KuraI2CDevice pressureI2CDevice = null;

    private LPS25H() {
    }

    public static synchronized LPS25H getPressureSensor(int bus, int address, int addressSize, int frequency) {
        if (pressureSensor == null && pressureI2CDevice == null) {
            pressureSensor = new LPS25H();
            try {
                pressureI2CDevice = new KuraI2CDevice(bus, address, addressSize, frequency);
            } catch (IOException e) {
                s_logger.error("Could not create I2C Device", e);
            }
        }
        return pressureSensor;
    }

    public boolean initDevice() {

        // Check if the device is reachable
        boolean result = false;
        if ((read(WHO_AM_I) & 0x000000FF) == WHO_AM_I_ID) {
            result = true;

            // Set control register : PD = 1 (active mode); ODR = 011 (25 Hz pressure & temperature output data rate)
            byte[] value = { (byte) 0xC0 };
            write(CTRL_REG1, value);
        }

        return result;
    }

    public static void closeDevice() {
        try {
            if (pressureI2CDevice != null) {
                // Power off the device : PD = 0 (power-down mode)
                byte[] value = { 0x00 };
                write(CTRL_REG1, value);
                pressureI2CDevice.close();
                pressureI2CDevice = null;
            }
            if (pressureSensor != null) {
                pressureSensor = null;
            }
        } catch (Exception e) {
            s_logger.error("Error in closing device", e);
        }
    }

    public float getPressure() {
        // get the pressure in millibars (mbar)
        return readPressure() / 4096F;
    }

    public float getTemperature() {
        // get the temperature in °C
        return 42.5F + readTemperature() / 480F;
    }

    public static int read(int register) {
        int result = 0;
        try {
            pressureI2CDevice.write(register);
            Thread.sleep(5);
            result = pressureI2CDevice.read();
        } catch (IOException e) {
            s_logger.error("Unable to read to I2C device", e);
        } catch (InterruptedException e1) {
            s_logger.error(e1.toString());
        }

        return result;
    }

    public static void write(int register, byte[] value) {
        try {
            pressureI2CDevice.write(register, 1, ByteBuffer.wrap(value));
        } catch (IOException e) {
            s_logger.error("Unable to write to I2C device", e);
        }
    }

    private int readPressure() {
        int[] pressure = new int[3];
        pressure[0] = read(PRESS_OUT_XL) & 0x000000FF;
        pressure[1] = read(PRESS_OUT_L) & 0x000000FF;
        pressure[2] = read(PRESS_OUT_H) & 0x000000FF;
        return pressure[2] << 16 | pressure[1] << 8 | pressure[0];
    }

    private int readTemperature() {
        int[] temperature = new int[2];
        temperature[0] = read(TEMP_OUT_L);
        temperature[1] = read(TEMP_OUT_H);
        return temperature[1] << 8 | temperature[0] & 0x000000FF;
    }

}
