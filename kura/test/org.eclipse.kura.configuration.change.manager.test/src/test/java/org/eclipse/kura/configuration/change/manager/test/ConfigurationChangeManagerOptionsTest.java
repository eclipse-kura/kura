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

package org.eclipse.kura.configuration.change.manager.test;

import static org.eclipse.kura.configuration.change.manager.ConfigurationChangeManagerOptions.DEFAULT_ENABLED;
import static org.eclipse.kura.configuration.change.manager.ConfigurationChangeManagerOptions.DEFAULT_SEND_DELAY;
import static org.eclipse.kura.configuration.change.manager.ConfigurationChangeManagerOptions.KEY_ENABLED;
import static org.eclipse.kura.configuration.change.manager.ConfigurationChangeManagerOptions.KEY_SEND_DELAY;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.configuration.change.manager.ConfigurationChangeManagerOptions;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationChangeManagerOptionsTest {

    private Map<String, Object> properties;
    private ConfigurationChangeManagerOptions options;
    private Object returnValue;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnEnabled() {
        givenProperty(KEY_ENABLED, true);
        givenOptionsInstatiated();

        whenIsEnabled();

        thenReturnValueIs(true);
    }

    @Test
    public void shouldReturnDisabled() {
        givenProperty(KEY_ENABLED, false);
        givenOptionsInstatiated();

        whenIsEnabled();

        thenReturnValueIs(false);
    }

    @Test
    public void shouldReturnDefaultEnabledValue() {
        givenOptionsInstatiated();

        whenIsEnabled();

        thenReturnValueIs(DEFAULT_ENABLED);
    }

    @Test
    public void shouldReturnCorrectSendDelay() {
        givenProperty(KEY_SEND_DELAY, 1000L);
        givenOptionsInstatiated();

        whenGetSendDelay();

        thenReturnValueIs(1000L);
    }

    @Test
    public void shouldReturnDefaultSendDelay() {
        givenOptionsInstatiated();

        whenGetSendDelay();

        thenReturnValueIs(DEFAULT_SEND_DELAY);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    private void givenOptionsInstatiated() {
        this.options = new ConfigurationChangeManagerOptions(this.properties);
    }

    /*
     * When
     */

    private void whenIsEnabled() {
        this.returnValue = this.options.isEnabled();
    }

    private void whenGetSendDelay() {
        this.returnValue = this.options.getSendDelay();
    }

    /*
     * Then
     */

    @SuppressWarnings("unchecked")
    private <T> void thenReturnValueIs(T expectedValue) {
        assertEquals(expectedValue, (T) this.returnValue);
    }


    /*
     * Utility
     */

    @Before
    public void cleanUp() {
        this.properties = new HashMap<>();
        this.options = null;
        this.returnValue = null;
    }

}
