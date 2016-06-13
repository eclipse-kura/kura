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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

public class CloudServiceTest extends TestCase implements CloudClientListener
{
	private static CountDownLatch dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
	private static CloudService   cloudService;
	
	private int            publishedMsgId;
	private boolean        publishPublished;
	private boolean        publishConfirmed;
	private boolean        publishArrived;

	private int            controlMsgId;
	private boolean        controlPublished;
	private boolean        controlConfirmed;
	private boolean        controlArrived;
	
	@BeforeClass
	public void setUp() {
		// Wait for OSGi dependencies
		try {
			dependencyLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("OSGi dependencies unfulfilled");
		}
	}
	
	public void setCloudService(CloudService cloudService) {
		CloudServiceTest.cloudService = cloudService;
		dependencyLatch.countDown();
	}
	
	@Test
	public void testDummy() {
		assertTrue(true);
	}
	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testServiceExists() {
		assertNotNull(CloudServiceTest.cloudService);
	}
	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testService() 
		throws Exception
	{
		publishArrived = false;
		controlArrived = false;
				
		CloudClient cloudAppClient = CloudServiceTest.cloudService.newCloudClient("testService");
		cloudAppClient.addCloudClientListener(this);
		
		// test regular subscriptions
		int count = 0;
		while (!cloudAppClient.isConnected() && count < 10) {
			Thread.sleep(1000);
			count++;
		}
		if (!cloudAppClient.isConnected()) {
			throw new Exception("Not connected");
		}
		cloudAppClient.subscribe("test", 1);

		// test default subscriptions
		int priority = 5;
		
		KuraPayload payload = new KuraPayload();
		payload.setBody("payload".getBytes());
		publishedMsgId = cloudAppClient.publish("test", payload, 1, false, priority);

		KuraPayload controlPayload = new KuraPayload();
		controlPayload.setBody("control_payload".getBytes());
		controlMsgId = cloudAppClient.controlPublish("control_test", controlPayload, 1, false, priority);
		
		Thread.sleep(1000);
		
		assertTrue("publish not published!", publishPublished);
		assertTrue("publish not confirmed!", publishConfirmed);
		assertTrue("publish not arrived!",   publishArrived);

		assertTrue("control not published!", controlPublished);
		assertTrue("control not confirmed!", controlConfirmed);
		assertTrue("control not arrived!",   controlArrived);

		cloudAppClient.release();
	}
	
	@Override
	public void onConnectionLost() {
		// Ignore
	}

	@Override
	public void onConnectionEstablished() {
		// Ignore
	}

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		assertEquals("control_test",    appTopic);
		assertEquals("control_payload", new String(msg.getBody()));
		controlArrived = true;
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		assertEquals("test",    appTopic);
		assertEquals("payload", new String(msg.getBody()));
		publishArrived = true;		
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		if (messageId == publishedMsgId) {
			assertEquals("test", appTopic);
			publishConfirmed = true;
		}
		if (messageId == controlMsgId) {
			assertEquals("control_test", appTopic);
			controlConfirmed = true;
		}
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		if (messageId == publishedMsgId) {
			assertEquals("test", appTopic);
			publishPublished = true;
		}
		if (messageId == controlMsgId) {
			assertEquals("control_test", appTopic);
			controlPublished = true;
		}
	}
}
