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
package org.eclipse.kura.container.orchestration.provider;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.provider.impl.DockerServiceImpl;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Image;

public class DockerServiceImplTest {

    private static final String DOCKER_HOST_URL = "dockerService.dockerHost";
    private static final String IS_ENABLED = "dockerService.enabled";
    private static final String DEFAULT_DOCKER_HOST_URL = "unix:///var/run/docker.sock";
    private static final boolean DEFAULT_IS_ENABLED = false;

    private static final String REPOSITORY_ENABLED = "dockerService.repository.enabled";
    private static final String REPOSITORY_URL = "dockerService.repository.hostname";
    private static final String REPOSITORY_USERNAME = "dockerService.repository.username";
    private static final String REPOSITORY_PASSWORD = "dockerService.repository.password";

    private static final boolean DEFAULT_REPOSITORY_ENABLED = false;
    private static final String DEFAULT_REPOSITORY_URL = "";
    private static final String DEFAULT_REPOSITORY_USERNAME = "";
    private static final String DEFAULT_REPOSITORY_PASSWORD = "";

    private DockerServiceImpl dockerService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DockerClient localDockerClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ListContainersCmd mockedListContainersCmd;

    // for tests
    private String[] runningContainers;
    private ContainerDescriptor[] runnignContainerDescriptor;

    private Map<String, Object> properties;
    private String createdContainerID;

    @Test
    public void testServiceActivateEmptyProperties() throws KuraException {
        // Should use default properties
        givenEmptyProperties();
        givenDockerServiceImpl();
        givenDockerClient();

        whenActivateInstance();

        thenNotStartedMicroservice();
        thenNotStoppedMicroservice();
    }

    @Test
    public void testServiceActivateDefaultPropertiesDisabled() throws KuraException {
        givenFullProperties(DEFAULT_IS_ENABLED);
        givenDockerServiceImpl();
        givenDockerClient();

        whenActivateInstance();

        thenNotStartedMicroservice();
        thenNotStoppedMicroservice();
    }

    @Test
    public void testServiceActivateDefaultPropertiesEnabledAndAuth() throws KuraException, InterruptedException {
        givenFullProperties(true, true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenActivateInstance();

        thenNotStartedMicroservice();
        thenNotStoppedMicroservice();
    }

    @Test
    public void testServiceActivateDefaultPropertiesEnabledWithAuthAndNoRepo()
            throws KuraException, InterruptedException {
        givenAuthWithRepoAndCredentials();
        givenDockerServiceImpl();
        givenDockerClient();

        whenActivateInstance();

        thenNotStartedMicroservice();
        thenNotStoppedMicroservice();
    }

    @Test
    public void testServiceUpdateDefaultPropertiesEnabled() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenUpdateInstance();

        thenNotStartedMicroservice();
        thenNotStoppedMicroservice();
    }

    @Test
    public void testServiceDeactivateDefaultPropertiesEnabled() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDeactivateInstance();

