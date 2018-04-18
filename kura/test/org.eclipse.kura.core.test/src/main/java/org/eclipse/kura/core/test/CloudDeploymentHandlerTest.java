/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudCallService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.test.util.CoreTestXmlUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class CloudDeploymentHandlerTest extends TestCase {

    private static final Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandlerTest.class);

    private static CountDownLatch s_dependencyLatch = new CountDownLatch(2);	// initialize with number of
    // dependencies
    private static CloudCallService s_cloudCallService;
    private static DeploymentAdmin s_deploymentAdmin;

    private static final String REMOTE_DP_NAME = "heater";
    private static final String REMOTE_DP_VERSION = "1.0.0";
    private static final String REMOTE_BUNDLE_NAME = "org.eclipse.kura.demo.heater";
    // private static final String BUNDLE_VERSION = "1.0.2.201504080206";
    private static final String DOWNLOAD_URI = "http://s3.amazonaws.com/kura-resources/dps/heater.dp";
    private static final String DOWNLOAD_PROTOCOL = "HTTP";

    private static final String LOCAL_DP_NAME = "org.eclipse.kura.test.helloworld";
    private static final String LOCAL_DP_VERSION = "1.0.0";
    private static final String LOCAL_BUNDLE_NAME = "org.eclipse.kura.test.helloworld";
    private static final String LOCAL_BUNDLE_VERSION = "1.0.0.201407161421";

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
            boolean ok = s_dependencyLatch.await(10, TimeUnit.SECONDS);
            if (!ok) {
                fail("Dependencies not resolved!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }

        DeploymentPackage localDp = s_deploymentAdmin.getDeploymentPackage(LOCAL_BUNDLE_NAME);
        if (localDp != null) {
            localDp.uninstall();
        }

        DeploymentPackage remoteDp = s_deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME);
        if (remoteDp != null) {
            remoteDp.uninstall();
        }
    }

    public void setCloudCallService(CloudCallService cloudCallService) {
        CloudDeploymentHandlerTest.s_cloudCallService = cloudCallService;
        s_dependencyLatch.countDown();
    }

    public void unsetCloudCallService(CloudCallService cloudCallService) {
        CloudDeploymentHandlerTest.s_cloudCallService = null;
    }

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        CloudDeploymentHandlerTest.s_deploymentAdmin = deploymentAdmin;
        s_dependencyLatch.countDown();
    }

    public void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        CloudDeploymentHandlerTest.s_deploymentAdmin = null;
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testExecInstallDeploymentPackage() throws Exception {
        assertTrue(s_cloudCallService.isConnected());
        assertNull(s_deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME));

        StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString()).append("/")
                .append(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        s_logger.warn(sb.toString());

        KuraPayload payload = new KuraPayload();
        payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, DOWNLOAD_URI);
        payload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, REMOTE_DP_NAME);
        payload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, REMOTE_DP_VERSION);
        payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, DOWNLOAD_PROTOCOL);
        payload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, Long.parseLong("1111"));
        payload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_INSTALL, true);

        KuraResponsePayload resp = s_cloudCallService.call(CloudDeploymentHandlerV2.APP_ID, sb.toString(), payload,
                5000);

        s_logger.warn("Response code: " + resp.getResponseCode());
        s_logger.warn("Response message: " + resp.getExceptionMessage());
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    @Ignore
    public void testGetPackages() throws Exception {

        assertTrue(s_cloudCallService.isConnected());

        DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(LOCAL_DP_NAME);
        if (dp == null) {
            s_logger.warn("Getting dp");
            InputStream is = getTestDpUrl().openStream();
            dp = s_deploymentAdmin.installDeploymentPackage(is);
        }

        StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString()).append("/")
                .append(CloudDeploymentHandlerV2.RESOURCE_PACKAGES);

        KuraResponsePayload resp = s_cloudCallService.call(CloudDeploymentHandlerV2.APP_ID, sb.toString(), null, 5000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        String s = new String(resp.getBody());

        // XmlDeploymentPackages xmlPackages = XmlUtil.unmarshal(s, XmlDeploymentPackages.class);
        XmlDeploymentPackages xmlPackages = CoreTestXmlUtil.unmarshal(s, XmlDeploymentPackages.class);

        XmlDeploymentPackage[] packages = xmlPackages.getDeploymentPackages();

        XmlDeploymentPackage xmlDp = null;
        if (packages != null) {
            for (XmlDeploymentPackage package1 : packages) {
                if (package1.getName().equals(LOCAL_DP_NAME)) {
                    xmlDp = package1;
                    break;
                }
            }
        }

        assertNotNull(xmlDp);
        assertEquals(LOCAL_DP_VERSION, xmlDp.getVersion());
        XmlBundleInfo[] bundleInfos = xmlDp.getBundleInfos();
        assertEquals(1, bundleInfos.length);
        assertEquals(LOCAL_BUNDLE_NAME, bundleInfos[0].getName());
        assertEquals(LOCAL_BUNDLE_VERSION, bundleInfos[0].getVersion());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetBundles() throws Exception {

        assertTrue(s_cloudCallService.isConnected());

        DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(LOCAL_DP_NAME);
        if (dp == null) {
            InputStream is = getTestDpUrl().openStream();
            dp = s_deploymentAdmin.installDeploymentPackage(is);
        }

        StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString()).append("/")
                .append(CloudDeploymentHandlerV2.RESOURCE_BUNDLES);

        KuraResponsePayload resp = s_cloudCallService.call(CloudDeploymentHandlerV2.APP_ID, sb.toString(), null, 5000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        String s = new String(resp.getBody());

        // XmlBundles xmlBundles = XmlUtil.unmarshal(s, XmlBundles.class);
        XmlBundles xmlBundles = CoreTestXmlUtil.unmarshal(s, XmlBundles.class);

        XmlBundle[] bundles = xmlBundles.getBundles();

        XmlBundle bundle = null;
        if (bundles != null) {
            for (XmlBundle bundle2 : bundles) {
                s_logger.warn("Bundle name: " + bundle2.getName());
                if (bundle2.getName().equals(LOCAL_BUNDLE_NAME)) {
                    bundle = bundle2;
                    break;
                }
            }
        }

        assertNotNull(bundle);
        assertEquals(LOCAL_BUNDLE_VERSION, bundle.getVersion());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testExecStartStop() throws Exception {

        assertTrue(s_cloudCallService.isConnected());

        DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(LOCAL_DP_NAME);
        if (dp == null) {
            InputStream is = getTestDpUrl().openStream();
            dp = s_deploymentAdmin.installDeploymentPackage(is);
        }

        Bundle bundle = dp.getBundle(LOCAL_BUNDLE_NAME);

        assertNotNull(bundle);

        if (bundle.getState() == Bundle.RESOLVED) {
            bundle.start();
        }

        assertEquals(Bundle.ACTIVE, bundle.getState());

        StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString()).append("/")
                .append(CloudDeploymentHandlerV2.RESOURCE_STOP).append("/").append(bundle.getBundleId());

        KuraResponsePayload resp = s_cloudCallService.call(CloudDeploymentHandlerV2.APP_ID, sb.toString(), null, 5000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        assertEquals(Bundle.RESOLVED, bundle.getState());

        // Start
        sb = new StringBuilder(CloudletTopic.Method.EXEC.toString()).append("/")
                .append(CloudDeploymentHandlerV2.RESOURCE_START).append("/").append(bundle.getBundleId());

        resp = s_cloudCallService.call(CloudDeploymentHandlerV2.APP_ID, sb.toString(), null, 5000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testExecUninstallDeploymentPackage() throws Exception {

        assertTrue(s_cloudCallService.isConnected());
        assertNull(s_deploymentAdmin.getDeploymentPackage(LOCAL_BUNDLE_NAME));

        InputStream is = getTestDpUrl().openStream();
        s_deploymentAdmin.installDeploymentPackage(is);

        StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString()).append("/")
                .append(CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);

        s_logger.warn("Uninstall topic: " + sb.toString());

        KuraPayload payload = new KuraPayload();
        // payload.setBody("org.eclipse.kura.test.helloworld".getBytes("UTF-8"));
        payload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, LOCAL_BUNDLE_NAME);
        payload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, Long.parseLong("1111"));

        KuraResponsePayload resp = s_cloudCallService.call(CloudDeploymentHandlerV2.APP_ID, sb.toString(), payload,
                5000);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

        int countdown = 10000;
        while (countdown > 0) {
            Thread.sleep(1000);
            countdown -= 1000;
        }

        assertNull(s_deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME));
    }
}
