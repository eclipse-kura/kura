/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.test;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.eclipse.kura.cloud.CloudCallService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public class CloudDeploymentHandlerTest extends TestCase {

	private static CountDownLatch s_dependencyLatch = new CountDownLatch(2);	// initialize with number of dependencies
	private static CloudCallService s_cloudCallService;
	private static DeploymentAdmin s_deploymentAdmin;
	
	private static long s_countdown = 30000;

	private static final String DP_NAME = "org.eclipse.kura.test.helloworld";
	private static final String DP_VERSION = "1.0.0";
	private static final String BUNDLE_NAME = "org.eclipse.kura.test.helloworld";
	private static final String BUNDLE_VERSION = "1.0.0.201407161421";
	
	private URL getTestDpUrl() {
		BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

		URL packageUrl = ctx.getBundle().getResource("src/main/resources/"+DP_NAME+".dp");
		if(packageUrl == null) {
			//handle case where running from a jar on a real target
			packageUrl = ctx.getBundle().getResource(DP_NAME+".dp");
		}
		
		return packageUrl;
	}
	
	public void setUp() {
		// Wait for OSGi dependencies
		try {
			s_dependencyLatch.await(5, TimeUnit.SECONDS);
			
			while (!s_cloudCallService.isConnected() && s_countdown > 0) {
				Thread.sleep(1000);
				s_countdown -= 1000;
			}
			if (!s_cloudCallService.isConnected()) {
				fail("Timed out waiting for the CloudCallService to connect");
			}
		} catch (InterruptedException e) {
			fail("OSGi dependencies unfulfilled");
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
	

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testExecInstallDeploymentPackage() throws Exception 
	{
		assertTrue(s_cloudCallService.isConnected());
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
		
		URL packageUrl = getTestDpUrl();
		
		KuraPayload payload = new KuraPayload();

		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URL, packageUrl.toString());

		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				payload,
				5000);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testGetPackages() throws Exception {
		
		assertTrue(s_cloudCallService.isConnected());
		
		DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(DP_NAME);
		if (dp == null) {
			InputStream is = getTestDpUrl().openStream();
			dp = s_deploymentAdmin.installDeploymentPackage(is);
		}
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_PACKAGES);
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				null,
				5000);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

		String s = new String(resp.getBody());

		XmlDeploymentPackages xmlPackages = XmlUtil.unmarshal(s, XmlDeploymentPackages.class);

		XmlDeploymentPackage[] packages = xmlPackages.getDeploymentPackages();

		XmlDeploymentPackage xmlDp = null;
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				if (packages[i].getName().equals(DP_NAME)) {
					xmlDp = packages[i];
					break;
				}
			}
		}

		assertNotNull(xmlDp);
		assertEquals(DP_VERSION, xmlDp.getVersion());
		XmlBundleInfo[] bundleInfos = xmlDp.getBundleInfos();
		assertEquals(1, bundleInfos.length);
		assertEquals(BUNDLE_NAME, bundleInfos[0].getName());
		assertEquals(BUNDLE_VERSION, bundleInfos[0].getVersion());
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testGetBundles() throws Exception {

		assertTrue(s_cloudCallService.isConnected());
		
		DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(DP_NAME);
		if (dp == null) {
			InputStream is = getTestDpUrl().openStream();
			dp = s_deploymentAdmin.installDeploymentPackage(is);
		}
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_BUNDLES);
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				null,
				5000);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

		String s = new String(resp.getBody());

		XmlBundles xmlBundles = XmlUtil.unmarshal(s, XmlBundles.class);

		XmlBundle[] bundles = xmlBundles.getBundles();

		XmlBundle bundle = null;
		if (bundles != null) {
			for (int i = 0; i < bundles.length; i++) {
				if (bundles[i].getName().equals(DP_NAME)) {
					bundle = bundles[i];
					break;
				}
			}
		}

		assertNotNull(bundle);
		assertEquals(BUNDLE_VERSION, bundle.getVersion());
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testExecStartStop() throws Exception {

		assertTrue(s_cloudCallService.isConnected());
		
		DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(DP_NAME);
		if (dp == null) {
			InputStream is = getTestDpUrl().openStream();
			dp = s_deploymentAdmin.installDeploymentPackage(is);
		}
		
		Bundle bundle = dp.getBundle(BUNDLE_NAME);
				
		assertNotNull(bundle);
		
		if (bundle.getState() == Bundle.RESOLVED) {
			bundle.start();
		}
		
		assertEquals(Bundle.ACTIVE, bundle.getState());
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_STOP)
		.append("/")
		.append(bundle.getBundleId());
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				null,
				5000);
		
		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

		assertEquals(Bundle.RESOLVED, bundle.getState());
		
		// Start
		sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_START)
		.append("/")
		.append(bundle.getBundleId());
		
		resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				null,
				5000);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		
		assertEquals(Bundle.ACTIVE, bundle.getState());
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testExecUninstallDeploymentPackage() throws Exception {

		assertTrue(s_cloudCallService.isConnected());
		
		DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(DP_NAME);
		if (dp == null) {
			InputStream is = getTestDpUrl().openStream();
			dp = s_deploymentAdmin.installDeploymentPackage(is);
		}
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
		
		KuraPayload payload = new KuraPayload();
		payload.setBody("org.eclipse.kura.test.helloworld".getBytes("UTF-8"));
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				payload,
				5000);
		
		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		assertNull(s_deploymentAdmin.getDeploymentPackage(DP_NAME));
	}
}
