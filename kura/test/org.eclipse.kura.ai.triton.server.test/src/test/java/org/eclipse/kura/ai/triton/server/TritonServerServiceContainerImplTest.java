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

public class TritonServerServiceContainerImplTest extends TritonServerServiceStepDefinitions {

    private Map<String, Object> properties = new HashMap<>();

    @Test
    public void isConfigurationValidWorksWithContainerConfiguration() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", "/fake-repository-path");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(true);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidImage() throws IOException {
        givenPropertyWith("container.image", null);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", "/fake-repository-path");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidImageTag() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", null);
        givenPropertyWith("local.model.repository.path", "/fake-repository-path");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidModelRepository() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", "");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isModelEncryptionEnabledWorkWhenPasswordIsNotSet() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", "/fake-repository-path");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsModelEncryptionEnabled(false);
    }

    @Test
    public void isModelEncryptionEnabledWorksWhenPasswordIsSet() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", "/fake-repository-path");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("local.model.repository.password", "keyboards");
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsModelEncryptionEnabled(true);
    }

    /*
     * Given
     */
    private void givenPropertyWith(String name, Object value) {
        this.properties.put(name, value);
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
