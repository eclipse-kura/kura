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
  *******************************************************************************/

package org.eclipse.kura.container.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.container.orchestration.RegistryCredentials;
import org.eclipse.kura.container.signature.ContainerSignatureValidationService;
import org.eclipse.kura.container.signature.ValidationResult;
import org.junit.After;
import org.junit.Test;

public class ContainerInstanceTest {

    private static final String CONTAINER_NAME = "container.name";
    private static final String CONTAINER_IMAGE_TAG = "container.image.tag";
    private static final String CONTAINER_IMAGE = "container.image";
    private static final String CONTAINER_ENABLED = "container.enabled";
    private static final String CONTAINER_TRUST_ANCHOR = "container.signature.trust.anchor";
    private static final String CONTAINER_VERIFY_TLOG = "container.signature.verify.transparency.log";
    private static final String CONTAINER_REGISTRY_USERNAME = "registry.username";
    private static final String CONTAINER_REGISTRY_PASSWORD = "registry.password";

    private static final ValidationResult FAILED_VALIDATION = new ValidationResult();

    private static final String CONTAINER_STATE_CREATED = "Created";
    private static final String CONTAINER_STATE_DISABLED = "Disabled";

    private ContainerOrchestrationService mockContainerOrchestrationService = mock(ContainerOrchestrationService.class);
    private ContainerSignatureValidationService mockContainerSignatureValidationService = mock(
            ContainerSignatureValidationService.class);
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, Object> newProperties = new HashMap<>();
    private ContainerInstance containerInstance = new ContainerInstance();
    private Exception occurredException;

