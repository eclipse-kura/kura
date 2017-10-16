/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag.tinyb;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// More documentation can be found in http://processors.wiki.ti.com/index.php/SensorTag_User_Guide for the CC2541
// and in http://processors.wiki.ti.com/index.php/CC2650_SensorTag_User's_Guide for the CC2650

public class TiSensorTag {

    private static final Logger logger = LoggerFactory.getLogger(TiSensorTag.class);

    private static final String DEVINFO = "devinfo";
    private static final String TEMPERATURE = "temperature";
    private static final String HUMIDITY = "humidity";
    private static final String PRESSURE = "pressure";
    private static final String MOVEMENT = "movement";
    private static final String ACCELEROMETER = "accelerometer";
    private static final String MAGNETOMETER = "magnetometer";
    private static final String GYROSCOPE = "gyroscope";
    private static final String OPTO = "opto";
    private static final String KEYS = "keys";
    private static final String IO = "io";

    private static final String IO_ERROR_MESSAGE = "Not IO Service on CC2541.";
    private static final String OPTO_ERROR_MESSAGE = "Not optical sensor on CC2541.";
    private static final String MOV_ERROR_MESSAGE = "Movement enable failed";

    private BluetoothLeDevice device;
    private boolean cc2650;
    private boolean keysNotification;
    private byte[] pressureCalibration;
    private Map<String, BluetoothLeGattService> gattServices;

    public TiSensorTag(BluetoothLeDevice bluetoothLeDevice) {
        this.device = bluetoothLeDevice;
        this.gattServices = new HashMap<>();
        if (this.device.getName().contains("SensorTag 2.0") || this.device.getName().contains("CC2650 SensorTag")) {
            this.cc2650 = true;
        } else {
            this.cc2650 = false;
        }
        this.keysNotification = false;
    }

    public BluetoothLeDevice getBluetoothLeDevice() {
        return this.device;
    }

    public void setBluetoothLeDevice(BluetoothLeDevice device) {
        this.device = device;
    }

    public boolean isConnected() {
        return this.device.isConnected();
    }

    public boolean isKeysNotificationEnabled() {
        return this.keysNotification;
    }

    public void connect() {
        try {
            this.device.connect();
        } catch (KuraException e) {
            logger.error("Connection failed", e);
        }

        if (isConnected()) {
            getGattServices();
        }
    }

    public void disconnect() {
        try {
            this.device.disconnect();
        } catch (KuraException e) {
            logger.error("Disconnection failed", e);
        }
    }

    public boolean isCC2650() {
        return this.cc2650;
    }

    public String getFirmareRevision() {
        String firmware = "";
        try {
            BluetoothLeGattCharacteristic devinfo = this.gattServices.get(DEVINFO)
                    .findCharacteristic(TiSensorTagGatt.UUID_DEVINFO_FIRMWARE_REVISION);
            firmware = new String(devinfo.readValue(), "UTF-8");
        } catch (KuraException | UnsupportedEncodingException e) {
            logger.error("Firmware revision read failed", e);
        }
        return firmware;
    }

    /*
     * Discover services
     */
    public Map<String, BluetoothLeGattService> discoverServices() {
        return this.gattServices;
    }

    public List<BluetoothLeGattCharacteristic> getCharacteristics() {
        List<BluetoothLeGattCharacteristic> characteristics = new ArrayList<>();
        for (Entry<String, BluetoothLeGattService> entry : this.gattServices.entrySet()) {
            try {
                characteristics.addAll(entry.getValue().findCharacteristics());
            } catch (KuraException e) {
                logger.error("Failed to get characteristic", e);
            }
        }
        return characteristics;
    }

    // ----------------------------------------------------------------------------------------------------------
    //
    // Temperature Sensor
    //
    // ----------------------------------------------------------------------------------------------------------
    /*
     * Enable temperature sensor
     */
    public void enableTermometer() {
        // Write "01" to enable temperature sensor
        byte[] value = { 0x01 };
        BluetoothLeGattCharacteristic tempEnableChar;
        try {
            tempEnableChar = this.gattServices.get(TEMPERATURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE);
            tempEnableChar.writeValue(value);
        } catch (KuraException e) {
            logger.error("Termometer enable failed", e);
        }
    }

    /*
     * Disable temperature sensor
     */
    public void disableTermometer() {
        // Write "00" to disable temperature sensor
        byte[] value = { 0x00 };
        BluetoothLeGattCharacteristic tempEnableChar;
        try {
            tempEnableChar = this.gattServices.get(TEMPERATURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE);
            tempEnableChar.writeValue(value);
        } catch (KuraException e) {
            logger.error("Termometer disable failed", e);
        }
    }

    /*
     * Read temperature sensor
     */
    public double[] readTemperature() {
        double[] temperatures = new double[2];
        BluetoothLeGattCharacteristic tempValueChar;
        try {
            tempValueChar = this.gattServices.get(TEMPERATURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE);
            temperatures = calculateTemperature(tempValueChar.readValue());
        } catch (KuraException e) {
            logger.error("Temperature read failed", e);
        }
        return temperatures;
    }

