/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.deployment.agent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.internal.rest.deployment.agent.ConcurrentUploadLimiter;
import org.eclipse.kura.internal.rest.deployment.agent.ConcurrentUploadLimiter.Guard;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;

public class ConcurrentUploadLimiterTest {

    private final DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
    private final ConcurrentUploadLimiter limiter = new ConcurrentUploadLimiter();
    private final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

    private final File[] files = new File[3];

    private ContainerRequestContext firstRequest;
    private ContainerRequestContext rejectedRequest;
    private WebApplicationException rejection;

    @Before
    public void setUp() throws IOException {
        this.limiter.setDeploymentAgentService(this.deploymentAgentService);

        for (int i = 0; i < this.files.length; i++) {
            this.files[i] = File.createTempFile("limiter-test", ".dp");
        }
    }

    @After
    public void tearDown() {
        for (final File file : this.files) {
            file.delete();
        }
    }

    @Test
    public void shouldAdmitTheFirstRequest() throws IOException {
        this.firstRequest = newRequest();

        this.limiter.filter(this.firstRequest);

        assertTrue(ConcurrentUploadLimiter.guardFromContext(this.firstRequest).isPresent());
    }

    @Test
    public void shouldRejectARequestWhileAnotherOneIsBeingReceived() throws IOException {
        givenARequestBeingReceived();

        whenARequestIsFiltered();

        thenTheRequestIsRejected();
        thenTheRejectedRequestHasNoGuard();
    }

    @Test
    public void shouldAdmitARequestAfterTheOneBeingReceivedCompletesWithoutInstalling() throws IOException {
        givenARequestBeingReceived();

        whenTheFirstRequestCompletes();
        whenARequestIsFiltered();

        thenTheRequestIsAdmitted();
    }

    @Test
    public void shouldKeepRejectingWhileTheInstallIsInProgress() throws IOException {
        givenAnInstallInProgressFor(0);

        whenTheFirstRequestCompletes();
        whenARequestIsFiltered();

        thenTheRequestIsRejected();
        thenTheFileExists(0);
    }

    @Test
    public void shouldNotBeReleasedByARejectedRequest() throws IOException {
        givenAnInstallInProgressFor(0);

        whenARequestIsFiltered();
        whenTheRejectedRequestCompletes();
        whenAnotherRequestIsFiltered();

        thenTheRequestIsRejected();
        thenTheFileExists(0);
    }

    @Test
    public void shouldAdmitARequestAfterTheInstallFinishes() throws IOException {
        givenAnInstallInProgressFor(0);

        whenTheInstallFinishes(0);
        whenARequestIsFiltered();

        thenTheRequestIsAdmitted();
    }

    @Test
    public void shouldDeleteTheUploadedFileWhenTheInstallFinishes() throws IOException {
        givenAnInstallInProgressFor(0);

        whenTheInstallFinishes(0);

        thenTheFileDoesNotExist(0);
    }

    @Test
    public void shouldIgnoreTheCompletionOfAnotherInstall() throws IOException {
        givenAnInstallInProgressFor(0);

        whenTheInstallFinishes(1);
        whenARequestIsFiltered();

        thenTheRequestIsRejected();
        thenTheFileExists(0);
    }

    @Test
    public void shouldAdmitARequestWhenTheCompletionEventWasLost() throws IOException {
        givenAnInstallInProgressFor(0);
        givenTheAgentIsInstalling(0, false);

        whenARequestIsFiltered();

        thenTheRequestIsAdmitted();
        thenTheFileDoesNotExist(0);
    }

    @Test
    public void shouldAdmitARequestAfterTheInstallCouldNotBeQueued() throws IOException {
        givenARequestBeingReceived();

        whenTheInstallIsAboutToStart(0);
        whenTheInstallCannotBeQueued();
        whenTheFirstRequestCompletes();
        whenARequestIsFiltered();

        thenTheRequestIsAdmitted();
        thenTheFileDoesNotExist(0);
    }

    @Test
    public void shouldNotTreatAnInstallThatIsNotQueuedYetAsStale() throws IOException {
        givenARequestBeingReceived();
        givenTheAgentIsInstalling(0, false);

        whenTheInstallIsAboutToStart(0);
        whenARequestIsFiltered();

        thenTheRequestIsRejected();
        thenTheFileExists(0);
    }

