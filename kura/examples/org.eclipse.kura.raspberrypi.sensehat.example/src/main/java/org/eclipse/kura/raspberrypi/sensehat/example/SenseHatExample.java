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
package org.eclipse.kura.raspberrypi.sensehat.example;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.raspberrypi.sensehat.SenseHat;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.Colors;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.JoystickEvent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseHatExample implements ConfigurableComponent {

    private static final Logger s_logger = LoggerFactory.getLogger(SenseHatExample.class);

    private static final int I2C_BUS = 1;
    private static final int I2C_ADDRESS_SIZE = 7;
    private static final int I2C_FREQUENCY = 400000;
    private static final int I2C_ACC_ADDRESS = 0x6A;
    private static final int I2C_MAG_ADDRESS = 0x1C;
    private static final int I2C_PRE_ADDRESS = 0x5C;
    private static final int I2C_HUM_ADDRESS = 0x5F;

    private static final String IMU_ACC_ENABLE = "imu.accelerometer.enable";
    private static final String IMU_GYRO_ENABLE = "imu.gyroscope.enable";
    private static final String IMU_COMP_ENABLE = "imu.compass.enable";
    private static final String IMU_SAMPLES = "imu.sample.number";
    private static final String PRE_ENABLE = "pressure.enable";
    private static final String HUM_ENABLE = "humidity.enable";
    private static final String LCD_ENABLE = "screen.enable";
    private static final String STICK_ENABLE = "stick.enable";
    private static final String SCREEN_MESSAGE = "screen.message";
    private static final String SCREEN_ROTATION = "screen.rotation";
    private static final String SCREEN_TEXT_COLOR = "screen.text.color";

    private boolean imuAccEnable = false;
    private boolean imuGyroEnable = false;
    private boolean imuCompEnable = false;
    private int imuSamples = 20;
    private boolean preEnable = false;
    private boolean humEnable = false;
    private boolean lcdEnable = false;
    private boolean stickEnable = false;
    private String screenMessage = "";
    private int screenRotation = 0;
    private short[] screenTextColor = Colors.ORANGE;

    private Joystick senseHatJoystick;
    private JoystickEvent je;
    private boolean runThread;

    private FrameBuffer frameBuffer;

    private ScheduledExecutorService joystickworker;
    private Future<?> joystickhandle;

    private SenseHat senseHat;

    private LSM9DS1 imuSensor;          // Inertial Measurement Unit (Accelerometer, Gyroscope, Magnetometer)
    private LPS25H pressureSensor;     // Atmospheric Pressure
    private HTS221 humiditySensor;     // Humidity
    private Map<String, Object> properties;

    private static ScheduledFuture<?> startUpdateThread;
    private ScheduledThreadPoolExecutor executor;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public SenseHatExample() {
        super();
    }

    public void setSenseHatService(SenseHat senseHat) {
        this.senseHat = senseHat;
    }

    public void unsetSenseHatService(SenseHat senseHat) {
        this.senseHat = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating Sense Hat Application...");

        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        getProperties(properties);

        if (startUpdateThread != null) {
            startUpdateThread.cancel(true);
            startUpdateThread = null;
        }
        startUpdateThread = this.executor.schedule(new Runnable() {

            @Override
            public void run() {
                update();
            }
        }, 0, TimeUnit.MILLISECONDS);

        s_logger.info("Activating Sense Hat Application... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivating Sense Hat Application...");

        LPS25H.closeDevice();
        HTS221.closeDevice();
        LSM9DS1.closeDevice();

        if (this.joystickhandle != null) {
            this.joystickhandle.cancel(true);
        }
        if (this.joystickworker != null) {
            this.joystickworker.shutdown();
        }
        if (this.senseHatJoystick != null) {
            Joystick.closeJoystick();
        }

        if (this.frameBuffer != null) {
            this.frameBuffer.clearFrameBuffer();
            FrameBuffer.closeFrameBuffer();
            this.frameBuffer = null;
        }

        if (startUpdateThread != null) {
            startUpdateThread.cancel(true);
            startUpdateThread = null;
        }
        this.executor = null;

        s_logger.info("Deactivating Sense Hat Application... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated Sense Hat Application...");

        // store the properties received
        getProperties(properties);

        if (startUpdateThread != null) {
            startUpdateThread.cancel(true);
            startUpdateThread = null;
        }
        startUpdateThread = this.executor.schedule(new Runnable() {

            @Override
            public void run() {
                update();
            }
        }, 0, TimeUnit.MILLISECONDS);

        s_logger.info("Updated Sense Hat Application... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    private void update() {
        if (this.imuAccEnable || this.imuGyroEnable || this.imuCompEnable) {

            this.imuSensor = this.senseHat.getIMUSensor(I2C_BUS, I2C_ACC_ADDRESS, I2C_MAG_ADDRESS, I2C_ADDRESS_SIZE,
                    I2C_FREQUENCY);
            boolean status = this.imuSensor.initDevice(this.imuAccEnable, this.imuGyroEnable, this.imuCompEnable);
            if (!status) {
                s_logger.error("Unable to initialize IMU sensor.");
            } else {
                if (this.imuAccEnable) {
                    float[] acc = new float[3];
                    for (int i = 0; i < this.imuSamples; i++) {
                        acc = this.imuSensor.getAccelerometerRaw();
                    }
                    s_logger.info("Acceleration X : " + acc[0] + " Y : " + acc[1] + " Z : " + acc[2]);
                }
                if (this.imuGyroEnable) {
                    float[] gyro = new float[3];
                    for (int i = 0; i < this.imuSamples; i++) {
                        gyro = this.imuSensor.getGyroscopeRaw();
                    }
                    s_logger.info("Orientation X : " + gyro[0] + " Y : " + gyro[1] + " Z : " + gyro[2]);
                }
                if (this.imuCompEnable) {
                    float[] comp = new float[3];
                    for (int i = 0; i < this.imuSamples; i++) {
                        comp = this.imuSensor.getCompassRaw();
                    }
                    s_logger.info("Compass X : " + comp[0] + " Y : " + comp[1] + " Z : " + comp[2]);
                }
            }
        } else {
            LSM9DS1.closeDevice();
        }

        if (this.preEnable) {

            this.pressureSensor = this.senseHat.getPressureSensor(I2C_BUS, I2C_PRE_ADDRESS, I2C_ADDRESS_SIZE,
                    I2C_FREQUENCY);
            boolean status = this.pressureSensor.initDevice();
            if (!status) {
                s_logger.error("Unable to initialize pressure sensor.");
            } else {
                s_logger.info("Pressure : {}", this.pressureSensor.getPressure());
                s_logger.info("Temperature : {}", this.pressureSensor.getTemperature());
            }

        } else {
            LPS25H.closeDevice();
        }

        if (this.humEnable) {

            this.humiditySensor = this.senseHat.getHumiditySensor(I2C_BUS, I2C_HUM_ADDRESS, I2C_ADDRESS_SIZE,
                    I2C_FREQUENCY);
            boolean status = this.humiditySensor.initDevice();
            if (!status) {
                s_logger.error("Unable to initialize humidity sensor.");
            } else {
                s_logger.info("Humidity : {}", this.humiditySensor.getHumidity());
                s_logger.info("Temperature : {}", this.humiditySensor.getTemperature());
            }

        } else {
            HTS221.closeDevice();
        }

        if (this.lcdEnable) {

            this.frameBuffer = this.senseHat.getFrameBuffer();
            FrameBuffer.setRotation(this.screenRotation);
            this.frameBuffer.showMessage(this.screenMessage, this.screenTextColor, Colors.BLACK);

        } else {
            if (this.frameBuffer != null) {
                this.frameBuffer.clearFrameBuffer();
                FrameBuffer.closeFrameBuffer();
                this.frameBuffer = null;
            }
        }

        if (this.stickEnable) {

            this.senseHatJoystick = this.senseHat.getJoystick();
            this.runThread = true;

            this.joystickworker = Executors.newSingleThreadScheduledExecutor();
            this.joystickhandle = this.joystickworker.submit(new Runnable() {

                @Override
                public void run() {

                    while (SenseHatExample.this.runThread) {
                        SenseHatExample.this.je = SenseHatExample.this.senseHatJoystick.read();
                        logJoystick(SenseHatExample.this.je);
                    }

                }
            });

        } else {
            this.runThread = false;
            if (this.joystickhandle != null) {
                this.joystickhandle.cancel(true);
            }
            if (this.joystickworker != null) {
                this.joystickworker.shutdownNow();
            }
            if (this.senseHatJoystick != null) {
                Joystick.closeJoystick();
            }
        }
    }

    private void logJoystick(JoystickEvent je) {

        if (je.getCode() == Joystick.KEY_ENTER) {
            if (je.getValue() == Joystick.STATE_PRESS) {
                s_logger.info("Enter key pressed.");
            } else if (je.getValue() == Joystick.STATE_RELEASE) {
                s_logger.info("Enter key released.");
            } else if (je.getValue() == Joystick.STATE_HOLD) {
                s_logger.info("Enter key held.");
            }
        } else if (je.getCode() == Joystick.KEY_LEFT) {
            if (je.getValue() == Joystick.STATE_PRESS) {
                s_logger.info("Lef key pressed.");
            } else if (je.getValue() == Joystick.STATE_RELEASE) {
                s_logger.info("Left key released.");
            } else if (je.getValue() == Joystick.STATE_HOLD) {
                s_logger.info("Left key held.");
            }
        } else if (je.getCode() == Joystick.KEY_RIGHT) {
            if (je.getValue() == Joystick.STATE_PRESS) {
                s_logger.info("Right key pressed.");
            } else if (je.getValue() == Joystick.STATE_RELEASE) {
                s_logger.info("Right key released.");
            } else if (je.getValue() == Joystick.STATE_HOLD) {
                s_logger.info("Right key held.");
            }
        } else if (je.getCode() == Joystick.KEY_UP) {
            if (je.getValue() == Joystick.STATE_PRESS) {
                s_logger.info("Up key pressed.");
            } else if (je.getValue() == Joystick.STATE_RELEASE) {
                s_logger.info("Up key released.");
            } else if (je.getValue() == Joystick.STATE_HOLD) {
                s_logger.info("Up key held.");
            }
        }
        if (je.getCode() == Joystick.KEY_DOWN) {
            if (je.getValue() == Joystick.STATE_PRESS) {
                s_logger.info("Down key pressed.");
            } else if (je.getValue() == Joystick.STATE_RELEASE) {
                s_logger.info("Down key released.");
            } else if (je.getValue() == Joystick.STATE_HOLD) {
                s_logger.info("Down key held.");
            }
        }

    }

    private void getProperties(Map<String, Object> properties) {

        this.properties = properties;
        if (this.properties.get(IMU_ACC_ENABLE) != null) {
            this.imuAccEnable = (Boolean) this.properties.get(IMU_ACC_ENABLE);
        }
        if (this.properties.get(IMU_GYRO_ENABLE) != null) {
            this.imuGyroEnable = (Boolean) this.properties.get(IMU_GYRO_ENABLE);
        }
        if (this.properties.get(IMU_COMP_ENABLE) != null) {
            this.imuCompEnable = (Boolean) this.properties.get(IMU_COMP_ENABLE);
        }
        if (this.properties.get(IMU_SAMPLES) != null) {
            this.imuSamples = (Integer) this.properties.get(IMU_SAMPLES);
        }
        if (this.properties.get(PRE_ENABLE) != null) {
            this.preEnable = (Boolean) this.properties.get(PRE_ENABLE);
        }
        if (this.properties.get(HUM_ENABLE) != null) {
            this.humEnable = (Boolean) this.properties.get(HUM_ENABLE);
        }
        if (this.properties.get(LCD_ENABLE) != null) {
            this.lcdEnable = (Boolean) this.properties.get(LCD_ENABLE);
        }
        if (this.properties.get(STICK_ENABLE) != null) {
            this.stickEnable = (Boolean) this.properties.get(STICK_ENABLE);
        }
        if (this.properties.get(SCREEN_MESSAGE) != null) {
            this.screenMessage = (String) this.properties.get(SCREEN_MESSAGE);
        }
        if (this.properties.get(SCREEN_ROTATION) != null) {
            this.screenRotation = (Integer) this.properties.get(SCREEN_ROTATION);
        }
        if (this.properties.get(SCREEN_TEXT_COLOR) != null) {
            if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("RED")) {
                this.screenTextColor = Colors.RED;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("ORANGE")) {
                this.screenTextColor = Colors.ORANGE;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("YELLOW")) {
                this.screenTextColor = Colors.YELLOW;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("GREEN")) {
                this.screenTextColor = Colors.GREEN;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("BLUE")) {
                this.screenTextColor = Colors.BLUE;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("PURPLE")) {
                this.screenTextColor = Colors.PURPLE;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("VIOLET")) {
                this.screenTextColor = Colors.VIOLET;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("WHITE")) {
                this.screenTextColor = Colors.WHITE;
            } else if (((String) this.properties.get(SCREEN_TEXT_COLOR)).contains("BLACK")) {
                this.screenTextColor = Colors.BLACK;
            }
        }

    }

}