    /*
     * Enable temperature notifications
     */
    public void enableTemperatureNotifications(Consumer<double[]> callback) {
        Consumer<byte[]> callbackTemp = valueBytes -> callback.accept(calculateTemperature(valueBytes));

        BluetoothLeGattCharacteristic tempValueChar;
        try {
            tempValueChar = this.gattServices.get(TEMPERATURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE);
            tempValueChar.enableValueNotifications(callbackTemp);
        } catch (KuraException e) {
            logger.error("Temperature notification enable failed", e);
        }
    }

    /*
     * Disable temperature notifications
     */
    public void disableTemperatureNotifications() {
        BluetoothLeGattCharacteristic tempValueChar;
        try {
            tempValueChar = this.gattServices.get(TEMPERATURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE);
            tempValueChar.disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Temperature notification disable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setTermometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic tempPeriodChar;
        try {
            tempPeriodChar = this.gattServices.get(TEMPERATURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_PERIOD);
            tempPeriodChar.writeValue(periodBytes);
        } catch (KuraException e) {
            logger.error("Termometer period set failed", e);
        }
    }

    /*
     * Calculate temperature
     */
    private double[] calculateTemperature(byte[] valueByte) {

        logger.info("Received temperature value: " + byteArrayToHexString(valueByte));

        double[] temperatures = new double[2];

        if (this.cc2650) {
            int ambT = shortUnsignedAtOffset(valueByte, 2);
            int objT = shortUnsignedAtOffset(valueByte, 0);
            temperatures[0] = (ambT >> 2) * 0.03125;
            temperatures[1] = (objT >> 2) * 0.03125;
        } else {

            int ambT = shortUnsignedAtOffset(valueByte, 2);
            int objT = shortSignedAtOffset(valueByte, 0);
            temperatures[0] = ambT / 128.0;

            double vobj2 = objT;
            vobj2 *= 0.00000015625;

            double tdie = ambT / 128.0 + 273.15;

            double s0 = 5.593E-14; // Calibration factor
            double a1 = 1.75E-3;
            double a2 = -1.678E-5;
            double b0 = -2.94E-5;
            double b1 = -5.7E-7;
            double b2 = 4.63E-9;
            double c2 = 13.4;
            double tref = 298.15;
            double s = s0 * (1 + a1 * (tdie - tref) + a2 * Math.pow(tdie - tref, 2));
            double vos = b0 + b1 * (tdie - tref) + b2 * Math.pow(tdie - tref, 2);
            double fObj = vobj2 - vos + c2 * Math.pow(vobj2 - vos, 2);
            double tObj = Math.pow(Math.pow(tdie, 4) + fObj / s, .25);

            temperatures[1] = tObj - 273.15;
        }

        return temperatures;
    }

    // ------------------------------------------------------------------------------------------------------------
    //
    // Accelerometer Sensor
    //
    // ------------------------------------------------------------------------------------------------------------
    /*
     * Enable accelerometer sensor
     */
    public void enableAccelerometer(byte[] config) {
        BluetoothLeGattCharacteristic enableChar;
        if (this.cc2650) {
            // 0: gyro X, 1: gyro Y, 2: gyro Z
            // 3: acc X, 4: acc Y, 5: acc Z
            // 6: mag
            // 7: wake-on-motion
            // 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
            try {
                enableChar = this.gattServices.get(MOVEMENT).findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE);
                writeOnCharacteristic(config, enableChar);
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            // Write "01" in order to enable the sensor in 2g range
            // Write "01" in order to select 2g range, "02" for 4g, "03" for 8g (only for firmware > 1.5)
            try {
                enableChar = this.gattServices.get(ACCELEROMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_ENABLE);
                enableChar.writeValue(config);
            } catch (KuraException e) {
                logger.error("Accelerometer enable failed", e);
            }
        }
    }

    /*
     * Disable accelerometer sensor
     */
    public void disableAccelerometer() {
        BluetoothLeGattCharacteristic enableChar;
        if (this.cc2650) {
            byte[] config = { 0x00, 0x00 };
            try {
                enableChar = this.gattServices.get(MOVEMENT).findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE);
                writeOnCharacteristic(config, enableChar);
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            byte[] config = { 0x00 };
            try {
                enableChar = this.gattServices.get(ACCELEROMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_ENABLE);
                enableChar.writeValue(config);
            } catch (KuraException e) {
                logger.error("Accelerometer enable failed", e);
            }
        }
    }

    /*
     * Read accelerometer sensor
     */
    public double[] readAcceleration() {
        double[] acceleration = new double[3];
        BluetoothLeGattCharacteristic accValueChar;
        try {
            if (this.cc2650) {
                accValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                acceleration = calculateAcceleration(accValueChar.readValue());
            } else {
                accValueChar = this.gattServices.get(ACCELEROMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_VALUE);
                acceleration = calculateAcceleration(accValueChar.readValue());
            }
        } catch (KuraException e) {
            logger.error("Acceleration read failed", e);
        }
        return acceleration;
    }

    /*
     * Enable accelerometer notifications
     */
    public void enableAccelerationNotifications(Consumer<double[]> callback) {
        Consumer<byte[]> callbackAcc = valueBytes -> callback.accept(calculateAcceleration(valueBytes));
        BluetoothLeGattCharacteristic accValueChar;
        try {
            if (this.cc2650) {
                accValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                accValueChar.enableValueNotifications(callbackAcc);
            } else {
                accValueChar = this.gattServices.get(ACCELEROMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_VALUE);
                accValueChar.enableValueNotifications(callbackAcc);
            }
        } catch (KuraException e) {
            logger.error("Accelaration notification enable failed", e);
        }
    }

    /*
     * Disable accelerometer notifications
     */
    public void disableAccelerationNotifications() {
        BluetoothLeGattCharacteristic accValueChar;
        try {
            if (this.cc2650) {
                accValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                accValueChar.disableValueNotifications();
            } else {
                accValueChar = this.gattServices.get(ACCELEROMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_VALUE);
                accValueChar.disableValueNotifications();
            }
        } catch (KuraException e) {
            logger.error("Accelaration notification disable failed", e);
        }
    }

    /*
     * Set sampling period
     */
    public void setAccelerometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic tempPeriodChar;
        try {
            if (this.isCC2650()) {
                tempPeriodChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD);
                tempPeriodChar.writeValue(periodBytes);
            } else {
                tempPeriodChar = this.gattServices.get(ACCELEROMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_PERIOD);
                tempPeriodChar.writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Acceleration period set failed", e);
        }
    }

