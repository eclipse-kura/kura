/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.container.orchestration.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerState;
import org.eclipse.kura.container.orchestration.ImageConfiguration;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.container.orchestration.provider.impl.ContainerOrchestrationServiceImpl;
import org.eclipse.kura.container.orchestration.provider.impl.enforcement.AllowlistEnforcementMonitor;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;

public class EnforcementSecurityTest {

    private static final String IMAGE_NAME = "nginx";
    private static final String CONTAINER_NAME = "frank";
    private static final String CONTAINER_ID = "1d3dewf34r5";

    private static final String REGISTRY_URL = "https://test";
    private static final String REGISTRY_USERNAME = "test";
    private static final String REGISTRY_PASSWORD = "test1";

    private static final String EMPTY_ALLOWLIST_CONTENT = "";
    private static final String FILLED_ALLOWLIST_CONTENT_NO_SPACE = "sha256:f9d633ff6640178c2d0525017174a688e2c1aef28f0a0130b26bd5554491f0da\nsha256:c26ae7472d624ba1fafd296e73cecc4f93f853088e6a9c13c0d52f6ca5865107";
    private static final String FILLED_ALLOWLIST_CONTENT_WITH_SPACES = " sha256:f9d633ff6640178c2d0525017174a688e2c1aef28f0a0130b26bd5554491f0da \n sha256:c26ae7472d624ba1fafd296e73cecc4f93f853088e6a9c13c0d52f6ca5865107";

    private static final String CORRECT_DIGEST = "sha256:c26ae7472d624ba1fafd296e73cecc4f93f853088e6a9c13c0d52f6ca5865107";
    private static final String WRONG_DIGEST = "sha256:0000000000000000000000000000000000000000000000000000000000000000";

    private AllowlistEnforcementMonitor allowlistEnforcementMonitor;
    private ContainerOrchestrationServiceImpl mockedContainerOrchestrationImpl;

    ContainerConfiguration containerConfig;

    private Map<String, Object> properties = new HashMap<>();

    public EnforcementSecurityTest() {
        this.properties.clear();
    }

    @Test
    public void shouldAllowStartingWithCorrectAllowlistContent() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationServiceWith(CONTAINER_ID, CONTAINER_NAME, IMAGE_NAME, ContainerState.ACTIVE,
                CORRECT_DIGEST);
        givenMockedDockerClient();
        givenAllowlistEnforcement(FILLED_ALLOWLIST_CONTENT_NO_SPACE);

        whenOnNext(CONTAINER_ID);

