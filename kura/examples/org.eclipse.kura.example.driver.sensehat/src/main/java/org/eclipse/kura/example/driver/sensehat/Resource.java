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

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.JoystickEvent;

public enum Resource {
    LED_MATRIX_CHARS,
    LED_MATRIX_FB_RGB565,
    LED_MATRIX_FB_MONOCHROME,
    LED_MATRIX_ROTATION,
    LED_MATRIX_CLEAR,
    LED_MATRIX_FRONT_COLOR_R,
    LED_MATRIX_FRONT_COLOR_G,
    LED_MATRIX_FRONT_COLOR_B,
    LED_MATRIX_BACK_COLOR_R,
    LED_MATRIX_BACK_COLOR_G,
    LED_MATRIX_BACK_COLOR_B,
    HUMIDITY(Sensor.HUMIDITY),
    PRESSURE(Sensor.PRESSURE),
    TEMPERATURE_FROM_HUMIDITY(Sensor.TEMPERATURE_FROM_HUMIDITY),
    TEMPERATURE_FROM_PRESSURE(Sensor.TEMPERATURE_FROM_PRESSURE),
    ACCELERATION_X(Sensor.ACCELEROMETER),
    ACCELERATION_Y(Sensor.ACCELEROMETER),
    ACCELERATION_Z(Sensor.ACCELEROMETER),
    MAGNETOMETER_X(Sensor.MAGNETOMETER),
    MAGNETOMETER_Y(Sensor.MAGNETOMETER),
    MAGNETOMETER_Z(Sensor.MAGNETOMETER),
    GYROSCOPE_X(Sensor.GYROSCOPE),
    GYROSCOPE_Y(Sensor.GYROSCOPE),
    GYROSCOPE_Z(Sensor.GYROSCOPE),
    JOYSTICK_ENTER_PRESS(Joystick.KEY_ENTER, Joystick.STATE_PRESS),
    JOYSTICK_ENTER_RELEASE(Joystick.KEY_ENTER, Joystick.STATE_RELEASE),
    JOYSTICK_ENTER_HOLD(Joystick.KEY_ENTER, Joystick.STATE_HOLD),
    JOYSTICK_LEFT_PRESS(Joystick.KEY_LEFT, Joystick.STATE_PRESS),
    JOYSTICK_LEFT_RELEASE(Joystick.KEY_LEFT, Joystick.STATE_RELEASE),
    JOYSTICK_LEFT_HOLD(Joystick.KEY_LEFT, Joystick.STATE_HOLD),
    JOYSTICK_RIGHT_PRESS(Joystick.KEY_RIGHT, Joystick.STATE_PRESS),
    JOYSTICK_RIGHT_RELEASE(Joystick.KEY_RIGHT, Joystick.STATE_RELEASE),
    JOYSTICK_RIGHT_HOLD(Joystick.KEY_RIGHT, Joystick.STATE_HOLD),
    JOYSTICK_UP_PRESS(Joystick.KEY_UP, Joystick.STATE_PRESS),
    JOYSTICK_UP_RELEASE(Joystick.KEY_UP, Joystick.STATE_RELEASE),
    JOYSTICK_UP_HOLD(Joystick.KEY_UP, Joystick.STATE_HOLD),
    JOYSTICK_DOWN_PRESS(Joystick.KEY_DOWN, Joystick.STATE_PRESS),
    JOYSTICK_DOWN_RELEASE(Joystick.KEY_DOWN, Joystick.STATE_RELEASE),
    JOYSTICK_DOWN_HOLD(Joystick.KEY_DOWN, Joystick.STATE_HOLD);

    private final Optional<Sensor> associatedSensor;
    private final Optional<Integer> joystickKey;
    private final Optional<Integer> joystickState;

    private Resource() {
        this.associatedSensor = Optional.empty();
        this.joystickKey = Optional.empty();
        this.joystickState = Optional.empty();
    }

    private Resource(Sensor associatedSensor) {
        this.associatedSensor = Optional.of(associatedSensor);
        this.joystickKey = Optional.empty();
        this.joystickState = Optional.empty();
    }

    private Resource(int joystickKey, int joystickState) {
        this.associatedSensor = Optional.empty();
        this.joystickKey = Optional.of(joystickKey);
        this.joystickState = Optional.of(joystickState);
    }

    public static Resource from(Map<String, Object> channelRecordProperties) {
        final String channelType = (String) channelRecordProperties
                .get(SenseHatChannelDescriptor.SENSEHAT_RESOURCE_PROP_NAME);

        return Resource.valueOf(channelType);
    }

    public static Optional<Resource> from(JoystickEvent event) {
        final int key = event.getCode();
        final int state = event.getValue();

        if (key == Joystick.KEY_UP) {
            if (state == Joystick.STATE_PRESS) {
                return Optional.of(JOYSTICK_UP_PRESS);
            } else if (state == Joystick.STATE_RELEASE) {
                return Optional.of(JOYSTICK_UP_RELEASE);
            } else if (state == Joystick.STATE_HOLD) {
                return Optional.of(JOYSTICK_UP_HOLD);
            }
        } else if (key == Joystick.KEY_DOWN) {
            if (state == Joystick.STATE_PRESS) {
                return Optional.of(JOYSTICK_DOWN_PRESS);
            } else if (state == Joystick.STATE_RELEASE) {
                return Optional.of(JOYSTICK_DOWN_RELEASE);
            } else if (state == Joystick.STATE_HOLD) {
                return Optional.of(JOYSTICK_DOWN_HOLD);
            }
        } else if (key == Joystick.KEY_LEFT) {
            if (state == Joystick.STATE_PRESS) {
                return Optional.of(JOYSTICK_LEFT_PRESS);
            } else if (state == Joystick.STATE_RELEASE) {
                return Optional.of(JOYSTICK_LEFT_RELEASE);
            } else if (state == Joystick.STATE_HOLD) {
                return Optional.of(JOYSTICK_LEFT_HOLD);
            }
        } else if (key == Joystick.KEY_RIGHT) {
            if (state == Joystick.STATE_PRESS) {
                return Optional.of(JOYSTICK_RIGHT_PRESS);
            } else if (state == Joystick.STATE_RELEASE) {
                return Optional.of(JOYSTICK_RIGHT_RELEASE);
            } else if (state == Joystick.STATE_HOLD) {
                return Optional.of(JOYSTICK_RIGHT_HOLD);
            }
        } else if (key == Joystick.KEY_ENTER) {
            if (state == Joystick.STATE_PRESS) {
                return Optional.of(JOYSTICK_ENTER_PRESS);
            } else if (state == Joystick.STATE_RELEASE) {
                return Optional.of(JOYSTICK_ENTER_RELEASE);
            } else if (state == Joystick.STATE_HOLD) {
                return Optional.of(JOYSTICK_ENTER_HOLD);
            }
        }
        return Optional.empty();
    }

    public Optional<Sensor> getAssociatedSensor() {
        return associatedSensor;
    }

    public Optional<Integer> getJoystickKey() {
        return joystickKey;
    }

    public Optional<Integer> getJoystickState() {
        return joystickState;
    }

    public boolean isSensorResource() {
        return associatedSensor.isPresent();
    }

    public boolean isJoystickEvent() {
        return joystickKey.isPresent();
    }

    public boolean isFramebufferResource() {
        return !(isSensorResource() || isJoystickEvent());
    }

    public enum Sensor {
        ACCELEROMETER,
        GYROSCOPE,
        MAGNETOMETER,
        PRESSURE,
        HUMIDITY,
        TEMPERATURE_FROM_HUMIDITY,
        TEMPERATURE_FROM_PRESSURE
    }
}
