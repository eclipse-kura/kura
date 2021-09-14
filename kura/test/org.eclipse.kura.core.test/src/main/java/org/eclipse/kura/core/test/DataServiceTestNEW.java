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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.test;

import static org.eclipse.kura.core.data.DataServiceOptions.AUTOCONNECT_PROP_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.AUTOCONNECT_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.CONNECT_DELAY_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.CONNECT_DELAY_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.DISCONNECT_DELAY_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.DISCONNECT_DELAY_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.MAX_IN_FLIGHT_MSGS_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.MAX_IN_FLIGHT_MSGS_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.REPUBLISH_IN_FLIGHT_MSGS_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.STORE_CAPACITY_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.STORE_HOUSEKEEPER_INTERVAL_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.STORE_HOUSEKEEPER_INTERVAL_PROP_NAME;
import static org.eclipse.kura.core.data.DataServiceOptions.STORE_PURGE_AGE_DEFAULT;
import static org.eclipse.kura.core.data.DataServiceOptions.STORE_PURGE_AGE_PROP_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.data.DataServiceImpl;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.dictionary.Dictionaries;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled
//not only this bunde may stay on external core test
@SuppressWarnings("deprecation")
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class DataServiceTestNEW implements DataServiceListener {

    private static final Logger s_logger = LoggerFactory.getLogger(DataServiceTestNEW.class);

    @InjectBundleContext
    BundleContext bc;
    // dependencies
    @InjectService(cardinality = 0)
    ServiceAware<DataService> s_dataService;
    @InjectService(timeout = 500)
    ConfigurationAdmin cm;

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

    // @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testConnect() throws KuraConnectException, IOException, InterruptedException {
        Configuration c = configureDataService(cm);

        DataService dataService = s_dataService.waitForService(1000);
        if (!dataService.isConnected()) {
            dataService.connect();
        }
        c.delete();
    }

    // @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testDisconnect() throws KuraConnectException, InterruptedException, IOException {
        Configuration c = configureDataService(cm);

        DataService dataService = s_dataService.waitForService(1000);

        if (!dataService.isConnected()) {
            dataService.connect();
        }

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

        c.delete();
    }

    // @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testPublish() throws KuraConnectException, InterruptedException, IOException {

        Configuration c = configureDataService(cm);

        DataService dataService = s_dataService.waitForService(1000);

        if (!dataService.isConnected()) {
            dataService.connect();
        }

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
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, DFLT_MSG_PRIORITY);
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
                    Integer id = dataService.publish(MSG_TOPIC1, MSG_PAYLOAD.getBytes(), 1, false, DFLT_MSG_PRIORITY);
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
                    s_logger.info("confirm check round {}", i);
                    s_qos12HighPriorityMsgIds
                            .forEach(element -> s_logger.info("To confirm s_qos12HighPriorityMsgIds: {}", element));
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
        c.delete();
    }

    // @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSubscribe() throws KuraException, InterruptedException, IOException {

        Configuration c = configureDataService(cm);

        DataService dataService = s_dataService.waitForService(1000);

        if (!dataService.isConnected()) {
            dataService.connect();
        }

        s_lock.lock();
        try {
            dataService.subscribe(MSG_TOPIC2, 0);
            dataService.publish(MSG_TOPIC2, MSG_PAYLOAD.getBytes(), 0, false, HIGH_MSG_PRIORITY);
            boolean arrived = s_arrived.await(5, TimeUnit.SECONDS);
            assertTrue(arrived);
        } catch (KuraException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            s_lock.unlock();
        }
        c.delete();
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

    public static Configuration configureDataService(ConfigurationAdmin cm) throws IOException {
        Configuration c = cm.createFactoryConfiguration(DataServiceImpl.PID,  "?");
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME, IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_DEFAULT);
        props.put(STORE_PURGE_AGE_PROP_NAME, STORE_PURGE_AGE_DEFAULT);
        props.put(REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME, REPUBLISH_IN_FLIGHT_MSGS_DEFAULT);
        props.put(STORE_CAPACITY_PROP_NAME, 1000);// TODO: maybe use
                                                                     // DataServiceOptions.STORE_CAPACITY_DEFAULT)
        props.put(DISCONNECT_DELAY_PROP_NAME, DISCONNECT_DELAY_DEFAULT);
        props.put(AUTOCONNECT_PROP_NAME, AUTOCONNECT_PROP_DEFAULT);
        props.put(CONNECT_DELAY_PROP_NAME, CONNECT_DELAY_DEFAULT);
        props.put(MAX_IN_FLIGHT_MSGS_PROP_NAME, MAX_IN_FLIGHT_MSGS_DEFAULT);
        props.put(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME, STORE_HOUSEKEEPER_INTERVAL_DEFAULT);
        c.update(Dictionaries.dictionaryOf());
        return c;
    }
}