        thenStopContainerWasNeverCalled();
        thenDeleteContainerWasNeverCalled();
    }

    @Test
    public void shouldAllowStartingWithCorrectAllowlistContentWithSpaces() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationServiceWith(CONTAINER_ID, CONTAINER_NAME, IMAGE_NAME, ContainerState.ACTIVE,
                CORRECT_DIGEST);
        givenMockedDockerClient();
        givenAllowlistEnforcement(FILLED_ALLOWLIST_CONTENT_WITH_SPACES);

        whenOnNext(CONTAINER_ID);

        thenStopContainerWasNeverCalled();
        thenDeleteContainerWasNeverCalled();
    }

    @Test
    public void shouldNotAllowStartingWithEmptyAllowlistContent() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationServiceWith(CONTAINER_ID, CONTAINER_NAME, IMAGE_NAME, ContainerState.ACTIVE,
                CORRECT_DIGEST);
        givenMockedDockerClient();
        givenAllowlistEnforcement(EMPTY_ALLOWLIST_CONTENT);

        whenOnNext(CONTAINER_ID);

        thenStopContainerWasCalledFor(CONTAINER_ID);
        thenDeleteContainerWasCalledFor(CONTAINER_ID);
    }

    @Test
    public void shouldNotAllowStartingWithWrongContainerDigest() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationServiceWith(CONTAINER_ID, CONTAINER_NAME, IMAGE_NAME, ContainerState.ACTIVE,
                WRONG_DIGEST);
        givenMockedDockerClient();
        givenAllowlistEnforcement(FILLED_ALLOWLIST_CONTENT_NO_SPACE);

        whenOnNext(CONTAINER_ID);

        thenStopContainerWasCalledFor(CONTAINER_ID);
        thenDeleteContainerWasCalledFor(CONTAINER_ID);
    }

    @Test
    public void shouldStopAndDeleteContainerWithWrongDigestAndActiveState() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationServiceWith(CONTAINER_ID, CONTAINER_NAME, IMAGE_NAME, ContainerState.ACTIVE,
                CORRECT_DIGEST);
        givenMockedDockerClient();
        givenAllowlistEnforcement(EMPTY_ALLOWLIST_CONTENT);

        whenVerifyAlreadyRunningContainersDigests(this.mockedContainerOrchestrationImpl.listContainerDescriptors());

        thenStopContainerWasCalledFor(CONTAINER_ID);
        thenDeleteContainerWasCalledFor(CONTAINER_ID);
    }

    @Test
    public void shouldOnlyDeleteContainerWithWrongDigestAndFailedState() throws KuraException, InterruptedException {

        givenMockedContainerOrchestrationServiceWith(CONTAINER_ID, CONTAINER_NAME, IMAGE_NAME, ContainerState.FAILED,
                CORRECT_DIGEST);
        givenMockedDockerClient();
        givenAllowlistEnforcement(EMPTY_ALLOWLIST_CONTENT);

        whenVerifyAlreadyRunningContainersDigests(this.mockedContainerOrchestrationImpl.listContainerDescriptors());

        thenStopContainerWasNeverCalled();
        thenDeleteContainerWasCalledFor(CONTAINER_ID);
    }

    /*
     * Given
     */

    ContainerInstanceDescriptor containerInstanceDescriptor;

    private void givenMockedContainerOrchestrationServiceWith(String containerId, String containerName,
            String imageName, ContainerState containerState, String digest) throws KuraException, InterruptedException {

        this.mockedContainerOrchestrationImpl = spy(new ContainerOrchestrationServiceImpl());

        this.containerInstanceDescriptor = ContainerInstanceDescriptor.builder().setContainerID(containerId)
                .setContainerName(containerName).setContainerImage(imageName).setContainerState(containerState).build();
        List<ContainerInstanceDescriptor> containerDescriptors = new ArrayList<>();
        containerDescriptors.add(containerInstanceDescriptor);

        ImageConfiguration imageConfig = new ImageConfiguration.ImageConfigurationBuilder().setImageName(imageName)
                .setImageTag("latest").setImageDownloadTimeoutSeconds(0)
                .setRegistryCredentials(Optional.of(new PasswordRegistryCredentials(Optional.of(REGISTRY_URL),
                        REGISTRY_USERNAME, new Password(REGISTRY_PASSWORD))))
                .build();

        this.containerConfig = ContainerConfiguration.builder().setContainerName(CONTAINER_NAME)
                .setImageConfiguration(imageConfig).setVolumes(Collections.singletonMap("test", "~/test/test"))
                .setDeviceList(Arrays.asList("/dev/gpio1", "/dev/gpio2"))
                .setEnvVars(Arrays.asList("test=test", "test2=test2")).build();

        doReturn(containerDescriptors).when(this.mockedContainerOrchestrationImpl).listContainerDescriptors();
        doNothing().when(this.mockedContainerOrchestrationImpl).pullImage(any(ImageConfiguration.class));
        doNothing().when(this.mockedContainerOrchestrationImpl).stopContainer(anyString());
        doNothing().when(this.mockedContainerOrchestrationImpl).deleteContainer(anyString());
        doReturn(new HashSet<>(Arrays.asList(digest))).when(this.mockedContainerOrchestrationImpl)
                .getImageDigestsByContainerId(containerId);
    }

    private void givenMockedDockerClient() {
        DockerClient mockedDockerClient = mock(DockerClient.class, Mockito.RETURNS_DEEP_STUBS);
        this.mockedContainerOrchestrationImpl.setDockerClient(mockedDockerClient);
    }

    private void givenAllowlistEnforcement(String rawAllowlistContent) {
        this.allowlistEnforcementMonitor = new AllowlistEnforcementMonitor(rawAllowlistContent,
                this.mockedContainerOrchestrationImpl);
    }

    /*
     * When
     */

    private void whenOnNext(String containerId) {
        this.allowlistEnforcementMonitor.onNext(new Event("start", containerId, "nginx:latest", 1708963202L));
    }

    private void whenVerifyAlreadyRunningContainersDigests(List<ContainerInstanceDescriptor> containerDescriptors) {
        this.allowlistEnforcementMonitor.enforceAllowlistFor(containerDescriptors);
    }

    /*
     * Then
     */

    private void thenStopContainerWasNeverCalled() throws KuraException {
        verify(this.mockedContainerOrchestrationImpl, never()).stopContainer(any(String.class));
    }

    private void thenStopContainerWasCalledFor(String containerId) throws KuraException {
        verify(this.mockedContainerOrchestrationImpl, times(1)).stopContainer(containerId);
    }

    private void thenDeleteContainerWasNeverCalled() throws KuraException {
        verify(this.mockedContainerOrchestrationImpl, never()).deleteContainer(any(String.class));
    }

    private void thenDeleteContainerWasCalledFor(String containerId) throws KuraException {
        verify(this.mockedContainerOrchestrationImpl, times(1)).deleteContainer(containerId);
    }
}
