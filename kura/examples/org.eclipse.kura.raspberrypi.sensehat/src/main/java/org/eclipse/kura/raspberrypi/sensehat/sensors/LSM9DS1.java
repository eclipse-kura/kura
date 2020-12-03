/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSM9DS1 {

    private static final Logger s_logger = LoggerFactory.getLogger(LSM9DS1.class);

    // Accelerometer and gyroscope register address map
    public static final int ACT_THS = 0x04;
    public static final int ACT_DUR = 0x05;
    public static final int INT_GEN_CFG_XL = 0x06;
    public static final int INT_GEN_THS_X_XL = 0x07;
    public static final int INT_GEN_THS_Y_XL = 0x08;
    public static final int INT_GEN_THS_Z_XL = 0x09;
    public static final int INT_GEN_DUR_XL = 0x0A;
    public static final int REFERENCE_G = 0x0B;
    public static final int INT1_CTRL = 0x0C;
    public static final int INT2_CTRL = 0x0D;
    public static final int WHO_AM_I_XG = 0x0F;
    public static final int CTRL_REG1_G = 0x10;
    public static final int CTRL_REG2_G = 0x11;
    public static final int CTRL_REG3_G = 0x12;
    public static final int ORIENT_CFG_G = 0x13;
    public static final int INT_GEN_SRC_G = 0x14;
    public static final int OUT_TEMP_L = 0x15;
    public static final int OUT_TEMP_H = 0x16;
    public static final int STATUS_REG_0 = 0x17;
    public static final int OUT_X_L_G = 0x18;
    public static final int OUT_X_H_G = 0x19;
    public static final int OUT_Y_L_G = 0x1A;
    public static final int OUT_Y_H_G = 0x1B;
    public static final int OUT_Z_L_G = 0x1C;
    public static final int OUT_Z_H_G = 0x1D;
    public static final int CTRL_REG4 = 0x1E;
    public static final int CTRL_REG5_XL = 0x1F;
    public static final int CTRL_REG6_XL = 0x20;
    public static final int CTRL_REG7_XL = 0x21;
    public static final int CTRL_REG8 = 0x22;
    public static final int CTRL_REG9 = 0x23;
    public static final int CTRL_REG10 = 0x24;
    public static final int INT_GEN_SRC_XL = 0x26;
    public static final int STATUS_REG_1 = 0x27;
    public static final int OUT_X_L_XL = 0x28;
    public static final int OUT_X_H_XL = 0x29;
    public static final int OUT_Y_L_XL = 0x2A;
    public static final int OUT_Y_H_XL = 0x2B;
    public static final int OUT_Z_L_XL = 0x2C;
    public static final int OUT_Z_H_XL = 0x2D;
    public static final int FIFO_CTRL = 0x2E;
    public static final int FIFO_SRC = 0x2F;
    public static final int INT_GEN_CFG_G = 0x30;
    public static final int INT_GEN_THS_XH_G = 0x31;
    public static final int INT_GEN_THS_XL_G = 0x32;
    public static final int INT_GEN_THS_YH_G = 0x33;
    public static final int INT_GEN_THS_YL_G = 0x34;
    public static final int INT_GEN_THS_ZH_G = 0x35;
    public static final int INT_GEN_THS_ZL_G = 0x36;
    public static final int INT_GEN_DUR_G = 0x37;

    // Magnetic sensor register address map
    public static final int OFFSET_X_REG_L_M = 0x05;
    public static final int OFFSET_X_REG_H_M = 0x06;
    public static final int OFFSET_Y_REG_L_M = 0x07;
    public static final int OFFSET_Y_REG_H_M = 0x08;
    public static final int OFFSET_Z_REG_L_M = 0x09;
    public static final int OFFSET_Z_REG_H_M = 0x0A;
    public static final int WHO_AM_I_M = 0x0F;
    public static final int CTRL_REG1_M = 0x20;
    public static final int CTRL_REG2_M = 0x21;
    public static final int CTRL_REG3_M = 0x22;
    public static final int CTRL_REG4_M = 0x23;
    public static final int CTRL_REG5_M = 0x24;
    public static final int STATUS_REG_M = 0x27;
    public static final int OUT_X_L_M = 0x28;
    public static final int OUT_X_H_M = 0x29;
    public static final int OUT_Y_L_M = 0x2A;
    public static final int OUT_Y_H_M = 0x2B;
    public static final int OUT_Z_L_M = 0x2C;
    public static final int OUT_Z_H_M = 0x2D;
    public static final int INT_CFG_M = 0x30;
    public static final int INT_SRC_M = 0x31;
    public static final int INT_THS_L_M = 0x32;
    public static final int INT_THS_H_M = 0x33;

    public static final int WHO_AM_I_AG_ID = 0x68;
    public static final int WHO_AM_I_M_ID = 0x3D;

    public static final int ACC_DEVICE = 0;
    public static final int MAG_DEVICE = 1;

    public static final float ACC_SCALE_2G = 0.000061F;
    public static final float ACC_SCALE_4G = 0.000122F;
    public static final float ACC_SCALE_8G = 0.000244F;
    public static final float ACC_SCALE_16G = 0.000732F;

    public static final float ACCEL_CAL_MIN_X = -0.988512F;
    public static final float ACCEL_CAL_MIN_Y = -1.011500F;
    public static final float ACCEL_CAL_MIN_Z = -1.012328F;
    public static final float ACCEL_CAL_MAX_X = 1.006410F;
    public static final float ACCEL_CAL_MAX_Y = 1.004973F;
    public static final float ACCEL_CAL_MAX_Z = 1.001244F;

    public static final float GYRO_SCALE_250 = (float) (Math.PI / 180.0) * 0.00875F;
    public static final float GYRO_SCALE_500 = (float) (Math.PI / 180.0) * 0.0175F;
    public static final float GYRO_SCALE_2000 = (float) (Math.PI / 180.0) * 0.07F;

    public static final float GYRO_BIAS_X_INIT = 0.024642F;
    public static final float GYRO_BIAS_Y_INIT = 0.020255F;
    public static final float GYRO_BIAS_Z_INIT = -0.011905F;

    public static final float GYRO_LEARNING_ALPHA = 2.0F;
    public static final float GYRO_CONTINIOUS_ALPHA = 0.01F;

    public static final float ACC_ZERO = 0.05F;
    public static final float GYRO_ZERO = 0.2F;

    public static final float MAG_SCALE_4 = 0.014F;
    public static final float MAG_SCALE_8 = 0.029F;
    public static final float MAG_SCALE_12 = 0.043F;
    public static final float MAG_SCALE_16 = 0.058F;

    public static final float COMPASS_ALPHA = 0.2F;
    public static final float COMPASS_MIN_X = -26.074535F;
    public static final float COMPASS_MIN_Y = -2.034567F;
    public static final float COMPASS_MIN_Z = -14.253133F;
    public static final float COMPASS_MAX_X = 49.599648F;
    public static final float COMPASS_MAX_Y = 70.567223F;
    public static final float COMPASS_MAX_Z = 55.166424F;
    public static final float COMPASS_ELLIPSOID_OFFSET_X = 0.268940F;
    public static final float COMPASS_ELLIPSOID_OFFSET_Y = 0.530345F;
    public static final float COMPASS_ELLIPSOID_OFFSET_Z = -0.120908F;
    public static final float COMPASS_ELLIPSOID_CORR_11 = 0.973294F;
    public static final float COMPASS_ELLIPSOID_CORR_12 = -0.014069F;
    public static final float COMPASS_ELLIPSOID_CORR_13 = -0.021423F;
    public static final float COMPASS_ELLIPSOID_CORR_21 = -0.014069F;
    public static final float COMPASS_ELLIPSOID_CORR_22 = 0.965692F;
    public static final float COMPASS_ELLIPSOID_CORR_23 = -0.002746F;
    public static final float COMPASS_ELLIPSOID_CORR_31 = -0.021423F;
    public static final float COMPASS_ELLIPSOID_CORR_32 = -0.002746F;
    public static final float COMPASS_ELLIPSOID_CORR_33 = 0.980103F;

    private static LSM9DS1 imuSensor = null;
    private static KuraI2CDevice accI2CDevice = null;
    private static KuraI2CDevice magI2CDevice = null;

    private float[] previousAcceleration = { 0F, 0F, 0F };
    private float gyroBiasX;
    private float gyroBiasY;
    private float gyroBiasZ;
    private int gyroSampleCount = 0;
    private static int gyroSampleRate = 0;

    private float compassScaleX;
    private float compassScaleY;
    private float compassScaleZ;
    private float compassOffsetX;
    private float compassOffsetY;
    private float compassOffsetZ;

    private final float[] CompassAverage = { 0.0F, 0.0F, 0.0F };

    private LSM9DS1() {

        setCalibrationData();

    }

    public static synchronized LSM9DS1 getIMUSensor(int bus, int accAddress, int magAddress, int addressSize,
            int frequency) {
        if (imuSensor == null && accI2CDevice == null && magI2CDevice == null) {
            imuSensor = new LSM9DS1();
            try {
                accI2CDevice = new KuraI2CDevice(bus, accAddress, addressSize, frequency);
                magI2CDevice = new KuraI2CDevice(bus, magAddress, addressSize, frequency);
            } catch (IOException e) {
                s_logger.error("Could not create I2C Device", e);
            }
        }
        return imuSensor;
    }

    public boolean initDevice(boolean enableAccelerometer, boolean enableGyroscope, boolean enableMagnetometer) {

        // Check if the device is reachable
        boolean result = false;
        try {
            if ((read(ACC_DEVICE, WHO_AM_I_XG) & 0x000000FF) == WHO_AM_I_AG_ID
                    && (read(MAG_DEVICE, WHO_AM_I_M) & 0x000000FF) == WHO_AM_I_M_ID) {
                result = true;

                if (enableAccelerometer) {
                    enableAccelerometer();
                } else {
                    disableAccelerometer();
                }

                if (enableGyroscope) {
                    enableGyroscope();
                } else {
                    disableGyroscope();
                }

                if (enableMagnetometer) {
                    enableMagnetometer();
                } else {
                    disableMagnetometer();
                }

            }
        } catch (KuraException e) {
            s_logger.info(e.toString());
        }

        return result;
    }

    public static void closeDevice() {
        try {
            if (accI2CDevice != null && magI2CDevice != null) {
                // Power off the device : PD = 0 (power-down mode)
                disableAccelerometer();
                disableGyroscope();
                disableMagnetometer();
                accI2CDevice.close();
                accI2CDevice = null;
                magI2CDevice.close();
                magI2CDevice = null;
            }
            if (imuSensor != null) {
                imuSensor = null;
            }
        } catch (Exception e) {
            s_logger.error("Error in closing device", e);
        }
    }

    public static int read(int device, int register) throws KuraException {
        int result = 0;
        try {
            if (device == 0) {
                accI2CDevice.write(register);
                Thread.sleep(5);
                result = accI2CDevice.read();
            } else if (device == 1) {
                magI2CDevice.write(register);
                Thread.sleep(5);
                result = magI2CDevice.read();
            } else {
                throw KuraException.internalError("Device not supported.");
            }
        } catch (IOException e) {
            s_logger.error("Unable to read to I2C device", e);
        } catch (InterruptedException e1) {
            s_logger.error(e1.toString());
        }

        return result;
    }

    public static void write(int device, int register, byte[] value) throws KuraException {
        try {
            if (device == ACC_DEVICE) {
                accI2CDevice.write(register, 1, ByteBuffer.wrap(value));
            } else if (device == MAG_DEVICE) {
                magI2CDevice.write(register, 1, ByteBuffer.wrap(value));
            } else {
                throw KuraException.internalError("Device not supported.");
            }
        } catch (IOException e) {
            s_logger.error("Unable to write to I2C device", e);
        }
    }

    public void getOrientationRadiants() {
        // Returns the current orientation in radians using the aircraft principal axes of pitch, roll and yaw
        s_logger.info("Method not yet implemented");
    }

    public void getOrientationDegrees() {
        // Returns the current orientation in degrees using the aircraft principal axes of pitch, roll and yaw
        s_logger.info("Method not yet implemented");
    }

    public void getCompass() {
        // Gets the direction of North from the magnetometer in degrees
        s_logger.info("Method not yet implemented");
    }

    public float[] getCompassRaw() {

        // Magnetometer x y z raw data in uT (micro teslas)
        float[] mag = new float[3];

        int magFS = 0;

        try {
            magFS = read(MAG_DEVICE, CTRL_REG2_M) & 0x00000060;
            if (magFS == 0x00000000) { // +/-4 Gauss
                mag[0] = (read(MAG_DEVICE, OUT_X_H_M) << 8 | read(MAG_DEVICE, OUT_X_L_M) & 0x000000FF) * MAG_SCALE_4;
                mag[1] = (read(MAG_DEVICE, OUT_Y_H_M) << 8 | read(MAG_DEVICE, OUT_Y_L_M) & 0x000000FF) * MAG_SCALE_4;
                mag[2] = (read(MAG_DEVICE, OUT_Z_H_M) << 8 | read(MAG_DEVICE, OUT_Z_L_M) & 0x000000FF) * MAG_SCALE_4;
            } else if (magFS == 0x00000020) { // +/-8 Gauss
                mag[0] = (read(MAG_DEVICE, OUT_X_H_M) << 8 | read(MAG_DEVICE, OUT_X_L_M) & 0x000000FF) * MAG_SCALE_8;
                mag[1] = (read(MAG_DEVICE, OUT_Y_H_M) << 8 | read(MAG_DEVICE, OUT_Y_L_M) & 0x000000FF) * MAG_SCALE_8;
                mag[2] = (read(MAG_DEVICE, OUT_Z_H_M) << 8 | read(MAG_DEVICE, OUT_Z_L_M) & 0x000000FF) * MAG_SCALE_8;
            } else if (magFS == 0x00000040) { // +/-12 Gauss
                mag[0] = (read(MAG_DEVICE, OUT_X_H_M) << 8 | read(MAG_DEVICE, OUT_X_L_M) & 0x000000FF) * MAG_SCALE_12;
                mag[1] = (read(MAG_DEVICE, OUT_Y_H_M) << 8 | read(MAG_DEVICE, OUT_Y_L_M) & 0x000000FF) * MAG_SCALE_12;
                mag[2] = (read(MAG_DEVICE, OUT_Z_H_M) << 8 | read(MAG_DEVICE, OUT_Z_L_M) & 0x000000FF) * MAG_SCALE_12;
            } else if (magFS == 0x00000060) { // +/-16 Gauss
                mag[0] = (read(MAG_DEVICE, OUT_X_H_M) << 8 | read(MAG_DEVICE, OUT_X_L_M) & 0x000000FF) * MAG_SCALE_16;
                mag[1] = (read(MAG_DEVICE, OUT_Y_H_M) << 8 | read(MAG_DEVICE, OUT_Y_L_M) & 0x000000FF) * MAG_SCALE_16;
                mag[2] = (read(MAG_DEVICE, OUT_Z_H_M) << 8 | read(MAG_DEVICE, OUT_Z_L_M) & 0x000000FF) * MAG_SCALE_16;
            }

            mag[0] = -mag[0];
            mag[2] = -mag[2];

            calibrateMagnetometer(mag);

        } catch (KuraException e) {
            s_logger.error("Unable to read to I2C device.", e);
        }

        // Swap X and Y axis to match SenseHat library
        float[] Compass = new float[3];
        Compass[0] = this.CompassAverage[1];
        Compass[1] = this.CompassAverage[0];
        Compass[2] = this.CompassAverage[2];

        return Compass;
    }

    public void getGyroscope() {
        // Gets the orientation in degrees from the gyroscope only
        s_logger.info("Method not yet implemented");
    }

    public float[] getGyroscopeRaw() {

        // Gyroscope x y z raw data in radians per second
        float[] gyro = new float[3];

        int gyroFSR = 0;

        try {
            gyroFSR = read(ACC_DEVICE, CTRL_REG1_G) & 0x00000018;
            if (gyroFSR == 0x00000000) { // 250
                gyro[0] = (read(ACC_DEVICE, OUT_X_H_G) << 8 | read(ACC_DEVICE, OUT_X_L_G) & 0x000000FF)
                        * GYRO_SCALE_250;
                gyro[1] = (read(ACC_DEVICE, OUT_Y_H_G) << 8 | read(ACC_DEVICE, OUT_Y_L_G) & 0x000000FF)
                        * GYRO_SCALE_250;
                gyro[2] = (read(ACC_DEVICE, OUT_Z_H_G) << 8 | read(ACC_DEVICE, OUT_Z_L_G) & 0x000000FF)
                        * GYRO_SCALE_250;
            } else if (gyroFSR == 0x00000008) { // 500
                gyro[0] = (read(ACC_DEVICE, OUT_X_H_G) << 8 | read(ACC_DEVICE, OUT_X_L_G) & 0x000000FF)
                        * GYRO_SCALE_500;
                gyro[1] = (read(ACC_DEVICE, OUT_Y_H_G) << 8 | read(ACC_DEVICE, OUT_Y_L_G) & 0x000000FF)
                        * GYRO_SCALE_500;
                gyro[2] = (read(ACC_DEVICE, OUT_Z_H_G) << 8 | read(ACC_DEVICE, OUT_Z_L_G) & 0x000000FF)
                        * GYRO_SCALE_500;
            } else if (gyroFSR == 0x00000018) { // 2000
                gyro[0] = (read(ACC_DEVICE, OUT_X_H_G) << 8 | read(ACC_DEVICE, OUT_X_L_G) & 0x000000FF)
                        * GYRO_SCALE_2000;
                gyro[1] = (read(ACC_DEVICE, OUT_Y_H_G) << 8 | read(ACC_DEVICE, OUT_Y_L_G) & 0x000000FF)
                        * GYRO_SCALE_2000;
                gyro[2] = (read(ACC_DEVICE, OUT_Z_H_G) << 8 | read(ACC_DEVICE, OUT_Z_L_G) & 0x000000FF)
                        * GYRO_SCALE_2000;
            }

            gyro[2] = -gyro[2];

            calibrateGyroscope(gyro);

        } catch (KuraException e) {
            s_logger.error("Unable to read to I2C device.", e);
        }

        return gyro;

    }

    public void getAccelerometer() {
        // Gets the orientation in degrees from the accelerometer only
        s_logger.info("Method not yet implemented");
    }

    public float[] getAccelerometerRaw() {

        // Accelerometer x y z raw data in Gs
        float[] acc = new float[3];

        int accFS = 0;

        try {
            accFS = read(ACC_DEVICE, CTRL_REG6_XL) & 0x00000018;
            if (accFS == 0x00000000) { // +/-2g
                acc[0] = (read(ACC_DEVICE, OUT_X_H_XL) << 8 | read(ACC_DEVICE, OUT_X_L_XL) & 0x000000FF) * ACC_SCALE_2G;
                acc[1] = (read(ACC_DEVICE, OUT_Y_H_XL) << 8 | read(ACC_DEVICE, OUT_Y_L_XL) & 0x000000FF) * ACC_SCALE_2G;
                acc[2] = (read(ACC_DEVICE, OUT_Z_H_XL) << 8 | read(ACC_DEVICE, OUT_Z_L_XL) & 0x000000FF) * ACC_SCALE_2G;
            } else if (accFS == 0x00000010) { // +/-4g
                acc[0] = (read(ACC_DEVICE, OUT_X_H_XL) << 8 | read(ACC_DEVICE, OUT_X_L_XL) & 0x000000FF) * ACC_SCALE_4G;
                acc[1] = (read(ACC_DEVICE, OUT_Y_H_XL) << 8 | read(ACC_DEVICE, OUT_Y_L_XL) & 0x000000FF) * ACC_SCALE_4G;
                acc[2] = (read(ACC_DEVICE, OUT_Z_H_XL) << 8 | read(ACC_DEVICE, OUT_Z_L_XL) & 0x000000FF) * ACC_SCALE_4G;
            } else if (accFS == 0x00000018) { // +/-8g
                acc[0] = (read(ACC_DEVICE, OUT_X_H_XL) << 8 | read(ACC_DEVICE, OUT_X_L_XL) & 0x000000FF) * ACC_SCALE_8G;
                acc[1] = (read(ACC_DEVICE, OUT_Y_H_XL) << 8 | read(ACC_DEVICE, OUT_Y_L_XL) & 0x000000FF) * ACC_SCALE_8G;
                acc[2] = (read(ACC_DEVICE, OUT_Z_H_XL) << 8 | read(ACC_DEVICE, OUT_Z_L_XL) & 0x000000FF) * ACC_SCALE_8G;
            } else if (accFS == 0x00000008) { // +/-16g
                acc[0] = (read(ACC_DEVICE, OUT_X_H_XL) << 8 | read(ACC_DEVICE, OUT_X_L_XL) & 0x000000FF)
                        * ACC_SCALE_16G;
                acc[1] = (read(ACC_DEVICE, OUT_Y_H_XL) << 8 | read(ACC_DEVICE, OUT_Y_L_XL) & 0x000000FF)
                        * ACC_SCALE_16G;
                acc[2] = (read(ACC_DEVICE, OUT_Z_H_XL) << 8 | read(ACC_DEVICE, OUT_Z_L_XL) & 0x000000FF)
                        * ACC_SCALE_16G;
            }

            acc[0] = -acc[0];
            acc[1] = -acc[1];

            calibrateAcceleration(acc);

            // Swap X and Y axis to match SenseHat library
            float accTemp = acc[1];
            acc[1] = acc[0];
            acc[0] = accTemp;

        } catch (KuraException e) {
            s_logger.error("Unable to read to I2C device.", e);
        }

        return acc;

    }

    public static void enableAccelerometer() {

        // Enable accelerometer with default settings (ODR=119Hz, BW=50Hz, FS=+/-8g)
        try {
            disableAccelerometer();
            Thread.sleep(1000);
            byte[] value = { 0x7B };
            write(ACC_DEVICE, CTRL_REG6_XL, value);
        } catch (KuraException e) {
            s_logger.error("Unable to write to I2C device.", e);
        } catch (InterruptedException e) {
            s_logger.error(e.toString());
        }
    }

    public static void disableAccelerometer() {

        int ctrl_reg = 0x00000000;
        try {
            ctrl_reg = read(ACC_DEVICE, CTRL_REG6_XL) & 0x000000FF;
            int value = ctrl_reg & 0x0000001F;
            byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
            write(ACC_DEVICE, CTRL_REG6_XL, valueBytes);
        } catch (KuraException e) {
            s_logger.error("Unable to write to I2C device.", e);
        }

    }

    public static void enableGyroscope() {

        // Enable gyroscope with default settings (ODR=119Hz, BW=31Hz, FSR=500, HPF=0.5Hz)
        try {
            disableGyroscope();
            Thread.sleep(1000);
            byte[] value = { 0x69 };
            write(ACC_DEVICE, CTRL_REG1_G, value);
            value[0] = 0x44;
            write(ACC_DEVICE, CTRL_REG3_G, value);
            gyroSampleRate = 119;
        } catch (KuraException e) {
            s_logger.error("Unable to write to I2C device.", e);
        } catch (InterruptedException e) {
            s_logger.error(e.toString());
        }

    }

    public static void disableGyroscope() {

        int ctrl_reg = 0x00000000;
        try {
            ctrl_reg = read(ACC_DEVICE, CTRL_REG1_G) & 0x000000FF;
            int value = ctrl_reg & 0x0000001F;
            byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
            write(ACC_DEVICE, CTRL_REG1_G, valueBytes);
        } catch (KuraException e) {
            s_logger.error("Can't write to the device.", e);
        }

    }

    public static void enableMagnetometer() {

        // Enable magnetometer with default settings (TEMP_COMP=0, DO=20Hz, FS=+/-400uT)
        try {
            disableMagnetometer();
            Thread.sleep(1000);
            byte[] value = { 0x14 };
            write(MAG_DEVICE, CTRL_REG1_M, value);
            value[0] = 0x00;
            write(MAG_DEVICE, CTRL_REG2_M, value);
            write(MAG_DEVICE, CTRL_REG3_M, value);
        } catch (KuraException e) {
            s_logger.error("Unable to write to I2C device.", e);
        } catch (InterruptedException e) {
            s_logger.error(e.toString());
        }

    }

    public static void disableMagnetometer() {

        try {
            byte[] value = { 0x03 };
            write(MAG_DEVICE, CTRL_REG3_M, value);
        } catch (KuraException e) {
            s_logger.error("Unable to write to I2C device.", e);
        }

    }

    private void calibrateAcceleration(float[] acc) {

        if (acc[0] >= 0.0) {
            acc[0] = acc[0] / ACCEL_CAL_MAX_X;
        } else {
            acc[0] = acc[0] / -ACCEL_CAL_MIN_X;
        }

        if (acc[1] >= 0.0) {
            acc[1] = acc[1] / ACCEL_CAL_MAX_Y;
        } else {
            acc[1] = acc[1] / -ACCEL_CAL_MIN_Y;
        }

        if (acc[2] >= 0.0) {
            acc[2] = acc[2] / ACCEL_CAL_MAX_Z;
        } else {
            acc[2] = acc[2] / -ACCEL_CAL_MIN_Z;
        }

    }

    private void calibrateGyroscope(float[] gyro) {

        float[] deltaAcceleration = { 0F, 0F, 0F };
        deltaAcceleration[0] = this.previousAcceleration[0];
        deltaAcceleration[1] = this.previousAcceleration[1];
        deltaAcceleration[2] = this.previousAcceleration[2];

        float[] currentAcceleration = getAccelerometerRaw();
        deltaAcceleration[0] -= currentAcceleration[0];
        deltaAcceleration[1] -= currentAcceleration[1];
        deltaAcceleration[2] -= currentAcceleration[2];

        this.previousAcceleration = currentAcceleration;

        float accVectorLength = (float) Math.sqrt(Math.pow(deltaAcceleration[0], 2) + Math.pow(deltaAcceleration[1], 2)
                + Math.pow(deltaAcceleration[2], 2));
        float gyroVectorLength = (float) Math.sqrt(Math.pow(gyro[0], 2) + Math.pow(gyro[1], 2) + Math.pow(gyro[2], 2));
        if (accVectorLength < ACC_ZERO && gyroVectorLength < GYRO_ZERO) {
            // Correct the initial bias with real measures
            if (this.gyroSampleCount < 5 * gyroSampleRate) {
                this.gyroBiasX = (1.0F - GYRO_LEARNING_ALPHA) * this.gyroBiasX + GYRO_LEARNING_ALPHA * gyro[0];
                this.gyroBiasY = (1.0F - GYRO_LEARNING_ALPHA) * this.gyroBiasY + GYRO_LEARNING_ALPHA * gyro[1];
                this.gyroBiasZ = (1.0F - GYRO_LEARNING_ALPHA) * this.gyroBiasZ + GYRO_LEARNING_ALPHA * gyro[2];

                this.gyroSampleCount++;
            } else {
                this.gyroBiasX = (1.0F - GYRO_CONTINIOUS_ALPHA) * this.gyroBiasX + GYRO_CONTINIOUS_ALPHA * gyro[0];
                this.gyroBiasY = (1.0F - GYRO_CONTINIOUS_ALPHA) * this.gyroBiasY + GYRO_CONTINIOUS_ALPHA * gyro[1];
                this.gyroBiasZ = (1.0F - GYRO_CONTINIOUS_ALPHA) * this.gyroBiasZ + GYRO_CONTINIOUS_ALPHA * gyro[2];
            }
        }

        gyro[0] -= this.gyroBiasX;
        gyro[1] -= this.gyroBiasY;
        gyro[2] -= this.gyroBiasZ;
    }

    public void setGyroSampleRate(int sampleRate) {
        gyroSampleRate = sampleRate;
    }

    private void calibrateMagnetometer(float[] mag) {

        mag[0] = (mag[0] - this.compassOffsetX) * this.compassScaleX;
        mag[1] = (mag[1] - this.compassOffsetY) * this.compassScaleY;
        mag[2] = (mag[2] - this.compassOffsetZ) * this.compassScaleZ;

        mag[0] -= COMPASS_ELLIPSOID_OFFSET_X;
        mag[1] -= COMPASS_ELLIPSOID_OFFSET_Y;
        mag[2] -= COMPASS_ELLIPSOID_OFFSET_Z;

        mag[0] = mag[0] * COMPASS_ELLIPSOID_CORR_11 + mag[1] * COMPASS_ELLIPSOID_CORR_12
                + mag[2] * COMPASS_ELLIPSOID_CORR_13;

        mag[1] = mag[0] * COMPASS_ELLIPSOID_CORR_21 + mag[1] * COMPASS_ELLIPSOID_CORR_22
                + mag[2] * COMPASS_ELLIPSOID_CORR_23;

        mag[2] = mag[0] * COMPASS_ELLIPSOID_CORR_31 + mag[1] * COMPASS_ELLIPSOID_CORR_32
                + mag[2] * COMPASS_ELLIPSOID_CORR_33;

        this.CompassAverage[0] = mag[0] * COMPASS_ALPHA + this.CompassAverage[0] * (1.0F - COMPASS_ALPHA);
        this.CompassAverage[1] = mag[1] * COMPASS_ALPHA + this.CompassAverage[1] * (1.0F - COMPASS_ALPHA);
        this.CompassAverage[2] = mag[2] * COMPASS_ALPHA + this.CompassAverage[2] * (1.0F - COMPASS_ALPHA);

    }

    private void setCalibrationData() {

        this.gyroBiasX = GYRO_BIAS_X_INIT;
        this.gyroBiasY = GYRO_BIAS_Y_INIT;
        this.gyroBiasZ = GYRO_BIAS_Z_INIT;

        float compassSwingX = COMPASS_MAX_X - COMPASS_MIN_X;
        float compassSwingY = COMPASS_MAX_Y - COMPASS_MIN_Y;
        float compassSwingZ = COMPASS_MAX_Z - COMPASS_MIN_Z;

        float maxCompassSwing = Math.max(compassSwingX, Math.max(compassSwingY, compassSwingZ)) / 2.0F;

        this.compassScaleX = maxCompassSwing / (compassSwingX / 2.0F);
        this.compassScaleY = maxCompassSwing / (compassSwingY / 2.0F);
        this.compassScaleZ = maxCompassSwing / (compassSwingZ / 2.0F);

        this.compassOffsetX = (COMPASS_MAX_X + COMPASS_MIN_X) / 2.0F;
        this.compassOffsetY = (COMPASS_MAX_Y + COMPASS_MIN_Y) / 2.0F;
        this.compassOffsetZ = (COMPASS_MAX_Z + COMPASS_MIN_Z) / 2.0F;

    }

}
