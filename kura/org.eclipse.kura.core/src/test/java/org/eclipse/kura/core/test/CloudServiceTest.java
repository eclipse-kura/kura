/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class CloudServiceTest {

    @InjectService
    CloudService cloudService;

    @Test
    public void testDummy() {
        assertTrue(true);
    }

    // @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(cloudService);
    }

    // @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testService() throws Exception {

        CloudClientListener cl = mock(CloudClientListener.class);

        CloudClient cloudAppClient = cloudService.newCloudClient("testService");
        cloudAppClient.addCloudClientListener(cl);

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

        Mockito.verify(cl, Mockito.after(1000).never()).onMessagePublished(Mockito.any(), Mockito.any());
        Mockito.verify(cl, Mockito.timeout(1000)).onMessagePublished(Mockito.any(), Mockito.any());

        // test default subscriptions
        int priority = 5;

        KuraPayload payload = new KuraPayload();
        payload.setBody("payload".getBytes());
        int publishedMsgId = cloudAppClient.publish("test", payload, 1, false, priority);

        KuraPayload controlPayload = new KuraPayload();
        controlPayload.setBody("control_payload".getBytes());
        int controlMsgId = cloudAppClient.controlPublish("control_test", controlPayload, 1, false, priority);

        Thread.sleep(1000);

        cloudAppClient.release();
    }
    // class MockCloudClientListener implements CloudClientListener {
    // @Override
    // public void onConnectionLost() {
    // // Ignore
    // }
    //
    // @Override
    // public void onConnectionEstablished() {
    // // Ignore
    // }
    //
    // @Override
    // public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
    // assertEquals("control_test", appTopic);
    // assertEquals("control_payload", new String(msg.getBody()));
    // this.controlArrived = true;
    // }
    //
    // @Override
    // public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
    // assertEquals("test", appTopic);
    // assertEquals("payload", new String(msg.getBody()));
    // this.publishArrived = true;
    // }
    //
    // @Override
    // public void onMessageConfirmed(int messageId, String appTopic) {
    // if (messageId == this.publishedMsgId) {
    // assertEquals("test", appTopic);
    // this.publishConfirmed = true;
    // }
    // if (messageId == this.controlMsgId) {
    // assertEquals("control_test", appTopic);
    // this.controlConfirmed = true;
    // }
    // }
    //
    // @Override
    // public void onMessagePublished(int messageId, String appTopic) {
    // if (messageId == this.publishedMsgId) {
    // assertEquals("test", appTopic);
    // this.publishPublished = true;
    // }
    // if (messageId == this.controlMsgId) {
    // assertEquals("control_test", appTopic);
    // this.controlPublished = true;
    // }
    // }
}
