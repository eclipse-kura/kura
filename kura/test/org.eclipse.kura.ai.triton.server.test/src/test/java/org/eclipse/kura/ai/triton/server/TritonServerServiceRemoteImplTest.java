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
package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TritonServerServiceRemoteImplTest extends TritonServerServiceStepDefinitions {

    @Test
    public void isConfigurationValidWorksWithDefaultConfiguration() throws IOException {
        givenTritonServerServiceRemoteImpl(defaultProperties());

        thenIsConfigurationValidReturns(true);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidRemoteConfiguration() throws IOException {
        givenTritonServerServiceRemoteImpl(invalidRemoteProperties());

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isModelEncryptionEnabledWorkWithDefault() throws IOException {
        givenTritonServerServiceRemoteImpl(defaultProperties());

        thenIsModelEncryptionEnabled(false);
    }

    /*
     * Helpers
     */
    private Map<String, Object> invalidRemoteProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("server.address", "");

        return properties;
    }

    /*
     * Then
     */
    private void thenIsConfigurationValidReturns(boolean expectedValue) {
        assertEquals(expectedValue, this.tritonServerService.isConfigurationValid());
    }

    private void thenIsModelEncryptionEnabled(boolean expectedValue) {
        assertEquals(expectedValue, this.tritonServerService.isModelEncryptionEnabled());
    }

}
