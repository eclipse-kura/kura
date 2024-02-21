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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
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
    private static final String CONTAINER_NETWORKING_MODE = "container.networkMode";

    private ContainerOrchestrationService dockerService;
    private Map<String, Object> properties = new HashMap<>();
    private ContainerInstance configurableGenericDockerService = new ContainerInstance();
    private Exception occurredException;
    private final CompletableFuture<Void> containerStarted = new CompletableFuture<>();

    public void testServiceActivateNullProperties() {
        givenDockerService();

        whenActivateInstanceIsCalledWith(null);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void testServiceActivateWithPropertiesDisabled() throws KuraException, InterruptedException {
        givenFullProperties(false);
        givenDockerService();

        whenActivateInstanceIsCalledWith(this.properties);

        thenNotStartedMicroservice();

    }

    @Test
    public void testServiceActivateWithPropertiesEnabled() throws KuraException {
        givenFullProperties(true);
        givenDockerService();

        whenActivateInstanceIsCalledWith(this.properties);

        thenNotStoppedMicroservice();
        thenStartedMicroservice();

    }

    @Test
    public void testServiceUpdateSameProperties() throws KuraException {
        givenFullProperties(false);
        givenDockerService();
        givenContainerInstanceWith(this.properties);

        whenUpdateInstanceIsCalledWith(this.properties);

        thenNotStoppedMicroservice();

    }

    @Test
    public void testServiceUpdateEnable() {
        givenFullProperties(false);
        givenDockerService();
        givenContainerInstanceWith(this.properties);
        givenFullProperties(true);

        whenUpdateInstanceIsCalledWith(this.properties);

        thenStartedMicroservice();
    }

    @Test
    public void testServiceUpdateDisable() throws KuraException {
        givenFullProperties(true);
        givenDockerService();
        givenContainerInstanceWith(this.properties);
        givenStartedContainer();
        givenFullProperties(false);

        whenUpdateInstanceIsCalledWith(this.properties);

        thenStoppedMicroservice();
    }

    @Test
    public void testServiceDeactivateNoRunningContainers() throws KuraException, InterruptedException {
        givenFullProperties(false);
        givenDockerService();
        givenContainerInstanceWith(this.properties);

        whenDeactivateInstance();

        thenNotStartedMicroservice();
    }

    @Test
    public void testServiceDeactivateStopContainer() throws KuraException {
        givenFullProperties(true);
        givenDockerService();
        givenContainerInstanceWith(this.properties);
        givenStartedContainer();

        whenDeactivateInstance();

        thenStoppedMicroservice();
    }

    private void givenDockerService() {
        this.dockerService = mock(ContainerOrchestrationService.class);
        this.configurableGenericDockerService.setContainerOrchestrationService(this.dockerService);
        try {
            final AtomicReference<ContainerConfiguration> config = new AtomicReference<>();

            when(this.dockerService.startContainer((ContainerConfiguration) any())).thenAnswer(i -> {
                config.set(i.getArgument(0, ContainerConfiguration.class));
                this.containerStarted.complete(null);
                return "1234";
            });
            when(this.dockerService.listContainerDescriptors()).thenAnswer(i -> {
                if (this.containerStarted.isDone()) {
                    return Collections.singletonList(ContainerInstanceDescriptor.builder()
                            .setContainerName(config.get().getContainerName()).setContainerID("1234").build());
                } else {
                    return Collections.emptyList();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // no need
        }
    }

    private void givenFullProperties(boolean enabled) {
        givenPropertiesWith(CONTAINER_ENABLED, enabled);
        givenPropertiesWith(CONTAINER_IMAGE, "myimage");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "mytag");
        givenPropertiesWith(CONTAINER_NAME, "myname");
        givenPropertiesWith(CONTAINER_PORTS_EXTERNAL, "");
        givenPropertiesWith(CONTAINER_PORTS_INTERNAL, "");
        givenPropertiesWith(CONTAINER_ARGS, "");
        givenPropertiesWith(CONTAINER_ENV1, "");
        givenPropertiesWith(CONTAINER_PATH_DESTINATION, "");
        givenPropertiesWith(CONTAINER_PATH_FILE_PATH, "");
        givenPropertiesWith(CONTAINER_DEVICE, "");
        givenPropertiesWith(CONTAINER_LOGGER_PARAMETERS, "");
        givenPropertiesWith(CONTAINER_LOGGING_TYPE, "default");
        givenPropertiesWith(CONTAINER_NETWORKING_MODE, "");
    }

    private void givenPropertiesWith(String key, Object value) {
        this.properties.put(key, value);
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

    private void givenContainerInstanceWith(Map<String, Object> configuration) {
        whenActivateInstanceIsCalledWith(configuration);
    }

    private void whenActivateInstanceIsCalledWith(Map<String, Object> configuration) {
        try {
            this.configurableGenericDockerService.activate(configuration);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenUpdateInstanceIsCalledWith(Map<String, Object> configuration) {
        this.configurableGenericDockerService.updated(configuration);
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

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

}
