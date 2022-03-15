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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
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
    private static final String CONTAINER_LOGGER_PARAMETERS = "container.loggerParameters";
    private static final String CONTAINER_LOGGING_TYPE = "container.loggingType";

    private ContainerOrchestrationService dockerService;
    private Map<String, Object> properties;
    private ContainerInstance configurableGenericDockerService;
    private final CompletableFuture<Void> containerStarted = new CompletableFuture<>();

    @Test(expected = IllegalArgumentException.class)
    public void testServiceActivateNullProperties() {
        givenNullProperties();
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();
    }

    @Test
    public void testServiceActivateWithPropertiesDisabled() throws KuraException, InterruptedException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();

        whenActivateInstance();

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

    }

    @Test
    public void testServiceUpdateEnable() {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenFullProperties(true);

        whenUpdateInstance();

        thenStartedMicroservice();

    }

    @Test
    public void testServiceUpdateDisable() throws KuraException {
        givenFullProperties(true);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenStartedContainer();
        givenFullProperties(false);

        whenUpdateInstance();

        thenStoppedMicroservice();
    }

    @Test
    public void testServiceDeactivateNoRunningContainers() throws KuraException, InterruptedException {
        givenFullProperties(false);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();

        whenDeactivateInstance();

        thenNotStartedMicroservice();
    }

    @Test
    public void testServiceDeactivateStopContainer() throws KuraException {
        givenFullProperties(true);
        givenConfigurableGenericDockerService();
        givenDockerService();
        givenActivateInstance();
        givenStartedContainer();

        whenDeactivateInstance();

        thenStoppedMicroservice();
    }

    private void givenDockerService() {
        this.dockerService = mock(ContainerOrchestrationService.class);
        this.configurableGenericDockerService.setContainerOrchestrationService(this.dockerService);
        try {
            when(this.dockerService.startContainer((ContainerConfiguration) any())).thenAnswer(i -> {
                this.containerStarted.complete(null);
                return "1234";
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // no need
        }
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
        this.properties.put(CONTAINER_LOGGER_PARAMETERS, "");
        this.properties.put(CONTAINER_LOGGING_TYPE, "default");
    }

    private void givenConfigurableGenericDockerService() {
        this.configurableGenericDockerService = new ContainerInstance();
    }

    private void givenStartedContainer() {
        try {
            this.containerStarted.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("interrupted while waiting container startup");
        } catch (final Exception e) {
            fail("container not started");
        }
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
        verify(this.dockerService, times(1)).stopContainer(any(String.class));
    }

    private void thenNotStoppedMicroservice() throws KuraException {
        verify(this.dockerService, times(0)).stopContainer(any(String.class));
    }

    private void thenNotStartedMicroservice() throws KuraException, InterruptedException {
        verify(this.dockerService, times(0)).startContainer(any(ContainerConfiguration.class));
    }

    private void thenStartedMicroservice() {
        givenStartedContainer();
    }

}
