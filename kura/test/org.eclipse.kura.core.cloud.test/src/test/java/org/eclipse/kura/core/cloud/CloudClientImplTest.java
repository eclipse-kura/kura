/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class CloudClientImplTest {

    @Test
    public void testCloudClientImpl() throws NoSuchFieldException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        assertEquals("appId", cloudClient.getApplicationId());
        assertEquals(mockDataService, (DataService) TestUtil.getFieldValue(cloudClient, "dataService"));
        assertEquals(mockCloudService, (CloudServiceImpl) TestUtil.getFieldValue(cloudClient, "cloudServiceImpl"));
        assertNotNull((List<CloudClientListenerAdapter>) TestUtil.getFieldValue(cloudClient, "listeners"));
    }

    @Test
    public void testRelease() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        cloudClient.release();
        verify(mockCloudService, times(1)).removeCloudClient(cloudClient);
    }

    @Test
    public void testAddCloudClientListener() throws NoSuchFieldException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        List<CloudClientListenerAdapter> listeners = (List<CloudClientListenerAdapter>) TestUtil
                .getFieldValue(cloudClient, "listeners");

        assertEquals(1, listeners.size());
        assertEquals(mocktListener, listeners.get(0).getCloudClientListenerAdapted());
    }

    @Test
    public void testRemoveCloudClientListener() throws NoSuchFieldException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);
        cloudClient.removeCloudClientListener(mocktListener);

        List<CloudClientListenerAdapter> listeners = (List<CloudClientListenerAdapter>) TestUtil
                .getFieldValue(cloudClient, "listeners");

        assertEquals(0, listeners.size());
    }

    @Test
    public void testIsConnected() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        doReturn(false).when(mockDataService).isConnected();
        assertFalse(cloudClient.isConnected());

        doReturn(true).when(mockDataService).isConnected();
        assertTrue(cloudClient.isConnected());
    }

    @Test
    public void testPublishStringKuraPayloadIntBoolean() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Parameters for publish method
        String appTopic = "appTopic";
        KuraPayload payload = new KuraPayload();
        int qos = 0;
        boolean retain = false;

        // Prepare data service and cloud service mocks
        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();
        byte[] appPayload = { 1, 2, 3 };
        int priority = 5;
        int expectedValue = 42;

        doReturn(appPayload).when(mockCloudService).encodePayload(payload);
        doReturn(expectedValue).when(mockDataService).publish(fullTopic, appPayload, qos, retain, priority);

        // Execute method
        int value = cloudClient.publish(appTopic, payload, qos, retain);
        assertEquals(expectedValue, value);
    }

    @Test
    public void testPublishStringKuraPayloadIntBooleanInt() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Parameters for publish method
        String appTopic = "appTopic";
        KuraPayload payload = new KuraPayload();
        int qos = 0;
        boolean retain = false;
        int priority = 6;

        // Prepare data service and cloud service mocks
        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();
        byte[] appPayload = { 1, 2, 3 };
        int expectedValue = 42;

        doReturn(appPayload).when(mockCloudService).encodePayload(payload);
        doReturn(expectedValue).when(mockDataService).publish(fullTopic, appPayload, qos, retain, priority);

        // Execute method
        int value = cloudClient.publish(appTopic, payload, qos, retain, priority);
        assertEquals(expectedValue, value);
    }

    @Test
    public void testPublishStringByteArrayIntBooleanInt() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Parameters for publish method
        String appTopic = "appTopic";
        byte[] payload = { 1, 2, 3 };
        int qos = 0;
        boolean retain = false;
        int priority = 6;

        // Prepare data service mock
        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();
        int expectedValue = 42;

        doReturn(expectedValue).when(mockDataService).publish(fullTopic, payload, qos, retain, priority);

        // Execute method
        int value = cloudClient.publish(appTopic, payload, qos, retain, priority);
        assertEquals(expectedValue, value);
    }

    @Test
    public void testControlPublishStringKuraPayloadIntBooleanInt() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Parameters for publish method
        String appTopic = "appTopic";
        KuraPayload payload = new KuraPayload();
        int qos = 0;
        boolean retain = false;
        int priority = 6;

        // Prepare data service and cloud service mocks
        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicControlPrefix()).append(options.getTopicSeparator());
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();
        byte[] appPayload = { 1, 2, 3 };
        int expectedValue = 42;

        doReturn(appPayload).when(mockCloudService).encodePayload(payload);
        doReturn(expectedValue).when(mockDataService).publish(fullTopic, appPayload, qos, retain, priority);

        // Execute method
        int value = cloudClient.controlPublish(appTopic, payload, qos, retain, priority);
        assertEquals(expectedValue, value);
    }

    @Test
    public void testSubscribeStringInt() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        String appTopic = "appTopic";
        int qos = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();

        // Execute method
        cloudClient.subscribe(appTopic, qos);
        verify(mockDataService, times(1)).subscribe(fullTopic, qos);
    }

    @Test
    public void testControlSubscribeStringInt() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        String appTopic = "appTopic";
        int qos = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicControlPrefix()).append(options.getTopicSeparator());
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();

        // Execute method
        cloudClient.controlSubscribe(appTopic, qos);
        verify(mockDataService, times(1)).subscribe(fullTopic, qos);
    }

    @Test
    public void testUnsubscribeString() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        String appTopic = "appTopic";

        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();

        // Execute method
        cloudClient.unsubscribe(appTopic);
        verify(mockDataService, times(1)).unsubscribe(fullTopic);
    }

    @Test
    public void testControlUnsubscribeStringString() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        String appTopic = "appTopic";

        StringBuilder sb = new StringBuilder();
        sb.append(options.getTopicControlPrefix()).append(options.getTopicSeparator());
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator());
        sb.append(options.getTopicClientIdToken()).append(options.getTopicSeparator()).append("appId");
        sb.append(options.getTopicSeparator()).append(appTopic);

        String fullTopic = sb.toString();

        // Execute method
        cloudClient.controlUnsubscribe(appTopic);
        verify(mockDataService, times(1)).unsubscribe(fullTopic);
    }

    @Test
    public void testGetUnpublishedMessageIds() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        StringBuilder sb = new StringBuilder();
        sb.append("^(").append("\\$EDC").append(options.getTopicSeparator()).append(")?");
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(".+");
        sb.append(options.getTopicSeparator()).append("appId").append("(/.+)?");

        String topicRegex = sb.toString();
        ArrayList<Integer> expectedMessageIds = new ArrayList<>();
        expectedMessageIds.add(1);
        expectedMessageIds.add(2);
        expectedMessageIds.add(3);

        doReturn(expectedMessageIds).when(mockDataService).getUnpublishedMessageIds(topicRegex);

        // Execute method
        List<Integer> messageIds = cloudClient.getUnpublishedMessageIds();
        assertArrayEquals(expectedMessageIds.toArray(), messageIds.toArray());
    }

    @Test
    public void testGetInFlightMessageIds() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        StringBuilder sb = new StringBuilder();
        sb.append("^(").append("\\$EDC").append(options.getTopicSeparator()).append(")?");
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(".+");
        sb.append(options.getTopicSeparator()).append("appId").append("(/.+)?");

        String topicRegex = sb.toString();
        ArrayList<Integer> expectedMessageIds = new ArrayList<>();
        expectedMessageIds.add(1);
        expectedMessageIds.add(2);
        expectedMessageIds.add(3);

        doReturn(expectedMessageIds).when(mockDataService).getInFlightMessageIds(topicRegex);

        // Execute method
        List<Integer> messageIds = cloudClient.getInFlightMessageIds();
        assertArrayEquals(expectedMessageIds.toArray(), messageIds.toArray());
    }

    @Test
    public void testGetDroppedInFlightMessageIds() throws KuraException {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudServiceOptions options = new CloudServiceOptions(null, null);
        doReturn(options).when(mockCloudService).getCloudServiceOptions();

        // Prepare data service mock
        StringBuilder sb = new StringBuilder();
        sb.append("^(").append("\\$EDC").append(options.getTopicSeparator()).append(")?");
        sb.append(options.getTopicAccountToken()).append(options.getTopicSeparator()).append(".+");
        sb.append(options.getTopicSeparator()).append("appId").append("(/.+)?");

        String topicRegex = sb.toString();
        ArrayList<Integer> expectedMessageIds = new ArrayList<>();
        expectedMessageIds.add(1);
        expectedMessageIds.add(2);
        expectedMessageIds.add(3);

        doReturn(expectedMessageIds).when(mockDataService).getDroppedInFlightMessageIds(topicRegex);

        // Execute method
        List<Integer> messageIds = cloudClient.getDroppedInFlightMessageIds();
        assertArrayEquals(expectedMessageIds.toArray(), messageIds.toArray());
    }

    @Test
    public void testOnMessageArrived() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        String deviceId = "deviceId";
        String appTopic = "appTopic";
        KuraPayload payload = new KuraPayload();
        int qos = 1;
        boolean retain = false;

        cloudClient.onMessageArrived(deviceId, appTopic, payload, qos, retain);

        verify(mocktListener, times(1)).onMessageArrived(deviceId, appTopic, payload, qos, retain);
    }

    @Test
    public void testOnControlMessageArrived() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        String deviceId = "deviceId";
        String appTopic = "appTopic";
        KuraPayload payload = new KuraPayload();
        int qos = 1;
        boolean retain = false;

        cloudClient.onControlMessageArrived(deviceId, appTopic, payload, qos, retain);

        verify(mocktListener, times(1)).onControlMessageArrived(deviceId, appTopic, payload, qos, retain);
    }

    @Test
    public void testOnMessageConfirmed() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        int pubId = 1;
        String appTopic = "appTopic";

        cloudClient.onMessageConfirmed(pubId, appTopic);

        verify(mocktListener, times(1)).onMessageConfirmed(pubId, appTopic);
    }

    @Test
    public void testOnMessagePublished() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        int pubId = 1;
        String appTopic = "appTopic";

        cloudClient.onMessagePublished(pubId, appTopic);

        verify(mocktListener, times(1)).onMessagePublished(pubId, appTopic);
    }

    @Test
    public void testOnConnectionEstablished() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        cloudClient.onConnectionEstablished();

        verify(mocktListener, times(1)).onConnectionEstablished();
    }

    @Test
    public void testOnConnectionLost() {
        DataService mockDataService = mock(DataService.class);
        CloudServiceImpl mockCloudService = mock(CloudServiceImpl.class);
        CloudClientImpl cloudClient = new CloudClientImpl("appId", mockDataService, mockCloudService);

        CloudClientListener mocktListener = mock(CloudClientListener.class);
        cloudClient.addCloudClientListener(mocktListener);

        cloudClient.onConnectionLost();

        verify(mocktListener, times(1)).onConnectionLost();
    }
}
