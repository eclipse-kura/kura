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
  *******************************************************************************/

package org.eclipse.kura.container.provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.DockerService;
import org.eclipse.kura.container.orchestration.listener.DockerServiceListener;
import org.junit.Ignore;
import org.junit.Test;

public class ContainerInstanceTest {

    private static final String CONTAINER_PATH_FILE_PATH = "container.path.filePath";
    private static final String CONTAINER_PATH_DESTINATION = "container.path.destination";
    private static final String CONTAINER_ENV1 = "container.env1";
    private static final String CONTAINER_ARGS = "container.args";
    private static final String CONTAINER_PORTS_INTERNAL = "container.ports.internal";
    private static final String CONTAINER_PORTS_EXTERNAL = "container.ports.external";
    private static final String CONTAINER_NAME = "container.name";
    private static final String CONTAINER_IMAGE_TAG = "container.image.tag";
    private static final String CONTAINER_IMAGE = "container.image";
    private static final String CONTAINER_ENABLED = "container.enabled";
    private static final String CONTAINER_DEVICE = "container.Device";

    private DockerService dockerService;
    private Map<String, Object> properties;
    private ContainerInstance configurableGenericDockerService;

    @Test(expected = IllegalArgumentException.class)
    public void testServiceActivateNullProperties() throws KuraException {
        givenNullProperties();
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();
    }

    @Test
    public void testServiceActivateWithPropertiesDisabled() throws KuraException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();

        thenStoppedMicroservice();
        thenNotStartedMicroservice();

    }

    @Test
    public void testServiceActivateWithPropertiesEnabled() throws KuraException {
        givenFullProperties(true);
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();

        thenNotStoppedMicroservice();
        thenStartedMicroservice();

    }

    @Test
    public void testServiceUpdateSameProperties() throws KuraException, InterruptedException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();

        whenUpdateInstance();

        thenStoppedMicroservice();
        thenNotStartedMicroservice();

    }

    @Test
    @Ignore
    public void testServiceUpdateEnable() throws KuraException, InterruptedException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenFullProperties(true);

        whenUpdateInstance();

        thenNotStoppedMicroservice();
        thenStartedMicroservice();

    }

    @Test
    public void testServiceUpdateDisable() throws KuraException, InterruptedException {
        givenFullProperties(true);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenDockerService();
        givenFullProperties(false);

        whenUpdateInstance();

        thenStoppedMicroservice();
    }

    @Test
    public void testServiceDeactivateNoRunningContainers() throws KuraException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();

        whenDeactivateInstance();

        thenNotStartedMicroservice();
    }

    @Test
    public void testServiceDeactivateRunningContainers() throws KuraException {
        givenFullProperties(true);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenDockerService();

        whenDeactivateInstance();

        thenStoppedMicroservice();
    }

    private void givenDockerService() {
        this.dockerService = mock(DockerService.class);
        this.configurableGenericDockerService.setDockerService(this.dockerService);
    }

    private void givenNullProperties() {
        this.properties = null;
    }

    private void givenFullProperties(boolean enabled) {
        this.properties = new HashMap<>();
        this.properties.put(CONTAINER_ENABLED, enabled);
        this.properties.put(CONTAINER_IMAGE, "myimage");
        this.properties.put(CONTAINER_IMAGE_TAG, "mytag");
        this.properties.put(CONTAINER_NAME, "myname");
        this.properties.put(CONTAINER_PORTS_EXTERNAL, "");
        this.properties.put(CONTAINER_PORTS_INTERNAL, "");
        this.properties.put(CONTAINER_ARGS, "");
        this.properties.put(CONTAINER_ENV1, "");
        this.properties.put(CONTAINER_PATH_DESTINATION, "");
        this.properties.put(CONTAINER_PATH_FILE_PATH, "");
        this.properties.put(CONTAINER_DEVICE, "");
    }

    private void givenConfigurableGenericDockerService() {
        this.configurableGenericDockerService = new ContainerInstance();
    }

    private void givenActivateInstance() {
        whenActivateInstance();
    }

    private void whenActivateInstance() {
        this.configurableGenericDockerService.activate(this.properties);
    }

    private void whenUpdateInstance() throws InterruptedException {
        this.configurableGenericDockerService.updated(this.properties);
    }

    private void whenDeactivateInstance() {
        this.configurableGenericDockerService.deactivate();
    }

    private void thenStoppedMicroservice() throws KuraException {
        verify(this.dockerService, times(1)).unregisterListener(any(DockerServiceListener.class));
    }

    private void thenNotStoppedMicroservice() throws KuraException {
        verify(this.dockerService, times(0)).unregisterListener(any(DockerServiceListener.class));
    }

    private void thenNotStartedMicroservice() throws KuraException {
        verify(this.dockerService, times(0)).registerListener(any(DockerServiceListener.class), any(String.class));
    }

    private void thenStartedMicroservice() throws KuraException {
        verify(this.dockerService, times(1)).registerListener(any(DockerServiceListener.class), any(String.class));
    }

}
