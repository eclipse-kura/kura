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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudCallService;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.test.util.CoreTestXmlUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudDeploymentHandlerTest implements EventHandler {
	private static final Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandlerTest.class);

	private static CountDownLatch s_dependencyLatch = new CountDownLatch(4);	// initialize with number of dependencies
	private static CloudCallService s_cloudCallService;
	private static CloudService s_cloudService;
	private static DeploymentAdmin s_deploymentAdmin;
	
	private static Object s_lock = new Object();
	private static Event s_event;

	private static final long DEPENDENCY_TIMEOUT = 60000;
	private static final int RESPONSE_TIMEOUT = 10000;
	private static final long INSTALL_TIMEOUT = 60000;
	
	private static final String REMOTE_DP_NAME = "heater";
	private static final String REMOTE_DP_VERSION = "1.0.0";
	private static final String REMOTE_BUNDLE_NAME = "org.eclipse.kura.demo.heater";
	private static final String DOWNLOAD_URI = "http://s3.amazonaws.com/kura-resources/dps/heater.dp";
	private static final String DOWNLOAD_PROTOCOL = "HTTP";
	
	private static final String LOCAL_DP_NAME = "org.eclipse.kura.test.helloworld";
	private static final String LOCAL_DP_VERSION = "1.0.0";
	private static final String LOCAL_BUNDLE_NAME = "org.eclipse.kura.test.helloworld";
	private static final String LOCAL_BUNDLE_VERSION = "1.0.0.201407161421";
		
	@BeforeClass
	public static void setUp() {
		// This will wait for the CloudDeploymentHandlerV2.
		// It will decrement the dependency latch.
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				waitForCloudDeploymentHandler();
			}
		});
		t.start();
		
		// Wait for OSGi dependencies
		try {
			if (!s_dependencyLatch.await(DEPENDENCY_TIMEOUT, TimeUnit.SECONDS)) {
				fail("OSGi dependencies unfulfilled");
			}
		} catch (InterruptedException e) {
			fail("Interrupted waiting for OSGi dependencies");
		} finally {
			t.interrupt();
		}
	}

	public void setCloudCallService(CloudCallService cloudCallService) {
		CloudDeploymentHandlerTest.s_cloudCallService = cloudCallService;
		s_dependencyLatch.countDown();
	}
	
	public void unsetCloudCallService(CloudCallService cloudCallService) {
		CloudDeploymentHandlerTest.s_cloudCallService = null;
	}

	public void setCloudService(CloudService cloudService) {
		CloudDeploymentHandlerTest.s_cloudService = cloudService;
		s_dependencyLatch.countDown();
	}
	
	public void unsetCloudService(CloudService cloudService) {
		CloudDeploymentHandlerTest.s_cloudService = null;
	}
	
	public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		CloudDeploymentHandlerTest.s_deploymentAdmin = deploymentAdmin;
		s_dependencyLatch.countDown();
	}

	public void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		CloudDeploymentHandlerTest.s_deploymentAdmin = null;
	}
	
	@Test
	public void testExecInstallDeploymentPackage() throws Exception 
	{
		assertTrue(s_cloudCallService.isConnected());
		assertNull(s_deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME));
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
		s_logger.warn(sb.toString());

		
		KuraPayload payload = new KuraPayload();
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, DOWNLOAD_URI);
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NAME, REMOTE_DP_NAME);
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_VERSION, REMOTE_DP_VERSION);
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, DOWNLOAD_PROTOCOL);
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_JOB_ID, Long.parseLong("1111")); 
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_INSTALL, true);

		synchronized (s_lock) {
			s_event = null;
			
			KuraResponsePayload resp = s_cloudCallService.call(
					CloudDeploymentHandlerV2.APP_ID,
					sb.toString(),
					payload,
					RESPONSE_TIMEOUT);

			assertNotNull(resp);

			s_logger.warn("Response code: " + resp.getResponseCode());
			s_logger.warn("Response message: " + resp.getExceptionMessage());
			assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		
			// Note that the above request will install the package asynchronously
			// For the stability of the test wait until installed.
			long timeout = INSTALL_TIMEOUT;
			final long starTime = System.currentTimeMillis();
			while (timeout > 0) {
				s_lock.wait(timeout);
				
				if (s_event != null) {
					String topic = s_event.getTopic();
					String dpName = (String) s_event.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME);
					Version dpVersion = (Version) s_event.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION);

					if ("org/osgi/service/deployment/INSTALL".equals(topic) &&
							REMOTE_DP_NAME.equals(dpName) &&
							REMOTE_DP_VERSION.equals(dpVersion.toString())) {
						break;
					}
				}
				
				final long elapsed = System.currentTimeMillis() - starTime;
				timeout -= elapsed;
			}
			
			assertTrue(timeout > 0);
		}	
	}

	@Test
	public void testGetPackages() throws Exception {
		
		assertTrue(s_cloudCallService.isConnected());

		ensureResourceDpInstalled(LOCAL_DP_NAME);
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_PACKAGES);
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				null,
				RESPONSE_TIMEOUT);

		assertNotNull(resp);
		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

		String s = new String(resp.getBody());

		XmlDeploymentPackages xmlPackages = CoreTestXmlUtil.unmarshal(s,  XmlDeploymentPackages.class);

		XmlDeploymentPackage[] packages = xmlPackages.getDeploymentPackages();

		XmlDeploymentPackage xmlDp = null;
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				if (packages[i].getName().equals(LOCAL_DP_NAME)) {
					xmlDp = packages[i];
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

	@Test
	public void testGetBundles() throws Exception {

		assertTrue(s_cloudCallService.isConnected());

		ensureResourceDpInstalled(LOCAL_DP_NAME);
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_BUNDLES);
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				null,
				RESPONSE_TIMEOUT);

		assertNotNull(resp);
		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

		String s = new String(resp.getBody());

		XmlBundles xmlBundles = CoreTestXmlUtil.unmarshal(s, XmlBundles.class);

		XmlBundle[] bundles = xmlBundles.getBundles();

		XmlBundle bundle = null;
		if (bundles != null) {
			for (int i = 0; i < bundles.length; i++) {
				s_logger.warn("Bundle name: " + bundles[i].getName());
				if (bundles[i].getName().equals(LOCAL_BUNDLE_NAME)) {
					bundle = bundles[i];
					break;
				}
			}
		}

		assertNotNull(bundle);
		assertEquals(LOCAL_BUNDLE_VERSION, bundle.getVersion());
	}

	@Test
	public void testExecStartStop() throws Exception {

		assertTrue(s_cloudCallService.isConnected());

		DeploymentPackage dp = ensureResourceDpInstalled(LOCAL_DP_NAME);
		
		Bundle bundle = dp.getBundle(LOCAL_BUNDLE_NAME);
				
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
				RESPONSE_TIMEOUT);
		
		assertNotNull(resp);
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
				RESPONSE_TIMEOUT);

		assertNotNull(resp);
		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		assertEquals(Bundle.ACTIVE, bundle.getState());
	}

	@Test
	public void testExecUninstallDeploymentPackage() throws Exception {

		assertTrue(s_cloudCallService.isConnected());
		
		DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME);
		s_logger.warn("dp value: {}", dp);
		if (dp == null) {
			testExecInstallDeploymentPackage();
		}
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudDeploymentHandlerV2.RESOURCE_UNINSTALL); 
		
		s_logger.warn("Uninstall topic: " + sb.toString());
		
		KuraPayload payload = new KuraPayload();
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NAME, REMOTE_BUNDLE_NAME);
		payload.addMetric(DeploymentPackageDownloadOptions.METRIC_JOB_ID, Long.parseLong("1111")); 
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudDeploymentHandlerV2.APP_ID,
				sb.toString(),
				payload,
				RESPONSE_TIMEOUT);
		
		assertNotNull(resp);
		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());		
		assertNull(s_deploymentAdmin.getDeploymentPackage(REMOTE_BUNDLE_NAME));
	}
	
	private URL getResourceDpUrl(String dpName) {
		BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

		URL packageUrl = ctx.getBundle().getResource("src/main/resources/"+dpName+".dp");
		if(packageUrl == null) {
			//FIXME: handle case where running from a jar on a real target
			packageUrl = ctx.getBundle().getResource(dpName+".dp");
		}
		
		return packageUrl;
	}
	
	private static void waitForCloudDeploymentHandler() {
		try {
			boolean exists = false;
			while (!exists) {
				if (s_cloudService != null) {
					String[] appIdS = s_cloudService.getCloudApplicationIdentifiers();
					for (String appId : appIdS) {
						if (CloudDeploymentHandlerV2.APP_ID.equals(appId)) {
							exists = true;
							break;
						}
					}
				}
				if (!exists) {
					Thread.sleep(1000);
				}
			}
			s_dependencyLatch.countDown();
		} catch (InterruptedException e) {
			fail("Interrupted waiting for CloudDeploymentHandlerV2");
		}
	}
	
	private DeploymentPackage ensureResourceDpInstalled(String dpName) throws IOException, DeploymentException {
		DeploymentPackage dp = s_deploymentAdmin.getDeploymentPackage(dpName);
		if (dp == null) {
			s_logger.info("Installing dp: '{}'", dpName);
			InputStream is = getResourceDpUrl(dpName).openStream();
			dp = s_deploymentAdmin.installDeploymentPackage(is);
		}
		return dp;
	}
	
	@Override
	public void handleEvent(Event event) {			
		synchronized (s_lock) {
			s_event = event;
			s_lock.notifyAll();
		}
	}
}
