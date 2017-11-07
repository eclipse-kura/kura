/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.watchdog.WatchdogService;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class DataServiceImplTest {

    @Test
    public void testStartDbStore() throws Throwable {
        // test starting the store

        final int hkInterval = 1000;
        final int age = 100;
        final int capacity = 5000;

        DataServiceImpl svc = new DataServiceImpl();

        H2DbService dbServiceMock = mock(H2DbService.class);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("store.housekeeper-interval", hkInterval);
        properties.put("store.purge-age", age);
        properties.put("store.capacity", capacity);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        List<DataMessage> messages = new ArrayList<>();
        int id = 123;
        int pmi = 1234;
        String sessionId = "session";
        DataMessage msg = new DataMessage.Builder(id).withPublishedMessageId(pmi).withSessionId(sessionId).build();
        messages.add(msg);
        when(storeMock.allInFlightMessagesNoPayload()).thenReturn(messages);

        // the actual invocation
        svc.setH2DbService(dbServiceMock);

        verify(storeMock, times(1)).start(dbServiceMock, hkInterval, age, capacity);

        Map<DataTransportToken, Integer> ifMsgs = (Map<DataTransportToken, Integer>) TestUtil.getFieldValue(svc,
                "inFlightMsgIds");

        assertEquals(1, ifMsgs.size());
        ifMsgs.forEach((key, value) -> {
            assertEquals(id, (int) value);
            assertEquals(pmi, key.getMessageId());
            assertEquals(sessionId, key.getSessionId());
        });
    }

    @Test
    public void testStartDbStoreErrorLog() throws Throwable {
        // produce the start store failure log message

        DataServiceImpl svc = new DataServiceImpl();

        H2DbService dbServiceMock = mock(H2DbService.class);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        doThrow(new KuraStoreException("test")).when(storeMock).start(dbServiceMock, 900, 60, 10000);

        Map<String, Object> properties = new HashMap<>();
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        // the actual invocation
        svc.setH2DbService(dbServiceMock);

        verify(storeMock, times(1)).start(dbServiceMock, 900, 60, 10000);
    }

    @Test
    public void testConnectionEstablished() throws NoSuchFieldException, KuraStoreException {
        // new session, don't publish in-flight messages

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", false);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(storeMock, times(1)).dropAllInFlightMessages();
        verify(inFlightMsgIds, times(1)).clear();
    }

    @Test
    public void testConnectionEstablishedErrorLog() throws NoSuchFieldException, KuraStoreException {
        // new session, don't publish in-flight messages, trigger error log

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        doThrow(new KuraStoreException("test")).when(storeMock).dropAllInFlightMessages();

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", false);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(storeMock, times(1)).dropAllInFlightMessages();
        verify(inFlightMsgIds, times(0)).clear();
    }

    @Test
    public void testConnectionEstablishedWithPublish() throws NoSuchFieldException, KuraStoreException {
        // new session, publish in-flight messages

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", true);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(storeMock, times(1)).unpublishAllInFlighMessages();
        verify(inFlightMsgIds, times(1)).clear();
    }

    @Test
    public void testConnectionEstablishedWithPublishErrorLog() throws NoSuchFieldException, KuraStoreException {
        // new session, publish in-flight messages, trigger error log

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        doThrow(new KuraStoreException("test")).when(storeMock).unpublishAllInFlighMessages();

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", true);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(storeMock, times(1)).unpublishAllInFlighMessages();
        verify(inFlightMsgIds, times(0)).clear();
    }

    @Test
    public void testStopStartConnectionMonitorExceptionStillRunning() throws NoSuchFieldException {
        // stop and start connection monitor task; trigger exception with task state

        DataServiceImpl svc = new DataServiceImpl();

        ScheduledFuture<?> connectionMonitorFutureMock = mock(ScheduledFuture.class);
        TestUtil.setFieldValue(svc, "connectionMonitorFuture", connectionMonitorFutureMock);
        when(connectionMonitorFutureMock.isDone()).thenReturn(false);

        WatchdogService wsMock = mock(WatchdogService.class);
        svc.setWatchdogService(wsMock);

        try {
            svc.onConnectionLost(new Exception("test"));
            fail("Exception expected");
        } catch (IllegalStateException e) {
            assertEquals("Reconnect task already running", e.getMessage());
        }

        verify(connectionMonitorFutureMock, times(2)).isDone();
        verify(connectionMonitorFutureMock, times(1)).cancel(true);
        verify(wsMock, times(1)).unregisterCriticalComponent(svc);
    }

    @Test
    public void testStopStartConnectionMonitorNoAutoConnect() throws NoSuchFieldException {
        // stop and start connection monitor task; don't auto-connect, so disable service

        DataServiceImpl svc = new DataServiceImpl();

        ScheduledFuture<?> connectionMonitorFutureMock = mock(ScheduledFuture.class);
        TestUtil.setFieldValue(svc, "connectionMonitorFuture", connectionMonitorFutureMock);
        when(connectionMonitorFutureMock.isDone()).thenReturn(false).thenReturn(true);

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        WatchdogService wsMock = mock(WatchdogService.class);
        svc.setWatchdogService(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("connect.auto-on-startup", false);
        properties.put("connect.retry-interval", 10);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        svc.onConnectionLost(new Exception("test"));

        verify(connectionMonitorFutureMock, times(2)).isDone();
        verify(connectionMonitorFutureMock, times(1)).cancel(true);
        verify(wsMock, times(2)).unregisterCriticalComponent(svc);
        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.OFF);
    }

    @Test
    public void testStopStartConnectionMonitor() throws NoSuchFieldException {
        // stop and start connection monitor task scheduling the executor

        DataServiceImpl svc = new DataServiceImpl();

        ScheduledFuture<?> connectionMonitorFutureMock = mock(ScheduledFuture.class);
        TestUtil.setFieldValue(svc, "connectionMonitorFuture", connectionMonitorFutureMock);
        when(connectionMonitorFutureMock.isDone()).thenReturn(false).thenReturn(true);

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        WatchdogService wsMock = mock(WatchdogService.class);
        svc.setWatchdogService(wsMock);

        ScheduledExecutorService cmeMock = mock(ScheduledExecutorService.class);
        TestUtil.setFieldValue(svc, "connectionMonitorExecutor", cmeMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("connect.auto-on-startup", true);
        properties.put("connect.retry-interval", 10);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        svc.onConnectionLost(new Exception("test"));

        verify(connectionMonitorFutureMock, times(2)).isDone();
        verify(connectionMonitorFutureMock, times(1)).cancel(true);
        verify(wsMock, times(1)).unregisterCriticalComponent(svc);
        verify(wsMock, times(1)).registerCriticalComponent(svc);
        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.SLOW_BLINKING);
        verify(cmeMock, times(1)).scheduleAtFixedRate(anyObject(), anyInt(), eq(10L), eq(TimeUnit.SECONDS));

        // future is reset to scheduler result (null)
        assertNull(TestUtil.getFieldValue(svc, "connectionMonitorFuture"));
    }

    @Test
    public void testMessageConfirmedNoMessageFound() throws NoSuchFieldException {
        // invokes the logger - inflight message not found

        DataServiceImpl svc = new DataServiceImpl();

        int msgId = 1234;
        String sessionId = "sess1234";
        DataTransportToken token = new DataTransportToken(msgId, sessionId);

        Map<DataTransportToken, Integer> inFlightMsgIds = new HashMap<>();
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.max-number", 0);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        svc.onMessageConfirmed(token);
    }

    @Test
    public void testMessageConfirmedConfirmedMessageException() throws NoSuchFieldException, KuraStoreException {
        // invokes the logger - confirmed message not found

        DataServiceImpl svc = new DataServiceImpl();

        int msgId = 1234;
        String sessionId = "sess1234";
        DataTransportToken token = new DataTransportToken(msgId, sessionId);

        Map<DataTransportToken, Integer> inFlightMsgIds = new HashMap<>();
        inFlightMsgIds.put(token, msgId);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        doThrow(new KuraStoreException("test")).when(storeMock).confirmed(msgId);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.max-number", 0);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        svc.onMessageConfirmed(token);

        verify(storeMock, times(1)).confirmed(msgId);
    }

    @Test
    public void testGetUnpublishedMessageIds() throws NoSuchFieldException, KuraStoreException {
        // build message ids from unpublished list

        DataServiceImpl svc = new DataServiceImpl();

        List<DataMessage> unpublished = new ArrayList<>();
        DataMessage msg = new DataMessage.Builder(1).withTopic("notpublished").build();
        unpublished.add(msg);
        msg = new DataMessage.Builder(2).withTopic("unpublished").build();
        unpublished.add(msg);

        List<DataMessage> inflight = new ArrayList<>();
        msg = new DataMessage.Builder(3).withTopic("unpublished").build();
        inflight.add(msg);

        List<DataMessage> dropped = new ArrayList<>();
        msg = new DataMessage.Builder(4).withTopic("unpublished").build();
        inflight.add(msg);

        expectAllMessages(svc, unpublished, inflight, dropped);

        String topic = "unp.*";

        List<Integer> ids = svc.getUnpublishedMessageIds(topic);

        assertEquals(1, ids.size());
        assertEquals(2, (int) ids.get(0));
    }

    @Test
    public void testGetInFlightMessageIds() throws NoSuchFieldException, KuraStoreException {
        // build message ids from in-flight list

        DataServiceImpl svc = new DataServiceImpl();

        List<DataMessage> unpublished = new ArrayList<>();
        DataMessage msg = new DataMessage.Builder(1).withTopic("inf").build();
        unpublished.add(msg);
        msg = new DataMessage.Builder(2).withTopic("unpub").build();
        unpublished.add(msg);

        List<DataMessage> inflight = new ArrayList<>();
        msg = new DataMessage.Builder(3).withTopic("unpublished").build();
        inflight.add(msg);
        msg = new DataMessage.Builder(4).withTopic("info").build();
        inflight.add(msg);

        List<DataMessage> dropped = new ArrayList<>();
        msg = new DataMessage.Builder(5).withTopic("inf").build();
        dropped.add(msg);

        expectAllMessages(svc, unpublished, inflight, dropped);

        String topic = "inf.*";

        List<Integer> ids = svc.getInFlightMessageIds(topic);

        assertEquals(1, ids.size());
        assertEquals(4, (int) ids.get(0));
    }

    @Test
    public void testGetDroppedMessageIds() throws NoSuchFieldException, KuraStoreException {
        // build message ids from the dropped list

        DataServiceImpl svc = new DataServiceImpl();

        List<DataMessage> unpublished = new ArrayList<>();
        DataMessage msg = new DataMessage.Builder(1).withTopic("someone dropped it").build();
        unpublished.add(msg);
        msg = new DataMessage.Builder(2).withTopic("unpub").build();
        unpublished.add(msg);

        List<DataMessage> inflight = new ArrayList<>();
        msg = new DataMessage.Builder(3).withTopic("unpublished").build();
        inflight.add(msg);
        msg = new DataMessage.Builder(4).withTopic("info").build();
        inflight.add(msg);
        msg = new DataMessage.Builder(5).withTopic("not dropped").build();
        inflight.add(msg);

        List<DataMessage> dropped = new ArrayList<>();
        msg = new DataMessage.Builder(6).withTopic("great drop").build();
        dropped.add(msg);
        msg = new DataMessage.Builder(7).withTopic("do not drop it").build();
        dropped.add(msg);

        expectAllMessages(svc, unpublished, inflight, dropped);

        String topic = ".*drop";

        List<Integer> ids = svc.getDroppedInFlightMessageIds(topic);

        assertEquals(1, ids.size());
        assertEquals(6, (int) ids.get(0));
    }

    private DataStore expectAllMessages(DataServiceImpl svc, List<DataMessage> unpublished, List<DataMessage> inFlight,
            List<DataMessage> dropped) throws NoSuchFieldException, KuraStoreException {

        DataStore storeMock = mock(DataStore.class);
        TestUtil.setFieldValue(svc, "store", storeMock);

        when(storeMock.allUnpublishedMessagesNoPayload()).thenReturn(unpublished);
        when(storeMock.allInFlightMessagesNoPayload()).thenReturn(inFlight);
        when(storeMock.allDroppedInFlightMessagesNoPayload()).thenReturn(dropped);

        return storeMock;
    }

    @Test
    public void testCongestion() throws Throwable {
        // run handleInFlightCongestion

        DataServiceImpl svc = new DataServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.congestion-timeout", 100);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        ScheduledExecutorService congestionMock = mock(ScheduledExecutorService.class);
        TestUtil.setFieldValue(svc, "congestionExecutor", congestionMock);

        TestUtil.invokePrivate(svc, "handleInFlightCongestion");

        verify(congestionMock, times(1)).schedule((Runnable) anyObject(), eq(100L), eq(TimeUnit.SECONDS));
    }

}
