/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.db.H2DbService;
import org.junit.Test;

public class DataServiceOptionsTest {

    private static final String AUTOCONNECT_PROP_NAME = "connect.auto-on-startup";
    private static final String CONNECT_DELAY_PROP_NAME = "connect.retry-interval";
    private static final String DISCONNECT_DELAY_PROP_NAME = "disconnect.quiesce-timeout";
    private static final String STORE_DB_SERVICE_INSTANCE_PROP_NAME = "store.db.service.pid";
    private static final String STORE_HOUSEKEEPER_INTERVAL_PROP_NAME = "store.housekeeper-interval";
    private static final String STORE_PURGE_AGE_PROP_NAME = "store.purge-age";
    private static final String STORE_CAPACITY_PROP_NAME = "store.capacity";
    private static final String REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.republish-on-new-session";
    private static final String MAX_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.max-number";
    private static final String IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME = "in-flight-messages.congestion-timeout";
    private static final String RATE_LIMIT_ENABLE_PROP_NAME = "enable.rate.limit";
    private static final String RATE_LIMIT_AVERAGE_RATE_PROP_NAME = "rate.limit.average";
    private static final String RATE_LIMIT_TIME_UNIT_PROP_NAME = "rate.limit.time.unit";
    private static final String RATE_LIMIT_BURST_SIZE_PROP_NAME = "rate.limit.burst.size";
    private static final String RECOVERY_ENABLE_PROP_NAME = "enable.recovery.on.connection.failure";
    private static final String RECOVERY_MAX_FAILURES_PROP_NAME = "connection.recovery.max.failures";
    private static final String CONNECTION_SCHEDULE_ENABLED = "connection.schedule.enabled";
    private static final String CONNECTION_SCHECULE_EXPRESSION = "connection.schedule.expression";
    private static final String CONNECTION_SCHEDULE_INACTIVITY_INTERVAL_SECONDS = "connection.schedule.inactivity.interval.seconds";
    private static final String CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE = "connection.schedule.priority.override.enable";
    private static final String CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD = "connection.schedule.priority.override.threshold";

    private static final boolean AUTOCONNECT_PROP_DEFAULT = false;
    private static final int CONNECT_DELAY_DEFAULT = 60;
    private static final int DISCONNECT_DELAY_DEFAULT = 10;
    private static final String DB_SERVICE_INSTANCE_DEFAULT = H2DbService.DEFAULT_INSTANCE_PID;
    private static final int STORE_HOUSEKEEPER_INTERVAL_DEFAULT = 900;
    private static final int STORE_PURGE_AGE_DEFAULT = 60;
    private static final int STORE_CAPACITY_DEFAULT = 10000;
    private static final boolean REPUBLISH_IN_FLIGHT_MSGS_DEFAULT = true;
    private static final int MAX_IN_FLIGHT_MSGS_DEFAULT = 9;
    private static final int IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_DEFAULT = 0;
    private static final boolean RATE_LIMIT_ENABLE_DEFAULT = true;
    private static final int RATE_LIMIT_AVERAGE_RATE_DEFAULT = 1;
    private static final int RATE_LIMIT_BURST_SIZE_DEFAULT = 1;
    private static final boolean RECOVERY_ENABLE_DEFAULT = true;
    private static final int RECOVERY_MAX_FAILURES_DEFAULT = 10;
    private static final boolean CONNECTION_SCHEDULE_ENABLED_DEFAULT = false;
    private static final boolean CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE_DEFAULT = false;
    private static final int CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD_DEFAULT = 1;

