/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.example.gpio.led;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class LedOptions {

    private static final String PROPRTY_LED_NOME = "switchLed";

    private static final boolean PROPRTY_LED_DEFAULT = false;

    private final boolean enableLed;

    public LedOptions(Map<String, Object> properties) {

        requireNonNull(properties, "Required not null");
        this.enableLed = getProperty(properties, PROPRTY_LED_NOME, PROPRTY_LED_DEFAULT);

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
