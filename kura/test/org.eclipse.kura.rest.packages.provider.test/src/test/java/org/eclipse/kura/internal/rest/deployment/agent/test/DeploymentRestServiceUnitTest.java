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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentPackageInfo;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
import org.eclipse.kura.rest.deployment.agent.api.DeploymentRequestStatus;
import org.eclipse.kura.rest.deployment.agent.api.DescriptorRequest;
import org.eclipse.kura.rest.deployment.agent.api.InstallRequest;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class DeploymentRestServiceUnitTest {

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    private DeploymentRestService deploymentRestService = new DeploymentRestService();

    private DeploymentRequestStatus resultingDeploymentRequestStatus;
    private List<DeploymentPackageInfo> resultingDepoloymentPackagesList;
    private Exception occurredException;

    private DeploymentAgentService mockDeploymentAgentService = mock(DeploymentAgentService.class);
    private DeploymentAdmin mockDeploymentAdmin = mock(DeploymentAdmin.class);
    private UserAdmin mockUserAdmin = mock(UserAdmin.class);
    private InputStream mockInputStream = mock(InputStream.class);
    private FormDataContentDisposition mockFormDataContent = mock(FormDataContentDisposition.class);

    private final ArrayList<DeploymentPackage> installedDeploymentPackages = new ArrayList<>();

    private MarketplacePackageDescriptor resultingMarketplacePackageDescriptor;

    @Test
    public void installDeploymentPackageWorksWithAreadyIssuedRequest() throws Exception {
        givenDeploymentRestService();

        givenAnInstallationRequestWasAlreadyIssuedFor("testPackage", true);

        whenAnInstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenDeploymentRequestStatusIs(DeploymentRequestStatus.INSTALLING);
        thenDeploymentAgentServiceIsNeverCalledToInstallDeploymentPackage();
    }

    @Test
    public void uninstallDeploymentPackageWorksWithAreadyIssuedRequest() throws Exception {
        givenDeploymentRestService();
        givenAnUninstallationRequestWasAlreadyIssuedFor("testPackage", true);

        whenAnUninstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenDeploymentRequestStatusIs(DeploymentRequestStatus.UNINSTALLING);
        thenDeploymentAgentServiceIsNeverCalledToUninstallDeploymentPackage();
    }

    @Test
    public void installDeploymentPackageWorksWhenErrorIsThrown() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceThrowsExceptionOnInstall();

        whenAnInstallationRequestIsIssuedFor("testPackage");

        thenExceptionOccurred(WebApplicationException.class);
        thenDeploymentAgentServiceIsCalledToInstallUrl("testPackage");
    }

    @Test
    public void installDeploymentPackageWorksWithNullRequest() throws Exception {
        givenDeploymentRestService();

        whenAnInstallationRequestIsIssuedFor(null);

        thenExceptionOccurred(WebApplicationException.class);
        thenDeploymentAgentServiceIsNeverCalledToInstallDeploymentPackage();
    }

    @Test
    public void installDeploymentPackageWorks() throws Exception {
        givenDeploymentRestService();

        givenAnInstallationRequestWasAlreadyIssuedFor("testPackage", false);

        whenAnInstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenDeploymentRequestStatusIs(DeploymentRequestStatus.REQUEST_RECEIVED);
        thenDeploymentAgentServiceIsCalledToInstallUrl("testPackage");
    }

    @Test
    public void uninstallDeploymentPackageWorksWhenExceptionIsThrown() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceThrowsExceptionOnUninstall();

        whenAnUninstallationRequestIsIssuedFor("testPackage");

        thenExceptionOccurred(WebApplicationException.class);
        thenDeploymentAgentServiceIsCalledToUninstall("testPackage");
    }

    @Test
    public void uninstallDeploymentPackageWorks() throws Exception {
        givenDeploymentRestService();
        givenAnUninstallationRequestWasAlreadyIssuedFor("testPackage", false);

        whenAnUninstallationRequestIsIssuedFor("testPackage");

        thenNoExceptionOccurred();
        thenDeploymentRequestStatusIs(DeploymentRequestStatus.REQUEST_RECEIVED);
        thenDeploymentAgentServiceIsCalledToUninstall("testPackage");
    }

    @Test
    public void listDeploymentPackagesWorksWithEmptyList() {
        givenDeploymentRestService();
        givenInstalledDeploymentPackageList();

        whenListDeploymentPackagesIsCalled();

        thenResponsePackageListHaveSize(0);
    }

    @Test
    public void listDeploymentPackagesWorksWithInstalledPackages() {
        givenDeploymentRestService();
        givenInstalledDeploymentPackageWith("testPackage", "1.0.0");
        givenInstalledDeploymentPackageList();

        whenListDeploymentPackagesIsCalled();

        thenResponsePackageListHaveSize(1);
        thenResponsePackageListContains("testPackage", "1.0.0");
    }

    @Test
    public void restDeployRoleGetsCreated() throws InterruptedException {
        givenDeploymentRestService();

        thenRoleIsCreated("kura.permission.rest.deploy", Role.GROUP);
    }

    @Test
    public void installUploadedDeploymentPackageWorks() throws Exception {
        givenDeploymentRestService();
        givenAMockInputStream();
        givenAMockFormDataContentWithFileName("mock.dp");

        whenInstallUploadedDeploymentPackageIsCalledWith(this.mockInputStream, this.mockFormDataContent);

        thenNoExceptionOccurred();
        thenDeploymentRequestStatusIs(DeploymentRequestStatus.REQUEST_RECEIVED);
        thenDeploymentAgentServiceIsCalledToInstallLocalUrl();
    }

    @Test
    public void installUploadedDeploymentPackageThrowsOnInputStreamReadFailure() throws Exception {
        givenDeploymentRestService();
        givenAMockInputStreamThrowingOnRead();
        givenAMockFormDataContentWithFileName("mock.dp");

        whenInstallUploadedDeploymentPackageIsCalledWith(this.mockInputStream, this.mockFormDataContent);

        thenExceptionOccurred(WebApplicationException.class);
        thenDeploymentAgentServiceIsNeverCalledToInstallDeploymentPackage();
    }

    @Test
    public void getMarketplacePackageDescriptorThrowsWithNullUrl() throws Exception {
        givenDeploymentRestService();

        whenGetMarketplacePackageDescriptorIsCalledFor(null);

        thenExceptionOccurred(WebApplicationException.class);
    }

    @Test
    public void getMarketplacePackageDescriptorThrowsWithEmptyUrl() throws Exception {
        givenDeploymentRestService();

        whenGetMarketplacePackageDescriptorIsCalledFor("");

        thenExceptionOccurred(WebApplicationException.class);
    }

    @Test
    public void getMarketplacePackageDescriptorThrowsWithWrongUrl() throws Exception {
        givenDeploymentRestService();

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "https://marketplace.ellipse.org/marketplace-client-intro?mpc_install=");

        thenExceptionOccurred(WebApplicationException.class);
    }

    @Test
    public void getMarketplacePackageDescriptorThrowsWithMissingNodeId() throws Exception {
        givenDeploymentRestService();

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=");

        thenExceptionOccurred(WebApplicationException.class);
    }

    @Test
    public void getMarketplacePackageDescriptorThrowsWhenDeploymentAgentServiceThrows() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceThrowsExceptionOnGetMarketplacePackageDescriptor();

        whenGetMarketplacePackageDescriptorIsCalledFor(null);

        thenExceptionOccurred(WebApplicationException.class);
    }

    @Test
    public void getMarketplacePackageDescriptorWorksWithUrl() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceReturnsMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder()
                .nodeId("testNodeId").url("testUrl").dpUrl("testDpUrl").minKuraVersion("1.0.0").maxKuraVersion("2.0.0")
                .currentKuraVersion("1.0.0").isCompatible(true).build());

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=55535");

        thenNoExceptionOccurred();
        thenDeploymentAgentServiceIsCalledWithURL("https://marketplace.eclipse.org/node/55535/api/p");
        thenResultingPackageDescriptorEquals(MarketplacePackageDescriptor.builder().nodeId("testNodeId").url("testUrl")
                .dpUrl("testDpUrl").minKuraVersion("1.0.0").maxKuraVersion("2.0.0").currentKuraVersion("1.0.0")
                .isCompatible(true).build());
    }

    @Test
    public void getMarketplacePackageDescriptorWorksWithHTTPUrl() throws Exception {
        givenDeploymentRestService();
        givenDeploymentAgentServiceReturnsMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder()
                .nodeId("testNodeId").url("testUrl").dpUrl("testDpUrl2").minKuraVersion("1.1.0").maxKuraVersion("5.4.0")
                .currentKuraVersion("5.4.0").isCompatible(true).build());

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=69");

        thenNoExceptionOccurred();
        thenDeploymentAgentServiceIsCalledWithURL("https://marketplace.eclipse.org/node/69/api/p");
        thenResultingPackageDescriptorEquals(MarketplacePackageDescriptor.builder().nodeId("testNodeId").url("testUrl")
                .dpUrl("testDpUrl2").minKuraVersion("1.1.0").maxKuraVersion("5.4.0").currentKuraVersion("5.4.0")
                .isCompatible(true).build());
    }

    /*
     * GIVEN
     */
    private void givenDeploymentRestService() {
        deploymentRestService.setDeploymentAgentService(this.mockDeploymentAgentService);
        deploymentRestService.setDeploymentAdmin(this.mockDeploymentAdmin);
        deploymentRestService.setUserAdmin(this.mockUserAdmin);
    }

    private void givenInstalledDeploymentPackageWith(String name, String version) {
        DeploymentPackage dp = mock(DeploymentPackage.class);
        when(dp.getName()).thenReturn(name);
        when(dp.getVersion()).thenReturn(new Version(version));
        this.installedDeploymentPackages.add(dp);
    }

    private void givenInstalledDeploymentPackageList() {
        DeploymentPackage[] deploymentPackagesArray = new DeploymentPackage[this.installedDeploymentPackages.size()];
        deploymentPackagesArray = this.installedDeploymentPackages.toArray(deploymentPackagesArray);
        when(this.mockDeploymentAdmin.listDeploymentPackages()).thenReturn(deploymentPackagesArray);
    }

    private void givenAnInstallationRequestWasAlreadyIssuedFor(String url, boolean alreadyIssued) {
        when(this.mockDeploymentAgentService.isInstallingDeploymentPackage(url)).thenReturn(alreadyIssued);
    }

    private void givenAnUninstallationRequestWasAlreadyIssuedFor(String name, boolean alreadyIssued) {
        when(this.mockDeploymentAgentService.isUninstallingDeploymentPackage(name)).thenReturn(alreadyIssued);
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnUninstall() throws Exception {
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.mockDeploymentAgentService)
                .uninstallDeploymentPackageAsync(any());
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnInstall() throws Exception {
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.mockDeploymentAgentService)
                .installDeploymentPackageAsync(any());
    }

    private void givenAMockFormDataContentWithFileName(String fileName) {
        when(this.mockFormDataContent.getFileName()).thenReturn(fileName);
    }

    private void givenAMockInputStream() throws IOException {
        when(this.mockInputStream.read(any())).thenReturn(-1);
    }

    private void givenAMockInputStreamThrowingOnRead() throws IOException {
        when(this.mockInputStream.read(any())).thenThrow(new IOException());
    }

    private void givenDeploymentAgentServiceReturnsMarketplacePackageDescriptor(MarketplacePackageDescriptor descriptorToBeReturned) {
        when(this.mockDeploymentAgentService.getMarketplacePackageDescriptor(any())).thenReturn(descriptorToBeReturned);
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnGetMarketplacePackageDescriptor() {
        when(this.mockDeploymentAgentService.getMarketplacePackageDescriptor(any()))
                .thenThrow(new RuntimeException());
    }

    /*
     * WHEN
     */
    private void whenAnInstallationRequestIsIssuedFor(String url) {
        try {
            InstallRequest installRequest = new InstallRequest(url);
            this.resultingDeploymentRequestStatus = deploymentRestService.installDeploymentPackage(installRequest);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenAnUninstallationRequestIsIssuedFor(String name) {
        try {
            this.resultingDeploymentRequestStatus = deploymentRestService.uninstallDeploymentPackage(name);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenListDeploymentPackagesIsCalled() {
        try {
            this.resultingDepoloymentPackagesList = deploymentRestService.listDeploymentPackages();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenInstallUploadedDeploymentPackageIsCalledWith(InputStream mockInputStream,
            FormDataContentDisposition mockFormDataContent) {
        try {
            this.resultingDeploymentRequestStatus = this.deploymentRestService
                    .installUploadedDeploymentPackage(mockInputStream, mockFormDataContent);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetMarketplacePackageDescriptorIsCalledFor(String url) {
        try {
            this.resultingMarketplacePackageDescriptor = this.deploymentRestService
                    .getMarketplacePackageDescriptor(new DescriptorRequest(url));
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * THEN
     */

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

    private void thenDeploymentRequestStatusIs(DeploymentRequestStatus expectedResponse) {
        assertNotNull(this.resultingDeploymentRequestStatus);
        assertEquals(expectedResponse, this.resultingDeploymentRequestStatus);
    }

    private void thenResponsePackageListHaveSize(int expectedSize) {
        assertNotNull(this.resultingDepoloymentPackagesList);
        assertEquals(expectedSize, this.resultingDepoloymentPackagesList.size());
    }

    private void thenResponsePackageListContains(String name, String version) {
        assertNotNull(this.resultingDepoloymentPackagesList);

        for (DeploymentPackageInfo dp : this.resultingDepoloymentPackagesList) {
            if (dp.getName().equals(name) && dp.getVersion().equals(version)) {
                return;
            }
        }

        fail(String.format("Package %s:%s not found", name, version));
    }

    private void thenRoleIsCreated(String role, int type) {
        verify(this.mockUserAdmin, times(1)).createRole(role, type);
    }

    private void thenDeploymentAgentServiceIsCalledToUninstall(String name) throws Exception {
        verify(this.mockDeploymentAgentService, times(1)).uninstallDeploymentPackageAsync(name);
    }

    private void thenDeploymentAgentServiceIsCalledToInstallUrl(String url) throws Exception {
        verify(this.mockDeploymentAgentService, times(1)).installDeploymentPackageAsync(url);
    }

    private void thenDeploymentAgentServiceIsCalledToInstallLocalUrl() throws Exception {
        final String localUri = System.getProperty(JAVA_IO_TMPDIR) + File.separator;
        final Path localPath = Paths.get(localUri);

        verify(this.mockDeploymentAgentService, times(1))
                .installDeploymentPackageAsync(startsWith(localPath.toUri().toURL().toString()));
    }

    private void thenDeploymentAgentServiceIsNeverCalledToInstallDeploymentPackage() throws Exception {
        verify(this.mockDeploymentAgentService, never()).installDeploymentPackageAsync(any());
    }

    private void thenDeploymentAgentServiceIsNeverCalledToUninstallDeploymentPackage() throws Exception {
        verify(this.mockDeploymentAgentService, never()).uninstallDeploymentPackageAsync(any());
    }

    private void thenResultingPackageDescriptorEquals(MarketplacePackageDescriptor expectedDescriptor) {
        assertEquals(expectedDescriptor, this.resultingMarketplacePackageDescriptor);
    }

    private void thenDeploymentAgentServiceIsCalledWithURL(String expectedUrl) {
        verify(this.mockDeploymentAgentService, times(1)).getMarketplacePackageDescriptor(expectedUrl);
    }

}