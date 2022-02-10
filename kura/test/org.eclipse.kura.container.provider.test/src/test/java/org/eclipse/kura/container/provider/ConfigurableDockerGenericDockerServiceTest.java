/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/

package org.eclipse.kura.container.provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.junit.Test;

import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;
import org.eclipse.kura.container.orchestration.provider.DockerService;

public class ConfigurableDockerGenericDockerServiceTest {

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
    private ConfigurableGenericDockerService configurableGenericDockerService;

    @Test(expected = IllegalArgumentException.class)
    public void testServiceActivateNullProperties() throws KuraException {
        givenNullProperties();
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();
    }

    @Test
    public void testServiceActivateEmptyProperties() throws KuraException {
        givenEmptyProperties();
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();

        thenNotStoppedMicroservice();
        thenNotStartedMicroservice();

    }

    @Test
    public void testServiceActivateWithPropertiesDisabled() throws KuraException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();

        thenNotStoppedMicroservice();
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
    public void testServiceUpdateSameProperties() throws KuraException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();

        whenUpdateInstance();

        thenNotStoppedMicroservice();
        thenNotStartedMicroservice();

    }

    @Test
    public void testServiceUpdateEnable() throws KuraException {
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
    public void testServiceUpdateDisable() throws KuraException {
        givenFullProperties(true);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenDockerService();
        givenFullProperties(false);

        whenUpdateInstance();

        thenStoppedMicroservice();
        thenNotStartedMicroservice();
    }

    @Test
    public void testServiceDeactivateNoRunningContainers() throws KuraException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();

        whenDeactivateInstance();

        thenNotStoppedMicroservice();
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
        thenNotStartedMicroservice();
    }

    private void givenDockerService() {
        this.dockerService = mock(DockerService.class);
        this.configurableGenericDockerService.setDockerService(this.dockerService);
    }

    private void givenNullProperties() {
        this.properties = null;
    }

    private void givenEmptyProperties() {
        this.properties = new HashMap<>();
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
        this.configurableGenericDockerService = new ConfigurableGenericDockerService();
    }

    private void givenActivateInstance() {
        whenActivateInstance();
    }

    private void whenActivateInstance() {
        this.configurableGenericDockerService.activate(this.properties);
    }

    private void whenUpdateInstance() {
        this.configurableGenericDockerService.updated(this.properties);
    }

    private void whenDeactivateInstance() {
        this.configurableGenericDockerService.deactivate();
    }

    private void thenStoppedMicroservice() throws KuraException {
        verify(this.dockerService, times(1)).stopContainer(any(ContainerDescriptor.class));
    }

    private void thenNotStoppedMicroservice() throws KuraException {
        verify(this.dockerService, times(0)).stopContainer(any(ContainerDescriptor.class));
    }

    private void thenNotStartedMicroservice() throws KuraException {
        verify(this.dockerService, times(0)).startContainer(any(ContainerDescriptor.class));
    }

    private void thenStartedMicroservice() throws KuraException {
        verify(this.dockerService, times(1)).startContainer(any(ContainerDescriptor.class));
    }

}