    @Test
    public void shouldNotLetAnOldRequestReleaseANewOne() throws IOException {
        givenARequestBeingReceived();

        // the install completes before the first request has been notified that it started
        whenTheInstallIsAboutToStart(0);
        whenTheInstallFinishes(0);
        whenARequestIsFiltered();
        whenTheFirstRequestReportsTheInstallStarted(0);
        whenTheFirstRequestCompletes();
        whenAnotherRequestIsFiltered();

        thenTheRequestIsRejected();
    }

    private void givenARequestBeingReceived() throws IOException {
        this.firstRequest = newRequest();
        this.limiter.filter(this.firstRequest);
    }

    private void givenAnInstallInProgressFor(final int file) throws IOException {
        givenARequestBeingReceived();
        givenTheAgentIsInstalling(file, true);

        whenTheInstallIsAboutToStart(file);
        guard(this.firstRequest).onInstallStarted(url(file));
    }

    private void givenTheAgentIsInstalling(final int file, final boolean installing) {
        when(this.deploymentAgentService.isInstallingDeploymentPackage(url(file))).thenReturn(installing);
    }

    private void whenARequestIsFiltered() {
        this.rejectedRequest = newRequest();
        this.rejection = filterAndGetRejection(this.rejectedRequest);
    }

    private void whenAnotherRequestIsFiltered() {
        this.rejection = filterAndGetRejection(newRequest());
    }

    private void whenTheInstallIsAboutToStart(final int file) {
        guard(this.firstRequest).onInstallStarting(url(file));
    }

    private void whenTheFirstRequestReportsTheInstallStarted(final int file) {
        guard(this.firstRequest).onInstallStarted(url(file));
    }

    private void whenTheInstallCannotBeQueued() {
        guard(this.firstRequest).onFailure();
    }

    private void whenTheFirstRequestCompletes() throws IOException {
        this.limiter.filter(this.firstRequest, this.responseContext);
    }

    private void whenTheRejectedRequestCompletes() throws IOException {
        this.limiter.filter(this.rejectedRequest, this.responseContext);
    }

    private void whenTheInstallFinishes(final int file) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(DeploymentAgentService.EVENT_PACKAGE_URL, url(file));
        properties.put(DeploymentAgentService.EVENT_SUCCESSFUL, true);

        this.limiter.handleEvent(new Event(DeploymentAgentService.EVENT_INSTALLED_TOPIC, properties));
    }

    private void thenTheRequestIsAdmitted() {
        assertNull("the request should have been admitted", this.rejection);
    }

    private void thenTheRequestIsRejected() {
        if (this.rejection == null) {
            fail("the request should have been rejected");
        }

        assertEquals(429, this.rejection.getResponse().getStatus());
        assertFalse(this.rejection.getResponse().getHeaders().containsKey("Retry-After"));
    }

    private void thenTheRejectedRequestHasNoGuard() {
        assertFalse(ConcurrentUploadLimiter.guardFromContext(this.rejectedRequest).isPresent());
    }

    private void thenTheFileExists(final int file) {
        assertTrue(this.files[file].exists());
    }

    private void thenTheFileDoesNotExist(final int file) {
        assertFalse(this.files[file].exists());
    }

    private WebApplicationException filterAndGetRejection(final ContainerRequestContext request) {
        try {
            this.limiter.filter(request);
            return null;
        } catch (final WebApplicationException e) {
            return e;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String url(final int file) {
        return this.files[file].toURI().toString();
    }

    private static Guard guard(final ContainerRequestContext request) {
        return ConcurrentUploadLimiter.guardFromContext(request)
                .orElseThrow(() -> new IllegalStateException("the request has no guard"));
    }

    private static ContainerRequestContext newRequest() {
        final Map<String, Object> properties = new HashMap<>();
        final ContainerRequestContext request = mock(ContainerRequestContext.class);

        doAnswer(i -> properties.put(i.getArgument(0), i.getArgument(1))).when(request).setProperty(anyString(),
                any());
        when(request.getProperty(anyString())).thenAnswer(i -> properties.get(i.getArgument(0)));

        return request;
    }
}
