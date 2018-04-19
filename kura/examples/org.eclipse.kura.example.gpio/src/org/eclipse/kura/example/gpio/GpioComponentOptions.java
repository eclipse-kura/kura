/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.example.gpio;

import static java.util.Objects.requireNonNull;

import java.util.Map;

final class GpioComponentOptions {

    protected static final String INPUT_READ_MODE_PIN_STATUS_LISTENER = "PIN_STATUS_LISTENER";
    protected static final String INPUT_READ_MODE_POLLING = "POLLING";

    private static final String PROP_NAME_GPIO_SERVICE_PID = "gpio.service.pid";
    private static final String PROP_NAME_INPUT_READ_MODE = "gpio.input.read.mode";
    private static final String PROP_NAME_GPIO_PINS = "gpio.pins";
    private static final String PROP_NAME_GPIO_DIRECTIONS = "gpio.directions";
    private static final String PROP_NAME_GPIO_MODES = "gpio.modes";
    private static final String PROP_NAME_GPIO_TRIGGERS = "gpio.triggers";

    private static final String DEFAULT_GPIO_SERVICE_PID = "org.eclipse.kura.gpio.GPIOService";
    private static final String DEFAULT_INPUT_READ_MODE = INPUT_READ_MODE_PIN_STATUS_LISTENER;
    private static final String[] DEFAULT_GPIO_PINS = {};
    private static final Integer[] DEFAULT_GPIO_DIRECTIONS = { 3, 3, 3, 3, 3 };
    private static final Integer[] DEFAULT_GPIO_MODES = { -1, -1, -1, -1, -1 };
    private static final Integer[] DEFAULT_GPIO_TRIGGERS = { -1, -1, -1, -1, -1 };

    private String gpioServicePid;
    private String inputReadMode;
    private String[] pins;
    private Integer[] directions;
    private Integer[] modes;
    private Integer[] triggers;

    public GpioComponentOptions(Map<String, Object> properties) {
        requireNonNull(properties, "Required not null");
        this.gpioServicePid = getProperty(properties, PROP_NAME_GPIO_SERVICE_PID, DEFAULT_GPIO_SERVICE_PID);
        this.inputReadMode = getProperty(properties, PROP_NAME_INPUT_READ_MODE, DEFAULT_INPUT_READ_MODE);
        this.pins = getProperty(properties, PROP_NAME_GPIO_PINS, DEFAULT_GPIO_PINS);
        this.directions = getProperty(properties, PROP_NAME_GPIO_DIRECTIONS, DEFAULT_GPIO_DIRECTIONS);
        this.modes = getProperty(properties, PROP_NAME_GPIO_MODES, DEFAULT_GPIO_MODES);
        this.triggers = getProperty(properties, PROP_NAME_GPIO_TRIGGERS, DEFAULT_GPIO_TRIGGERS);
    }

    public String getGpioServicePid() {
        return gpioServicePid;
    }

    public String getInputReadMode() {
        return inputReadMode;
    }

    public String[] getPins() {
        return pins;
    }

    public Integer[] getDirections() {
        return directions;
    }

    public Integer[] getModes() {
        return modes;
    }

    public Integer[] getTriggers() {
        return triggers;
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(Map<String, Object> properties, String propertyName, T defaultValue) {
        Object prop = properties.getOrDefault(propertyName, defaultValue);
        if (prop != null && prop.getClass().isAssignableFrom(defaultValue.getClass())) {
            return (T) prop;
        } else {
            return defaultValue;
        }
    }

}