    @Test
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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);
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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);
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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);

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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);

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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);
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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);
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
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
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
        givenContainerStateIs(CONTAINER_STATE_CREATED);

        givenContainerOrchestratorWithRunningContainer("pippo", "1234");
        givenNewPropertiesWith(CONTAINER_ENABLED, false);

        whenUpdateInstanceIsCalledWith(this.newProperties);

        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);
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
        givenContainerStateIs(CONTAINER_STATE_CREATED);

        givenContainerOrchestratorWithRunningContainer("pippo", "1234");

        whenDeactivateInstanceIsCalled();

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_DISABLED);
        thenStopContainerWasCalledFor("1234");
        thenDeleteContainerWasCalledFor("1234");
    }

    @Test
    public void signatureValidationDoesntGetCalledWithMissingTrustAnchor() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenContainerSignatureValidationServiceReturningFailureFor("nginx", "latest");
        givenContainerInstanceWith(this.mockContainerSignatureValidationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");
        givenPropertiesWith(CONTAINER_VERIFY_TLOG, true);

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
        thenStartContainerWasCalledWith(this.properties);
        thenVerifySignatureWasNeverCalled();
    }

    @Test
    public void signatureValidationDoesntGetCalledWithInvalidTrustAnchor() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenContainerSignatureValidationServiceReturningFailureFor("nginx", "latest");
        givenContainerInstanceWith(this.mockContainerSignatureValidationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");
        givenPropertiesWith(CONTAINER_TRUST_ANCHOR, "");
        givenPropertiesWith(CONTAINER_VERIFY_TLOG, true);

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
        thenStartContainerWasCalledWith(this.properties);
        thenVerifySignatureWasNeverCalled();
    }

    @Test
    public void signatureValidationDoesntGetCalledWithMissingSignatureValidationService()
            throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");
        givenPropertiesWith(CONTAINER_TRUST_ANCHOR, "anotherRealTrustAnchor ;)");
        givenPropertiesWith(CONTAINER_VERIFY_TLOG, true);

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
        thenStartContainerWasCalledWith(this.properties);
        thenVerifySignatureWasNeverCalled();
    }

    @Test
    public void signatureValidationWorks() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenContainerSignatureValidationServiceReturningSuccessFor("nginx", "latest");
        givenContainerInstanceWith(this.mockContainerSignatureValidationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");
        givenPropertiesWith(CONTAINER_TRUST_ANCHOR, "aRealTrustAnchor ;)");
        givenPropertiesWith(CONTAINER_VERIFY_TLOG, true);

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
        thenStartContainerWasCalledWith(this.properties);
        thenVerifySignatureWasCalledFor("nginx", "latest", "aRealTrustAnchor ;)", true);
    }

    @Test
    public void signatureValidationWorksWithThrowingValidationService() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenContainerSignatureValidationServiceThrows();
        givenContainerInstanceWith(this.mockContainerSignatureValidationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");
        givenPropertiesWith(CONTAINER_TRUST_ANCHOR, "aRealTrustAnchor ;)");
        givenPropertiesWith(CONTAINER_VERIFY_TLOG, true);

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
        thenStartContainerWasCalledWith(this.properties);
        thenVerifySignatureWasCalledFor("nginx", "latest", "aRealTrustAnchor ;)", true);
    }

    @Test
    public void signatureValidationWorksWithAuthentication() throws KuraException, InterruptedException {
        givenContainerOrchestratorWithNoRunningContainers();
        givenContainerOrchestratorReturningOnStart("1234");
        givenContainerInstanceWith(this.mockContainerOrchestrationService);

        givenContainerSignatureValidationServiceReturningFailureForAuthenticated("nginx", "latest");
        givenContainerInstanceWith(this.mockContainerSignatureValidationService);

        givenPropertiesWith(CONTAINER_ENABLED, true);
        givenPropertiesWith(CONTAINER_NAME, "pippo");
        givenPropertiesWith(CONTAINER_IMAGE, "nginx");
        givenPropertiesWith(CONTAINER_IMAGE_TAG, "latest");
        givenPropertiesWith(CONTAINER_TRUST_ANCHOR, "aRealTrustAnchor ;)");
        givenPropertiesWith(CONTAINER_VERIFY_TLOG, true);
        givenPropertiesWith(CONTAINER_REGISTRY_USERNAME, "username");
        givenPropertiesWith(CONTAINER_REGISTRY_PASSWORD, "password");

        whenActivateInstanceIsCalledWith(this.properties);

        thenNoExceptionOccurred();
        thenWaitForContainerInstanceToBecome(CONTAINER_STATE_CREATED);
        thenStartContainerWasCalledWith(this.properties);
        thenAuthenticatedVerifySignatureWasCalledFor("nginx", "latest", "aRealTrustAnchor ;)", true,
                new PasswordRegistryCredentials(Optional.empty(), "username", new Password("password")));
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

    private void givenContainerStateIs(String expectedState) {
        int count = 10;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!expectedState.equals(this.containerInstance.getState()) && count-- > 0);

        if (count <= 0) {
            fail(String.format("Container instance state is not \"%s\" (currently \"%s\")", expectedState,
                    this.containerInstance.getState()));
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

    private void givenContainerSignatureValidationServiceReturningFailureFor(String imageName, String imageTag)
            throws KuraException {
        when(this.mockContainerSignatureValidationService.verify(eq(imageName), eq(imageTag), any(String.class),
                any(Boolean.class))).thenReturn(FAILED_VALIDATION);
    }

    private void givenContainerSignatureValidationServiceReturningSuccessFor(String imageName, String imageTag)
            throws KuraException {
        // Generate random sha256 string
        String sha256 = "sha256:" + Long.toHexString(Double.doubleToLongBits(Math.random()));
        when(this.mockContainerSignatureValidationService.verify(eq(imageName), eq(imageTag), any(String.class),
                any(Boolean.class))).thenReturn(new ValidationResult(true, sha256));
    }

    private void givenContainerSignatureValidationServiceReturningFailureForAuthenticated(String imageName,
            String imageTag) throws KuraException {
        when(this.mockContainerSignatureValidationService.verify(eq(imageName), eq(imageTag), any(String.class),
                any(Boolean.class), any(RegistryCredentials.class))).thenReturn(FAILED_VALIDATION);
    }

    private void givenContainerSignatureValidationServiceThrows() throws KuraException {
        when(this.mockContainerSignatureValidationService.verify(any(String.class), any(String.class),
                any(String.class), any(Boolean.class))).thenThrow(new KuraException(KuraErrorCode.SECURITY_EXCEPTION));
    }

    private void givenContainerInstanceWith(ContainerSignatureValidationService signatureValidationService) {
        this.containerInstance.setContainerSignatureValidationService(signatureValidationService);
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

    private void thenWaitForContainerInstanceToBecome(String expectedState) {
        int count = 10;
        do {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!expectedState.equals(this.containerInstance.getState()) && count-- > 0);

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

    private void thenVerifySignatureWasNeverCalled() throws KuraException {
        verify(this.mockContainerSignatureValidationService, never()).verify(any(String.class), any(String.class),
                any(String.class), any(Boolean.class));
        verify(this.mockContainerSignatureValidationService, never()).verify(any(String.class), any(String.class),
                any(String.class), any(Boolean.class), any(RegistryCredentials.class));
    }

    private void thenVerifySignatureWasCalledFor(String imageName, String imageTag, String trustAnchor,
            boolean verifyTlog) throws KuraException {
        verify(this.mockContainerSignatureValidationService, times(1)).verify(imageName, imageTag, trustAnchor,
                verifyTlog);
    }

    private void thenAuthenticatedVerifySignatureWasCalledFor(String imageName, String imageTag, String trustAnchor,
            boolean verifyTlog, PasswordRegistryCredentials passwordRegistryCredentials) throws KuraException {
        verify(this.mockContainerSignatureValidationService, times(1)).verify(imageName, imageTag, trustAnchor,
                verifyTlog, passwordRegistryCredentials);
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
