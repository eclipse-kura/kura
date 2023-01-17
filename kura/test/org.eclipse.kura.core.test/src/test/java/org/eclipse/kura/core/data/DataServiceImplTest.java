/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.data;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.message.store.provider.MessageStoreProvider;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.watchdog.WatchdogService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class DataServiceImplTest {

    @Test
    public void testStartDbStore() throws Throwable {
        // test starting the store

        final int hkInterval = 1000;
        final int age = 100;
        final int capacity = 5000;

        DataServiceImpl svc = new DataServiceImpl();

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("store.housekeeper-interval", hkInterval);
        properties.put("store.purge-age", age);
        properties.put("store.capacity", capacity);
        properties.put("kura.service.pid", "foo");
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        List<StoredMessage> messages = new ArrayList<>();
        int id = 123;
        int pmi = 1234;
        String sessionId = "session";
        StoredMessage msg = new StoredMessage.Builder(id).withDataTransportToken(new DataTransportToken(pmi, sessionId))
                .build();
        messages.add(msg);
        when(messageStoreMock.getInFlightMessages()).thenReturn(messages);

        // the actual invocation
        svc.setMessageStoreProvider(messageStoreProviderMock);

        verify(messageStoreProviderMock, times(1)).openMessageStore("foo", capacity);

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
    public void testConnectionEstablished() throws NoSuchFieldException, KuraStoreException {
        // new session, don't publish in-flight messages

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", false);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        svc.setMessageStoreProvider(messageStoreProviderMock);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(messageStoreMock, times(1)).dropAllInFlightMessages();
        verify(inFlightMsgIds, times(1)).clear();
    }

    @Test
    public void testConnectionEstablishedErrorLog() throws NoSuchFieldException, KuraStoreException {
        // new session, don't publish in-flight messages, trigger error log

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        doThrow(new KuraStoreException("test")).when(messageStoreMock).dropAllInFlightMessages();

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", false);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.setMessageStoreProvider(messageStoreProviderMock);
        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(messageStoreMock, times(1)).dropAllInFlightMessages();
        verify(inFlightMsgIds, times(0)).clear();
    }

    @Test
    public void testConnectionEstablishedWithPublish() throws NoSuchFieldException, KuraStoreException {
        // new session, publish in-flight messages

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", true);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        svc.setMessageStoreProvider(messageStoreProviderMock);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(messageStoreMock, times(1)).unpublishAllInFlighMessages();
        verify(inFlightMsgIds, times(1)).clear();
    }

    @Test
    public void testConnectionEstablishedWithPublishErrorLog() throws NoSuchFieldException, KuraStoreException {
        // new session, publish in-flight messages, trigger error log

        DataServiceImpl svc = new DataServiceImpl();

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        doThrow(new KuraStoreException("test")).when(messageStoreMock).unpublishAllInFlighMessages();

        ComponentContext ctxMock = mock(ComponentContext.class);
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.republish-on-new-session", true);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        Map<DataTransportToken, Integer> inFlightMsgIds = mock(Map.class);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.setMessageStoreProvider(messageStoreProviderMock);
        svc.onConnectionEstablished(true);

        verify(ccssMock, times(1)).updateStatus(svc, CloudConnectionStatusEnum.ON);
        verify(messageStoreMock, times(1)).unpublishAllInFlighMessages();
        verify(inFlightMsgIds, times(0)).clear();
    }

    @Test
    public void testActivateAndConnect() throws NoSuchFieldException, InterruptedException, KuraStoreException {
        // stop and start connection monitor task scheduling the executor, wait for it
        // to run a couple of times, then
        // connect

        DataServiceImpl svc = new DataServiceImpl();

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        ScheduledFuture<?> connectionMonitorFutureMock = mock(ScheduledFuture.class);
        TestUtil.setFieldValue(svc, "connectionMonitorFuture", connectionMonitorFutureMock);
        when(connectionMonitorFutureMock.isDone()).thenReturn(false).thenReturn(true);

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        WatchdogService wsMock = mock(WatchdogService.class);
        svc.setWatchdogService(wsMock);

        Object lock = new Object();
        doAnswer(invocation -> {
            synchronized (lock) {
                lock.notifyAll();
            }
            return null;
        }).when(wsMock).unregisterCriticalComponent(svc);

        ScheduledExecutorService cme = Executors.newSingleThreadScheduledExecutor();
        TestUtil.setFieldValue(svc, "connectionMonitorExecutor", cme);

        Map<String, Object> properties = new HashMap<>();
        properties.put("connect.auto-on-startup", true);
        properties.put("connect.retry-interval", 2);
        properties.put("kura.service.pid", "foo");
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);
        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(mock(BundleContext.class));
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        DataTransportService dtsMock = mock(DataTransportService.class);
        TestUtil.setFieldValue(svc, "dataTransportService", dtsMock);
        when(dtsMock.isConnected()).thenReturn(false); // trigger call to connect()

        svc.activate(ctxMock, properties);

        // attach a H2DbService after a while
        // add also DataTransportService - to report it's not connected
        Thread.sleep(3000);

        svc.setMessageStoreProvider(messageStoreProviderMock);

        synchronized (lock) {
            lock.wait(20000);
        }

        verify(wsMock, times(1)).unregisterCriticalComponent(svc);
    }

    @Test
    public void testActivateAndFailConnecting()
            throws NoSuchFieldException, InterruptedException, KuraConnectException, KuraStoreException {
        // stop and start connection monitor task scheduling the executor, then try to
        // connect and fail with
        // authentication exceptions and a few more

        DataServiceImpl svc = new DataServiceImpl();

        ScheduledFuture<?> connectionMonitorFutureMock = mock(ScheduledFuture.class);
        TestUtil.setFieldValue(svc, "connectionMonitorFuture", connectionMonitorFutureMock);
        when(connectionMonitorFutureMock.isDone()).thenReturn(false).thenReturn(true);

        CloudConnectionStatusService ccssMock = mock(CloudConnectionStatusService.class);
        svc.setCloudConnectionStatusService(ccssMock);

        WatchdogService wsMock = mock(WatchdogService.class);
        svc.setWatchdogService(wsMock);

        AtomicInteger count = new AtomicInteger(0);
        Object lock = new Object();
        doAnswer(invocation -> {
            if (count.incrementAndGet() > 8) { // shouldn't happen
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
            return null;
        }).when(wsMock).checkin(svc);

        ScheduledExecutorService cme = Executors.newSingleThreadScheduledExecutor();
        TestUtil.setFieldValue(svc, "connectionMonitorExecutor", cme);

        // executor service parameters
        Map<String, Object> properties = new HashMap<>();
        properties.put("connect.auto-on-startup", true);
        properties.put("connect.retry-interval", 1);
        properties.put("connection.recovery.max.failures", 4);
        properties.put("kura.service.pid", "foo");
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);
        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(mock(BundleContext.class));
        DataServiceListenerS dataServiceListeners = new DataServiceListenerS(ctxMock);
        TestUtil.setFieldValue(svc, "dataServiceListeners", dataServiceListeners);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        // for connection monitor task to try connecting and fail
        DataTransportService dtsMock = mock(DataTransportService.class);
        TestUtil.setFieldValue(svc, "dataTransportService", dtsMock);
        when(dtsMock.isConnected()).thenReturn(false); // trigger call to connect()

        // for the exception to be verified as non-critical: 1-3: authentication exc.,
        // 4.-: ordinary exceptions
        MqttException cause = new MqttException(MqttException.REASON_CODE_FAILED_AUTHENTICATION);
        Throwable exc1 = new KuraConnectException(cause, "test");
        cause = new MqttException(MqttException.REASON_CODE_INVALID_CLIENT_ID);
        Throwable exc2 = new KuraConnectException(cause, "test");
        cause = new MqttException(MqttException.REASON_CODE_NOT_AUTHORIZED);
        Throwable exc3 = new KuraConnectException(cause, "test");
        cause = new MqttException(MqttException.REASON_CODE_BROKER_UNAVAILABLE);
        Throwable exc4 = new KuraConnectException(cause, "test");
        doThrow(exc1).doThrow(exc2).doThrow(exc3).doThrow(exc4)
                .doThrow(new KuraConnectException("test ordinary exception")).when(dtsMock).connect();

        svc.setMessageStoreProvider(messageStoreProviderMock);
        svc.activate(ctxMock, properties);

        // wait long enough for the task can run 7-8 times
        synchronized (lock) {
            lock.wait(8000);
        }

        // initial checkin + 3 * authentication + (other mqtt + 3)
        assertEquals(8, count.intValue());
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

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock);

        doThrow(new KuraStoreException("test")).when(messageStoreMock).markAsConfirmed(msgId);

        Map<String, Object> properties = new HashMap<>();
        properties.put("in-flight-messages.max-number", 0);
        DataServiceOptions dataServiceOptions = new DataServiceOptions(properties);

        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        svc.setMessageStoreProvider(messageStoreProviderMock);

        Map<DataTransportToken, Integer> inFlightMsgIds = new HashMap<>();
        inFlightMsgIds.put(token, msgId);
        TestUtil.setFieldValue(svc, "inFlightMsgIds", inFlightMsgIds);

        svc.onMessageConfirmed(token);

        verify(messageStoreMock, times(1)).markAsConfirmed(msgId);
    }

    @Test
    public void testGetUnpublishedMessageIds() throws NoSuchFieldException, KuraStoreException {
        // build message ids from unpublished list

        DataServiceImpl svc = new DataServiceImpl();

        DataServiceOptions dataServiceOptions = new DataServiceOptions(Collections.emptyMap());
        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        List<StoredMessage> unpublished = new ArrayList<>();
        StoredMessage msg = new StoredMessage.Builder(1).withTopic("notpublished").build();
        unpublished.add(msg);
        msg = new StoredMessage.Builder(2).withTopic("unpublished").build();
        unpublished.add(msg);

        List<StoredMessage> inflight = new ArrayList<>();
        msg = new StoredMessage.Builder(3).withTopic("unpublished").build();
        inflight.add(msg);

        List<StoredMessage> dropped = new ArrayList<>();
        msg = new StoredMessage.Builder(4).withTopic("unpublished").build();
        inflight.add(msg);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock, unpublished, inflight, dropped);
        svc.setMessageStoreProvider(messageStoreProviderMock);

        String topic = "unp.*";

        List<Integer> ids = svc.getUnpublishedMessageIds(topic);

        assertEquals(1, ids.size());
        assertEquals(2, (int) ids.get(0));
    }

    @Test
    public void testGetInFlightMessageIds() throws NoSuchFieldException, KuraStoreException {
        // build message ids from in-flight list

        DataServiceImpl svc = new DataServiceImpl();

        DataServiceOptions dataServiceOptions = new DataServiceOptions(Collections.emptyMap());
        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        List<StoredMessage> unpublished = new ArrayList<>();
        StoredMessage msg = new StoredMessage.Builder(1).withTopic("inf").build();
        unpublished.add(msg);
        msg = new StoredMessage.Builder(2).withTopic("unpub").build();
        unpublished.add(msg);

        List<StoredMessage> inflight = new ArrayList<>();
        msg = new StoredMessage.Builder(3).withTopic("unpublished").build();
        inflight.add(msg);
        msg = new StoredMessage.Builder(4).withTopic("info").build();
        inflight.add(msg);

        List<StoredMessage> dropped = new ArrayList<>();
        msg = new StoredMessage.Builder(5).withTopic("inf").build();
        dropped.add(msg);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock, unpublished, inflight, dropped);
        svc.setMessageStoreProvider(messageStoreProviderMock);

        String topic = "inf.*";

        List<Integer> ids = svc.getInFlightMessageIds(topic);

        assertEquals(1, ids.size());
        assertEquals(4, (int) ids.get(0));
    }

    @Test
    public void testGetDroppedMessageIds() throws NoSuchFieldException, KuraStoreException {
        // build message ids from the dropped list

        DataServiceImpl svc = new DataServiceImpl();

        DataServiceOptions dataServiceOptions = new DataServiceOptions(Collections.emptyMap());
        TestUtil.setFieldValue(svc, "dataServiceOptions", dataServiceOptions);

        List<StoredMessage> unpublished = new ArrayList<>();
        StoredMessage msg = new StoredMessage.Builder(1).withTopic("someone dropped it").build();
        unpublished.add(msg);
        msg = new StoredMessage.Builder(2).withTopic("unpub").build();
        unpublished.add(msg);

        List<StoredMessage> inflight = new ArrayList<>();
        msg = new StoredMessage.Builder(3).withTopic("unpublished").build();
        inflight.add(msg);
        msg = new StoredMessage.Builder(4).withTopic("info").build();
        inflight.add(msg);
        msg = new StoredMessage.Builder(5).withTopic("not dropped").build();
        inflight.add(msg);

        List<StoredMessage> dropped = new ArrayList<>();
        msg = new StoredMessage.Builder(6).withTopic("great drop").build();
        dropped.add(msg);
        msg = new StoredMessage.Builder(7).withTopic("do not drop it").build();
        dropped.add(msg);

        MessageStoreProvider messageStoreProviderMock = mock(MessageStoreProvider.class);
        MessageStore messageStoreMock = mock(MessageStore.class);
        initMockMessageStore(messageStoreProviderMock, messageStoreMock, unpublished, inflight, dropped);
        svc.setMessageStoreProvider(messageStoreProviderMock);

        String topic = ".*drop";

        List<Integer> ids = svc.getDroppedInFlightMessageIds(topic);

        assertEquals(1, ids.size());
        assertEquals(6, (int) ids.get(0));
    }

    private void initMockMessageStore(final MessageStoreProvider messageStoreProviderMock,
            final MessageStore messageStoreMock, List<StoredMessage> unpublished,
            List<StoredMessage> inFlight,
            List<StoredMessage> dropped) throws KuraStoreException {

        when(messageStoreProviderMock.openMessageStore(Mockito.any(), Mockito.anyInt()))
                .thenReturn(messageStoreMock);

        when(messageStoreMock.getUnpublishedMessages()).thenReturn(unpublished);
        when(messageStoreMock.getInFlightMessages()).thenReturn(inFlight);
        when(messageStoreMock.getDroppedMessages()).thenReturn(dropped);

    }

    private void initMockMessageStore(final MessageStoreProvider messageStoreProvider,
            final MessageStore messageStore) throws KuraStoreException {
        initMockMessageStore(messageStoreProvider, messageStore, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList());
    }

}
