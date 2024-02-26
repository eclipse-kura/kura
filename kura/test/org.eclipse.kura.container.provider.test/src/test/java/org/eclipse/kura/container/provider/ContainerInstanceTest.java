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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.provider.ContainerInstance.ContainerInstanceState;
import org.junit.After;
import org.junit.Test;

public class ContainerInstanceTest {

    private static final String CONTAINER_NAME = "container.name";
    private static final String CONTAINER_IMAGE_TAG = "container.image.tag";
    private static final String CONTAINER_IMAGE = "container.image";
    private static final String CONTAINER_ENABLED = "container.enabled";

    private ContainerOrchestrationService mockContainerOrchestrationService = mock(ContainerOrchestrationService.class);
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, Object> newProperties = new HashMap<>();
    private ContainerInstance containerInstance = new ContainerInstance();
    private Exception occurredException;

    public void activateContainerInstanceWithNullPropertiesThrows() {
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        whenActivateInstanceIsCalledWith(null);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void activateContainerInstanceWithDisabledContainerWorks() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, false);

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);
        thenStartContainerWasNeverCalled();
    }

    @Test
    public void activateContainerInstanceWithEnabledContainerWorks() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "myContainer");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.CREATED);
        thenStopContainerWasNeverCalled();
        thenStartContainerWasCalledWith(this.properties);
    }

    @Test
    public void activateContainerInstanceWithDisconnectedContainerOrchestratorWorks()
            throws KuraException, InterruptedException {
        givenContainerOrchestratorIsNotConnected();
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "myContainer");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);
        thenStartContainerWasNeverCalled();
    }

    @Test
    public void activateContainerInstanceWithAreadyRunningContainerWorksWithDisabled()
            throws KuraException, InterruptedException {
        givenContainerOrchestratorWithRunningContainer("myRunningContainer", "123456");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, false);
        givenPropertiesWith(CONTAINER_NAME, "myRunningContainer");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);

        // Are we sure about this?
        thenStartContainerWasNeverCalled();
        thenStopContainerWasNeverCalled();
        thenDeleteContainerWasNeverCalled();
    }

    @Test
    public void activateContainerInstanceWithAreadyRunningContainerWorksWithEnabled()
            throws KuraException, InterruptedException {
        givenContainerOrchestratorWithRunningContainer("myRunningContainer", "123456");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "myRunningContainer");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.CREATED);

        // Are we sure about this?
        thenStartContainerWasCalledWith(this.properties);
    }

    @Test
    public void updateContainerInstanceWithNullPropertiesThrows() throws KuraException, InterruptedException {
        givenPropertiesWith(CONTAINER_ENABLED, false);
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerInstanceWith(this.mockContainerOrchestrationService);
        givenContainerInstanceActivatedWith(this.properties);

        whenUpdateInstanceIsCalledWith(null);

        thenExceptionOccurred(IllegalArgumentException.class);
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);
        thenStopContainerWasNeverCalled();
        thenStartContainerWasNeverCalled();
        thenDeleteContainerWasNeverCalled();
    }

    @Test
    public void updateContainerInstanceWithSamePropertiesWorks() throws KuraException {
        givenPropertiesWith(CONTAINER_ENABLED, false);
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerInstanceWith(this.mockContainerOrchestrationService);
        givenContainerInstanceActivatedWith(this.properties);

        whenUpdateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);
        thenStopContainerWasNeverCalled();
        thenDeleteContainerWasNeverCalled();
    }

    @Test
    public void updateDisabledContainerInstanceWithEnabledContainerWorks() throws KuraException, InterruptedException {
        givenPropertiesWith(CONTAINER_ENABLED, false);
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerInstanceWith(this.mockContainerOrchestrationService);
        givenContainerInstanceActivatedWith(this.properties);

        givenNewPropertiesWith(CONTAINER_ENABLED, true);
        givenNewPropertiesWith(CONTAINER_NAME, "myContainer");
        givenNewPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenNewPropertiesWith(CONTAINER_IMAGE_TAG, "latest");

        whenUpdateInstanceIsCalledWith(this.newProperties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.CREATED);
        thenStartContainerWasCalledWith(this.newProperties);
    }

    @Test
    public void updateEnabledContainerInstanceWithDisabledContainerWorks() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");

        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenContainerInstanceActivatedWith(this.properties);
        givenContainerStateIs(ContainerInstanceState.CREATED);

        givenContainerOrchestratorWithRunningContainer("pippo", "1234");
        givenNewPropertiesWith(CONTAINER_ENABLED, false);

        whenUpdateInstanceIsCalledWith(this.newProperties);

        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);
        thenNoExceptionOccurred();
        thenStopContainerWasCalledFor("1234");
        thenDeleteContainerWasCalledFor("1234");
    }

    @Test
    public void deactivateContainerInstanceWithDisabledContainerWorks() throws KuraException, InterruptedException {
        givenPropertiesWith(CONTAINER_ENABLED, false);
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerInstanceWith(this.mockContainerOrchestrationService);
        givenContainerInstanceActivatedWith(this.properties);

        whenDeactivateInstanceIsCalled();

        thenNoExceptionOccurred();
        thenStartContainerWasNeverCalled();
    }

    @Test
    public void deactivateContainerInstanceWithEnabledContainerWorks() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");

        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenContainerInstanceActivatedWith(this.properties);
        givenContainerStateIs(ContainerInstanceState.CREATED);

        givenContainerOrchestratorWithRunningContainer("pippo", "1234");

        whenDeactivateInstanceIsCalled();

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(ContainerInstanceState.DISABLED);
        thenStopContainerWasCalledFor("1234");
        thenDeleteContainerWasCalledFor("1234");
    }

    @After
    public void tearDown() {
        this.containerInstance.deactivate();
    }

    /*
     * GIVEN
     */

    private void givenContainerInstanceWith(ContainerOrchestrationService cos) {
        this.containerInstance.setContainerOrchestrationService(cos);
    }

    private void givenPropertiesWith(String key, Object value) {
        this.properties.put(key, value);
    }

    private void givenNewPropertiesWith(String key, Object value) {
        this.newProperties.put(key, value);
    }

    private void givenContainerInstanceActivatedWith(Map<String, Object> configuration) {
        try {
            this.containerInstance.activate(configuration);
        } catch (Exception e) {
            fail("Failed to activate container instance. Caused by: " + e.getMessage());
        }
    }

    private void givenContainerStateIs(ContainerInstanceState expectedState) {
        int count = 10;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (this.containerInstance.getState() != expectedState && count-- > 0);

        if (count <= 0) {
            fail("Container instance state is not " + expectedState);
        }
    }

    private void givenContainerOrchestratorIsNotConnected() throws KuraException, InterruptedException {
        when(this.mockContainerOrchestrationService.listContainerDescriptors())
                .thenThrow(new IllegalStateException("Not connected"));
    }

    private void givenContainerOrchestratorWithNoRunningContainers() {
        when(this.mockContainerOrchestrationService.listContainerDescriptors()).thenReturn(Collections.emptyList());
    }

    private void givenContainerOrchestratorWithRunningContainer(String containerName, String containerId) {
        List<ContainerInstanceDescriptor> runningContainers = Collections.singletonList(ContainerInstanceDescriptor
                .builder().setContainerName(containerName).setContainerID(containerId).build());
        when(this.mockContainerOrchestrationService.listContainerDescriptors()).thenReturn(runningContainers);
    }

    private void givenContainerOrchestratorReturningOnStart(String containerId)
            throws KuraException, InterruptedException {
        when(this.mockContainerOrchestrationService.startContainer(any(ContainerConfiguration.class)))
                .thenReturn(containerId);
    }

    /*
     * WHEN
     */

    private void whenActivateInstanceIsCalledWith(Map<String, Object> configuration) {
        try {
            this.containerInstance.activate(configuration);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenUpdateInstanceIsCalledWith(Map<String, Object> configuration) {
        try {
            this.containerInstance.updated(configuration);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenDeactivateInstanceIsCalled() {
        try {
            this.containerInstance.deactivate();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * THEN
     */

    private void thenWaitForContainerInstanceToBecome(ContainerInstanceState expectedState) {
        int count = 10;
        do {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (this.containerInstance.getState() != expectedState && count-- > 0);

        assertEquals(expectedState, this.containerInstance.getState());
    }

    private void thenStopContainerWasNeverCalled() throws KuraException {
        verify(this.mockContainerOrchestrationService, never()).stopContainer(any(String.class));
    }

    private void thenStopContainerWasCalledFor(String containerId) throws KuraException {
        verify(this.mockContainerOrchestrationService, times(1)).stopContainer(containerId);
    }

    private void thenStartContainerWasNeverCalled() throws KuraException, InterruptedException {
        verify(this.mockContainerOrchestrationService, times(0)).startContainer(any(ContainerConfiguration.class));
    }

    private void thenStartContainerWasCalledWith(Map<String, Object> props) throws KuraException, InterruptedException {
        ContainerInstanceOptions options = new ContainerInstanceOptions(props);
        verify(this.mockContainerOrchestrationService, times(1)).startContainer(options.getContainerConfiguration());
    }

    private void thenDeleteContainerWasNeverCalled() throws KuraException {
        verify(this.mockContainerOrchestrationService, never()).deleteContainer(any(String.class));
    }

    private void thenDeleteContainerWasCalledFor(String containerId) throws KuraException {
        verify(this.mockContainerOrchestrationService, times(1)).deleteContainer(containerId);
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
