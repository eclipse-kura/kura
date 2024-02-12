/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.ContainerState;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

public class TritonServerContainerManagerTest {

    private static final String TRITON_IMAGE_NAME = "tritonserver";
    private static final String TRITON_IMAGE_TAG = "latest";
    private static final String TRITON_INTERNAL_MODEL_REPO = "/models";
    private static final String MOCK_DECRYPT_FOLDER = "testDecryptionFolder";
    private static final String TRITON_CONTAINER_NAME = "tritonserver-kura";
    private static final String TRITON_CONTAINER_ID = "tritonserver-kura-ID";
    private static final String TRITON_REPOSITORY_PATH = "/path/to/repository";
    private static final String TRITON_INTERNAL_BACKENDS_FOLDER = "/backends";
    private static final String TRITON_BACKENDS_PATH = "/path/to/backends";

    private Map<String, Object> properties = new HashMap<>();
    private TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

    private ContainerOrchestrationService orc;
    private TritonServerContainerManager manager;

    @Captor
    ArgumentCaptor<ContainerConfiguration> configurationCaptor = ArgumentCaptor.forClass(ContainerConfiguration.class);
    private ContainerConfiguration capturedContainerConfig;

    @Test
    public void isServerRunningWorksWhenNotRunning() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        thenServerIsRunningReturns(false);
    }

    @Test
    public void isServerRunningWorksWhenRunning() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonContainerIsRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        thenServerIsRunningReturns(true);
    }

    @Test
    public void isServerRunningWorksWhenOrchestratorNotConnected() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenOrchestratorIsNotConnected();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        thenServerIsRunningReturns(false);
    }

    @Test
    public void stopMethodShouldWork() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonContainerIsRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStopIsCalled();

        thenContainerOrchestrationStopContainerWasCalledWith(TRITON_CONTAINER_ID);
        thenContainerOrchestrationDeleteContainerWasCalledWith(TRITON_CONTAINER_ID);
    }

    @Test
    public void stopMethodShouldWorkWhenNotRunning() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStopIsCalled();

        thenContainerOrchestrationStopContainerWasNotCalled();
        thenContainerOrchestrationDeleteContainerWasNotCalled();
    }

    @Test
    public void killMethodShouldWork() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonContainerIsRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenKillIsCalled();

        thenContainerOrchestrationStopContainerWasCalledWith(TRITON_CONTAINER_ID);
        thenContainerOrchestrationDeleteContainerWasCalledWith(TRITON_CONTAINER_ID);
    }

    @Test
    public void startMethodShouldWorkWhenOrchestratorNotConnected() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenOrchestratorIsNotConnected();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasNotCalled();
    }

    @Test
    public void startMethodShouldWorkIfImageIsNotAvailable() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsNotAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasNotCalled();
    }

    @Test
    public void startMethodShouldWorkIfImageIsAvailableWithDifferentTag() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageWithDifferentTagIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasNotCalled();
    }

    @Test
    public void startMethodShouldWorkIfImageIsAvailable() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasCalled();
        thenContainerConfigurationIsFrameworkManaged(true);
        thenContainerConfigurationPortsEquals(Arrays.asList(4000, 4001, 4002));
        thenContainerConfigurationImageEquals(TRITON_IMAGE_NAME);
        thenContainerConfigurationImageTagEquals(TRITON_IMAGE_TAG);
        thenContainerConfigurationNameEquals(TRITON_CONTAINER_NAME);
        thenContainerConfigurationVolumesEquals(
                Collections.singletonMap(TRITON_REPOSITORY_PATH, TRITON_INTERNAL_MODEL_REPO));
        thenContainerConfigurationEntrypointOverrideContains("--model-control-mode=explicit");

        thenContainerConfigurationMemoryIsPresent(false);
        thenContainerConfigurationCpusIsPresent(false);
        thenContainerConfigurationGpusIsPresent(false);
        thenContainerConfigurationRuntimeIsPresent(false);
        thenContainerConfigurationDevicesIsFilled(false);
    }

    @Test
    public void startMethodShouldWorkWhenAlreadyRunning() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasNotCalled();
    }

    @Test
    public void containerManagerShouldUseDecryptionFolderIfPasswordIsSet() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("local.model.repository.password", "hutini");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasCalled();
        thenContainerConfigurationVolumesEquals(
                Collections.singletonMap(MOCK_DECRYPT_FOLDER, TRITON_INTERNAL_MODEL_REPO));
    }

    @Test
    public void containerDeviceRuntimeOptionsAreCorrectlySet() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("container.memory", "7g");
        givenPropertyWith("container.cpus", 1.5F);
        givenPropertyWith("container.gpus", "all");
        givenPropertyWith("container.runtime", "myCoolRuntime");
        givenPropertyWith("devices", "/dev/tty0,/dev/video1");
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasCalled();

        thenContainerConfigurationMemoryIsPresent(true);
        thenContainerConfigurationMemoryEquals(7516192768L);

        thenContainerConfigurationCpusIsPresent(true);
        thenContainerConfigurationCpusEquals(1.5F);

        thenContainerConfigurationGpusIsPresent(true);
        thenContainerConfigurationGpusEquals("all");

        thenContainerConfigurationRuntimeIsPresent(true);
        thenContainerConfigurationRuntimeEquals("myCoolRuntime");

        thenContainerConfigurationDevicesIsFilled(true);
        thenContainerConfigurationDevicesEquals(Arrays.asList("/dev/tty0", "/dev/video1"));
    }

    @Test
    public void containerBackendConfigOptionsAreCorrectlySet() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("local.backends.config", "testConfiguration");
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasCalled();
        thenContainerConfigurationIsFrameworkManaged(true);
        thenContainerConfigurationPortsEquals(Arrays.asList(4000, 4001, 4002));
        thenContainerConfigurationImageEquals(TRITON_IMAGE_NAME);
        thenContainerConfigurationEntrypointOverrideContains("--backend-config=testConfiguration");
    }

    @Test
    public void startMethodShouldWorkWithTritonModelsOnly() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasCalled();
        thenContainerConfigurationIsFrameworkManaged(true);
        thenContainerConfigurationPortsEquals(Arrays.asList(4000, 4001, 4002));
        thenContainerConfigurationImageEquals(TRITON_IMAGE_NAME);
        thenContainerConfigurationImageTagEquals(TRITON_IMAGE_TAG);
        thenContainerConfigurationNameEquals(TRITON_CONTAINER_NAME);
        thenContainerConfigurationVolumesEquals(
                Collections.singletonMap(TRITON_REPOSITORY_PATH, TRITON_INTERNAL_MODEL_REPO));
        thenContainerConfigurationEntrypointOverrideContains("--model-control-mode=explicit");
    }

    @Test
    public void startMethodShouldWorkWithTritonModelsAndBackends() {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("local.backends.path", TRITON_BACKENDS_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenServiceOptionsBuiltWith(properties);

        givenMockContainerOrchestrationService();
        givenTritonImageIsAvailable();
        givenTritonContainerIsNotRunning();
        givenLocalManagerBuiltWith(this.options, this.orc, MOCK_DECRYPT_FOLDER);

        whenStartIsCalled();

        thenContainerOrchestrationStartContainerWasCalled();
        thenContainerConfigurationIsFrameworkManaged(true);
        thenContainerConfigurationPortsEquals(Arrays.asList(4000, 4001, 4002));
        thenContainerConfigurationImageEquals(TRITON_IMAGE_NAME);
        thenContainerConfigurationImageTagEquals(TRITON_IMAGE_TAG);
        thenContainerConfigurationNameEquals(TRITON_CONTAINER_NAME);
        thenContainerConfigurationVolumesEquals(Stream.of(new String[][] {
                { TRITON_REPOSITORY_PATH, TRITON_INTERNAL_MODEL_REPO },
                { TRITON_BACKENDS_PATH, TRITON_INTERNAL_BACKENDS_FOLDER },
        }).collect(Collectors.collectingAndThen(
                Collectors.toMap(data -> data[0], data -> data[1]),
                Collections::<String, String>unmodifiableMap)));
        thenContainerConfigurationEntrypointOverrideContains("--model-control-mode=explicit");
    }

    /*
     * Given
     */
    private void givenPropertyWith(String name, Object value) {
        this.properties.put(name, value);
    }

    private void givenServiceOptionsBuiltWith(Map<String, Object> properties) {
        this.options = new TritonServerServiceOptions(properties);
    }

    private void givenMockContainerOrchestrationService() {
        this.orc = mock(ContainerOrchestrationService.class);
    }

    private void givenTritonImageIsAvailable() {
        ImageInstanceDescriptor imageDescriptor = mock(ImageInstanceDescriptor.class);
        when(imageDescriptor.getImageName()).thenReturn(TRITON_IMAGE_NAME);
        when(imageDescriptor.getImageTag()).thenReturn(TRITON_IMAGE_TAG);

        when(this.orc.listImageInstanceDescriptors()).thenReturn(Arrays.asList(imageDescriptor));
    }

    private void givenTritonImageWithDifferentTagIsAvailable() {
        ImageInstanceDescriptor imageDescriptor = mock(ImageInstanceDescriptor.class);
        when(imageDescriptor.getImageName()).thenReturn(TRITON_IMAGE_NAME);
        when(imageDescriptor.getImageTag()).thenReturn("py3-min");

        when(this.orc.listImageInstanceDescriptors()).thenReturn(Arrays.asList(imageDescriptor));
    }

    private void givenTritonImageIsNotAvailable() {
        when(this.orc.listImageInstanceDescriptors()).thenReturn(Arrays.asList());
    }

    private void givenOrchestratorIsNotConnected() {
        when(this.orc.listImageInstanceDescriptors()).thenThrow(new IllegalStateException());
        when(this.orc.listContainerDescriptors()).thenThrow(new IllegalStateException());
    }

    private void givenTritonContainerIsRunning() {
        ContainerInstanceDescriptor runningContainer = mock(ContainerInstanceDescriptor.class);
        when(runningContainer.getContainerName()).thenReturn(TRITON_CONTAINER_NAME);
        when(runningContainer.getContainerId()).thenReturn(TRITON_CONTAINER_ID);
        when(runningContainer.getContainerState()).thenReturn(ContainerState.ACTIVE);
        when(runningContainer.getContainerImage()).thenReturn(TRITON_IMAGE_NAME);
        when(runningContainer.getContainerImageTag()).thenReturn(TRITON_IMAGE_TAG);

        when(this.orc.listContainerDescriptors()).thenReturn(Arrays.asList(runningContainer));
    }

    private void givenTritonContainerIsNotRunning() {
        when(this.orc.listContainerDescriptors()).thenReturn(Arrays.asList());
    }

    private void givenLocalManagerBuiltWith(TritonServerServiceOptions options, ContainerOrchestrationService orc,
            String decryptionFolder) {
        this.manager = new TritonServerContainerManager(options, orc, decryptionFolder);
    }

    /*
     * When
     */
    private void whenKillIsCalled() {
        this.manager.kill();
    }

    private void whenStopIsCalled() {
        this.manager.stop();
    }

    private void whenStartIsCalled() {
        this.manager.start();

        try {
            // Wait for monitor thread to do its job
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail();
        }
    }

    /*
     * Then
     */
    private void thenServerIsRunningReturns(boolean expectedState) {
        assertEquals(expectedState, this.manager.isServerRunning());
    }

    private void thenContainerOrchestrationStopContainerWasCalledWith(String containerID) {
        try {
            verify(this.orc, times(1)).stopContainer(containerID);
        } catch (KuraException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void thenContainerOrchestrationDeleteContainerWasCalledWith(String containerID) {
        try {
            verify(this.orc, times(1)).deleteContainer(containerID);
        } catch (KuraException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void thenContainerOrchestrationStartContainerWasCalled() {
        try {
            verify(this.orc, times(1)).startContainer(configurationCaptor.capture());
            this.capturedContainerConfig = configurationCaptor.getValue();
        } catch (KuraException | InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void thenContainerOrchestrationStartContainerWasNotCalled() {
        try {
            verify(this.orc, never()).startContainer((ContainerConfiguration) any(Object.class));
        } catch (KuraException | InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void thenContainerOrchestrationStopContainerWasNotCalled() {
        try {
            verify(this.orc, never()).stopContainer(any(String.class));
        } catch (KuraException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void thenContainerOrchestrationDeleteContainerWasNotCalled() {
        try {
            verify(this.orc, never()).deleteContainer(any(String.class));
        } catch (KuraException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void thenContainerConfigurationImageEquals(String expectedImage) {
        assertEquals(expectedImage, this.capturedContainerConfig.getContainerImage());
    }

    private void thenContainerConfigurationImageTagEquals(String expectedImageTag) {
        assertEquals(expectedImageTag, this.capturedContainerConfig.getContainerImageTag());
    }

    private void thenContainerConfigurationNameEquals(String expectedContainerName) {
        assertEquals(expectedContainerName, this.capturedContainerConfig.getContainerName());
    }

    private void thenContainerConfigurationPortsEquals(List<Integer> expectedPorts) {
        assertEquals(expectedPorts, this.capturedContainerConfig.getContainerPortsExternal());
    }

    private void thenContainerConfigurationVolumesEquals(Map<String, String> expectedVolumes) {
        assertEquals(expectedVolumes, this.capturedContainerConfig.getContainerVolumes());
    }

    private void thenContainerConfigurationMemoryIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.capturedContainerConfig.getMemory().isPresent());
    }

    private void thenContainerConfigurationMemoryEquals(Long expectedMemory) {
        assertEquals(expectedMemory, this.capturedContainerConfig.getMemory().get());
    }

    private void thenContainerConfigurationCpusIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.capturedContainerConfig.getCpus().isPresent());
    }

    private void thenContainerConfigurationCpusEquals(Float expectedCpus) {
        assertEquals(expectedCpus, this.capturedContainerConfig.getCpus().get());
    }

    private void thenContainerConfigurationGpusIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.capturedContainerConfig.getGpus().isPresent());
    }

    private void thenContainerConfigurationGpusEquals(String expectedGpus) {
        assertEquals(expectedGpus, this.capturedContainerConfig.getGpus().get());
    }

    private void thenContainerConfigurationIsFrameworkManaged(boolean expectedResult) {
        assertEquals(expectedResult, this.capturedContainerConfig.isFrameworkManaged());
    }

    private void thenContainerConfigurationEntrypointOverrideContains(String expectedString) {
        assertTrue(this.capturedContainerConfig.getEntryPoint().contains(expectedString));
    }

    private void thenContainerConfigurationRuntimeIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.capturedContainerConfig.getRuntime().isPresent());
    }

    private void thenContainerConfigurationRuntimeEquals(String expectedRuntime) {
        assertEquals(expectedRuntime, this.capturedContainerConfig.getRuntime().get());
    }

    private void thenContainerConfigurationDevicesIsFilled(boolean expectedResult) {
        assertEquals(expectedResult, !this.capturedContainerConfig.getContainerDevices().isEmpty());
    }

    private void thenContainerConfigurationDevicesEquals(List<String> expectedDevices) {
        assertEquals(expectedDevices, this.capturedContainerConfig.getContainerDevices());
    }

}