        thenNotStartedMicroservice();
        thenNotStoppedMicroservice();
    }

    @Test
    public void testServiceListContainerWhenEmpty() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockHasNoContainers();

        thenContainerListEqualsExpectedStringArray();
    }

    @Test
    public void testServiceListContainerWithSomeContainer() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockSomeContainers();

        thenContainerListEqualsExpectedStringArray();
    }

    @Test
    public void testServiceListContainerByIdWhenEmpty() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockHasNoContainers();

        thenContainerListByIdEqualsExpectedStringArray();
    }

    @Test
    public void testServiceListContainerByIdWithSomeContainer() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockSomeContainers();

        thenContainerListByIdEqualsExpectedStringArray();
    }

    @Test
    public void testServiceListContainerByContainerDescriptorWhenEmpty() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockHasNoContainers();

        thenContainerListByContainerDescriptorEqualsExpectedStringArray();
    }

    @Test
    public void testServiceListContainerByContainerDescriptorWithSomeContainer() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockSomeContainers();

        thenContainerListByContainerDescriptorEqualsExpectedStringArray();
    }

    @Test
    public void testGetContainerIDbyName() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockSomeContainers();

        thenGetFirstContainerIDbyName();
    }

    @Test
    public void testCreateContainer() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenDockerClient();

        whenMockforContainerCreation();
        whenCreateContainer();

        thenTestIfNewContainerExists();
    }

    @Test
    public void testCreateContainerOfflineQueue() throws KuraException {
        givenFullProperties(false);
        givenDockerServiceImpl();
        givenDockerClient();

        whenDockerClientMockListNoContainers();
        whenMockforContainerCreation();
        whenCreateContainer();
        whenRunContainer();
        whenPropertiesChangeAfterInit(true);
        whenUpdateInstance();

        thenTestIfNewContainerExists();
    }

    @Test
    public void testStopContainer() throws KuraException {
        givenFullProperties(true);
        givenDockerServiceImpl();
        givenForcedProperties();
        givenDockerClient();

        whenDockerClientMockSomeContainers();
        whenMockforContainerCreation();
        whenCreateContainer();
        whenRunContainer();
        whenStopContainer();

        thenTestIfNewContainerDoesNotExists();
    }

    /**
     * givens
     */

    private void givenDockerServiceImpl() {
        this.dockerService = new DockerServiceImpl();
    }

    private void givenDockerClient() {
        this.localDockerClient = mock(DockerClient.class, Mockito.RETURNS_DEEP_STUBS);
        this.dockerService.setDockerClient(this.localDockerClient);
    }

    private void givenEmptyProperties() {
        this.properties = new HashMap<>();
    }

    private void whenPropertiesChangeAfterInit(boolean b) {
        givenFullProperties(b);
    }

    private void givenFullProperties(boolean b) {
        this.properties = new HashMap<>();

        this.properties.put(DOCKER_HOST_URL, DEFAULT_DOCKER_HOST_URL);
        this.properties.put(IS_ENABLED, b);

        this.properties.put(REPOSITORY_ENABLED, DEFAULT_REPOSITORY_ENABLED);
        this.properties.put(REPOSITORY_URL, DEFAULT_REPOSITORY_URL);
        this.properties.put(REPOSITORY_USERNAME, DEFAULT_REPOSITORY_USERNAME);
        this.properties.put(REPOSITORY_PASSWORD, DEFAULT_REPOSITORY_PASSWORD);
    }

    private void givenFullProperties(boolean b, boolean customRepoEnabled) {
        this.properties = new HashMap<>();

        this.properties.put(DOCKER_HOST_URL, DEFAULT_DOCKER_HOST_URL);
        this.properties.put(IS_ENABLED, b);

        this.properties.put(REPOSITORY_ENABLED, customRepoEnabled);
        this.properties.put(REPOSITORY_URL, DEFAULT_REPOSITORY_URL);
        this.properties.put(REPOSITORY_USERNAME, DEFAULT_REPOSITORY_USERNAME);
        this.properties.put(REPOSITORY_PASSWORD, DEFAULT_REPOSITORY_PASSWORD);
    }

    private void givenAuthWithRepoAndCredentials() {
        this.properties = new HashMap<>();

        this.properties.put(DOCKER_HOST_URL, DEFAULT_DOCKER_HOST_URL);
        this.properties.put(IS_ENABLED, true);

        this.properties.put(REPOSITORY_ENABLED, true);
        this.properties.put(REPOSITORY_URL, "testrepo.net");
        this.properties.put(REPOSITORY_USERNAME, "Tester");
        this.properties.put(REPOSITORY_PASSWORD, "ng4#$fuhn834F84nf8nw8GF3");
    }

    private void givenForcedProperties() {
        this.dockerService.updated(this.properties);
    }

    /**
     * when
     */

    private void whenActivateInstance() {
        this.dockerService.activate(this.properties);
    }

    private void whenUpdateInstance() {
        this.dockerService.activate(this.properties);
    }

    private void whenDeactivateInstance() {
        this.dockerService.deactivate();
    }

    private void whenDockerClientMockHasNoContainers() {
        List<Container> containerListmock = new LinkedList<>();
        this.runningContainers = new String[0];
        this.runnignContainerDescriptor = new ContainerDescriptor[0];
        this.mockedListContainersCmd = mock(ListContainersCmd.class, Mockito.RETURNS_DEEP_STUBS);
        when(this.localDockerClient.listContainersCmd()).thenReturn(this.mockedListContainersCmd);
        when(this.mockedListContainersCmd.withShowAll(true)).thenReturn(this.mockedListContainersCmd);
        when(this.mockedListContainersCmd.exec()).thenReturn(containerListmock);
    }

    private void whenDockerClientMockSomeContainers() {
        List<Container> containerListmock = new LinkedList<>();
        // Build Container Mock
        Container mcont1 = mock(Container.class);
        when(mcont1.getId()).thenReturn("1f12d3s23");
        when(mcont1.toString()).thenReturn("1f12d3s23");
        when(mcont1.getNames()).thenReturn(new String[] { "jim", "/jim" });
        when(mcont1.getImage()).thenReturn("nginx");
        when(mcont1.getPorts()).thenReturn(new ContainerPort[0]);
        when(mcont1.getState()).thenReturn("running");
        containerListmock.add(mcont1);

        Container mcont2 = mock(Container.class);
        when(mcont2.getId()).thenReturn("1f134f3s4");
        when(mcont2.toString()).thenReturn("1f134f3s4");
        when(mcont2.getNames()).thenReturn(new String[] { "frank", "/frank" });
        when(mcont2.getImage()).thenReturn("nginx2");
        when(mcont2.getPorts()).thenReturn(new ContainerPort[0]);
        when(mcont2.getState()).thenReturn("running");
        containerListmock.add(mcont2);

        this.runningContainers = new String[] { mcont1.toString(), mcont2.toString() };

        // Build Respective CD's
        ContainerDescriptor mcontCD1 = ContainerDescriptor.builder().setContainerID(mcont1.getId())
                .setContainerName(mcont1.getNames()[0]).setContainerImage(mcont1.getImage()).build();
        new ContainerDescriptor();
        ContainerDescriptor mcontCD2 = ContainerDescriptor.builder().setContainerID(mcont2.getId())
                .setContainerName(mcont2.getNames()[0]).setContainerImage(mcont2.getImage()).build();
        this.runnignContainerDescriptor = new ContainerDescriptor[] { mcontCD1, mcontCD2 };

        this.mockedListContainersCmd = mock(ListContainersCmd.class, Mockito.RETURNS_DEEP_STUBS);
        when(this.localDockerClient.listContainersCmd()).thenReturn(this.mockedListContainersCmd);
        when(this.mockedListContainersCmd.withShowAll(true)).thenReturn(this.mockedListContainersCmd);
        when(this.mockedListContainersCmd.exec()).thenReturn(containerListmock);
    }

    private void whenDockerClientMockListNoContainers() {
        List<Container> containerListmock = new LinkedList<>();
        // Build Container Mock
        this.runningContainers = new String[0];

        this.mockedListContainersCmd = mock(ListContainersCmd.class, Mockito.RETURNS_DEEP_STUBS);
        when(this.localDockerClient.listContainersCmd()).thenReturn(this.mockedListContainersCmd);
        when(this.mockedListContainersCmd.withShowAll(true)).thenReturn(this.mockedListContainersCmd);
        when(this.mockedListContainersCmd.exec()).thenReturn(containerListmock);
    }

    private void whenMockforContainerCreation() {

        // Build Respective CD's
        ContainerDescriptor mcontCD1 = ContainerDescriptor.builder().setContainerID("1d3dewf34r5")
                .setContainerName("frank").setContainerImage("nginx").build();

        this.runnignContainerDescriptor = new ContainerDescriptor[] { mcontCD1 };

        CreateContainerCmd CCC = mock(CreateContainerCmd.class, Mockito.RETURNS_DEEP_STUBS);
        when(this.localDockerClient.createContainerCmd(mcontCD1.getContainerImage())).thenReturn(CCC);
        when(CCC.exec().getId()).thenReturn(mcontCD1.getContainerId());

        List<Image> images = new LinkedList<>();
        Image mockImage = mock(Image.class);

        when(mockImage.getRepoTags()).thenReturn(new String[] { "nginx", "latest", "nginx:latest" });

        images.add(mockImage);

        when(this.localDockerClient.listImagesCmd()).thenReturn(mock(ListImagesCmd.class));
        when(this.localDockerClient.listImagesCmd().exec()).thenReturn(images);

    }

    private void whenCreateContainer() {
        this.createdContainerID = this.dockerService.pullImageAndCreateContainer(this.runnignContainerDescriptor[0]);
    }

    private void whenRunContainer() {
        // startContainer
        this.dockerService.startContainer(this.runnignContainerDescriptor[0]);
    }

    private void whenStopContainer() {
        // startContainer
        this.dockerService.stopContainer(this.runnignContainerDescriptor[0]);
    }

    private void thenNotStoppedMicroservice() throws KuraException {
        verify(this.localDockerClient, times(0)).removeContainerCmd(any(String.class));
    }

    private void thenNotStartedMicroservice() throws KuraException {
        verify(this.localDockerClient, times(0)).startContainerCmd(any(String.class));
    }

    private void thenContainerListEqualsExpectedStringArray() {
        assertArrayEquals(this.dockerService.listContainers(), this.runningContainers);
    }

    private void thenContainerListByIdEqualsExpectedStringArray() {
        assertArrayEquals(this.dockerService.listContainersByID(), this.runningContainers);
    }

    private void thenContainerListByContainerDescriptorEqualsExpectedStringArray() {
        if (this.runnignContainerDescriptor.length > 0) {
            assertEquals(this.dockerService.listByContainerDescriptor().length, this.runnignContainerDescriptor.length);
        } else {
            assertArrayEquals(this.dockerService.listByContainerDescriptor(), this.runnignContainerDescriptor);
        }
    }

    private void thenGetFirstContainerIDbyName() {
        assertEquals(this.dockerService.getContainerIDbyName(this.runnignContainerDescriptor[0].getContainerName()),
                this.runnignContainerDescriptor[0].getContainerId());
    }

    private void thenTestIfNewContainerExists() {
        assertEquals(this.createdContainerID, this.runnignContainerDescriptor[0].getContainerId());
    }

    private void thenTestIfNewContainerDoesNotExists() {
        assertNotEquals(this.createdContainerID, this.runnignContainerDescriptor[0].getContainerId());
    }

}
