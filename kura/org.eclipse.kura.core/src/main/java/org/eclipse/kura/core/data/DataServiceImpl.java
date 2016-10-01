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
package org.eclipse.kura.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.KuraTooManyInflightMessagesException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.data.store.DbDataStore;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataServiceImpl
        implements DataService, DataTransportListener, ConfigurableComponent, CloudConnectionStatusComponent {

    private static final Logger s_logger = LoggerFactory.getLogger(DataServiceImpl.class);

    private static final int TRANSPORT_TASK_TIMEOUT = 1; // In seconds

    private static final String AUTOCONNECT_PROP_NAME = "connect.auto-on-startup";
    private static final String CONNECT_DELAY_PROP_NAME = "connect.retry-interval";
    private static final String DISCONNECT_DELAY_PROP_NAME = "disconnect.quiesce-timeout";
    private static final String STORE_HOUSEKEEPER_INTERVAL_PROP_NAME = "store.housekeeper-interval";
    private static final String STORE_PURGE_AGE_PROP_NAME = "store.purge-age";
    private static final String STORE_CAPACITY_PROP_NAME = "store.capacity";
    private static final String REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.republish-on-new-session";
    private static final String MAX_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.max-number";
    private static final String IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME = "in-flight-messages.congestion-timeout";

    private final Map<String, Object> m_properties = new HashMap<String, Object>();

    private DataTransportService m_dataTransportService;
    private DbService m_dbService;
    private DataServiceListenerS m_dataServiceListeners;

    protected ScheduledExecutorService m_reconnectExecutor;
    private ScheduledFuture<?> m_reconnectFuture;

    // A dedicated executor for the publishing task
    private ScheduledExecutorService m_publisherExecutor;

    private DataStore m_store;

    private Map<DataTransportToken, Integer> m_inFlightMsgIds;

    private ScheduledExecutorService m_congestionExecutor;
    private ScheduledFuture<?> m_congestionFuture;

    private CloudConnectionStatusService m_cloudConnectionStatusService;
    private CloudConnectionStatusEnum m_notificationStatus = CloudConnectionStatusEnum.OFF;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        String pid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        s_logger.info("Activating {}...", pid);

        this.m_reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
        this.m_publisherExecutor = Executors.newSingleThreadScheduledExecutor();
        this.m_congestionExecutor = Executors.newSingleThreadScheduledExecutor();

        this.m_properties.putAll(properties);

        String[] parts = pid.split("-");
        String table = "ds_messages";
        if (parts.length > 1) {
            table += "_" + parts[1];
        }
        this.m_store = new DbDataStore(table);

        try {
            this.m_store.start(this.m_dbService, (Integer) this.m_properties.get(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME),
                    (Integer) this.m_properties.get(STORE_PURGE_AGE_PROP_NAME),
                    (Integer) this.m_properties.get(STORE_CAPACITY_PROP_NAME));

            // The initial list of in-flight messages
            List<DataMessage> inFlightMsgs = this.m_store.allInFlightMessagesNoPayload();

            // The map associating a DataTransportToken with a message ID
            this.m_inFlightMsgIds = new ConcurrentHashMap<DataTransportToken, Integer>();

            if (inFlightMsgs != null) {
                for (DataMessage message : inFlightMsgs) {

                    DataTransportToken token = new DataTransportToken(message.getPublishedMessageId(),
                            message.getSessionId());
                    this.m_inFlightMsgIds.put(token, message.getId());

                    s_logger.debug("Restored in-fligh messages from store. Topic: {}, ID: {}, MQTT message ID: {}",
                            new Object[] { message.getTopic(), message.getId(), message.getPublishedMessageId() });
                }
            }
        } catch (KuraStoreException e) {
            s_logger.error("Failed to start store", e);
            throw new ComponentException("Failed to start store", e);
        }

        this.m_dataServiceListeners = new DataServiceListenerS(componentContext);

        // Register the component in the CloudConnectionStatus Service
        this.m_cloudConnectionStatusService.register(this);

        this.m_dataTransportService.addDataTransportListener(this);

        startReconnectTask();
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updating {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        stopReconnectTask();

        this.m_properties.clear();
        this.m_properties.putAll(properties);

        this.m_store.update((Integer) this.m_properties.get(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME),
                (Integer) this.m_properties.get(STORE_PURGE_AGE_PROP_NAME),
                (Integer) this.m_properties.get(STORE_CAPACITY_PROP_NAME));

        if (!this.m_dataTransportService.isConnected()) {
            startReconnectTask();
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivating {}...", this.m_properties.get(ConfigurationService.KURA_SERVICE_PID));

        stopReconnectTask();
        this.m_reconnectExecutor.shutdownNow();

        this.m_congestionExecutor.shutdownNow();

        disconnect();

        // Await termination of the publisher executor tasks
        try {
            this.m_publisherExecutor.awaitTermination(TRANSPORT_TASK_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            s_logger.info("Interrupted", e);
        }
        this.m_publisherExecutor.shutdownNow();

        this.m_dataTransportService.removeDataTransportListener(this);

        this.m_store.stop();
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setDataTransportService(DataTransportService dataTransportService) {
        this.m_dataTransportService = dataTransportService;
    }

    public void unsetDataTransportService(DataTransportService dataTransportService) {
        this.m_dataTransportService = null;
    }

    public void setDbService(DbService dbService) {
        this.m_dbService = dbService;
    }

    public void unsetDbService(DbService dbService) {
        this.m_dbService = null;
    }

    public void setCloudConnectionStatusService(CloudConnectionStatusService cloudConnectionStatusService) {
        this.m_cloudConnectionStatusService = cloudConnectionStatusService;
    }

    public void unsetCloudConnectionStatusService(CloudConnectionStatusService cloudConnectionStatusService) {
        this.m_cloudConnectionStatusService = null;
    }

    @Override
    public void addDataServiceListener(DataServiceListener listener) {
        this.m_dataServiceListeners.add(listener);
    }

    @Override
    public void removeDataServiceListener(DataServiceListener listener) {
        this.m_dataServiceListeners.remove(listener);
    }

    @Override
    public void onConnectionEstablished(boolean newSession) {

        s_logger.info("Notified connected");
        this.m_cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.ON);

        // On a new session all messages the were in-flight in the previous session
        // would be lost and never confirmed by the DataPublisherService.
        //
        // If the DataPublisherService is configured with Clean Start flag set to true,
        // then the session and connection boundaries are the same.
        // Otherwise, a session spans multiple connections as far as the client connects
        // to the same broker instance with the same client ID.
        //
        // We have two options here:
        // Forget them.
        // Unpublish them so they will be republished on the new session.
        //
        // The latter has the potential drawback that duplicates can be generated with any QoS.
        // This can occur for example if the DataPublisherService is connecting with a different client ID
        // or to a different broker URL resolved to the same broker instance.
        //
        // Also note that unpublished messages will be republished accordingly to their
        // original priority. Thus a message reordering may occur too.
        // Even if we artificially upgraded the priority of unpublished messages to -1 so to
        // republish them first, their relative order would not necessarily match the order
        // in the DataPublisherService persistence.

        if (newSession) {
            Boolean unpublishInFlightMsgs = (Boolean) this.m_properties.get(REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME);

            if (unpublishInFlightMsgs) {
                s_logger.info(
                        "New session established. Unpublishing all in-flight messages. Disregarding the QoS level, this may cause duplicate messages.");
                try {
                    this.m_store.unpublishAllInFlighMessages();
                    this.m_inFlightMsgIds.clear();
                } catch (KuraStoreException e) {
                    s_logger.error("Failed to unpublish in-flight messages", e);
                }
            } else {
                s_logger.info("New session established. Dropping all in-flight messages.");
                try {
                    this.m_store.dropAllInFlightMessages();
                    this.m_inFlightMsgIds.clear();
                } catch (KuraStoreException e) {
                    s_logger.error("Failed to drop in-flight messages", e);
                }
            }
        }

        // Notify the listeners
        this.m_dataServiceListeners.onConnectionEstablished();

        // Schedule execution of a publisher task
        submitPublishingWork();
    }

    @Override
    public void onDisconnecting() {
        s_logger.info("Notified disconnecting");

        // Notify the listeners
        this.m_dataServiceListeners.onDisconnecting();

        // Schedule execution of a publisher task waiting until done or timeout.
        Future<?> future = submitPublishingWork();
        try {
            future.get(TRANSPORT_TASK_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            s_logger.info("Interrupted while waiting for the publishing work to complete");
        } catch (ExecutionException e) {
            s_logger.warn("ExecutionException while waiting for the publishing work to complete", e);
        } catch (TimeoutException e) {
            s_logger.warn("Timeout while waiting for the publishing work to complete");
        }
    }

    @Override
    public void onDisconnected() {
        s_logger.info("Notified disconnected");
        this.m_cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.OFF);

        // Notify the listeners
        this.m_dataServiceListeners.onDisconnected();
    }

    @Override
    public void onConfigurationUpdating(boolean wasConnected) {
        s_logger.info("Notified DataTransportService configuration updating...");
        stopReconnectTask();
        disconnect(0);
    }

    @Override
    public void onConfigurationUpdated(boolean wasConnected) {
        s_logger.info("Notified DataTransportService configuration updated.");
        boolean autoConnect = startReconnectTask();
        if (!autoConnect && wasConnected) {
            try {
                connect();
            } catch (KuraConnectException e) {
                s_logger.error("Error during re-connect after configuration update.", e);
            }
        }
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        s_logger.info("connectionLost");

        stopReconnectTask(); // Just in case...
        startReconnectTask();

        // Notify the listeners
        this.m_dataServiceListeners.onConnectionLost(cause);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {

        s_logger.debug("Message arrived on topic: {}", topic);

        // Notify the listeners
        this.m_dataServiceListeners.onMessageArrived(topic, payload, qos, retained);

        submitPublishingWork();
    }

    @Override
    // It's very important that the publishInternal and messageConfirmed methods are synchronized
    public synchronized void onMessageConfirmed(DataTransportToken token) {

        s_logger.debug("Confirmed message with MQTT message ID: {} on session ID: {}", token.getMessageId(),
                token.getSessionId());

        Integer messageId = this.m_inFlightMsgIds.remove(token);
        if (messageId == null) {
            s_logger.info(
                    "Confirmed message published with MQTT message ID: {} not tracked in the map of in-flight messages",
                    token.getMessageId());
        } else {

            DataMessage confirmedMessage = null;
            try {
                s_logger.info("Confirmed message ID: {} to store", messageId);
                this.m_store.confirmed(messageId);
                confirmedMessage = this.m_store.get(messageId);
            } catch (KuraStoreException e) {
                s_logger.error("Cannot confirm message to store", e);
            }

            // Notify the listeners
            if (confirmedMessage != null) {
                String topic = confirmedMessage.getTopic();
                this.m_dataServiceListeners.onMessageConfirmed(messageId, topic);
            } else {
                s_logger.error("Confirmed Message with ID {} could not be loaded from the DataStore.", messageId);
            }
        }

        if (this.m_inFlightMsgIds.size() < (Integer) this.m_properties.get(MAX_IN_FLIGHT_MSGS_PROP_NAME)) {
            handleInFlightDecongestion();
        }

        submitPublishingWork();
    }

    @Override
    public void connect() throws KuraConnectException {
        stopReconnectTask();
        if (!this.m_dataTransportService.isConnected()) {
            this.m_dataTransportService.connect();
        }
    }

    @Override
    public boolean isConnected() {
        return this.m_dataTransportService.isConnected();
    }

    @Override
    public boolean isAutoConnectEnabled() {
        return (Boolean) this.m_properties.get(AUTOCONNECT_PROP_NAME);
    }

    @Override
    public int getRetryInterval() {
        return (Integer) this.m_properties.get(CONNECT_DELAY_PROP_NAME);
    }

    @Override
    public void disconnect(long quiesceTimeout) {
        stopReconnectTask();
        this.m_dataTransportService.disconnect(quiesceTimeout);
    }

    @Override
    public void subscribe(String topic, int qos) throws KuraTimeoutException, KuraException, KuraNotConnectedException {
        this.m_dataTransportService.subscribe(topic, qos);
    }

    @Override
    public void unsubscribe(String topic) throws KuraTimeoutException, KuraException, KuraNotConnectedException {
        this.m_dataTransportService.unsubscribe(topic);
    }

    @Override
    public int publish(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraStoreException {

        s_logger.info("Storing message on topic :{}, priority: {}", topic, priority);

        DataMessage dataMsg = this.m_store.store(topic, payload, qos, retain, priority);
        s_logger.info("Stored message on topic :{}, priority: {}", topic, priority);

        submitPublishingWork();

        return dataMsg.getId();
    }

    @Override
    public List<Integer> getUnpublishedMessageIds(String topicRegex) throws KuraStoreException {
        List<DataMessage> messages = this.m_store.allUnpublishedMessagesNoPayload();
        return buildMessageIds(messages, topicRegex);
    }

    @Override
    public List<Integer> getInFlightMessageIds(String topicRegex) throws KuraStoreException {
        List<DataMessage> messages = this.m_store.allInFlightMessagesNoPayload();
        return buildMessageIds(messages, topicRegex);
    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds(String topicRegex) throws KuraStoreException {
        List<DataMessage> messages = this.m_store.allDroppedInFlightMessagesNoPayload();
        return buildMessageIds(messages, topicRegex);
    }

    private boolean startReconnectTask() {
        if (this.m_reconnectFuture != null && !this.m_reconnectFuture.isDone()) {
            s_logger.error("Reconnect task already running");
            throw new IllegalStateException("Reconnect task already running");
        }

        //
        // Establish a reconnect Thread based on the reconnect interval
        boolean autoConnect = (Boolean) this.m_properties.get(AUTOCONNECT_PROP_NAME);
        int reconnectInterval = (Integer) this.m_properties.get(CONNECT_DELAY_PROP_NAME);
        if (autoConnect) {

            // Change notification status to slow blinking when connection is expected to happen in the future
            this.m_cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.SLOW_BLINKING);
            // add a delay on the reconnect
            int maxDelay = reconnectInterval / 5;
            maxDelay = maxDelay > 0 ? maxDelay : 1;
            int initialDelay = new Random().nextInt(maxDelay);

            s_logger.info("Starting reconnect task with initial delay {}", initialDelay);
            this.m_reconnectFuture = this.m_reconnectExecutor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    String originalName = Thread.currentThread().getName();
                    Thread.currentThread().setName("DataServiceImpl:ReconnectTask");
                    boolean connected = false;
                    try {
                        s_logger.info("Connecting...");
                        if (DataServiceImpl.this.m_dataTransportService.isConnected()) {
                            s_logger.info("Already connected. Reconnect task will be terminated.");
                        } else {
                            DataServiceImpl.this.m_dataTransportService.connect();
                            s_logger.info("Connected. Reconnect task will be terminated.");
                        }
                        connected = true;
                    } catch (Exception e) {
                        s_logger.warn("Connect failed", e.getCause().getMessage());
                    } catch (Error e) {
                        // There's nothing we can do here but log an exception.
                        s_logger.error("Unexpected Error. Task will be terminated", e);
                        throw e;
                    } finally {
                        Thread.currentThread().setName(originalName);
                        if (connected) {
                            // Throwing an exception will suppress subsequent executions of this periodic task.
                            throw new RuntimeException("Connected. Reconnect task will be terminated.");
                        }
                    }
                }
            }, initialDelay,   		// initial delay
                    reconnectInterval,   // repeat every reconnect interval until we stopped.
                    TimeUnit.SECONDS);
        } else {
            // Change notification status to off. Connection is not expected to happen in the future
            this.m_cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.OFF);
        }
        return autoConnect;
    }

    private void stopReconnectTask() {
        if (this.m_reconnectFuture != null && !this.m_reconnectFuture.isDone()) {

            s_logger.info("Reconnect task running. Stopping it");

            this.m_reconnectFuture.cancel(true);
        }
    }

    private void disconnect() {
        long millis = (Integer) this.m_properties.get(DISCONNECT_DELAY_PROP_NAME) * 1000L;
        this.m_dataTransportService.disconnect(millis);
    }

    // Submit a new publishing work if any
    // TODO: only one instance of the Runnable is needed
    private Future<?> submitPublishingWork() {
        return this.m_publisherExecutor.submit(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName("DataServiceImpl:Submit");
                if (!DataServiceImpl.this.m_dataTransportService.isConnected()) {
                    s_logger.info("DataPublisherService not connected");
                    return;
                }
                try {

                    // Compared with getting all unpublished messages, getting one message at a time
                    // is a little bit inefficient (we query the underlying DB every time)
                    // but improves responsiveness to high priority message.
                    // TODO: add a getUnpublishedMessages with a limit argument?
                    // getNextMessage is a special case with limit = 1.
                    DataMessage message = null;
                    while ((message = DataServiceImpl.this.m_store.getNextMessage()) != null) {

                        // Further limit the maximum number of in-flight messages
                        if (message.getQos() > 0) {
                            if (DataServiceImpl.this.m_inFlightMsgIds
                                    .size() >= (Integer) DataServiceImpl.this.m_properties
                                            .get(MAX_IN_FLIGHT_MSGS_PROP_NAME)) {
                                s_logger.warn("The configured maximum number of in-flight messages has been reached");
                                handleInFlightCongestion();
                                break;
                            }
                        }

                        publishInternal(message);

                        // TODO: add a 'message throttle' configuration parameter to
                        // slow down publish rate?

                        // Notify the listeners
                        DataServiceImpl.this.m_dataServiceListeners.onMessagePublished(message.getId(),
                                message.getTopic());
                    }
                } catch (KuraConnectException e) {
                    s_logger.info("DataPublisherService is not connected", e);
                } catch (KuraTooManyInflightMessagesException e) {
                    s_logger.info("Too many in-flight messages", e);
                    handleInFlightCongestion();
                } catch (Exception e) {
                    s_logger.error("Probably an unrecoverable exception", e);
                }
            }
        });
    }

    // It's very important that the publishInternal and messageConfirmed methods are synchronized
    private synchronized void publishInternal(DataMessage message)
            throws KuraConnectException, KuraTooManyInflightMessagesException, KuraStoreException, KuraException {

        String topic = message.getTopic();
        byte[] payload = message.getPayload();
        int qos = message.getQos();
        boolean retain = message.isRetain();
        int msgId = message.getId();

        s_logger.debug("Publishing message with ID: {} on topic: {}, priority: {}",
                new Object[] { msgId, topic, message.getPriority() });

        DataTransportToken token = this.m_dataTransportService.publish(topic, payload, qos, retain);

        if (token == null) {
            this.m_store.published(msgId);
            s_logger.debug("Published message with ID: {}", msgId);
        } else {

            // Check if the token is already tracked in the map (in which case we are in trouble)
            Integer trackedMsgId = this.m_inFlightMsgIds.get(token);
            if (trackedMsgId != null) {
                s_logger.error("Token already tracked: " + token.getSessionId() + "-" + token.getMessageId());
            }

            this.m_inFlightMsgIds.put(token, msgId);
            this.m_store.published(msgId, token.getMessageId(), token.getSessionId());
            s_logger.debug("Published message with ID: {} and MQTT message ID: {}", msgId, token.getMessageId());
        }
    }

    private List<Integer> buildMessageIds(List<DataMessage> messages, String topicRegex) {
        Pattern topicPattern = Pattern.compile(topicRegex);
        List<Integer> ids = new ArrayList<Integer>();

        if (messages != null) {
            for (DataMessage message : messages) {
                String topic = message.getTopic();
                if (topicPattern.matcher(topic).matches()) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    private void handleInFlightCongestion() {
        int timeout = (Integer) this.m_properties.get(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME);

        // Do not schedule more that one task at a time
        if (timeout != 0 && (this.m_congestionFuture == null || this.m_congestionFuture.isDone())) {
            s_logger.warn("In-flight message congestion timeout started");
            this.m_congestionFuture = this.m_congestionExecutor.schedule(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("DataServiceImpl:InFlightCongestion");
                    s_logger.warn("In-flight message congestion timeout elapsed. Disconnecting and reconnecting again");
                    disconnect();
                    startReconnectTask();
                }
            }, timeout, TimeUnit.SECONDS);
        }
    }

    private void handleInFlightDecongestion() {
        if (this.m_congestionFuture != null && !this.m_congestionFuture.isDone()) {
            this.m_congestionFuture.cancel(true);
        }
    }

    @Override
    public int getNotificationPriority() {
        return CloudConnectionStatusService.PRIORITY_LOW;
    }

    @Override
    public CloudConnectionStatusEnum getNotificationStatus() {
        return this.m_notificationStatus;
    }

    @Override
    public void setNotificationStatus(CloudConnectionStatusEnum status) {
        this.m_notificationStatus = status;
    }
}
