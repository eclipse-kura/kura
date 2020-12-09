/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.watchdog;

import java.util.Map;

public class WatchdogServiceOptions {

    private static final ConfigurationProperty<Boolean> PROPERTY_ENABLED = new ConfigurationProperty<>("enabled",
            false);
    private static final ConfigurationProperty<Integer> PROPERTY_PING_INTERVAL = new ConfigurationProperty<>(
            "pingInterval", 10000);
    private static final ConfigurationProperty<String> PROPERTY_WD_DEVICE = new ConfigurationProperty<>(
            "watchdogDevice", "/dev/watchdog");
    private static final ConfigurationProperty<String> PROPERTY_REBOOT_CAUSE_FILE_PATH = new ConfigurationProperty<>(
            "rebootCauseFilePath", "/opt/eclipse/kura/data/kura-reboot-cause");

    private static final String WD_ENABLED_TEMPORARY_FILE_PATH = "/tmp/watchdog";

    private final Map<String, Object> properties;

    public WatchdogServiceOptions(Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return PROPERTY_ENABLED.get(this.properties);
    }

    public Integer getPingInterval() {
        return PROPERTY_PING_INTERVAL.get(this.properties);
    }

    public String getWatchdogDevice() {
        return PROPERTY_WD_DEVICE.get(this.properties);
    }

    public String getRebootCauseFilePath() {
        return PROPERTY_REBOOT_CAUSE_FILE_PATH.get(this.properties);
    }

    public String getWatchdogEnabledTemporaryFilePath() {
        return WD_ENABLED_TEMPORARY_FILE_PATH;
    }

    private static class ConfigurationProperty<T> {

        private final String key;
        private final T defaultValue;

        public ConfigurationProperty(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(Map<String, Object> properties) {
            final Object value = properties.get(this.key);
            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return this.defaultValue;
        }
    }
}
