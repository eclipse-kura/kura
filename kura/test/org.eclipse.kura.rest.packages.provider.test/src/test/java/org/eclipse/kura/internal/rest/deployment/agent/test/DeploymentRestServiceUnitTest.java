/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package org.eclipse.kura.internal.rest.deployment.agent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
//import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

import org.eclipse.kura.internal.rest.deployment.agent.DeploymentPackageInfo;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
import org.eclipse.kura.rest.deployment.agent.api.DeploymentRequestStatus;
import org.eclipse.kura.rest.deployment.agent.api.InstallRequest;

public class DeploymentRestServiceUnitTest {

    public void testIsInstallingDeploymentPackage() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        when(deploymentAgentService.isInstallingDeploymentPackage("testPackage")).thenReturn(true);

        InstallRequest installRequest = new InstallRequest("testPackage");

        DeploymentRequestStatus result = deploymentRestService.installDeploymentPackage(installRequest);

        assertNotNull(result);
        assertEquals(DeploymentRequestStatus.INSTALLING, result);

    }

    @Test
    public void testIsUninstallingDeploymentPackage() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        when(deploymentAgentService.isUninstallingDeploymentPackage("testPackage")).thenReturn(true);

        DeploymentRequestStatus result = deploymentRestService.uninstallDeploymentPackage("testPackage");

        assertNotNull(result);
        assertEquals(DeploymentRequestStatus.UNINSTALLING, result);
    }

    @Test(expected = WebApplicationException.class)
    public void testInstallDPError() throws Exception {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        // doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(deploymentAgentService)
        // .installDeploymentPackageAsync(anyObject());

        InstallRequest installRequest = new InstallRequest("test");

        deploymentRestService.installDeploymentPackage(installRequest);

    }

    @Test(expected = WebApplicationException.class)
    public void testInstallDPNullRequest() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        deploymentRestService.installDeploymentPackage(null);

    }

    @Test
    public void testInstallDP() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        InstallRequest installRequest = new InstallRequest("test");

        DeploymentRequestStatus result = deploymentRestService.installDeploymentPackage(installRequest);

        assertNotNull(result);
        assertEquals(DeploymentRequestStatus.REQUEST_RECEIVED, result);
    }

    @Test(expected = WebApplicationException.class)
    public void testUninstallDPError() throws Exception {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        // doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(deploymentAgentService)
        // .uninstallDeploymentPackageAsync(anyObject());

        deploymentRestService.uninstallDeploymentPackage("test");

    }

    @Test
    public void testUninstallDP() {
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        DeploymentAgentService deploymentAgentService = mock(DeploymentAgentService.class);
        deploymentRestService.setDeploymentAgentService(deploymentAgentService);

        DeploymentRequestStatus result = deploymentRestService.uninstallDeploymentPackage("test");

        assertNotNull(result);
        assertEquals(DeploymentRequestStatus.REQUEST_RECEIVED, result);
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
        deploymentPackages[0] = new DeploymentPackage() {

            @Override
            public boolean uninstallForced() throws DeploymentException {
                return false;
            }

            @Override
            public void uninstall() throws DeploymentException {

            }

            @Override
            public boolean isStale() {
                return false;
            }

            @Override
            public Version getVersion() {
                return new Version("1.0.0");
            }

            @Override
            public String[] getResources() {
                return null;
            }

            @Override
            public ServiceReference getResourceProcessor(String arg0) {
                return null;
            }

            @Override
            public String getResourceHeader(String arg0, String arg1) {
                return null;
            }

            @Override
            public String getName() {
                return "testPackage";
            }

            @Override
            public URL getIcon() {
                return null;
            }

            @Override
            public String getHeader(String arg0) {
                return null;
            }

            @Override
            public String getDisplayName() {
                return null;
            }

            @Override
            public BundleInfo[] getBundleInfos() {
                return null;
            }

            @Override
            public Bundle getBundle(String arg0) {
                return null;
            }
        };
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
        DeploymentRestService deploymentRestService = new DeploymentRestService();

        UserAdmin userAdmin = mock(UserAdmin.class);

        deploymentRestService.setUserAdmin(userAdmin);

        verify(userAdmin, times(1)).createRole("kura.permission.rest.deploy", Role.GROUP);

    }

}
