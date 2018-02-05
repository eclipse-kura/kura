/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.driver.sensehat;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.example.driver.sensehat.Resource.Sensor;
import org.eclipse.kura.raspberrypi.sensehat.SenseHat;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseHatInterface implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(SenseHatInterface.class);

    private static final int I2C_BUS = 1;
    private static final int I2C_ADDRESS_SIZE = 7;
    private static final int I2C_FREQUENCY = 400000;
    private static final int I2C_ACC_ADDRESS = 0x6A;
    private static final int I2C_MAG_ADDRESS = 0x1C;
    private static final int I2C_PRE_ADDRESS = 0x5C;
    private static final int I2C_HUM_ADDRESS = 0x5F;

    private FramebufferHandler frameBufferHandler;
    private JoystickHandler joystickHandler;

    private HTS221 humiditySensor;
    private LPS25H pressureSensor;
    private LSM9DS1 imuSensor;

    private float[] accelerometer = new float[3];
    private float[] gyroscope = new float[3];
    private float[] magnetometer = new float[3];

    private float humidity;
    private float temperatureFromHumidity;

    private float pressure;
    private float temperatureFromPerssure;

    public SenseHatInterface(SenseHat senseHat) {
        this.imuSensor = senseHat.getIMUSensor(I2C_BUS, I2C_ACC_ADDRESS, I2C_MAG_ADDRESS, I2C_ADDRESS_SIZE,
                I2C_FREQUENCY);
        if (!this.imuSensor.initDevice(true, true, true)) {
            logger.error("Unable to initialize IMU sensor.");
            LSM9DS1.closeDevice();
            this.imuSensor = null;
        }

        this.pressureSensor = senseHat.getPressureSensor(I2C_BUS, I2C_PRE_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
        if (!this.pressureSensor.initDevice()) {
            logger.error("Unable to initialize pressure sensor.");
            LPS25H.closeDevice();
            this.pressureSensor = null;
        }

        this.humiditySensor = senseHat.getHumiditySensor(I2C_BUS, I2C_HUM_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
        if (!this.humiditySensor.initDevice()) {
            logger.error("Unable to initialize humidity sensor.");
            HTS221.closeDevice();
            this.humiditySensor = null;
        }

        FrameBuffer.setRotation(0);

        this.joystickHandler = new JoystickHandler(senseHat.getJoystick());
        this.frameBufferHandler = new FramebufferHandler(senseHat.getFrameBufferRaw());
    }

    @Override
    public void close() throws IOException {
        if (this.joystickHandler != null) {
            this.joystickHandler.close();
            this.joystickHandler = null;
        }
        if (this.imuSensor != null) {
            LSM9DS1.closeDevice();
            this.imuSensor = null;
        }
        if (this.pressureSensor != null) {
            LPS25H.closeDevice();
            this.pressureSensor = null;
        }
        if (this.humiditySensor != null) {
            HTS221.closeDevice();
            this.humiditySensor = null;
        }
        if (this.frameBufferHandler != null) {
            this.frameBufferHandler.close();
            this.frameBufferHandler = null;
        }
    }

    public void fetch(EnumSet<Sensor> sensors) {
        if (sensors.contains(Sensor.ACCELEROMETER)) {
            logger.debug("fetching Accelerometer data...");
            this.accelerometer = this.imuSensor.getAccelerometerRaw();
            logger.debug("fetching Accelerometer data...done");
        }
        if (sensors.contains(Sensor.GYROSCOPE)) {
            logger.debug("fetching Gyroscope data...");
            this.gyroscope = this.imuSensor.getGyroscopeRaw();
            logger.debug("fetching Gyroscope data...done");
        }
        if (sensors.contains(Sensor.MAGNETOMETER)) {
            logger.debug("fetching Magnetometer data...");
            this.magnetometer = this.imuSensor.getCompassRaw();
            logger.debug("fetching Magnetometer data...done");
        }
        if (sensors.contains(Sensor.HUMIDITY)) {
            logger.debug("fetching Humidity data...");
            this.humidity = this.humiditySensor.getHumidity();
            logger.debug("fetching Humidity data...done");
        }
        if (sensors.contains(Sensor.PRESSURE)) {
            logger.debug("fetching Pressure data...");
            this.pressure = this.pressureSensor.getPressure();
            logger.debug("fetching Pressure data...done");
        }
        if (sensors.contains(Sensor.TEMPERATURE_FROM_HUMIDITY)) {
            logger.debug("fetching Temperature from Humidity...");
            this.temperatureFromHumidity = this.humiditySensor.getTemperature();
            logger.debug("fetching Temperature from Humidity...done");
        }
        if (sensors.contains(Sensor.TEMPERATURE_FROM_PRESSURE)) {
            logger.debug("fetching Temperature from Pressure...");
            this.temperatureFromPerssure = this.pressureSensor.getTemperature();
            logger.debug("fetching Temperature from Pressure...done");
        }
    }

    public float getAccelerometerX() {
        return this.accelerometer[0];
    }

    public float getAccelerometerY() {
        return this.accelerometer[1];
    }

    public float getAccelerometerZ() {
        return this.accelerometer[2];
    }

    public float getGyroscopeX() {
        return this.gyroscope[0];
    }

    public float getGyroscopeY() {
        return this.gyroscope[1];
    }

    public float getGyroscopeZ() {
        return this.gyroscope[2];
    }

    public float getMagnetometerX() {
        return this.magnetometer[0];
    }

    public float getMagnetometerY() {
        return this.magnetometer[1];
    }

    public float getMagnetometerZ() {
        return this.magnetometer[2];
    }

    public float getPressure() {
        return pressure;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getTemperatureFromHumidity() {
        return temperatureFromHumidity;
    }

    public float getTemperatureFromPressure() {
        return temperatureFromPerssure;
    }

    public Long getLastJoystickEventTimestamp(Resource event) {
        return this.joystickHandler.getLastJoystickEventTimestamp(event);
    }

    public synchronized void runReadRequest(SenseHatReadRequest request) {
        this.fetch(request.getInvolvedSensors());
        request.getTasks().forEach(task -> task.exec(this));
    }

    public synchronized void runFramebufferRequest(final FramebufferRequest request) throws IOException {
        this.frameBufferHandler.runFramebufferRequest(request);
    }

    public static class SenseHatReadRequest {

        private final EnumSet<Sensor> involvedSensors;
        private final List<ChannelRecord> records;
        private final List<ReadTask> tasks;

        protected SenseHatReadRequest(List<ChannelRecord> records) {
            this.records = records;
            this.involvedSensors = EnumSet.noneOf(Sensor.class);
            this.tasks = new ArrayList<>(records.size());
        }

        protected void addInvolvedSensor(Sensor sensor) {
            this.involvedSensors.add(sensor);
        }

        protected void addTask(ReadTask task) {
            this.tasks.add(task);
        }

        public EnumSet<Sensor> getInvolvedSensors() {
            return involvedSensors;
        }

        public List<ChannelRecord> getRecords() {
            return records;
        }

        public List<ReadTask> getTasks() {
            return tasks;
        }
    }

    public interface JoystickEventListener {

        public void onJoystickEvent(Resource event, long timestamp);
    }

    public void addJoystickEventListener(final JoystickEventListener listener) {
        this.joystickHandler.addJoystickEventListener(listener);
    }

    public void removeJoystickEventListener(final JoystickEventListener listener) {
        this.joystickHandler.removeJoystickEventListener(listener);
    }
}
