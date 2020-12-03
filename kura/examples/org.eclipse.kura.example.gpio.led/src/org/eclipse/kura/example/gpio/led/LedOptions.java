/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.example.gpio.led;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class LedOptions {

    private static final String PROPRTY_PIN_NOME = "configurePin";
    private static final int PROPRTY_PIN_DEFAULT = 6;

    private static final String PROPRTY_LED_NOME = "switchLed";
    private static final boolean PROPRTY_LED_DEFAULT = false;

    private final int configPin;
    private final boolean enableLed;

    public LedOptions(Map<String, Object> properties) {

        requireNonNull(properties, "Required not null");
        this.configPin = getProperty(properties, PROPRTY_PIN_NOME, PROPRTY_PIN_DEFAULT);
        this.enableLed = getProperty(properties, PROPRTY_LED_NOME, PROPRTY_LED_DEFAULT);

    }

    public int isConfigPin() {
        return this.configPin;
    }

    public boolean isEnableLed() {
        return this.enableLed;
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
