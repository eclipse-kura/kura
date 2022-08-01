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

public class TritonServerServiceNativeImplTest extends TritonServerServiceStepDefinitions {

    @Test
    public void isConfigurationValidWorksWithNativeConfiguration() throws IOException {
        givenTritonServerServiceNativeImpl(validNativeProperties());

        thenIsConfigurationValidReturns(true);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidBackend() throws IOException {
        givenTritonServerServiceNativeImpl(invalidBackendNativeProperties());

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidModelRepository() throws IOException {
        givenTritonServerServiceNativeImpl(invalidModelRepositoryNativeProperties());

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isModelEncryptionEnabledWorkWithNativeConfiguration() throws IOException {
        givenTritonServerServiceNativeImpl(validNativeProperties());

        thenIsModelEncryptionEnabled(false);
    }

    @Test
    public void isModelEncryptionEnabledWorksWhenPasswordIsSet() throws IOException {
        givenTritonServerServiceNativeImpl(validEncryptionNativeProperties());

        thenIsModelEncryptionEnabled(true);
    }

    /*
     * Helpers
     */
    private Map<String, Object> invalidBackendNativeProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("local.backends.path", null);
        properties.put("local.model.repository.path", "/fake-repository-path");

        return properties;
    }

    private Map<String, Object> invalidModelRepositoryNativeProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("local.backends.path", null);
        properties.put("local.model.repository.path", "/fake-repository-path");

        return properties;
    }

    private Map<String, Object> validNativeProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("server.ports", new Integer[] { 4001, 4002, 4003 });
        properties.put("local.backends.path", "/fake-backends-path");
        properties.put("local.model.repository.path", "/fake-repository-path");

        return properties;
    }

    private Map<String, Object> validEncryptionNativeProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("server.ports", new Integer[] { 4001, 4002, 4003 });
        properties.put("local.backends.path", "/fake-backends-path");
        properties.put("local.model.repository.path", "/fake-repository-path");
        properties.put("local.model.repository.password", "hutini");

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