    /*
     * Calculate acceleration
     */
    private double[] calculateAcceleration(byte[] valueByte) {

        logger.info("Received accelerometer value: " + byteArrayToHexString(valueByte));

        double[] acceleration = new double[3];
        if (this.cc2650) {
            final float scale = (float) 4096.0;

            int x = shortSignedAtOffset(valueByte, 6);
            int y = shortSignedAtOffset(valueByte, 8);
            int z = shortSignedAtOffset(valueByte, 10);

            acceleration[0] = x / scale * -1;
            acceleration[1] = y / scale;
            acceleration[2] = z / scale * -1;
        } else {
            int x = unsignedToSigned((int) valueByte[0], 8);
            int y = unsignedToSigned((int) valueByte[1], 8);
            int z = unsignedToSigned((int) valueByte[2], 8) * -1;

            acceleration[0] = x / 64.0;
            acceleration[1] = y / 64.0;
            acceleration[2] = z / 64.0;
        }

        return acceleration;
    }

    //
    // -------------------------------------------------------------------------------------------------------
    //
    // Humidity Sensor
    //
    // -------------------------------------------------------------------------------------------------------
    /*
     * Enable humidity sensor
     */
    public void enableHygrometer() {
        // Write "01" to enable humidity sensor
        byte[] value = { 0x01 };
        BluetoothLeGattCharacteristic humEnableChar;
        try {
            humEnableChar = this.gattServices.get(HUMIDITY).findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_ENABLE);
            humEnableChar.writeValue(value);
        } catch (KuraException e) {
            logger.error("Hygrometer enable failed", e);
        }
    }

    /*
     * Disable humidity sensor
     */
    public void disableHygrometer() {
        // Write "00" to disable humidity sensor
        byte[] value = { 0x00 };
        BluetoothLeGattCharacteristic humEnableChar;
        try {
            humEnableChar = this.gattServices.get(HUMIDITY).findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_ENABLE);
            humEnableChar.writeValue(value);
        } catch (KuraException e) {
            logger.error("Hygrometer disable failed", e);
        }
    }

    /*
     * Read humidity sensor
     */
    public float readHumidity() {
        float humidity = 0F;
        BluetoothLeGattCharacteristic humValueChar;
        try {
            humValueChar = this.gattServices.get(HUMIDITY).findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_VALUE);
            humidity = calculateHumidity(humValueChar.readValue());
        } catch (KuraException e) {
            logger.error("Humidity read failed", e);
        }
        return humidity;
    }

    /*
     * Enable humidity notifications
     */
    public void enableHumidityNotifications(Consumer<Float> callback) {
        Consumer<byte[]> callbackHum = valueBytes -> callback.accept(calculateHumidity(valueBytes));
        BluetoothLeGattCharacteristic humValueChar;
        try {
            humValueChar = this.gattServices.get(HUMIDITY).findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_VALUE);
            humValueChar.enableValueNotifications(callbackHum);
        } catch (KuraException e) {
            logger.error("Humidity notification enable failed", e);
        }
    }

    /*
     * Disable humidity notifications
     */
    public void disableHumidityNotifications() {
        BluetoothLeGattCharacteristic humValueChar;
        try {
            humValueChar = this.gattServices.get(HUMIDITY).findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_VALUE);
            humValueChar.disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Humidity notification enable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setHygrometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic humPeriodChar;
        try {
            humPeriodChar = this.gattServices.get(HUMIDITY).findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_PERIOD);
            humPeriodChar.writeValue(periodBytes);
        } catch (KuraException e) {
            logger.error("Hygrometer period set failed", e);
        }
    }

    /*
     * Calculate Humidity
     */
    private float calculateHumidity(byte[] valueByte) {

        logger.info("Received hygrometer value: " + byteArrayToHexString(valueByte));

        int hum = shortUnsignedAtOffset(valueByte, 2);
        float humf;
        if (this.cc2650) {
            humf = hum / 65536f * 100f;
        } else {
            hum = hum - hum % 4;
            humf = -6f + 125f * (hum / 65535f);
        }
        return humf;
    }

    // -----------------------------------------------------------------------------------------------------------
    //
    // Magnetometer Sensor
    //
    // -----------------------------------------------------------------------------------------------------------
    /*
     * Enable magnetometer sensor
     */
    public void enableMagnetometer(byte[] config) {
        BluetoothLeGattCharacteristic enableChar;
        if (this.cc2650) {
            // 0: gyro X, 1: gyro Y, 2: gyro Z
            // 3: acc X, 4: acc Y, 5: acc Z
            // 6: mag
            // 7: wake-on-motion
            // 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
            try {
                enableChar = this.gattServices.get(MOVEMENT).findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE);
                writeOnCharacteristic(config, enableChar);
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            // Write "01" in order to enable the sensor
            try {
                enableChar = this.gattServices.get(MAGNETOMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_ENABLE);
                enableChar.writeValue(config);
            } catch (KuraException e) {
                logger.error("Magnetometer enable failed", e);
            }
        }
    }

    /*
     * Disable magnetometer sensor
     */
    public void disableMagnetometer() {
        BluetoothLeGattCharacteristic enableChar;
        if (this.cc2650) {
            byte[] config = { 0x00, 0x00 };
            try {
                enableChar = this.gattServices.get(MOVEMENT).findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE);
                writeOnCharacteristic(config, enableChar);
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            byte[] config = { 0x00 };
            try {
                enableChar = this.gattServices.get(MAGNETOMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_ENABLE);
                enableChar.writeValue(config);
            } catch (KuraException e) {
                logger.error("Magnetometer enable failed", e);
            }
        }
    }

    /*
     * Read magnetometer sensor
     */
    public float[] readMagneticField() {
        float[] magneticField = new float[3];
        BluetoothLeGattCharacteristic magValueChar;
        try {
            if (this.cc2650) {
                magValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                magneticField = calculateMagneticField(magValueChar.readValue());
            } else {
                magValueChar = this.gattServices.get(MAGNETOMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_VALUE);
                magneticField = calculateMagneticField(magValueChar.readValue());
            }
        } catch (KuraException e) {
            logger.error("Magnetic field read failed", e);
        }
        return magneticField;
    }

    /*
     * Enable magnetometer notifications
     */
    public void enableMagneticFieldNotifications(Consumer<float[]> callback) {
        Consumer<byte[]> callbackMag = valueBytes -> callback.accept(calculateMagneticField(valueBytes));
        BluetoothLeGattCharacteristic magValueChar;
        try {
            if (this.cc2650) {
                magValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                magValueChar.enableValueNotifications(callbackMag);
            } else {
                magValueChar = this.gattServices.get(MAGNETOMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_VALUE);
                magValueChar.enableValueNotifications(callbackMag);
            }
        } catch (KuraException e) {
            logger.error("Magnetic field notification enable failed", e);
        }
    }

    /*
     * Disable magnetometer notifications
     */
    public void disableMagneticFieldNotifications() {
        BluetoothLeGattCharacteristic magValueChar;
        try {
            if (this.cc2650) {
                magValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                magValueChar.disableValueNotifications();
            } else {
                magValueChar = this.gattServices.get(MAGNETOMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_VALUE);
                magValueChar.disableValueNotifications();
            }
        } catch (KuraException e) {
            logger.error("Magnetic field notification disable failed", e);
        }
    }

    /*
     * Set sampling period
     */
    public void setMagnetometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic magPeriodChar;
        try {
            if (this.isCC2650()) {
                magPeriodChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD);
                magPeriodChar.writeValue(periodBytes);
            } else {
                magPeriodChar = this.gattServices.get(MAGNETOMETER)
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_PERIOD);
                magPeriodChar.writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Magnetometer period set failed", e);
        }
    }

    /*
     * Calculate Magnetic Field
     */
    private float[] calculateMagneticField(byte[] valueByte) {

        logger.info("Received magnetometer value: " + byteArrayToHexString(valueByte));

        float[] magneticField = new float[3];
        if (this.cc2650) {

            final float scale = (float) 32768 / 4912;

            int x = shortSignedAtOffset(valueByte, 12);
            int y = shortSignedAtOffset(valueByte, 14);
            int z = shortSignedAtOffset(valueByte, 16);

            magneticField[0] = x / scale;
            magneticField[1] = y / scale;
            magneticField[2] = z / scale;
        } else {

            int x = shortSignedAtOffset(valueByte, 0);
            int y = shortSignedAtOffset(valueByte, 2);
            int z = shortSignedAtOffset(valueByte, 4);

            magneticField[0] = x * (2000f / 65536f) * -1;
            magneticField[1] = y * (2000f / 65536f) * -1;
            magneticField[2] = z * (2000f / 65536f);
        }

        return magneticField;
    }

    // ------------------------------------------------------------------------------------------------------------------
    //
    // Barometric Pressure Sensor
    //
    // ------------------------------------------------------------------------------------------------------------------

    /*
     * Enable pressure sensor
     */
    public void enableBarometer() {
        // Write "01" to enable pressure sensor
        byte[] value = { 0x01 };
        BluetoothLeGattCharacteristic preEnableChar;
        try {
            preEnableChar = this.gattServices.get(PRESSURE).findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE);
            preEnableChar.writeValue(value);
        } catch (KuraException e) {
            logger.error("Barometer enable failed", e);
        }
    }

    /*
     * Disable pressure sensor
     */
    public void disableBarometer() {
        // Write "00" to enable pressure sensor
        byte[] value = { 0x01 };
        BluetoothLeGattCharacteristic preEnableChar;
        try {
            preEnableChar = this.gattServices.get(PRESSURE).findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE);
            preEnableChar.writeValue(value);
        } catch (KuraException e) {
            logger.error("Barometer disable failed", e);
        }
    }

    /*
     * Calibrate pressure sensor (only for CC2541)
     */
    public void calibrateBarometer() {
        if (!this.cc2650) {
            // Write "02" to enable pressure sensor
            byte[] value = { 0x02 };
            BluetoothLeGattCharacteristic preValueChar;
            try {
                preValueChar = this.gattServices.get(PRESSURE)
                        .findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE);
                preValueChar.writeValue(value);
                this.pressureCalibration = readCalibrationPressure();
            } catch (KuraException e) {
                logger.error("Barometer calibration failed", e);
            }
        }
    }

    /*
     * Read calibration pressure (only for CC2541)
     */
    private byte[] readCalibrationPressure() {
        byte[] pressure = { 0x00 };
        BluetoothLeGattCharacteristic preValueChar;
        try {
            preValueChar = this.gattServices.get(PRESSURE)
                    .findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_CALIBRATION);
            pressure = preValueChar.readValue();
        } catch (KuraException e) {
            logger.error("Pressure read failed", e);
        }
        return pressure;
    }

    /*
     * Read pressure sensor
     */
    public double readPressure() {
        double pressure = 0;
        BluetoothLeGattCharacteristic preValueChar;
        try {
            preValueChar = this.gattServices.get(PRESSURE).findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE);
            pressure = calculatePressure(preValueChar.readValue());
        } catch (KuraException e) {
            logger.error("Pressure read failed", e);
        }
        return pressure;
    }

    /*
     * Enable pressure notifications
     */
    public void enablePressureNotifications(Consumer<Double> callback) {
        Consumer<byte[]> callbackPre = valueBytes -> callback.accept(calculatePressure(valueBytes));
        BluetoothLeGattCharacteristic preValueChar;
        try {
            preValueChar = this.gattServices.get(PRESSURE).findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE);
            preValueChar.enableValueNotifications(callbackPre);
        } catch (KuraException e) {
            logger.error("Pressure notification enable failed", e);
        }
    }

    /*
     * Disable pressure notifications
     */
    public void disablePressureNotifications() {
        BluetoothLeGattCharacteristic preValueChar;
        try {
            preValueChar = this.gattServices.get(PRESSURE).findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE);
            preValueChar.disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Pressure notification enable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setBarometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic prePeriodChar;
        try {
            prePeriodChar = this.gattServices.get(PRESSURE).findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_PERIOD);
            prePeriodChar.writeValue(periodBytes);
        } catch (KuraException e) {
            logger.error("Pressure period set failed", e);
        }
    }

    /*
     * Calculate pressure
     */
    private double calculatePressure(byte[] valueByte) {

        logger.info("Received pressure value: " + byteArrayToHexString(valueByte));

        double pa;
        if (this.cc2650) {

            if (valueByte.length > 4) {
                Integer val = twentyFourBitUnsignedAtOffset(valueByte, 3);
                pa = val / 100.0;
            } else {
                int mantissa;
                int exponent;
                Integer pre = shortUnsignedAtOffset(valueByte, 2);

                mantissa = pre & 0x0FFF;
                exponent = pre >> 12 & 0xFF;

                double output;
                double magnitude = Math.pow(2.0, exponent);
                output = mantissa * magnitude;
                pa = output / 100.0;
            }

        } else {

            int tr = shortSignedAtOffset(valueByte, 0);
            int pr = shortUnsignedAtOffset(valueByte, 2);

            int[] c = new int[8];
            c[0] = shortUnsignedAtOffset(this.pressureCalibration, 0);
            c[1] = shortUnsignedAtOffset(this.pressureCalibration, 2);
            c[2] = shortUnsignedAtOffset(this.pressureCalibration, 4);
            c[3] = shortUnsignedAtOffset(this.pressureCalibration, 6);
            c[4] = shortSignedAtOffset(this.pressureCalibration, 8);
            c[5] = shortSignedAtOffset(this.pressureCalibration, 10);
            c[6] = shortSignedAtOffset(this.pressureCalibration, 12);
            c[7] = shortSignedAtOffset(this.pressureCalibration, 14);

            // Ignore temperature from pressure sensor
            double s = c[2] + c[3] * tr / Math.pow(2, 17) + c[4] * tr / Math.pow(2, 15) * tr / Math.pow(2, 19);
            double o = c[5] * Math.pow(2, 14) + c[6] * tr / Math.pow(2, 3)
                    + c[7] * tr / Math.pow(2, 15) * tr / Math.pow(2, 4);
            pa = (s * pr + o) / Math.pow(2, 14) / 100.0;

        }

        return pa;
    }

    // --------------------------------------------------------------------------------------------------------
    //
    // Gyroscope Sensor
    //
    // --------------------------------------------------------------------------------------------------------
    /*
     * Enable gyroscope sensor
     */
    public void enableGyroscope(byte[] config) {
        BluetoothLeGattCharacteristic enableChar;
        if (this.cc2650) {
            // 0: gyro X, 1: gyro Y, 2: gyro Z
            // 3: acc X, 4: acc Y, 5: acc Z
            // 6: mag
            // 7: wake-on-motion
            // 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
            try {
                enableChar = this.gattServices.get(MOVEMENT).findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE);
                writeOnCharacteristic(config, enableChar);
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            // Write "00" to turn off gyroscope, "01" to enable X axis only, "02" to enable Y axis only,
            // "03" = X and Y, "04" = Z only, "05" = X and Z, "06" = Y and Z and "07" = X, Y and Z.
            try {
                enableChar = this.gattServices.get(GYROSCOPE)
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_ENABLE);
                enableChar.writeValue(config);
            } catch (KuraException e) {
                logger.error("Gyroscope enable failed", e);
            }
        }
    }

    /*
     * Disable gyroscope sensor
     */
    public void disableGyroscope() {
        BluetoothLeGattCharacteristic enableChar;
        if (this.cc2650) {
            byte[] config = { 0x00, 0x00 };
            try {
                enableChar = this.gattServices.get(MOVEMENT).findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE);
                writeOnCharacteristic(config, enableChar);
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            byte[] config = { 0x00 };
            try {
                enableChar = this.gattServices.get(GYROSCOPE)
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_ENABLE);
                enableChar.writeValue(config);
            } catch (KuraException e) {
                logger.error("Gyroscope enable failed", e);
            }
        }
    }

    /*
     * Read gyroscope sensor
     */
    public float[] readGyroscope() {
        float[] gyroscope = new float[3];
        BluetoothLeGattCharacteristic gyroValueChar;
        try {
            if (this.cc2650) {
                gyroValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                gyroscope = calculateGyroscope(gyroValueChar.readValue());
            } else {
                gyroValueChar = this.gattServices.get(GYROSCOPE)
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_VALUE);
                gyroscope = calculateGyroscope(gyroValueChar.readValue());
            }
        } catch (KuraException e) {
            logger.error("Gyroscope read failed", e);
        }
        return gyroscope;
    }

    /*
     * Enable gyroscope notifications
     */
    public void enableGyroscopeNotifications(Consumer<float[]> callback) {
        Consumer<byte[]> callbackGyro = valueBytes -> callback.accept(calculateGyroscope(valueBytes));
        BluetoothLeGattCharacteristic gyroValueChar;
        try {
            if (this.cc2650) {
                gyroValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                gyroValueChar.enableValueNotifications(callbackGyro);
            } else {
                gyroValueChar = this.gattServices.get(GYROSCOPE)
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_VALUE);
                gyroValueChar.enableValueNotifications(callbackGyro);
            }
        } catch (KuraException e) {
            logger.error("Gyroscope notification enable failed", e);
        }
    }

    /*
     * Disable gyroscope notifications
     */
    public void disableGyroscopeNotifications() {
        BluetoothLeGattCharacteristic gyroValueChar;
        try {
            if (this.cc2650) {
                gyroValueChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE);
                gyroValueChar.disableValueNotifications();
            } else {
                gyroValueChar = this.gattServices.get(GYROSCOPE)
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_VALUE);
                gyroValueChar.disableValueNotifications();
            }
        } catch (KuraException e) {
            logger.error("Gyroscope notification disable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setGyroscopePeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic gyroPeriodChar;
        try {
            if (this.isCC2650()) {
                gyroPeriodChar = this.gattServices.get(MOVEMENT)
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD);
                gyroPeriodChar.writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Gyroscope period set failed", e);
        }
    }

    /*
     * Calculate gyroscope
     */
    private float[] calculateGyroscope(byte[] valueByte) {

        logger.info("Received gyro value: " + byteArrayToHexString(valueByte));

        float[] gyroscope = new float[3];

        int y = shortSignedAtOffset(valueByte, 0);
        int x = shortSignedAtOffset(valueByte, 2);
        int z = shortSignedAtOffset(valueByte, 4);

        if (this.cc2650) {

            final float scale = (float) 65535 / 500;

            gyroscope[0] = x / scale;
            gyroscope[1] = y / scale;
            gyroscope[2] = z / scale;
        } else {
            gyroscope[0] = x * (500f / 65536f);
            gyroscope[1] = y * (500f / 65536f) * -1;
            gyroscope[2] = z * (500f / 65536f);
        }

        return gyroscope;
    }

    // -------------------------------------------------------------------------------------------------------
    //
    // Optical Sensor
    //
    // -------------------------------------------------------------------------------------------------------
    /*
     * Enable optical sensor
     */
    public void enableLuxometer() {
        // Write "01" to enable light sensor
        if (this.cc2650) {
            byte[] value = { 0x01 };
            BluetoothLeGattCharacteristic optoEnableChar;
            try {
                optoEnableChar = this.gattServices.get(OPTO)
                        .findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_ENABLE);
                optoEnableChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Luxometer enable failed", e);
            }
        } else {
            logger.info(OPTO_ERROR_MESSAGE);
        }
    }

    /*
     * Disable optical sensor
     */
    public void disableLuxometer() {
        // Write "00" to disable light sensor
        if (this.cc2650) {
            byte[] value = { 0x00 };
            BluetoothLeGattCharacteristic optoEnableChar;
            try {
                optoEnableChar = this.gattServices.get(OPTO)
                        .findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_ENABLE);
                optoEnableChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Luxometer enable failed", e);
            }
        } else {
            logger.info(OPTO_ERROR_MESSAGE);
        }
    }

    /*
     * Read optical sensor
     */
    public double readLight() {
        double light = 0.0;
        if (this.cc2650) {
            try {
                BluetoothLeGattCharacteristic optoValueChar;
                optoValueChar = this.gattServices.get(OPTO).findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE);
                light = calculateLight(optoValueChar.readValue());
            } catch (KuraException e) {
                logger.error("Luxometer read failed", e);
            }
        } else {
            logger.info(OPTO_ERROR_MESSAGE);
        }
        return light;
    }

    /*
     * Enable optical notifications
     */
    public void enableLightNotifications(Consumer<Double> callback) {
        if (this.cc2650) {
            Consumer<byte[]> callbackOpto = valueBytes -> callback.accept(calculateLight(valueBytes));
            BluetoothLeGattCharacteristic optoValueChar;
            try {
                optoValueChar = this.gattServices.get(OPTO).findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE);
                optoValueChar.enableValueNotifications(callbackOpto);
            } catch (KuraException e) {
                logger.error("Light notification enable failed", e);
            }
        } else {
            logger.info(OPTO_ERROR_MESSAGE);
        }
    }

    /*
     * Disable optical notifications
     */
    public void disableLightNotifications() {
        if (this.cc2650) {
            BluetoothLeGattCharacteristic optoValueChar;
            try {
                optoValueChar = this.gattServices.get(OPTO).findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE);
                optoValueChar.disableValueNotifications();
            } catch (KuraException e) {
                logger.error("Light notification disable failed", e);
            }
        } else {
            logger.info(OPTO_ERROR_MESSAGE);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setLuxometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        BluetoothLeGattCharacteristic optoPeriodChar;
        try {
            if (this.isCC2650()) {
                optoPeriodChar = this.gattServices.get(OPTO)
                        .findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_PERIOD);
                optoPeriodChar.writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Gyroscope period set failed", e);
        }
    }

    /*
     * Calculate light
     */
    private double calculateLight(byte[] valueByte) {

        logger.info("Received luxometer value: " + byteArrayToHexString(valueByte));

        int sfloat = shortUnsignedAtOffset(valueByte, 0);
        int mantissa;
        int exponent;
        mantissa = sfloat & 0x0FFF;
        exponent = (sfloat & 0xF000) >> 12;

        return mantissa * (0.01 * Math.pow(2.0, exponent));

    }

    // --------------------------------------------------------------------------------------------
    //
    // Keys
    //
    // --------------------------------------------------------------------------------------------
    /*
     * Enable keys notification
     */
    public void enableKeysNotification(Consumer<Integer> callback) {
        Consumer<byte[]> callbackKeys = valueBytes -> callback.accept((int) valueBytes[0]);
        BluetoothLeGattCharacteristic keysValueChar;
        try {
            keysValueChar = this.gattServices.get(KEYS).findCharacteristic(TiSensorTagGatt.UUID_KEYS_STATUS);
            keysValueChar.enableValueNotifications(callbackKeys);
            this.keysNotification = true;
        } catch (KuraException e) {
            logger.error("Keys notification enable failed", e);
        }
    }

    /*
     * Disable keys notifications
     */
    public void disableKeysNotifications() {
        BluetoothLeGattCharacteristic keysValueChar;
        try {
            keysValueChar = this.gattServices.get(KEYS).findCharacteristic(TiSensorTagGatt.UUID_KEYS_STATUS);
            if (keysValueChar != null) {
                keysValueChar.disableValueNotifications();
                this.keysNotification = false;
            }
        } catch (KuraException e) {
            logger.error("Keys notification disable failed", e);
        }

    }

    // -------------------------------------------------------------------------------------------------------
    //
    // IO Service
    //
    // -------------------------------------------------------------------------------------------------------
    /*
     * Enable IO Service
     */
    public void enableIOService() {
        // Write "01" to enable IO Service
        if (this.cc2650) {
            byte[] value = { 0x01 };
            BluetoothLeGattCharacteristic ioEnableChar;
            try {
                ioEnableChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_ENABLE);
                ioEnableChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("IO Service enable failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }

    }

    /*
     * Disable IO Service
     */
    public void disableIOService() {
        // Write "00" to disable IO Service
        if (this.cc2650) {
            byte[] value = { 0x00 };
            BluetoothLeGattCharacteristic ioEnableChar;
            try {
                ioEnableChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_ENABLE);
                ioEnableChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("IO Service enable failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    /*
     * Switch on red led
     */
    public void switchOnRedLed() {
        // Write "01" to switch on red led
        if (this.cc2650) {
            BluetoothLeGattCharacteristic ioValueChar;
            try {
                ioValueChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE);
                int status = (int) ioValueChar.readValue()[0] | 0x01;
                byte[] value = { ByteBuffer.allocate(4).putInt(status).array()[3] };
                ioValueChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Switch on red led failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    /*
     * Switch off red led
     */
    public void switchOffRedLed() {
        // Write "00" to switch off red led
        if (this.cc2650) {
            BluetoothLeGattCharacteristic ioValueChar;
            try {
                ioValueChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE);
                int status = (int) ioValueChar.readValue()[0] & 0xFE;
                byte[] value = { ByteBuffer.allocate(4).putInt(status).array()[3] };
                ioValueChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Switch off red led failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    /*
     * Switch on green led
     */
    public void switchOnGreenLed() {
        // Write "02" to switch on green led
        if (this.cc2650) {
            BluetoothLeGattCharacteristic ioValueChar;
            try {
                ioValueChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE);
                int status = (int) ioValueChar.readValue()[0] | 0x02;
                byte[] value = { ByteBuffer.allocate(4).putInt(status).array()[3] };
                ioValueChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Switch on green led failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    /*
     * Switch off green led
     */
    public void switchOffGreenLed() {
        // Write "00" to switch off green led
        if (this.cc2650) {
            BluetoothLeGattCharacteristic ioValueChar;
            try {
                ioValueChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE);
                int status = (int) ioValueChar.readValue()[0] & 0xFD;
                byte[] value = { ByteBuffer.allocate(4).putInt(status).array()[3] };
                ioValueChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Switch off green led failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    /*
     * Switch on buzzer
     */
    public void switchOnBuzzer() {
        // Write "04" to switch on buzzer
        if (this.cc2650) {
            BluetoothLeGattCharacteristic ioValueChar;
            try {
                ioValueChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE);
                int status = (int) ioValueChar.readValue()[0] | 0x04;
                byte[] value = { ByteBuffer.allocate(4).putInt(status).array()[3] };
                ioValueChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Switch on buzzer failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    /*
     * Switch off buzzer
     */
    public void switchOffBuzzer() {
        // Write "00" to switch off buzzer
        if (this.cc2650) {
            BluetoothLeGattCharacteristic ioValueChar;
            try {
                ioValueChar = this.gattServices.get(IO).findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE);
                int status = (int) ioValueChar.readValue()[0] & 0xFB;
                byte[] value = { ByteBuffer.allocate(4).putInt(status).array()[3] };
                ioValueChar.writeValue(value);
            } catch (KuraException e) {
                logger.error("Switch of buzzer failed", e);
            }
        } else {
            logger.info(IO_ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------------------------------------
    //
    // Auxiliary methods
    //
    // ---------------------------------------------------------------------------------------------

    private String byteArrayToHexString(byte[] value) {
        final char[] hexadecimals = "0123456789ABCDEF".toCharArray();
        char[] hexValue = new char[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            int v = value[j] & 0xFF;
            hexValue[j * 2] = hexadecimals[v >>> 4];
            hexValue[j * 2 + 1] = hexadecimals[v & 0x0F];
        }
        return new String(hexValue);
    }

    private int unsignedToSigned(int unsigned, int bitLength) {
        int unsignedResult = 0;
        if ((unsigned & 1 << bitLength - 1) != 0) {
            unsignedResult = -1 * ((1 << bitLength - 1) - (unsigned & (1 << bitLength - 1) - 1));
        }
        return unsignedResult;
    }

    private static Integer shortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1]; // Interpret MSB as signedan
        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = c[offset] & 0xFF;
        Integer upperByte = c[offset + 1] & 0xFF;
        return (upperByte << 8) + lowerByte;
    }

    private static Integer twentyFourBitUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = c[offset] & 0xFF;
        Integer mediumByte = c[offset + 1] & 0xFF;
        Integer upperByte = c[offset + 2] & 0xFF;
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

    private void getGattServices() {
        try {
            if (gattServices != null) {
                gattServices.put(DEVINFO, this.device.findService(TiSensorTagGatt.UUID_DEVINFO_SERVICE));
                gattServices.put(TEMPERATURE, this.device.findService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE));
                gattServices.put(HUMIDITY, this.device.findService(TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE));
                gattServices.put(PRESSURE, this.device.findService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE));
                gattServices.put(KEYS, this.device.findService(TiSensorTagGatt.UUID_KEYS_SERVICE));
                if (isCC2650()) {
                    gattServices.put(OPTO, this.device.findService(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE));
                    gattServices.put(MOVEMENT, this.device.findService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE));
                    gattServices.put(IO, this.device.findService(TiSensorTagGatt.UUID_IO_SENSOR_SERVICE));
                } else {
                    gattServices.put(ACCELEROMETER, this.device.findService(TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE));
                    gattServices.put(MAGNETOMETER, this.device.findService(TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE));
                    gattServices.put(GYROSCOPE, this.device.findService(TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE));
                }
            }
        } catch (KuraException e) {
            logger.error("Failed to get GATT service", e);
        }
    }

    private void writeOnCharacteristic(byte[] config, BluetoothLeGattCharacteristic enableChar)
            throws KuraBluetoothIOException {
        byte[] oldConfig = enableChar.readValue();
        Integer newConfig = ByteBuffer.wrap(config).getShort() | ByteBuffer.wrap(oldConfig).getShort();
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(newConfig.shortValue());
        enableChar.writeValue(bb.array());
    }
}