    private static final boolean AUTOCONNECT_PROP_CHANGED = true;
    private static final int CONNECT_DELAY_CHANGED = 65;
    private static final int DISCONNECT_DELAY_CHANGED = 15;
    private static final String DB_SERVICE_INSTANCE_CHANGED = H2DbService.DEFAULT_INSTANCE_PID;
    private static final int STORE_HOUSEKEEPER_INTERVAL_CHANGED = 950;
    private static final int STORE_PURGE_AGE_CHANGED = 65;
    private static final int STORE_CAPACITY_CHANGED = 10050;
    private static final boolean REPUBLISH_IN_FLIGHT_MSGS_CHANGED = true;
    private static final int MAX_IN_FLIGHT_MSGS_CHANGED = 5;
    private static final int IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_CHANGED = 1;
    private static final boolean RATE_LIMIT_ENABLE_CHANGED = false;
    private static final int RATE_LIMIT_AVERAGE_RATE_CHANGED = 2;
    private static final int RATE_LIMIT_BURST_SIZE_CHANGED = 2;
    private static final boolean RECOVERY_ENABLE_CHANGED = false;
    private static final int RECOVERY_MAX_FAILURES_CHANGED = 15;
    private static final boolean CONNECTION_SCHEDULE_ENABLED_CHANGED = true;
    private static final boolean CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE_CHANGED = true;
    private static final int CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD_CHANGED = 2;

    private static final int CONNECTION_SCHEDULE_INACTIVITY_INTERVAL_SECONDS_CHANGED = 5;
    private static final String RATE_LIMIT_TIME_UNIT_PROP_NAME_CHANGED = "MILLISECONDS";
    private static final String CONNECTION_SCHECULE_EXPRESSION_CHANGED = "0 0 0 ? * * *";

    private DataServiceOptions dataServiceOptions;
    Map<String, Object> properties;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnDefaultsTest() {
        givenEmptyProperties();
        whenDataServiceOptionsIsCreated();
        thenCheckIfAllDefaultsAreSet();

    }

    @Test
    public void shouldReturnChangedTest() {
        givenFullChangedProperties();
        whenDataServiceOptionsIsCreated();
        thenCheckIfAllChangesAreSet();

    }

    @Test
    public void shouldReturnTrueToPriorityOverrideEnabled() {
        givenPropertiesThatEnableOverrideSchedule();
        whenDataServiceOptionsIsCreated();
        thenConnectionScheduleEnabled();
        thenCheckpriorityEquals(3);
    }

