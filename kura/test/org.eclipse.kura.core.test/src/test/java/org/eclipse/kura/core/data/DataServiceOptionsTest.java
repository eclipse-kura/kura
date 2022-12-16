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

import org.junit.Test;

public class DataServiceOptionsTest {

    private DataServiceOptions dataServiceOptions;
    Map<String, Object> properties;

    /*
     * Scenarios
     */

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

}
