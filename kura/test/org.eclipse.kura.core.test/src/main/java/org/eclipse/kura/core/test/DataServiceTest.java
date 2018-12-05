/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class DataServiceTest implements DataServiceListener {

    private static final Logger s_logger = LoggerFactory.getLogger(DataServiceTest.class);
    
    private static CountDownLatch s_dependencyLatch = new CountDownLatch(1);	// initialize with number of
    // dependencies
    private static DataService s_dataService;

    private static Set<Integer> s_qos0MsgIds = new HashSet<Integer>();
    private static Set<Integer> s_qos12MsgIds = new HashSet<Integer>();
    private static Set<Integer> s_qos12HighPriorityMsgIds = new HashSet<Integer>();

    private static Lock s_lock = new ReentrantLock();
    private static Condition s_connected = s_lock.newCondition();
    private static Condition s_disconnecting = s_lock.newCondition();
    private static Condition s_disconnected = s_lock.newCondition();
    private static Condition s_arrived = s_lock.newCondition();

    static final int MAX_MSGS = 100;
    static final int ALL_PUBLISHED_TIMEOUT = 30;
    static final int ALL_CONFIRMED_QOS1_TIMEOUT = 60;
    static final int ALL_CONFIRMED_QOS2_TIMEOUT = 120;
    static final int DFLT_MSG_PRIORITY = 5;
    static final int HIGH_MSG_PRIORITY = 0;
    static final String MSG_SEMATIC_TOPIC1 = "data/service/test/" + UUID.randomUUID().toString();
    static final String MSG_SEMATIC_TOPIC2 = "data/service/test/" + UUID.randomUUID().toString();
    static final String MSG_TOPIC1 = "#account-name/#client-id/" + MSG_SEMATIC_TOPIC1;
    static final String MSG_TOPIC2 = "#account-name/#client-id/" + MSG_SEMATIC_TOPIC2;
    static final String MSG_PAYLOAD = "Lorem ipsum dolor sit amet";

    // JUnit 4 and is called once
    @BeforeClass
    public static void setUpBeforeClass() {
        // Wait for OSGi dependencies
        try {
            s_dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }
    }

    public void setDataService(DataService dataService) {
        s_dataService = dataService;
        s_dependencyLatch.countDown();
    }

    public void unsetDataService(DataService dataService) {
        s_dataService = null;
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testConnect() throws KuraConnectException {
        if (!s_dataService.isConnected()) {
            s_dataService.connect();
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testDisconnect() throws KuraConnectException, InterruptedException {
        if (!s_dataService.isConnected()) {
            s_dataService.connect();
        }

        s_dataService.disconnect(0);
        assertFalse(s_dataService.isConnected());

        // TODO: if auto-connect is enabled check it does not
        // automatically reconnects.

        // test onConnectionEstablished
        s_lock.lock();
        try {
            s_dataService.connect();
            s_connected.await(30, TimeUnit.SECONDS);
        } catch (KuraConnectException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            s_lock.unlock();
        }

        // test onDisconnecting/onDisconnected
        s_lock.lock();
        s_dataService.disconnect(0);
        s_disconnecting.await(1, TimeUnit.SECONDS);
        s_disconnected.await(1, TimeUnit.SECONDS);
        s_lock.unlock();
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testPublish() throws KuraConnectException, InterruptedException {
        if (!s_dataService.isConnected()) {
            s_dataService.connect();
        }

        // publish at QoS = 0
        synchronized (s_qos0MsgIds) {
            s_qos0MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos0MsgIds) {
                    Integer id = s_dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 0, false, DFLT_MSG_PRIORITY);
                    s_qos0MsgIds.add(id);
                }
                Thread.sleep(1000);
            } catch (KuraStoreException e) {
                break;
            }
        }

        boolean allPublished = false;
        for (int i = 0; i < ALL_PUBLISHED_TIMEOUT; i++) {
            synchronized (s_qos0MsgIds) {
                if (s_qos0MsgIds.isEmpty()) {
                    allPublished = true;
                    break;
                }
            }
            Thread.sleep(1000);
        }

        assertTrue(allPublished);

        // publish at QoS = 1
        synchronized (s_qos12MsgIds) {
            s_qos12MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos12MsgIds) {
                    Integer id = s_dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, DFLT_MSG_PRIORITY);
                    s_qos12MsgIds.add(id);
                    s_logger.info("Added id: {}", id);
                }
            } catch (KuraStoreException e) {
                break;
            }
        }

        boolean allConfirmed = false;
        for (int i = 0; i < ALL_CONFIRMED_QOS1_TIMEOUT; i++) {
            synchronized (s_qos12MsgIds) {
                s_logger.info("confirm check round {}", i);
                s_qos12MsgIds.forEach(element -> s_logger.info("To confirm: {}", element));
                if (s_qos12MsgIds.isEmpty()) {
                    allConfirmed = true;
                    break;
                }
            }
            Thread.sleep(1000);
        }

        s_logger.info("All confirmed value: {}", allConfirmed);
        assertTrue(allConfirmed);

        // publish at QoS = 2
        synchronized (s_qos12MsgIds) {
            s_qos12MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos12MsgIds) {
                    Integer id = s_dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 2, false, DFLT_MSG_PRIORITY);
                    s_qos12MsgIds.add(id);
                }
            } catch (KuraStoreException e) {
                break;
            }
        }

        allConfirmed = false;
        for (int i = 0; i < ALL_CONFIRMED_QOS2_TIMEOUT; i++) {
            synchronized (s_qos12MsgIds) {
                if (s_qos12MsgIds.isEmpty()) {
                    allConfirmed = true;
                    break;
                }
            }
            Thread.sleep(1000);
        }

        assertTrue(allConfirmed);

        //
        // publish at two different priorities at QoS = 1

        // First publish half of the messages at default priority
        synchronized (s_qos12MsgIds) {
            s_qos12MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos12MsgIds) {
                    Integer id = s_dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, DFLT_MSG_PRIORITY);
                    s_qos12MsgIds.add(id);
                    s_logger.info("Added id: {}", id);
                }
            } catch (KuraStoreException e) {
                break;
            }
        }

        // ... then publish half of the messages at higher priority
        synchronized (s_qos12HighPriorityMsgIds) {
            s_qos12HighPriorityMsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos12HighPriorityMsgIds) {
                    Integer id = s_dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, HIGH_MSG_PRIORITY);
                    s_qos12HighPriorityMsgIds.add(id);
                }
            } catch (KuraStoreException e) {
                break;
            }
        }

        // messages published at higher priority are expected to be
        // confirmed before messages published at default priority
        allConfirmed = false;
        for (int i = 0; i < ALL_CONFIRMED_QOS1_TIMEOUT; i++) {
            synchronized (s_qos12MsgIds) {
                synchronized (s_qos12HighPriorityMsgIds) {
                    s_logger.info("confirm check round {}", i);
                    s_qos12HighPriorityMsgIds.forEach(element -> s_logger.info("To confirm s_qos12HighPriorityMsgIds: {}", element));
                    s_qos12MsgIds.forEach(element -> s_logger.info("To confirm s_qos12MsgIds: {}", element));
                    if (!s_qos12HighPriorityMsgIds.isEmpty() && s_qos12MsgIds.isEmpty()) {
                        fail("High priority messages should be confirmed before default priority messages");
                    } else if (s_qos12HighPriorityMsgIds.isEmpty() && s_qos12MsgIds.isEmpty()) {
                        allConfirmed = true;
                        break;
                    }
                }
            }
            Thread.sleep(1000);
        }

        s_logger.info("All confirmed value: {}", allConfirmed);
        assertTrue(allConfirmed);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSubscribe() throws KuraException, InterruptedException {
        if (!s_dataService.isConnected()) {
            s_dataService.connect();
        }

        s_lock.lock();
        try {
            s_dataService.subscribe(MSG_TOPIC2, 0);
            s_dataService.publish(MSG_TOPIC2, MSG_PAYLOAD.getBytes(), 0, false, HIGH_MSG_PRIORITY);
            boolean arrived = s_arrived.await(5, TimeUnit.SECONDS);
            assertTrue(arrived);
        } catch (KuraException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            s_lock.unlock();
        }
    }

    @Override
    public void onConnectionEstablished() {
        s_lock.lock();
        s_connected.signal();
        s_lock.unlock();
    }

    @Override
    public void onDisconnecting() {
        s_lock.lock();
        s_disconnecting.signal();
        s_lock.unlock();
    }

    @Override
    public void onDisconnected() {
        s_lock.lock();
        s_disconnected.signal();
        s_lock.unlock();
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        s_lock.lock();
        if (topic.endsWith(MSG_SEMATIC_TOPIC2)) {
            s_arrived.signal();
        }
        s_lock.unlock();
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        synchronized (s_qos0MsgIds) {
            s_qos0MsgIds.remove(messageId);
        }
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        synchronized (s_qos12MsgIds) {
            s_qos12MsgIds.remove(messageId);
        }
        synchronized (s_qos12HighPriorityMsgIds) {
            s_qos12HighPriorityMsgIds.remove(messageId);
        }
    }
}
