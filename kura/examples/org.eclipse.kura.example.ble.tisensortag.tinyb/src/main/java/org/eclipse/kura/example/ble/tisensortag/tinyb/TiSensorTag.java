/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
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

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
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

    private static final String IO_ERROR_MESSAGE = "No IO Service on CC2541.";
    private static final String OPTO_ERROR_MESSAGE = "No optical sensor on CC2541.";
    private static final String MOV_ERROR_MESSAGE = "Movement sensor failed to be enabled";

    private static final int SERVICE_TIMEOUT = 10000;

    private BluetoothLeDevice device;
    private boolean cc2650;
    private byte[] pressureCalibration;
    private Map<String, TiSensorTagGattResources> gattResources;

    public TiSensorTag(BluetoothLeDevice bluetoothLeDevice) {
        this.device = bluetoothLeDevice;
        this.gattResources = new HashMap<>();
        if (this.device.getName().contains("SensorTag 2.0") || this.device.getName().contains("CC2650 SensorTag")) {
            this.cc2650 = true;
        } else {
            this.cc2650 = false;
        }
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

    public void connect() throws KuraBluetoothConnectionException {
        this.device.connect();
        // Wait a bit to ensure that the device is really connected and services discovered
        Long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < SERVICE_TIMEOUT) {
            if (this.device.isServicesResolved()) {
                break;
            }
            BluetoothLe.waitFor(1000);
        }
        if (!isConnected() || !this.device.isServicesResolved()) {
            this.device.disconnect();
            throw new KuraBluetoothConnectionException("Connection failed");
        }

    }

    public void init() throws KuraBluetoothConnectionException {
        if (isConnected() && this.gattResources.size() != 8) {
            getGattResources();
        }
    }

    public void disconnect() throws KuraBluetoothConnectionException {
        if (isTermometerNotifying()) {
            disableTemperatureNotifications();
        }
        if (isHygrometerNotifying()) {
            disableHumidityNotifications();
        }
        if (isBarometerNotifying()) {
            disablePressureNotifications();
        }
        if (isAccelerometerNotifying()) {
            disableAccelerationNotifications();
        }
        if (isMagnetometerNotifying()) {
            disableMagneticFieldNotifications();
        }
        if (isGyroscopeNotifying()) {
            disableGyroscopeNotifications();
        }
        if (isLuxometerNotifying()) {
            disableLightNotifications();
        }
        if (isKeysNotifying()) {
            disableKeysNotifications();
        }
        this.device.disconnect();
        if (isConnected()) {
            throw new KuraBluetoothConnectionException("Disconnection failed");
        }
        // Wait a while after disconnection
        BluetoothLe.waitFor(1000);
    }

    public boolean isCC2650() {
        return this.cc2650;
    }

    public String getFirmareRevision() {
        String firmware = "";
        try {
            BluetoothLeGattCharacteristic devinfo = this.gattResources.get(DEVINFO).getGattService()
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
        Map<String, BluetoothLeGattService> services = new HashMap<>();
        for (TiSensorTagGattResources resources : this.gattResources.values()) {
            services.put(resources.getName(), resources.getGattService());
        }
        return services;
    }

    public List<BluetoothLeGattCharacteristic> getCharacteristics() {
        List<BluetoothLeGattCharacteristic> characteristics = new ArrayList<>();
        for (Entry<String, TiSensorTagGattResources> entry : this.gattResources.entrySet()) {
            try {
                characteristics.addAll(entry.getValue().getGattService().findCharacteristics());
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
        try {
            this.gattResources.get(TEMPERATURE).getGattService()
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE).writeValue(value);
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
        try {
            this.gattResources.get(TEMPERATURE).getGattService()
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE).writeValue(value);
        } catch (KuraException e) {
            logger.error("Termometer disable failed", e);
        }
    }

    /*
     * Read temperature sensor
     */
    public double[] readTemperature() {
        double[] temperatures = new double[2];
        try {
            temperatures = calculateTemperature(
                    this.gattResources.get(TEMPERATURE).getGattValueCharacteristic().readValue());
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

        try {
            this.gattResources.get(TEMPERATURE).getGattValueCharacteristic().enableValueNotifications(callbackTemp);
        } catch (KuraException e) {
            logger.error("Temperature notification enable failed", e);
        }
    }

    /*
     * Disable temperature notifications
     */
    public void disableTemperatureNotifications() {
        try {
            this.gattResources.get(TEMPERATURE).getGattValueCharacteristic().disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Temperature notification disable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setTermometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        try {
            this.gattResources.get(TEMPERATURE).getGattService()
                    .findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_PERIOD).writeValue(periodBytes);
        } catch (KuraException e) {
            logger.error("Termometer period set failed", e);
        }
    }

    public boolean isTermometerNotifying() {
        return isNotifying(TEMPERATURE);
    }

    /*
     * Calculate temperature
     */
    private double[] calculateTemperature(byte[] valueByte) {

        logger.info("Received temperature value: {}", byteArrayToHexString(valueByte));

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
        if (this.cc2650) {
            // 0: gyro X, 1: gyro Y, 2: gyro Z
            // 3: acc X, 4: acc Y, 5: acc Z
            // 6: mag
            // 7: wake-on-motion
            // 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
            try {
                writeOnCharacteristic(config, this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE));
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            // Write "01" in order to enable the sensor in 2g range
            // Write "01" in order to select 2g range, "02" for 4g, "03" for 8g (only for firmware > 1.5)
            try {
                this.gattResources.get(ACCELEROMETER).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_ENABLE).writeValue(config);
            } catch (KuraException e) {
                logger.error("Accelerometer enable failed", e);
            }
        }
    }

    /*
     * Disable accelerometer sensor
     */
    public void disableAccelerometer() {
        if (this.cc2650) {
            byte[] config = { 0x00, 0x00 };
            try {
                writeOnCharacteristic(config, this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE));
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            byte[] config = { 0x00 };
            try {
                this.gattResources.get(ACCELEROMETER).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_ENABLE).writeValue(config);
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
        try {
            if (this.cc2650) {
                acceleration = calculateAcceleration(
                        this.gattResources.get(MOVEMENT).getGattValueCharacteristic().readValue());
            } else {
                acceleration = calculateAcceleration(
                        this.gattResources.get(ACCELEROMETER).getGattValueCharacteristic().readValue());
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
        try {
            if (this.cc2650) {
                this.gattResources.get(MOVEMENT).getGattValueCharacteristic().enableValueNotifications(callbackAcc);
            } else {
                this.gattResources.get(ACCELEROMETER).getGattValueCharacteristic()
                        .enableValueNotifications(callbackAcc);
            }
        } catch (KuraException e) {
            logger.error("Accelaration notification enable failed", e);
        }
    }

    /*
     * Disable accelerometer notifications
     */
    public void disableAccelerationNotifications() {
        try {
            if (this.cc2650) {
                this.gattResources.get(MOVEMENT).getGattValueCharacteristic().disableValueNotifications();
            } else {
                this.gattResources.get(ACCELEROMETER).getGattValueCharacteristic().disableValueNotifications();
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
        try {
            if (this.isCC2650()) {
                this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD).writeValue(periodBytes);
            } else {
                this.gattResources.get(ACCELEROMETER).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_PERIOD).writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Acceleration period set failed", e);
        }
    }

    public boolean isAccelerometerNotifying() {
        if (this.isCC2650()) {
            return isNotifying(MOVEMENT);
        } else {
            return isNotifying(ACCELEROMETER);
        }

    }

    /*
     * Calculate acceleration
     */
    private double[] calculateAcceleration(byte[] valueByte) {

        logger.info("Received accelerometer value: {}", byteArrayToHexString(valueByte));

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
            int x = unsignedToSigned(valueByte[0], 8);
            int y = unsignedToSigned(valueByte[1], 8);
            int z = unsignedToSigned(valueByte[2], 8) * -1;

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
        try {
            this.gattResources.get(HUMIDITY).getGattService().findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_ENABLE)
                    .writeValue(value);
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
        try {
            this.gattResources.get(HUMIDITY).getGattService().findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_ENABLE)
                    .writeValue(value);
        } catch (KuraException e) {
            logger.error("Hygrometer disable failed", e);
        }
    }

    /*
     * Read humidity sensor
     */
    public float readHumidity() {
        float humidity = 0F;
        try {
            humidity = calculateHumidity(this.gattResources.get(HUMIDITY).getGattValueCharacteristic().readValue());
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
        try {
            this.gattResources.get(HUMIDITY).getGattValueCharacteristic().enableValueNotifications(callbackHum);
        } catch (KuraException e) {
            logger.error("Humidity notification enable failed", e);
        }
    }

    /*
     * Disable humidity notifications
     */
    public void disableHumidityNotifications() {
        try {
            this.gattResources.get(HUMIDITY).getGattValueCharacteristic().disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Humidity notification enable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setHygrometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        try {
            this.gattResources.get(HUMIDITY).getGattService().findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_PERIOD)
                    .writeValue(periodBytes);
        } catch (KuraException e) {
            logger.error("Hygrometer period set failed", e);
        }
    }

    public boolean isHygrometerNotifying() {
        return isNotifying(HUMIDITY);
    }

    /*
     * Calculate Humidity
     */
    private float calculateHumidity(byte[] valueByte) {

        logger.info("Received hygrometer value: {}", byteArrayToHexString(valueByte));

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
        if (this.cc2650) {
            // 0: gyro X, 1: gyro Y, 2: gyro Z
            // 3: acc X, 4: acc Y, 5: acc Z
            // 6: mag
            // 7: wake-on-motion
            // 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
            try {
                writeOnCharacteristic(config, this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE));
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            // Write "01" in order to enable the sensor
            try {
                this.gattResources.get(MAGNETOMETER).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_ENABLE).writeValue(config);
            } catch (KuraException e) {
                logger.error("Magnetometer enable failed", e);
            }
        }
    }

    /*
     * Disable magnetometer sensor
     */
    public void disableMagnetometer() {
        if (this.cc2650) {
            byte[] config = { 0x00, 0x00 };
            try {
                writeOnCharacteristic(config, this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE));
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            byte[] config = { 0x00 };
            try {
                this.gattResources.get(MAGNETOMETER).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_ENABLE).writeValue(config);
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
        try {
            if (this.cc2650) {
                magneticField = calculateMagneticField(
                        this.gattResources.get(MOVEMENT).getGattValueCharacteristic().readValue());
            } else {
                magneticField = calculateMagneticField(
                        this.gattResources.get(MAGNETOMETER).getGattValueCharacteristic().readValue());
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
        try {
            if (this.cc2650) {
                this.gattResources.get(MOVEMENT).getGattValueCharacteristic().enableValueNotifications(callbackMag);
            } else {
                this.gattResources.get(MAGNETOMETER).getGattValueCharacteristic().enableValueNotifications(callbackMag);
            }
        } catch (KuraException e) {
            logger.error("Magnetic field notification enable failed", e);
        }
    }

    /*
     * Disable magnetometer notifications
     */
    public void disableMagneticFieldNotifications() {
        try {
            if (this.cc2650) {
                this.gattResources.get(MOVEMENT).getGattValueCharacteristic().disableValueNotifications();
            } else {
                this.gattResources.get(MAGNETOMETER).getGattValueCharacteristic().disableValueNotifications();
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
        try {
            if (this.isCC2650()) {
                this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD).writeValue(periodBytes);
            } else {
                this.gattResources.get(MAGNETOMETER).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_PERIOD).writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Magnetometer period set failed", e);
        }
    }

    public boolean isMagnetometerNotifying() {
        if (this.isCC2650()) {
            return isNotifying(MOVEMENT);
        } else {
            return isNotifying(MAGNETOMETER);
        }
    }

    /*
     * Calculate Magnetic Field
     */
    private float[] calculateMagneticField(byte[] valueByte) {

        logger.info("Received magnetometer value: {}", byteArrayToHexString(valueByte));

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
        try {
            this.gattResources.get(PRESSURE).getGattService().findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE)
                    .writeValue(value);
        } catch (KuraException e) {
            logger.error("Barometer enable failed", e);
        }
    }

    /*
     * Disable pressure sensor
     */
    public void disableBarometer() {
        // Write "00" to disable pressure sensor
        byte[] value = { 0x00 };
        try {
            this.gattResources.get(PRESSURE).getGattService().findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE)
                    .writeValue(value);
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
            try {
                this.gattResources.get(PRESSURE).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE).writeValue(value);
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
        try {
            pressure = this.gattResources.get(PRESSURE).getGattService()
                    .findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_CALIBRATION).readValue();
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
        try {
            pressure = calculatePressure(this.gattResources.get(PRESSURE).getGattValueCharacteristic().readValue());
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
        try {
            this.gattResources.get(PRESSURE).getGattValueCharacteristic().enableValueNotifications(callbackPre);
        } catch (KuraException e) {
            logger.error("Pressure notification enable failed", e);
        }
    }

    /*
     * Disable pressure notifications
     */
    public void disablePressureNotifications() {
        try {
            this.gattResources.get(PRESSURE).getGattValueCharacteristic().disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Pressure notification enable failed", e);
        }
    }

    /*
     * Set sampling period (only for CC2650)
     */
    public void setBarometerPeriod(int period) {
        byte[] periodBytes = { ByteBuffer.allocate(4).putInt(period).array()[3] };
        try {
            this.gattResources.get(PRESSURE).getGattService().findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_PERIOD)
                    .writeValue(periodBytes);
        } catch (KuraException e) {
            logger.error("Pressure period set failed", e);
        }
    }

    public boolean isBarometerNotifying() {
        return isNotifying(PRESSURE);
    }

    /*
     * Calculate pressure
     */
    private double calculatePressure(byte[] valueByte) {

        logger.info("Received pressure value: {}", byteArrayToHexString(valueByte));

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
        if (this.cc2650) {
            // 0: gyro X, 1: gyro Y, 2: gyro Z
            // 3: acc X, 4: acc Y, 5: acc Z
            // 6: mag
            // 7: wake-on-motion
            // 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
            try {
                writeOnCharacteristic(config, this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE));
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            // Write "00" to turn off gyroscope, "01" to enable X axis only, "02" to enable Y axis only,
            // "03" = X and Y, "04" = Z only, "05" = X and Z, "06" = Y and Z and "07" = X, Y and Z.
            try {
                this.gattResources.get(GYROSCOPE).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_ENABLE).writeValue(config);
            } catch (KuraException e) {
                logger.error("Gyroscope enable failed", e);
            }
        }
    }

    /*
     * Disable gyroscope sensor
     */
    public void disableGyroscope() {
        if (this.cc2650) {
            byte[] config = { 0x00, 0x00 };
            try {
                writeOnCharacteristic(config, this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE));
            } catch (KuraException e) {
                logger.error(MOV_ERROR_MESSAGE, e);
            }
        } else {
            byte[] config = { 0x00 };
            try {
                this.gattResources.get(GYROSCOPE).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_ENABLE).writeValue(config);
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
        try {
            if (this.cc2650) {
                gyroscope = calculateGyroscope(
                        this.gattResources.get(MOVEMENT).getGattValueCharacteristic().readValue());
            } else {
                gyroscope = calculateGyroscope(
                        this.gattResources.get(GYROSCOPE).getGattValueCharacteristic().readValue());
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
        try {
            if (this.cc2650) {
                this.gattResources.get(MOVEMENT).getGattValueCharacteristic().enableValueNotifications(callbackGyro);
            } else {
                this.gattResources.get(GYROSCOPE).getGattValueCharacteristic().enableValueNotifications(callbackGyro);
            }
        } catch (KuraException e) {
            logger.error("Gyroscope notification enable failed", e);
        }
    }

    /*
     * Disable gyroscope notifications
     */
    public void disableGyroscopeNotifications() {
        try {
            if (this.cc2650) {
                this.gattResources.get(MOVEMENT).getGattValueCharacteristic().disableValueNotifications();
            } else {
                this.gattResources.get(GYROSCOPE).getGattValueCharacteristic().disableValueNotifications();
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
        try {
            if (this.isCC2650()) {
                this.gattResources.get(MOVEMENT).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD).writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Gyroscope period set failed", e);
        }
    }

    public boolean isGyroscopeNotifying() {
        if (this.isCC2650()) {
            return isNotifying(MOVEMENT);
        } else {
            return isNotifying(GYROSCOPE);
        }
    }

    /*
     * Calculate gyroscope
     */
    private float[] calculateGyroscope(byte[] valueByte) {

        logger.info("Received gyro value: {}", byteArrayToHexString(valueByte));

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
            try {
                this.gattResources.get(OPTO).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_ENABLE).writeValue(value);
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
            try {
                this.gattResources.get(OPTO).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_ENABLE).writeValue(value);
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
                light = calculateLight(this.gattResources.get(OPTO).getGattValueCharacteristic().readValue());
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
            try {
                this.gattResources.get(OPTO).getGattValueCharacteristic().enableValueNotifications(callbackOpto);
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
            try {
                this.gattResources.get(OPTO).getGattValueCharacteristic().disableValueNotifications();
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
        try {
            if (this.isCC2650()) {
                this.gattResources.get(OPTO).getGattService()
                        .findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_PERIOD).writeValue(periodBytes);
            }
        } catch (KuraException e) {
            logger.error("Gyroscope period set failed", e);
        }
    }

    public boolean isLuxometerNotifying() {
        if (this.isCC2650()) {
            return isNotifying(OPTO);
        } else {
            return false;
        }
    }

    /*
     * Calculate light
     */
    private double calculateLight(byte[] valueByte) {

        logger.info("Received luxometer value: {}", byteArrayToHexString(valueByte));

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
        try {
            this.gattResources.get(KEYS).getGattValueCharacteristic().enableValueNotifications(callbackKeys);
        } catch (KuraException e) {
            logger.error("Keys notification enable failed", e);
        }
    }

    /*
     * Disable keys notifications
     */
    public void disableKeysNotifications() {
        try {
            this.gattResources.get(KEYS).getGattValueCharacteristic().disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Keys notification disable failed", e);
        }

    }

    public boolean isKeysNotifying() {
        TiSensorTagGattResources resource = this.gattResources.get(KEYS);
        if (resource != null) {
            return resource.getGattValueCharacteristic().isNotifying();
        } else {
            return false;
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
            try {
                this.gattResources.get(IO).getGattService().findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_ENABLE)
                        .writeValue(value);
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
            try {
                this.gattResources.get(IO).getGattService().findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_ENABLE)
                        .writeValue(value);
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
                ioValueChar = this.gattResources.get(IO).getGattValueCharacteristic();
                int status = ioValueChar.readValue()[0] | 0x01;
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
                ioValueChar = this.gattResources.get(IO).getGattValueCharacteristic();
                int status = ioValueChar.readValue()[0] & 0xFE;
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
                ioValueChar = this.gattResources.get(IO).getGattValueCharacteristic();
                int status = ioValueChar.readValue()[0] | 0x02;
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
                ioValueChar = this.gattResources.get(IO).getGattValueCharacteristic();
                int status = ioValueChar.readValue()[0] & 0xFD;
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
                ioValueChar = this.gattResources.get(IO).getGattValueCharacteristic();
                int status = ioValueChar.readValue()[0] | 0x04;
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
                ioValueChar = this.gattResources.get(IO).getGattValueCharacteristic();
                int status = ioValueChar.readValue()[0] & 0xFB;
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

    private void getGattResources() throws KuraBluetoothConnectionException {
        try {
            if (gattResources != null) {
                gattResources.put(DEVINFO, new TiSensorTagGattResources(DEVINFO,
                        this.device.findService(TiSensorTagGatt.UUID_DEVINFO_SERVICE), null));

                BluetoothLeGattService tempService = this.device.findService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE);
                if (tempService != null) {
                    gattResources.put(TEMPERATURE, new TiSensorTagGattResources(TEMPERATURE, tempService,
                            tempService.findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE)));
                }

                BluetoothLeGattService humService = this.device.findService(TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE);
                if (humService != null) {
                    gattResources.put(HUMIDITY, new TiSensorTagGattResources(HUMIDITY, humService,
                            humService.findCharacteristic(TiSensorTagGatt.UUID_HUM_SENSOR_VALUE)));
                }

                BluetoothLeGattService presService = this.device.findService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE);
                if (presService != null) {
                    gattResources.put(PRESSURE, new TiSensorTagGattResources(PRESSURE, presService,
                            presService.findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE)));
                }

                BluetoothLeGattService keysService = this.device.findService(TiSensorTagGatt.UUID_KEYS_SERVICE);
                if (keysService != null) {
                    gattResources.put(KEYS, new TiSensorTagGattResources(KEYS, keysService,
                            keysService.findCharacteristic(TiSensorTagGatt.UUID_KEYS_STATUS)));
                }

                if (isCC2650()) {
                    getCC2650GattResources();
                } else {
                    getCC2541GattResources();
                }
            }
        } catch (KuraBluetoothResourceNotFoundException e) {
            logger.error("Failed to get GATT service", e);
            disconnect();
        }
    }

    private void getCC2541GattResources() throws KuraBluetoothResourceNotFoundException {
        BluetoothLeGattService accService = this.device.findService(TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE);
        if (accService != null) {
            gattResources.put(ACCELEROMETER, new TiSensorTagGattResources(ACCELEROMETER, accService,
                    accService.findCharacteristic(TiSensorTagGatt.UUID_ACC_SENSOR_VALUE)));
        }

        BluetoothLeGattService magService = this.device.findService(TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE);
        if (magService != null) {
            gattResources.put(MAGNETOMETER, new TiSensorTagGattResources(MAGNETOMETER, magService,
                    magService.findCharacteristic(TiSensorTagGatt.UUID_MAG_SENSOR_VALUE)));
        }

        BluetoothLeGattService gyrService = this.device.findService(TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE);
        if (gyrService != null) {
            gattResources.put(GYROSCOPE, new TiSensorTagGattResources(GYROSCOPE, gyrService,
                    gyrService.findCharacteristic(TiSensorTagGatt.UUID_GYR_SENSOR_VALUE)));
        }
    }

    private void getCC2650GattResources() throws KuraBluetoothResourceNotFoundException {
        BluetoothLeGattService optoService = this.device.findService(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE);
        if (optoService != null) {
            gattResources.put(OPTO, new TiSensorTagGattResources(OPTO, optoService,
                    optoService.findCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE)));
        }

        BluetoothLeGattService movService = this.device.findService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE);
        if (movService != null) {
            gattResources.put(MOVEMENT, new TiSensorTagGattResources(MOVEMENT, movService,
                    movService.findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE)));
        }

        BluetoothLeGattService ioService = this.device.findService(TiSensorTagGatt.UUID_IO_SENSOR_SERVICE);
        if (ioService != null) {
            gattResources.put(IO, new TiSensorTagGattResources(IO, ioService,
                    ioService.findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE)));
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

    private boolean isNotifying(String resourceName) {
        TiSensorTagGattResources resource = this.gattResources.get(resourceName);
        if (resource != null) {
            return resource.getGattValueCharacteristic().isNotifying();
        } else {
            return false;
        }
    }
}
