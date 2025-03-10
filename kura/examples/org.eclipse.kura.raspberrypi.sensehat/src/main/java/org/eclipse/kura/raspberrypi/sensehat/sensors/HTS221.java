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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTS221 {

    private static final Logger logger = LoggerFactory.getLogger(HTS221.class);

    public static final int WHO_AM_I = 0x0F;
    public static final int AV_CONF = 0x10;
    public static final int CTRL_REG1 = 0x20;
    public static final int CTRL_REG2 = 0x21;
    public static final int CTRL_REG3 = 0x22;
    public static final int STATUS_REG = 0x27;
    public static final int HUMIDITY_L_REG = 0x28;
    public static final int HUMIDITY_H_REG = 0x29;
    public static final int TEMP_L_REG = 0x2A;
    public static final int TEMP_H_REG = 0x2B;
    public static final int H0_RH_X2 = 0x30;
    public static final int H1_RH_X2 = 0x31;
    public static final int T0_DEGC_X8 = 0x32;
    public static final int T1_DEGC_X8 = 0x33;
    public static final int T0_T1_MSB = 0x35;
    public static final int H0_T0_OUT_L = 0x36;
    public static final int H0_T0_OUT_H = 0x37;
    public static final int H1_T0_OUT_L = 0x3A;
    public static final int H1_T0_OUT_H = 0x3B;
    public static final int T0_OUT_L = 0x3C;
    public static final int T0_OUT_H = 0x3D;
    public static final int T1_OUT_L = 0x3E;
    public static final int T1_OUT_H = 0x3F;

    public static final int WHO_AM_I_ID = 0xBC;

    private static HTS221 humiditySensor = null;
    private static KuraI2CDevice humidityI2CDevice = null;

    private static float mt;
    private static float qt;
    private static float mh;
    private static float qh;

    private HTS221() {
    }

    public static synchronized HTS221 getHumiditySensor(int bus, int address, int addressSize, int frequency) {
        if (humiditySensor == null && humidityI2CDevice == null) {
            humiditySensor = new HTS221();
            try {
                humidityI2CDevice = new KuraI2CDevice(bus, address, addressSize, frequency);
            } catch (IOException e) {
                logger.error("Could not create I2C Device", e);
            }
        }
        return humiditySensor;
    }

    public boolean initDevice() {

        // Check if the device is reachable
        boolean result = false;
        if ((read(WHO_AM_I) & 0x000000FF) == WHO_AM_I_ID) {
            result = true;

            // Set control register : PD = 1 (active mode); ODR = 11 (12.5 Hz humidity & temperature output data rate)
            byte[] value = { (byte) 0x83 };
            write(CTRL_REG1, value);

            // Read calibration
            readCalibration();
        }

        return result;
    }

    public static void closeDevice() {
        try {
            if (humidityI2CDevice != null) {
                // Power off the device : PD = 0 (power-down mode)
                byte[] value = { 0x00 };
                write(CTRL_REG1, value);
                humidityI2CDevice.close();
                humidityI2CDevice = null;
            }
            if (humiditySensor != null) {
                humiditySensor = null;
            }
        } catch (Exception e) {
            logger.error("Error in closing device", e);
        }
    }

    public float getHumidity() {

        return mh * readHumidity() + qh;

    }

    public float getTemperature() {

        return mt * readTemperature() + qt;

    }

    public boolean isHumidityReady() {
        return ((read(STATUS_REG) & 0x00000002) == 2);
    }

    public boolean isTemperatureReady() {
        return (read(STATUS_REG) & 0x00000001) == 1;

    }

    public static int read(int register) {
        int result = 0;
        try {
            humidityI2CDevice.write(register);
            Thread.sleep(5);
            result = humidityI2CDevice.read();
        } catch (IOException e) {
            logger.error("Unable to read to I2C device", e);
        } catch (InterruptedException e1) {
            logger.error(e1.toString());
            Thread.currentThread().interrupt();
        }

        return result;
    }

    public static void write(int register, byte[] value) {
        try {
            humidityI2CDevice.write(register, 1, ByteBuffer.wrap(value));
        } catch (IOException e) {
            logger.error("Unable to write to I2C device", e);
        }
    }

    private static void readCalibration() {

        int h0RhX2 = read(H0_RH_X2) & 0x000000FF;
        int h1RhX2 = read(H1_RH_X2) & 0x000000FF;
        int t0DegCX8 = (read(T0_T1_MSB) & 0x00000003) << 8 | read(T0_DEGC_X8) & 0x000000FF;
        int t1DegCX8 = (read(T0_T1_MSB) & 0x0000000C) << 6 | read(T1_DEGC_X8) & 0x000000FF;

        float h0Rh = (float) h0RhX2 / 2;
        float h1Rh = (float) h1RhX2 / 2;
        float t0DegC = (float) t0DegCX8 / 8;
        float t1DegC = (float) t1DegCX8 / 8;

        int h0T0 = read(H0_T0_OUT_H) << 8 | read(H0_T0_OUT_L) & 0x000000FF;
        int h1T0 = read(H1_T0_OUT_H) << 8 | read(H1_T0_OUT_L) & 0x000000FF;
        int t0Out = read(T0_OUT_H) << 8 | read(T0_OUT_L) & 0x000000FF;
        int t1Out = read(T1_OUT_H) << 8 | read(T1_OUT_L) & 0x000000FF;

        mt = (t1DegC - t0DegC) / (t1Out - t0Out);
        qt = -mt * t0Out + t0DegC;

        mh = (h1Rh - h0Rh) / (h1T0 - h0T0);
        qh = -mh * h0T0 + h0Rh;

        logger.debug("t0_degC : {}", t0DegC);
        logger.debug("t1_degC : {}", t1DegC);
        logger.debug("t0_out : {}", t0Out);
        logger.debug("t1_out : {}", t1Out);

        logger.debug("h0_rh : {}", h0Rh);
        logger.debug("h1_rh : {}", h1Rh);
        logger.debug("h0_t0 : {}", h0T0);
        logger.debug("h1_t0 : {}", h1T0);

        logger.debug("mt : {}", mt);
        logger.debug("qt : {}", qt);
        logger.debug("mh : {}", mh);
        logger.debug("qh : {}", qh);
    }

    private int readHumidity() {
        int hum = read(HUMIDITY_H_REG) << 8 | read(HUMIDITY_L_REG) & 0x000000FF;
        logger.debug("hum : {}", hum);
        return hum;
    }

    private int readTemperature() {
        int temp = read(TEMP_H_REG) << 8 | read(TEMP_L_REG) & 0x000000FF;
        logger.debug("temp : {}", temp);
        return temp;
    }
}
