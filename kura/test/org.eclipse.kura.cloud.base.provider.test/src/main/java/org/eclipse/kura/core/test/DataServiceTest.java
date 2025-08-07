/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataServiceListener;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class DataServiceTest extends BaseCloudTests implements DataServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceTest.class);

    private static Set<Integer> s_qos0MsgIds = new HashSet<Integer>();
    private static Set<Integer> s_qos12MsgIds = new HashSet<Integer>();
    private static Set<Integer> s_qos12HighPriorityMsgIds = new HashSet<Integer>();

    private static Lock s_lock = new ReentrantLock();
    private static Condition s_connected = s_lock.newCondition();
    private static Condition s_disconnecting = s_lock.newCondition();
    private static Condition s_disconnected = s_lock.newCondition();
    private static Condition s_arrived = s_lock.newCondition();

    static final int MAX_MSGS = 100;
    static final int ALL_CONFIRMED_QOS1_TIMEOUT = 60;
    static final int ALL_CONFIRMED_QOS2_TIMEOUT = 120;
    static final int DFLT_MSG_PRIORITY = 5;
    static final int HIGH_MSG_PRIORITY = 0;
    static final String MSG_SEMATIC_TOPIC1 = "data/service/test/" + UUID.randomUUID().toString();
    static final String MSG_SEMATIC_TOPIC2 = "data/service/test/" + UUID.randomUUID().toString();
    static final String MSG_TOPIC1 = "#account-name/#client-id/" + MSG_SEMATIC_TOPIC1;
    static final String MSG_TOPIC2 = "#account-name/#client-id/" + MSG_SEMATIC_TOPIC2;
    static final String MSG_PAYLOAD = "Lorem ipsum dolor sit amet";

    @Test
    public void testConnect() throws KuraConnectException {
        connectDataService();
    }

    @Test
    public void testDisconnect() throws KuraConnectException, InterruptedException {
        connectDataService();

        dataService.disconnect(0);
        assertFalse(dataService.isConnected());

        // TODO: if auto-connect is enabled check it does not
        // automatically reconnects.

        // test onConnectionEstablished
        s_lock.lock();
        try {
            dataService.connect();
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
        dataService.disconnect(0);
        s_disconnecting.await(1, TimeUnit.SECONDS);
        s_disconnected.await(1, TimeUnit.SECONDS);
        s_lock.unlock();
    }

    @Test
    @Ignore // reason: don't know what this does with all the locks
    public void testPublish() throws KuraConnectException, KuraStoreException {
        connectDataService();

        // publish at QoS = 0
        synchronized (s_qos0MsgIds) {
            s_qos0MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos0MsgIds) {
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 0, false, DFLT_MSG_PRIORITY);
                    s_qos0MsgIds.add(id);
                }
            } catch (KuraStoreException e) {
                break;
            }
        }

        // publish at QoS = 1
        synchronized (s_qos12MsgIds) {
            s_qos12MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos12MsgIds) {
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, DFLT_MSG_PRIORITY);
                    s_qos12MsgIds.add(id);
                    logger.info("Added id: {}", id);
                }
            } catch (KuraStoreException e) {
                break;
            }
        }

        boolean allConfirmed = false;
        for (int i = 0; i < ALL_CONFIRMED_QOS1_TIMEOUT; i++) {
            synchronized (s_qos12MsgIds) {
                logger.info("confirm check round {}", i);
                s_qos12MsgIds.forEach(element -> logger.info("To confirm: {}", element));
                if (s_qos12MsgIds.isEmpty()) {
                    allConfirmed = true;
                    break;
                }
            }
        }

        logger.info("All confirmed value: {}", allConfirmed);
        assertTrue(allConfirmed);

        // publish at QoS = 2
        synchronized (s_qos12MsgIds) {
            s_qos12MsgIds.clear();
        }

        for (int i = 0; i < MAX_MSGS; i++) {
            try {
                synchronized (s_qos12MsgIds) {
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 2, false, DFLT_MSG_PRIORITY);
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
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, DFLT_MSG_PRIORITY);
                    s_qos12MsgIds.add(id);
                    logger.info("Added id: {}", id);
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
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, HIGH_MSG_PRIORITY);
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
                    logger.info("confirm check round {}", i);
                    s_qos12HighPriorityMsgIds
                            .forEach(element -> logger.info("To confirm s_qos12HighPriorityMsgIds: {}", element));
                    s_qos12MsgIds.forEach(element -> logger.info("To confirm s_qos12MsgIds: {}", element));
                    if (!s_qos12HighPriorityMsgIds.isEmpty() && s_qos12MsgIds.isEmpty()) {
                        fail("High priority messages should be confirmed before default priority messages");
                    } else if (s_qos12HighPriorityMsgIds.isEmpty() && s_qos12MsgIds.isEmpty()) {
                        allConfirmed = true;
                        break;
                    }
                }
            }
        }

        logger.info("All confirmed value: {}", allConfirmed);
        assertTrue(allConfirmed);
    }

    @Test
    @Ignore // reason: this can never work
    public void testSubscribe() throws KuraException, InterruptedException {
        connectDataService();

        s_lock.lock();
        try {
            dataService.subscribe(MSG_TOPIC2, 0);
            dataService.publish(MSG_TOPIC2, MSG_PAYLOAD.getBytes(), 0, false, HIGH_MSG_PRIORITY);
            boolean arrived = s_arrived.await(5, TimeUnit.SECONDS);
            assertTrue("Message did not arrive to subscriber", arrived);
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
