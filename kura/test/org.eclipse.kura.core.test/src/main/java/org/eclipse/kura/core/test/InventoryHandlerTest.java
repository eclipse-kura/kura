/*******************************************************************************
 * Copyright (c) 2021, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.inventory.InventoryHandlerV1;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import org.eclipse.kura.cloudconnection.CloudEndpoint;

import junit.framework.TestCase;

public class InventoryHandlerTest extends TestCase {

    private static CountDownLatch dependencyLatch = new CountDownLatch(3);
    private static CloudEndpointPublisher cloudEndpointPublisher;
    private static DeploymentAdmin deploymentAdmin;
    private static CloudEndpoint cloudEndpoint;
    private static DataService dataService;

    private static final String REMOTE_BUNDLE_NAME = "org.eclipse.kura.demo.heater";
    private static final String LOCAL_DP_NAME = "org.eclipse.kura.test.helloworld";
    private static final String LOCAL_BUNDLE_NAME = "org.eclipse.kura.test.helloworld";

    private URL getTestDpUrl() {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

        URL packageUrl = ctx.getBundle().getResource("src/main/resources/" + LOCAL_DP_NAME + ".dp");
        if (packageUrl == null) {
            // handle case where running from a jar on a real target
            packageUrl = ctx.getBundle().getResource(LOCAL_DP_NAME + ".dp");
        }

        return packageUrl;
    }

    @Override
    public void setUp() throws DeploymentException {
        // Wait for OSGi dependencies
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);
            if (!ok) {
                fail("Dependencies not resolved!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }

        DeploymentPackage localDp = deploymentAdmin.getDeploymentPackage(LOCAL_BUNDLE_NAME);
        if (localDp != null) {
            localDp.uninstall();
        }

        DeploymentPackage remoteDp = deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME);
        if (remoteDp != null) {
            remoteDp.uninstall();
        }

        cloudEndpointPublisher = new CloudEndpointPublisher(cloudEndpoint, dataService);
    }

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        InventoryHandlerTest.deploymentAdmin = deploymentAdmin;
        dependencyLatch.countDown();
    }

    public void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        InventoryHandlerTest.deploymentAdmin = null;
    }

    public void setCloudEndpoint(CloudEndpoint cloudEndpoint) {
        this.cloudEndpoint = cloudEndpoint;
        dependencyLatch.countDown();
    }

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
        dependencyLatch.countDown();
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetPackages() throws Exception {
        StringBuilder sb = init();
        sb.append(InventoryHandlerV1.RESOURCE_DEPLOYMENT_PACKAGES);

        KuraResponsePayload resp = cloudEndpointPublisher.call(InventoryHandlerV1.APP_ID, sb.toString(), null, 5000);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        String body = new String(resp.getBody());
        assertTrue(body.contains(
                "{\"deploymentPackages\":[{\"name\":\"org.eclipse.kura.test.helloworld\",\"version\":\"1.0.0\",\"bundles\":[{\"name\":\"org.eclipse.kura.test.helloworld\",\"version\":\"1.0.0.201407161421\""));
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetBundles() throws Exception {
        StringBuilder sb = init();
        sb.append(InventoryHandlerV1.RESOURCE_BUNDLES);

        KuraResponsePayload resp = cloudEndpointPublisher.call(InventoryHandlerV1.APP_ID, sb.toString(), null, 5000);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        String body = new String(resp.getBody());
        assertTrue(body.contains("{\"name\":\"org.eclipse.kura.test.helloworld\",\"version\":\"1.0.0.201407161421\""));
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetInventory() throws Exception {
        StringBuilder sb = init();
        sb.append(InventoryHandlerV1.INVENTORY);

        KuraResponsePayload resp = cloudEndpointPublisher.call(InventoryHandlerV1.APP_ID, sb.toString(), null, 5000);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        String body = new String(resp.getBody());
        assertTrue(body.startsWith("{\"inventory\":"));
        assertTrue(body.contains(
                "{\"name\":\"org.eclipse.kura.test.helloworld\",\"version\":\"1.0.0.201407161421\",\"type\":\"BUNDLE\"},{\"name\":\"org.eclipse.kura.test.helloworld\",\"version\":\"1.0.0\",\"type\":\"DP\"}"));
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetSystemPackages() throws Exception {
        StringBuilder sb = init();
        sb.append(InventoryHandlerV1.RESOURCE_SYSTEM_PACKAGES);

        KuraResponsePayload resp = cloudEndpointPublisher.call(InventoryHandlerV1.APP_ID, sb.toString(), null, 5000);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        String body = new String(resp.getBody());
        assertTrue(body.startsWith("{\"systemPackages\":["));
    }

    private StringBuilder init() throws IOException, DeploymentException {
        assertTrue(dataService.isConnected());

        DeploymentPackage dp = deploymentAdmin.getDeploymentPackage(LOCAL_DP_NAME);
        if (dp == null) {
            InputStream is = getTestDpUrl().openStream();
            dp = deploymentAdmin.installDeploymentPackage(is);
        }

        return new StringBuilder(CloudletTopic.Method.GET.toString()).append("/");
    }
}
