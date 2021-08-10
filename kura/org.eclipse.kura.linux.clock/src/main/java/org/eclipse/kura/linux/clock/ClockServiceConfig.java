/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.clock;

import java.util.Map;

import org.eclipse.kura.util.configuration.Property;

public class ClockServiceConfig {

    private static final Property<Boolean> PROPERTY_ENABLE = new Property<>("enabled", true);

    private static final Property<Boolean> PROPERTY_HW_CLOCK_ENABLED = new Property<>("clock.set.hwclock", true);
    private static final Property<String> PROPERTY_CLOCK_PROVIDER = new Property<>("clock.provider", "java-ntp");
    private static final Property<String> PROPERTY_NTP_HOST = new Property<>("clock.ntp.host", "0.pool.ntp.org");
    private static final Property<Integer> PROPERTY_NTP_PORT = new Property<>("clock.ntp.port", 123);
    private static final Property<Integer> PROPERTY_NTP_TIMEOUT = new Property<>("clock.ntp.timeout", 10000);
    private static final Property<Integer> PROPERTY_NTP_MAX_RETRIES = new Property<>("clock.ntp.max-retry", 0);
    private static final Property<Integer> PROPERTY_NTP_RETRY_INTERVAL = new Property<>("clock.ntp.max-retry", 5);
    private static final Property<Integer> PROPERTY_NTP_REFRESH_INTERVAL = new Property<>("clock.ntp.refresh-interval",
            3600);
    private static final Property<String> PROPERTY_RTC_FILENAME = new Property<>("rtc.filename", "/dev/rtc0");
    private static final Property<String> PROPERTY_CHRONY_ADVANCED_CONFIG = new Property<>("chrony.advanced.config",
            "");

    private final boolean enabled;
    private final boolean hwclockEnabled;
    private final String clockProvider;
    private final String ntpHost;
    private final int ntpPort;
    private final int ntpTimeout;
    private final int ntpMaxRetries;
    private final int ntpRetryInterval;
    private final int ntpRefreshInterval;
    private final String rtcFilename;
    private final String chronyAdvancedConfig;

    public ClockServiceConfig(Map<String, Object> properties) {
        this.enabled = PROPERTY_ENABLE.get(properties);
        this.hwclockEnabled = PROPERTY_HW_CLOCK_ENABLED.get(properties);
        this.clockProvider = PROPERTY_CLOCK_PROVIDER.get(properties);
        this.ntpHost = PROPERTY_NTP_HOST.get(properties);
        this.ntpPort = PROPERTY_NTP_PORT.get(properties);
        this.ntpTimeout = PROPERTY_NTP_TIMEOUT.get(properties);
        this.ntpMaxRetries = PROPERTY_NTP_MAX_RETRIES.get(properties);
        this.ntpRetryInterval = PROPERTY_NTP_RETRY_INTERVAL.get(properties);
        this.ntpRefreshInterval = PROPERTY_NTP_REFRESH_INTERVAL.get(properties);
        this.rtcFilename = PROPERTY_RTC_FILENAME.get(properties);
        this.chronyAdvancedConfig = PROPERTY_CHRONY_ADVANCED_CONFIG.get(properties);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isHwclockEnabled() {
        return this.hwclockEnabled;
    }

    public String getClockProvider() {
        return this.clockProvider;
    }

    public String getNtpHost() {
        return this.ntpHost;
    }

    public int getNtpPort() {
        return this.ntpPort;
    }

    public int getNtpTimeout() {
        return this.ntpTimeout;
    }

    public int getNtpMaxRetries() {
        return this.ntpMaxRetries;
    }

    public int getNtpRetryInterval() {
        return this.ntpRetryInterval;
    }

    public int getNtpRefreshInterval() {
        return this.ntpRefreshInterval;
    }

    public String getRtcFilename() {
        return this.rtcFilename;
    }

    public String getChronyAdvancedConfig() {
        return this.chronyAdvancedConfig;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.chronyAdvancedConfig == null ? 0 : this.chronyAdvancedConfig.hashCode());
        result = prime * result + (this.clockProvider == null ? 0 : this.clockProvider.hashCode());
        result = prime * result + (this.enabled ? 1231 : 1237);
        result = prime * result + (this.hwclockEnabled ? 1231 : 1237);
        result = prime * result + (this.ntpHost == null ? 0 : this.ntpHost.hashCode());
        result = prime * result + this.ntpMaxRetries;
        result = prime * result + this.ntpPort;
        result = prime * result + this.ntpRefreshInterval;
        result = prime * result + this.ntpRetryInterval;
        result = prime * result + this.ntpTimeout;
        result = prime * result + (this.rtcFilename == null ? 0 : this.rtcFilename.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ClockServiceConfig other = (ClockServiceConfig) obj;
        if (this.chronyAdvancedConfig == null) {
            if (other.chronyAdvancedConfig != null) {
                return false;
            }
        } else if (!this.chronyAdvancedConfig.equals(other.chronyAdvancedConfig)) {
            return false;
        }
        if (this.clockProvider == null) {
            if (other.clockProvider != null) {
                return false;
            }
        } else if (!this.clockProvider.equals(other.clockProvider)) {
            return false;
        }
        if (this.enabled != other.enabled) {
            return false;
        }
        if (this.hwclockEnabled != other.hwclockEnabled) {
            return false;
        }
        if (this.ntpHost == null) {
            if (other.ntpHost != null) {
                return false;
            }
        } else if (!this.ntpHost.equals(other.ntpHost)) {
            return false;
        }
        if (this.ntpMaxRetries != other.ntpMaxRetries) {
            return false;
        }
        if (this.ntpPort != other.ntpPort) {
            return false;
        }
        if (this.ntpRefreshInterval != other.ntpRefreshInterval) {
            return false;
        }
        if (this.ntpRetryInterval != other.ntpRetryInterval) {
            return false;
        }
        if (this.ntpTimeout != other.ntpTimeout) {
            return false;
        }
        if (this.rtcFilename == null) {
            if (other.rtcFilename != null) {
                return false;
            }
        } else if (!this.rtcFilename.equals(other.rtcFilename)) {
            return false;
        }
        return true;
    }

}
