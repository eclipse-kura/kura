/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentPackageInfo;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
import org.eclipse.kura.rest.deployment.agent.api.DeploymentRequestStatus;
import org.eclipse.kura.rest.deployment.agent.api.InstallRequest;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class DeploymentRestServiceUnitTest {

    private DeploymentRestService deploymentRestService = new DeploymentRestService();
    private DeploymentAgentService mockDeploymentAgentService = mock(DeploymentAgentService.class);
    private UserAdmin mockUserAdmin = mock(UserAdmin.class);
    private DeploymentRequestStatus resultingDeploymentRequestStatus;

    private Exception occurredException;

    @Test
    public void installDeploymentPackageWorksWithAreadyIssuedRequest() {
        givenDeploymentRestService();

        givenAnInstallationRequestWasAlreadyIssuedFor("testPackage", true);

        whenAnInstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenInstallationResponseIs(DeploymentRequestStatus.INSTALLING);
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

    private void thenInstallationResponseIs(DeploymentRequestStatus expectedResponse) {
        assertNotNull(this.resultingDeploymentRequestStatus);
        assertEquals(expectedResponse, this.resultingDeploymentRequestStatus);
    }

    private void whenAnInstallationRequestIsIssuedFor(String url) {
        try {
            InstallRequest installRequest = new InstallRequest(url);
            this.resultingDeploymentRequestStatus = deploymentRestService.installDeploymentPackage(installRequest);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void givenAnInstallationRequestWasAlreadyIssuedFor(String url, boolean alreadyIssued) {
        when(this.mockDeploymentAgentService.isInstallingDeploymentPackage(url)).thenReturn(alreadyIssued);
    }

    private void givenDeploymentRestService() {
        deploymentRestService.setDeploymentAgentService(this.mockDeploymentAgentService);
        deploymentRestService.setUserAdmin(this.mockUserAdmin);
    }

    @Test
    public void uninstallDeploymentPackageWorksWithAreadyIssuedRequest() {
        givenDeploymentRestService();
        givenAnUninstallationRequestWasAlreadyIssuedFor("testPackage", true);

        whenAnUninstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenInstallationResponseIs(DeploymentRequestStatus.UNINSTALLING);
    }

    private void whenAnUninstallationRequestIsIssuedFor(String name) {
        try {
            this.resultingDeploymentRequestStatus = deploymentRestService.uninstallDeploymentPackage(name);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void givenAnUninstallationRequestWasAlreadyIssuedFor(String name, boolean alreadyIssued) {
        when(this.mockDeploymentAgentService.isUninstallingDeploymentPackage(name)).thenReturn(alreadyIssued);
    }

    @Test
    public void testInstallDPError() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceThrowsExceptionOnInstall();

        whenAnInstallationRequestIsIssuedFor("testPackage");

        thenExceptionOccurred(WebApplicationException.class);
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnInstall() throws Exception {
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.mockDeploymentAgentService)
                .installDeploymentPackageAsync(any());
    }

    @Test
    public void testInstallDPNullRequest() {
        givenDeploymentRestService();

        whenAnInstallationRequestIsIssuedFor(null);

        thenExceptionOccurred(WebApplicationException.class);
    }

    @Test
    public void testInstallDP() {
        givenDeploymentRestService();

        givenAnInstallationRequestWasAlreadyIssuedFor("testPackage", false);

        whenAnInstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenInstallationResponseIs(DeploymentRequestStatus.REQUEST_RECEIVED);
    }

    @Test
    public void testUninstallDPError() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceThrowsExceptionOnUninstall();

        whenAnUninstallationRequestIsIssuedFor("testPackage");

        thenExceptionOccurred(WebApplicationException.class);
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnUninstall() throws Exception {
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.mockDeploymentAgentService)
                .uninstallDeploymentPackageAsync(any());
    }

    @Test
    public void testUninstallDP() {
        givenDeploymentRestService();
        givenAnUninstallationRequestWasAlreadyIssuedFor("testPackage", false);

        whenAnUninstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenInstallationResponseIs(DeploymentRequestStatus.REQUEST_RECEIVED);
    }

    @Test
    public void testListPackagesEmptyResult() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        deploymentRestService.setDeploymentAdmin(deploymentAdmin);
        when(deploymentAdmin.listDeploymentPackages()).thenReturn(new DeploymentPackage[0]);

        List<DeploymentPackageInfo> result = deploymentRestService.listDeploymentPackages();
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testListPackages() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        deploymentRestService.setDeploymentAdmin(deploymentAdmin);
        DeploymentPackage[] deploymentPackages = new DeploymentPackage[1];
        DeploymentPackage mockDeploymentPackage = mock(DeploymentPackage.class);
        deploymentPackages[0] = mockDeploymentPackage;
        when(mockDeploymentPackage.getName()).thenReturn("testPackage");
        when(mockDeploymentPackage.getVersion()).thenReturn(new Version("1.0.0"));
        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deploymentPackages);

        List<DeploymentPackageInfo> result = deploymentRestService.listDeploymentPackages();
        assertNotNull(result);
        assertEquals(1, result.size());

        DeploymentPackageInfo content = result.get(0);

        assertEquals("testPackage", content.getName());
        assertEquals("1.0.0", content.getVersion());
    }

    @Test
    public void testActivateWUserAdmin() throws InterruptedException {
        givenDeploymentRestService();

        thenRoleIsCreated("kura.permission.rest.deploy", Role.GROUP);
    }

    private void thenRoleIsCreated(String role, int type) {
        verify(this.mockUserAdmin, times(1)).createRole(role, type);
    }

}
