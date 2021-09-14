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
package org.eclipse.kura.core.data;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.H2DbService;

public class DataServiceOptions {

    public static final String AUTOCONNECT_PROP_NAME = "connect.auto-on-startup";
    public static final String CONNECT_DELAY_PROP_NAME = "connect.retry-interval";
    public static final String DISCONNECT_DELAY_PROP_NAME = "disconnect.quiesce-timeout";
    public static final String STORE_DB_SERVICE_INSTANCE_PROP_NAME = "store.db.service.pid";
    public static final String STORE_HOUSEKEEPER_INTERVAL_PROP_NAME = "store.housekeeper-interval";
    public static final String STORE_PURGE_AGE_PROP_NAME = "store.purge-age";
    public static final String STORE_CAPACITY_PROP_NAME = "store.capacity";
    public static final String REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.republish-on-new-session";
    public static final String MAX_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.max-number";
    public static final String IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME = "in-flight-messages.congestion-timeout";
    public static final String RATE_LIMIT_ENABLE_PROP_NAME = "enable.rate.limit";
    public static final String RATE_LIMIT_AVERAGE_RATE_PROP_NAME = "rate.limit.average";
    public static final String RATE_LIMIT_TIME_UNIT_PROP_NAME = "rate.limit.time.unit";
    public static final String RATE_LIMIT_BURST_SIZE_PROP_NAME = "rate.limit.burst.size";
    public static final String RECOVERY_ENABLE_PROP_NAME = "enable.recovery.on.connection.failure";
    public static final String RECOVERY_MAX_FAILURES_PROP_NAME = "connection.recovery.max.failures";

    public static final boolean AUTOCONNECT_PROP_DEFAULT = false;
    public static final int CONNECT_DELAY_DEFAULT = 60;
    public static final int DISCONNECT_DELAY_DEFAULT = 10;
    public static final String DB_SERVICE_INSTANCE_DEFAULT = H2DbService.DEFAULT_INSTANCE_PID;
    public static final int STORE_HOUSEKEEPER_INTERVAL_DEFAULT = 900;
    public static final int STORE_PURGE_AGE_DEFAULT = 60;
    public static final int STORE_CAPACITY_DEFAULT = 10000;
    public static final boolean REPUBLISH_IN_FLIGHT_MSGS_DEFAULT = true;
    public static final int MAX_IN_FLIGHT_MSGS_DEFAULT = 9;
    public static final int IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_DEFAULT = 0;
    public static final boolean RATE_LIMIT_ENABLE_DEFAULT = true;
    public static final int RATE_LIMIT_AVERAGE_RATE_DEFAULT = 1;
    public static final String RATE_LIMIT_TIME_UNIT_DEFAULT = "SECONDS";
    public static final int RATE_LIMIT_BURST_SIZE_DEFAULT = 1;
    public static final boolean RECOVERY_ENABLE_DEFAULT = true;
    public static final int RECOVERY_MAX_FAILURES_DEFAULT = 10;

    public static final int CONNECT_CRITICAL_COMPONENT_TIMEOUT_MULTIPLIER = 5000;

    private final Map<String, Object> properties;

    DataServiceOptions(Map<String, Object> properties) {
        requireNonNull(properties, "Required not null");
        this.properties = Collections.unmodifiableMap(properties);
    }

    int getStoreHousekeeperInterval() {
        return (int) this.properties.getOrDefault(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME,
                STORE_HOUSEKEEPER_INTERVAL_DEFAULT);
    }

    int getStorePurgeAge() {
        return (int) this.properties.getOrDefault(STORE_PURGE_AGE_PROP_NAME, STORE_PURGE_AGE_DEFAULT);
    }

    int getStoreCapacity() {
        return (int) this.properties.getOrDefault(STORE_CAPACITY_PROP_NAME, STORE_CAPACITY_DEFAULT);
    }

    boolean isPublishInFlightMessages() {
        return (boolean) this.properties.getOrDefault(REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME,
                REPUBLISH_IN_FLIGHT_MSGS_DEFAULT);
    }

    int getMaxInFlightMessages() {
        return (int) this.properties.getOrDefault(MAX_IN_FLIGHT_MSGS_PROP_NAME, MAX_IN_FLIGHT_MSGS_DEFAULT);
    }

    int getInFlightMessagesCongestionTimeout() {
        return (int) this.properties.getOrDefault(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME,
                IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_DEFAULT);
    }

    boolean isAutoConnect() {
        return (boolean) this.properties.getOrDefault(AUTOCONNECT_PROP_NAME, AUTOCONNECT_PROP_DEFAULT);
    }

    int getConnectDelay() {
        return (int) this.properties.getOrDefault(CONNECT_DELAY_PROP_NAME, CONNECT_DELAY_DEFAULT);
    }

    int getDisconnectDelay() {
        return (int) this.properties.getOrDefault(DISCONNECT_DELAY_PROP_NAME, DISCONNECT_DELAY_DEFAULT);
    }

    boolean isRateLimitEnabled() {
        return (boolean) this.properties.getOrDefault(RATE_LIMIT_ENABLE_PROP_NAME, RATE_LIMIT_ENABLE_DEFAULT);
    }

    int getRateLimitAverageRate() {
        return (int) this.properties.getOrDefault(RATE_LIMIT_AVERAGE_RATE_PROP_NAME, RATE_LIMIT_AVERAGE_RATE_DEFAULT);
    }

    int getRateLimitBurstSize() {
        return (int) this.properties.getOrDefault(RATE_LIMIT_BURST_SIZE_PROP_NAME, RATE_LIMIT_BURST_SIZE_DEFAULT);
    }

    long getRateLimitTimeUnit() {
        String timeUnitString = (String) this.properties.getOrDefault(RATE_LIMIT_TIME_UNIT_PROP_NAME,
                RATE_LIMIT_TIME_UNIT_DEFAULT);
        TimeUnit timeUnit;

        if (TimeUnit.MILLISECONDS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.MILLISECONDS;
        } else if (TimeUnit.SECONDS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.SECONDS;
        } else if (TimeUnit.MINUTES.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.MINUTES;
        } else if (TimeUnit.HOURS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.HOURS;
        } else if (TimeUnit.DAYS.name().equals(timeUnitString)) {
            timeUnit = TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Illegal time unit");
        }

        return timeUnit.toNanos(1);
    }

    String getDbServiceInstancePid() {
        return (String) this.properties.getOrDefault(STORE_DB_SERVICE_INSTANCE_PROP_NAME, DB_SERVICE_INSTANCE_DEFAULT);
    }

    String getKuraServicePid() {
        return (String) this.properties.get(ConfigurationService.KURA_SERVICE_PID);
    }

    boolean isConnectionRecoveryEnabled() {
        return (boolean) this.properties.getOrDefault(RECOVERY_ENABLE_PROP_NAME, RECOVERY_ENABLE_DEFAULT);
    }

    int getRecoveryMaximumAllowedFailures() {
        return (int) this.properties.getOrDefault(RECOVERY_MAX_FAILURES_PROP_NAME, RECOVERY_MAX_FAILURES_DEFAULT);
    }

    int getCriticalComponentTimeout() {
        return getConnectDelay() * CONNECT_CRITICAL_COMPONENT_TIMEOUT_MULTIPLIER;
    }
}
