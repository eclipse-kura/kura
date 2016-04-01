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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudCallService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.CloudConfigurationHandler;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.core.test.util.CoreTestXmlUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationServiceTest implements IConfigurationServiceTest, ConfigurableComponent
{
	private static final Logger s_logger = LoggerFactory.getLogger(ConfigurationServiceTest.class);
	
	private static CountDownLatch 		s_dependencyLatch = new CountDownLatch(3);	// initialize with number of dependencies
	private static Object 				s_lock = new Object();
	private static ConfigurationService s_configService;
	private static CloudCallService     s_cloudCallService;
	private static SystemService        s_systemService;
	
	@SuppressWarnings("unused")
	private static ComponentContext 	s_componentContext;
	
	private static Map<String, Object>  s_properties;
	private static boolean              s_updated;
	
	private static final String PID = "org.eclipse.kura.core.test.IConfigurationServiceTest";
	private static final long UPDATE_TIMEOUT = 10000;
	private static final long CONNECT_TIMEOUT = 10000;
	private static final int RESPONSE_TIMEOUT = 10000;
	private static final long ROLLBACK_SETTLE_DELAY = 5000;
		
	@BeforeClass
	public static void setUp() 
	{
		// Wait for OSGi dependencies
		try {
			if (!s_dependencyLatch.await(5, TimeUnit.SECONDS)) {
				fail("OSGi dependencies unfulfilled");
			}
		} catch (InterruptedException e) {
			fail("Interrupted waiting for OSGi dependencies");
		}
	}
	
	public void setConfigurationService(ConfigurationService configurationService) {
		s_configService = configurationService;
		s_dependencyLatch.countDown();
	}

	public void setCloudCallService(CloudCallService cloudCallService) {
		s_cloudCallService = cloudCallService;
		s_dependencyLatch.countDown();
	}

	public void setSystemService(SystemService systemService) {
		s_systemService = systemService;
		s_dependencyLatch.countDown();
	}
	
	public void unsetConfigurationService(ConfigurationService configurationService) {
		s_configService = null;
	}

	public void unsetCloudCallService(CloudCallService cloudCallService) {
		s_cloudCallService = null;
	}

	public void unsetSystemService(SystemService systemService) {
		s_systemService = null;
	}
	
	@Test
	public void testServiceExists() {
		assertNotNull(s_configService);
	}
	
	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("ConfigurationServiceTest.activate...");
		s_componentContext = componentContext;
		s_properties = properties;
	}
		
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("ConfigurationServiceTest.deactivate...");
	}
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated called: "+properties);
		
		Set<Entry<String, Object>> entries = properties.entrySet();
		for (Map.Entry<String, Object> entry : entries) {
			System.err.println("\t\t" + entry.getKey() + " = " + entry.getValue());
		}
		
		s_properties = properties;
		synchronized (s_lock) {
			s_updated = true;
			s_lock.notifyAll();
		}
	}
	
	@Test
	public void testLocalConfiguration()
		throws Exception
	{		
		final Map<String, Object> backupProps = s_properties;
		
		//
		// take a snapshot
		s_logger.info("configService 2:"+s_configService);
		s_logger.info("Taking snapshot...");
		
		final long sid = s_configService.snapshot();
				
		//
		// test a positive update flow
		Hashtable<String,Object> props = new Hashtable<String,Object>();
		Set<String> keys = s_properties.keySet();
		for (String key : keys) {
			props.put(key, s_properties.get(key));
		}
		
		final String stringValue = UUID.randomUUID().toString();
		final Long longValue = new Random().nextLong();
		final Double doubleValue = new Random().nextDouble();
		final Float floatValue = new Random().nextFloat();
		final Integer intValue = new Random().nextInt();
		final Character charValue = stringValue.charAt(0);
		final Boolean boolValue = !(Boolean) s_properties.get("prop.boolean");
		final Short shortValue = (short) new Random().nextInt(Short.MAX_VALUE);
		final Byte byteValue = (byte) new Random().nextInt(Byte.MAX_VALUE);
		
		props.put("prop.string",    stringValue);
		props.put("prop.long",      longValue);
		props.put("prop.double",    doubleValue);
		props.put("prop.float",     floatValue);
		props.put("prop.integer",   intValue);
		props.put("prop.character", charValue);
		props.put("prop.boolean",   boolValue);
		props.put("prop.short",     shortValue);
		props.put("prop.byte",      byteValue);
		
		s_logger.info("configService 3:"+s_configService);
		s_logger.info("Updating configuration with new values for " + PID + " with props: " + props);
		synchronized (s_lock) {
			s_updated = false;
			s_configService.updateConfiguration(PID, props);
			s_lock.wait(UPDATE_TIMEOUT);
			assertTrue(s_updated);
		}

		s_logger.info("Asserting values...");
		assertConfigPropsEqual(props, s_properties);
		
		// test a negative update flow
		props.clear();
		keys = s_properties.keySet();
		for (String key : keys) {
			props.put(key, s_properties.get(key));
		}
		props.put("prop.long", "AAAA");
		try {
			s_configService.updateConfiguration(PID, props);
			assertFalse("Configuration update should have failed", false);
		}
		catch (KuraException e) {
			assertTrue("Configuration update has failed as expected", true);
			assertEquals(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e.getCode());
		}

		// test a negative update flow
		props.clear();
		keys = s_properties.keySet();
		for (String key : keys) {
			props.put(key, s_properties.get(key));
		}
		props.remove("prop.string");
		try {
			s_configService.updateConfiguration(PID, props);
			assertFalse("Configuration update should have failed", false);
		}
		catch (KuraException e) {
			assertTrue("Configuration update has failed as expected", true);
			assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
		}
		
		//
		// restore a snapshot.
		s_logger.info("Rolling back...");
		synchronized (s_lock) {
			s_updated = false;
			s_configService.rollback(sid);
			s_lock.wait(UPDATE_TIMEOUT);
			assertTrue(s_updated);
		}
		assertConfigPropsEqual(backupProps, s_properties);
		
		// the CloudService will briefly disconnect after a rollback
		Thread.sleep(ROLLBACK_SETTLE_DELAY);
	}
	
	@Test
	public void testRemoteGetConfiguration() 
			throws Exception
    {
		waitForConnection();
		assertTrue(s_cloudCallService.isConnected());
		
		s_logger.info("Starting testRemoteGetConfiguration");
		
		Map<String, Object> props = s_configService.getComponentConfiguration(PID).getConfigurationProperties();
		
		// load the current configuration
		s_logger.info("loading the current configuration");
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.GET.toString())
		.append("/")
		.append(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS)
		.append("/")
		.append(PID);
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudConfigurationHandler.APP_ID,
				sb.toString(),
				null,
				RESPONSE_TIMEOUT);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		assertNotNull(resp.getBody());
		
		// unmarshall the response
		String s = new String(resp.getBody(), "UTF-8");
		StringReader sr = new StringReader(s);
		XmlComponentConfigurations xmlConfigs = XmlUtil.unmarshal(sr, XmlComponentConfigurations.class);
		
		System.err.println("Checking current configuration");
		List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
		
		assertEquals(1, configs.size());
		
		final Map<String, Object> otherProps = configs.get(0).getConfigurationProperties();
		
		assertConfigPropsEqual(props, otherProps);
	}

	@Test
	public void testRemoteConfiguration()
		throws Exception
	{
		waitForConnection();
		assertTrue(s_cloudCallService.isConnected());
		
		s_logger.info("Starting testRemoteConfiguration");
		
		final Map<String, Object> backupProps = s_properties;
				
		// take a snapshot
		System.err.println("taking a snapshot");
		
		StringBuilder sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudConfigurationHandler.RESOURCE_SNAPSHOT);
		
		KuraResponsePayload resp = s_cloudCallService.call(
				CloudConfigurationHandler.APP_ID,
				sb.toString(),
				null,
				RESPONSE_TIMEOUT);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		assertNotNull(resp.getBody());
		
		// unmarshall the response
		String s = new String(resp.getBody(), "UTF-8");
		StringReader sr = new StringReader(s);
		//XmlSnapshotIdResult snapshotIds = XmlUtil.unmarshal(sr, XmlSnapshotIdResult.class);
		XmlSnapshotIdResult snapshotIds = CoreTestXmlUtil.unmarshal(sr, XmlSnapshotIdResult.class);

		s_logger.info("validating configuration");
		assertNotNull(snapshotIds);
		assertEquals(1, snapshotIds.getSnapshotIds().size());
		
		long sid = snapshotIds.getSnapshotIds().get(0);

		// modify the configuration
		s_logger.info("modifying configuration");
		ComponentConfigurationImpl  ccnew = new ComponentConfigurationImpl();
		ccnew.setPid(PID);
		Hashtable<String,Object> propsnew = new Hashtable<String,Object>();
		final String stringValue = UUID.randomUUID().toString();
		propsnew.put("prop.string", stringValue);
		ccnew.setProperties(propsnew);
		
		XmlComponentConfigurations newConfigs = new XmlComponentConfigurations();
		List<ComponentConfigurationImpl> newccs = new ArrayList<ComponentConfigurationImpl>();
		newccs.add(ccnew);
		newConfigs.setConfigurations(newccs);
		
		StringWriter sw = new StringWriter();
		XmlUtil.marshal(newConfigs, sw);
		
		KuraPayload payload = new KuraPayload();
		payload.setBody(sw.toString().getBytes());
		
		sb = new StringBuilder(CloudletTopic.Method.PUT.toString())
		.append("/")
		.append(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS)
		.append("/")
		.append(PID);
		
		synchronized (s_lock) {
			s_updated = false;
			resp = s_cloudCallService.call(
					CloudConfigurationHandler.APP_ID,
					sb.toString(),
					payload,
					RESPONSE_TIMEOUT);

			assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

			s_lock.wait(UPDATE_TIMEOUT);
			assertTrue(s_updated);
		}
				
		s_logger.info("validating modified configuration");
		s_logger.info("Checking these are equal: " + s_properties.get("prop.string") + " AND " + stringValue);
		assertEquals(stringValue, s_properties.get("prop.string"));
		
		// reload the current configuration
		s_logger.info("reloading the current configuration");

		sb = new StringBuilder(CloudletTopic.Method.GET.toString())
		.append("/")
		.append(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS)
		.append("/")
		.append(PID);
		
		resp = s_cloudCallService.call(
				CloudConfigurationHandler.APP_ID,
				sb.toString(),
				null,
				RESPONSE_TIMEOUT);

		assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());
		assertNotNull(resp.getBody());
		
		// unmarshall the response
		s = new String(resp.getBody(), "UTF-8");
		System.err.println(s);			
		
		sr = new StringReader(s);
		XmlComponentConfigurations xmlConfigs = XmlUtil.unmarshal(sr, XmlComponentConfigurations.class);
		
		s_logger.info("validating modified configuration");
		if(xmlConfigs == null) {
			s_logger.info("ERROR: xmlConfigs is null");
		}
		assertNotNull(xmlConfigs);
		ComponentConfigurationImpl ccmod = xmlConfigs.getConfigurations().get(0);
		s_logger.info("Checking these are equal: " + ccmod.getConfigurationProperties().get("prop.string") + " AND " + stringValue);
		assertEquals(stringValue, ccmod.getConfigurationProperties().get("prop.string"));
		
		// rollback
		sb = new StringBuilder(CloudletTopic.Method.EXEC.toString())
		.append("/")
		.append(CloudConfigurationHandler.RESOURCE_ROLLBACK)
		.append("/")
		.append(sid);

		synchronized (s_lock) {
			s_updated = false;
			resp = s_cloudCallService.call(
					CloudConfigurationHandler.APP_ID,
					sb.toString(),
					null,
					RESPONSE_TIMEOUT);

			assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resp.getResponseCode());

			s_lock.wait(UPDATE_TIMEOUT);
			assertTrue(s_updated);
		}
		assertConfigPropsEqual(backupProps, s_properties);
		
		// the CloudService will briefly disconnect after a rollback
		Thread.sleep(ROLLBACK_SETTLE_DELAY);
	}
	
	@Test
	public void testSnapshotsMaxCount()
		throws Exception
	{
		int maxCount = s_systemService.getKuraSnapshotsCount();
		for (int i=0; i<maxCount*2; i++) {
			s_configService.snapshot();
		}
		
		Set<Long> sids = s_configService.getSnapshots();
		assertEquals(maxCount, sids.size());
	}
	
	private void assertConfigPropsEqual(Map<String, Object> a, Map<String, Object> b)
	{
		assertEquals(a.keySet().size(), b.keySet().size());
		for (String key : a.keySet()) {
			Object oa = a.get(key);
			Object ob = b.get(key);
			assertEquals(oa.getClass(), ob.getClass());
			if (!oa.getClass().isArray()) {
				assertEquals(a.get(key), b.get(key));
			} else {
				// TODO
			}
		}
	}
	
	private void waitForConnection() throws InterruptedException
	{
		long timeout = CONNECT_TIMEOUT;
		while (!s_cloudCallService.isConnected() && timeout > 0) {
			s_logger.warn("Waiting for connection");
			Thread.sleep(1000);
			timeout -= 1000;
		}
	}
}