    @Test
    public void shouldReturnTrueToPriorityOverrideDisabled() {
        givenPropertiesThatDisableOverrideSchedule();
        whenDataServiceOptionsIsCreated();
        thenConnectionScheduleDisabled();
        thenCheckpriorityEquals(3);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenPropertiesThatEnableOverrideSchedule() {
        // Create DataServiceOptions
        properties = new HashMap<>();

        properties.put("connection.schedule.priority.override.enable", true);
        properties.put("connection.schedule.priority.override.threshold", 3);
        properties.put("connect.auto-on-startup", true);
        properties.put("connection.schedule.inactivity.interval.seconds", 2);
        properties.put("connection.schedule.enabled", true);
        properties.put("connection.schedule.expression", "0 0 0 ? * * *");
    }

    private void givenPropertiesThatDisableOverrideSchedule() {
        // Create DataServiceOptions
        properties = new HashMap<>();

        properties.put("connection.schedule.priority.override.enable", false);
        properties.put("connection.schedule.priority.override.threshold", 3);
        properties.put("connect.auto-on-startup", false);
        properties.put("connection.schedule.inactivity.interval.seconds", 2);
        properties.put("connection.schedule.enabled", false);
    }

    private void givenEmptyProperties() {
        // Create DataServiceOptions
        properties = new HashMap<>();
    }

    private void givenFullChangedProperties() {
        // Create DataServiceOptions
        properties = new HashMap<>();

        properties.put(AUTOCONNECT_PROP_NAME, AUTOCONNECT_PROP_CHANGED);
        properties.put(CONNECT_DELAY_PROP_NAME, CONNECT_DELAY_CHANGED);
        properties.put(DISCONNECT_DELAY_PROP_NAME, DISCONNECT_DELAY_CHANGED);
        properties.put(STORE_DB_SERVICE_INSTANCE_PROP_NAME, DB_SERVICE_INSTANCE_CHANGED);
        properties.put(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME, STORE_HOUSEKEEPER_INTERVAL_CHANGED);
        properties.put(STORE_PURGE_AGE_PROP_NAME, STORE_PURGE_AGE_CHANGED);
        properties.put(STORE_CAPACITY_PROP_NAME, STORE_CAPACITY_CHANGED);
        properties.put(REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME, REPUBLISH_IN_FLIGHT_MSGS_CHANGED);
        properties.put(MAX_IN_FLIGHT_MSGS_PROP_NAME, MAX_IN_FLIGHT_MSGS_CHANGED);
        properties.put(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME, IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_CHANGED);
        properties.put(RATE_LIMIT_ENABLE_PROP_NAME, RATE_LIMIT_ENABLE_CHANGED);
        properties.put(RATE_LIMIT_AVERAGE_RATE_PROP_NAME, RATE_LIMIT_AVERAGE_RATE_CHANGED);
        properties.put(RATE_LIMIT_TIME_UNIT_PROP_NAME, RATE_LIMIT_TIME_UNIT_PROP_NAME_CHANGED);
        properties.put(RATE_LIMIT_BURST_SIZE_PROP_NAME, RATE_LIMIT_BURST_SIZE_CHANGED);
        properties.put(RECOVERY_ENABLE_PROP_NAME, RECOVERY_ENABLE_CHANGED);
        properties.put(RECOVERY_MAX_FAILURES_PROP_NAME, RECOVERY_MAX_FAILURES_CHANGED);
        properties.put(CONNECTION_SCHEDULE_ENABLED, CONNECTION_SCHEDULE_ENABLED_CHANGED);
        properties.put(CONNECTION_SCHECULE_EXPRESSION, CONNECTION_SCHECULE_EXPRESSION_CHANGED);
        properties.put(CONNECTION_SCHEDULE_INACTIVITY_INTERVAL_SECONDS,
                CONNECTION_SCHEDULE_INACTIVITY_INTERVAL_SECONDS_CHANGED);
        properties.put(CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE,
                CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE_CHANGED);
        properties.put(CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD,
                CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD_CHANGED);
    }

    /*
     * When
     */

    private void whenDataServiceOptionsIsCreated() {
        this.dataServiceOptions = new DataServiceOptions(properties);
    }

    /*
     * Then
     */
    private void thenConnectionScheduleEnabled() {
        assertTrue(this.dataServiceOptions.isConnectionSchedulePriorityOverrideEnabled());
    }

    private void thenConnectionScheduleDisabled() {
        assertFalse(this.dataServiceOptions.isConnectionSchedulePriorityOverrideEnabled());
    }

    private void thenCheckpriorityEquals(int priority) {
        assertEquals(this.dataServiceOptions.getConnectionSchedulePriorityOverridePriority(), priority);
    }

    private void thenCheckIfAllDefaultsAreSet() {

        assertEquals(AUTOCONNECT_PROP_DEFAULT, this.dataServiceOptions.isAutoConnect());
        assertEquals(CONNECT_DELAY_DEFAULT, this.dataServiceOptions.getConnectDelay());
        assertEquals(DISCONNECT_DELAY_DEFAULT, this.dataServiceOptions.getDisconnectDelay());
        assertEquals(DB_SERVICE_INSTANCE_DEFAULT, this.dataServiceOptions.getDbServiceInstancePid());
        assertEquals(STORE_HOUSEKEEPER_INTERVAL_DEFAULT, this.dataServiceOptions.getStoreHousekeeperInterval());
        assertEquals(STORE_PURGE_AGE_DEFAULT, this.dataServiceOptions.getStorePurgeAge());
        assertEquals(STORE_CAPACITY_DEFAULT, this.dataServiceOptions.getStoreCapacity());
        assertEquals(REPUBLISH_IN_FLIGHT_MSGS_DEFAULT, this.dataServiceOptions.isPublishInFlightMessages());
        assertEquals(MAX_IN_FLIGHT_MSGS_DEFAULT, this.dataServiceOptions.getMaxInFlightMessages());
        assertEquals(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_DEFAULT,
                this.dataServiceOptions.getInFlightMessagesCongestionTimeout());
        assertEquals(RATE_LIMIT_ENABLE_DEFAULT, this.dataServiceOptions.isRateLimitEnabled());
        assertEquals(RATE_LIMIT_AVERAGE_RATE_DEFAULT, this.dataServiceOptions.getRateLimitAverageRate());
        assertEquals(RATE_LIMIT_BURST_SIZE_DEFAULT, this.dataServiceOptions.getRateLimitBurstSize());
        assertEquals(RECOVERY_ENABLE_DEFAULT, this.dataServiceOptions.isConnectionRecoveryEnabled());
        assertEquals(RECOVERY_MAX_FAILURES_DEFAULT, this.dataServiceOptions.getRecoveryMaximumAllowedFailures());
        assertEquals(CONNECTION_SCHEDULE_ENABLED_DEFAULT, this.dataServiceOptions.isConnectionScheduleEnabled());
        assertEquals(CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE_DEFAULT,
                this.dataServiceOptions.isConnectionSchedulePriorityOverrideEnabled());
        assertEquals(CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD_DEFAULT,
                this.dataServiceOptions.getConnectionSchedulePriorityOverridePriority());
    }

    private void thenCheckIfAllChangesAreSet() {

        assertEquals(AUTOCONNECT_PROP_CHANGED, this.dataServiceOptions.isAutoConnect());
        assertEquals(CONNECT_DELAY_CHANGED, this.dataServiceOptions.getConnectDelay());
        assertEquals(DISCONNECT_DELAY_CHANGED, this.dataServiceOptions.getDisconnectDelay());
        assertEquals(DB_SERVICE_INSTANCE_CHANGED, this.dataServiceOptions.getDbServiceInstancePid());
        assertEquals(STORE_HOUSEKEEPER_INTERVAL_CHANGED, this.dataServiceOptions.getStoreHousekeeperInterval());
        assertEquals(STORE_PURGE_AGE_CHANGED, this.dataServiceOptions.getStorePurgeAge());
        assertEquals(STORE_CAPACITY_CHANGED, this.dataServiceOptions.getStoreCapacity());
        assertEquals(REPUBLISH_IN_FLIGHT_MSGS_CHANGED, this.dataServiceOptions.isPublishInFlightMessages());
        assertEquals(MAX_IN_FLIGHT_MSGS_CHANGED, this.dataServiceOptions.getMaxInFlightMessages());
        assertEquals(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_CHANGED,
                this.dataServiceOptions.getInFlightMessagesCongestionTimeout());
        assertEquals(RATE_LIMIT_ENABLE_CHANGED, this.dataServiceOptions.isRateLimitEnabled());
        assertEquals(RATE_LIMIT_AVERAGE_RATE_CHANGED, this.dataServiceOptions.getRateLimitAverageRate());
        assertEquals(RATE_LIMIT_BURST_SIZE_CHANGED, this.dataServiceOptions.getRateLimitBurstSize());
        assertEquals(RECOVERY_ENABLE_CHANGED, this.dataServiceOptions.isConnectionRecoveryEnabled());
        assertEquals(RECOVERY_MAX_FAILURES_CHANGED, this.dataServiceOptions.getRecoveryMaximumAllowedFailures());
        assertEquals(CONNECTION_SCHEDULE_ENABLED_CHANGED, this.dataServiceOptions.isConnectionScheduleEnabled());
        assertEquals(CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_ENABLE_CHANGED,
                this.dataServiceOptions.isConnectionSchedulePriorityOverrideEnabled());
        assertEquals(CONNECTION_SCHEDULE_PRIORITY_OVERRIDE_THRESHOLD_CHANGED,
                this.dataServiceOptions.getConnectionSchedulePriorityOverridePriority());
    }
}
