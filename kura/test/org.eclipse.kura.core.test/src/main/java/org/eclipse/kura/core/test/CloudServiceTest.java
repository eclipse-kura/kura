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

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

public class CloudServiceTest extends TestCase implements CloudClientListener {

    private static CountDownLatch dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
    private static CloudService cloudService;

    private int publishedMsgId;
    private boolean publishPublished;
    private boolean publishConfirmed;
    private boolean publishArrived;

    private int controlMsgId;
    private boolean controlPublished;
    private boolean controlConfirmed;
    private boolean controlArrived;

    @Override
    @BeforeClass
    public void setUp() {
        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(CloudServiceTest.cloudService);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testService() throws Exception {
        this.publishArrived = false;
        this.controlArrived = false;

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
        this.publishedMsgId = cloudAppClient.publish("test", payload, 1, false, priority);

        KuraPayload controlPayload = new KuraPayload();
        controlPayload.setBody("control_payload".getBytes());
        this.controlMsgId = cloudAppClient.controlPublish("control_test", controlPayload, 1, false, priority);

        Thread.sleep(1000);

        assertTrue("publish not published!", this.publishPublished);
        assertTrue("publish not confirmed!", this.publishConfirmed);
        assertTrue("publish not arrived!", this.publishArrived);

        assertTrue("control not published!", this.controlPublished);
        assertTrue("control not confirmed!", this.controlConfirmed);
        assertTrue("control not arrived!", this.controlArrived);

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
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        assertEquals("control_test", appTopic);
        assertEquals("control_payload", new String(msg.getBody()));
        this.controlArrived = true;
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        assertEquals("test", appTopic);
        assertEquals("payload", new String(msg.getBody()));
        this.publishArrived = true;
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        if (messageId == this.publishedMsgId) {
            assertEquals("test", appTopic);
            this.publishConfirmed = true;
        }
        if (messageId == this.controlMsgId) {
            assertEquals("control_test", appTopic);
            this.controlConfirmed = true;
        }
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        if (messageId == this.publishedMsgId) {
            assertEquals("test", appTopic);
            this.publishPublished = true;
        }
        if (messageId == this.controlMsgId) {
            assertEquals("control_test", appTopic);
            this.controlPublished = true;
        }
    }
}
