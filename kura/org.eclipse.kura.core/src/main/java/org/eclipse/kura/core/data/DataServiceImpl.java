/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.KuraTooManyInflightMessagesException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.data.store.DbDataStore;
import org.eclipse.kura.core.internal.data.TokenBucket;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataServiceImpl implements DataService, DataTransportListener, ConfigurableComponent,
        CloudConnectionStatusComponent, CriticalComponent {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);

    private static final int TRANSPORT_TASK_TIMEOUT = 1; // In seconds

    private DataServiceOptions dataServiceOptions;

    private DataTransportService dataTransportService;
    private H2DbService dbService;
    private DataServiceListenerS dataServiceListeners;

    protected ScheduledExecutorService connectionMonitorExecutor;
    private ScheduledFuture<?> connectionMonitorFuture;

    // A dedicated executor for the publishing task
    private ExecutorService publisherExecutor;

    private DataStore store;

    private Map<DataTransportToken, Integer> inFlightMsgIds;

    private ScheduledExecutorService congestionExecutor;
    private ScheduledFuture<?> congestionFuture;

    private CloudConnectionStatusService cloudConnectionStatusService;
    private CloudConnectionStatusEnum notificationStatus = CloudConnectionStatusEnum.OFF;

    private TokenBucket throttle;

    private final Lock lock = new ReentrantLock();
    private boolean notifyPending;
    private final Condition lockCondition = this.lock.newCondition();

    private final AtomicBoolean publisherEnabled = new AtomicBoolean();

    private ServiceTracker<H2DbService, H2DbService> dbServiceTracker;
    private ComponentContext componentContext;

    private WatchdogService watchdogService;

    private AtomicInteger connectionAttempts;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        String pid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        logger.info("Activating {}...", pid);

        this.componentContext = componentContext;

        this.dataServiceOptions = new DataServiceOptions(properties);

        this.connectionMonitorExecutor = Executors.newSingleThreadScheduledExecutor();
        this.publisherExecutor = Executors.newSingleThreadExecutor();
        this.congestionExecutor = Executors.newSingleThreadScheduledExecutor();

        createThrottle();
        submitPublishingWork();

        this.store = new DbDataStore(pid);

        restartDbServiceTracker(this.dataServiceOptions.getDbServiceInstancePid());

        this.dataServiceListeners = new DataServiceListenerS(componentContext);

        // Register the component in the CloudConnectionStatus Service
        this.cloudConnectionStatusService.register(this);

        this.dataTransportService.addDataTransportListener(this);

        startConnectionMonitorTask();
    }

    private void restartDbServiceTracker(String kuraServicePid) {
        stopDbServiceTracker();
        try {
            final Filter filter = FrameworkUtil
                    .createFilter("(" + ConfigurationService.KURA_SERVICE_PID + "=" + kuraServicePid + ")");
            this.dbServiceTracker = new ServiceTracker<>(this.componentContext.getBundleContext(), filter,
                    new ServiceTrackerCustomizer<H2DbService, H2DbService>() {

                        @Override
                        public H2DbService addingService(ServiceReference<H2DbService> reference) {
                            logger.info("H2DbService instance found");
                            H2DbService contextDbService = DataServiceImpl.this.componentContext.getBundleContext()
                                    .getService(reference);
                            setH2DbService(contextDbService);
                            return contextDbService;
                        }

                        @Override
                        public void modifiedService(ServiceReference<H2DbService> reference, H2DbService service) {
                            logger.info("H2DbService instance updated, recreating table if needed...");
                            synchronized (DataServiceImpl.this) {
                                DataServiceImpl.this.store.update(
                                        DataServiceImpl.this.dataServiceOptions.getStoreHousekeeperInterval(),
                                        DataServiceImpl.this.dataServiceOptions.getStorePurgeAge(),
                                        DataServiceImpl.this.dataServiceOptions.getStoreCapacity());
                            }
                        }

                        @Override
                        public void removedService(ServiceReference<H2DbService> reference, H2DbService service) {
                            logger.info("H2DbService instance removed");
                            unsetH2DbService(DataServiceImpl.this.dbService);
                            DataServiceImpl.this.componentContext.getBundleContext().ungetService(reference);
                        }
                    });
            this.dbServiceTracker.open();
        } catch (InvalidSyntaxException e) {
            throw new ComponentException(e);
        }
    }

    private void stopDbServiceTracker() {
        if (this.dbServiceTracker != null) {
            this.dbServiceTracker.close();
            this.dbServiceTracker = null;
        }
    }

    private synchronized void startDbStore() {
        try {
            this.store.start(this.dbService, this.dataServiceOptions.getStoreHousekeeperInterval(),
                    this.dataServiceOptions.getStorePurgeAge(), this.dataServiceOptions.getStoreCapacity());

            // The initial list of in-flight messages
            List<DataMessage> inFlightMsgs = this.store.allInFlightMessagesNoPayload();

            // The map associating a DataTransportToken with a message ID
            this.inFlightMsgIds = new ConcurrentHashMap<>();

            if (inFlightMsgs != null) {
                for (DataMessage message : inFlightMsgs) {

                    DataTransportToken token = new DataTransportToken(message.getPublishedMessageId(),
                            message.getSessionId());
                    this.inFlightMsgIds.put(token, message.getId());

                    logger.debug("Restored in-fligh messages from store. Topic: {}, ID: {}, MQTT message ID: {}",
                            new Object[] { message.getTopic(), message.getId(), message.getPublishedMessageId() });
                }
            }
        } catch (KuraStoreException e) {
            logger.error("Failed to start store", e);
        }
    }

    public synchronized void updated(Map<String, Object> properties) {
        logger.info("Updating {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        stopConnectionMonitorTask();

        final String oldDbServicePid = this.dataServiceOptions.getDbServiceInstancePid();

        this.dataServiceOptions = new DataServiceOptions(properties);

        createThrottle();

        final String currentDbServicePid = this.dataServiceOptions.getDbServiceInstancePid();

        if (oldDbServicePid.equals(currentDbServicePid)) {
            if (this.dbService != null) {
                this.store.update(this.dataServiceOptions.getStoreHousekeeperInterval(),
                        this.dataServiceOptions.getStorePurgeAge(), this.dataServiceOptions.getStoreCapacity());
            }
        } else {
            restartDbServiceTracker(currentDbServicePid);
        }

        if (!this.dataTransportService.isConnected()) {
            startConnectionMonitorTask();
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating {}...", this.dataServiceOptions.getKuraServicePid());

        stopConnectionMonitorTask();
        this.connectionMonitorExecutor.shutdownNow();

        this.congestionExecutor.shutdownNow();

        disconnect();

        // Await termination of the publisher executor tasks
        try {
            // Waits to publish latest messages e.g. disconnect message
            Thread.sleep(TRANSPORT_TASK_TIMEOUT * 1000L);

            // Clean publisher thread shutdown
            this.publisherEnabled.set(false);
            signalPublisher();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Interrupted", e);
        }
        this.publisherExecutor.shutdownNow();

        this.dataTransportService.removeDataTransportListener(this);

        this.store.stop();

        stopDbServiceTracker();
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setDataTransportService(DataTransportService dataTransportService) {
        this.dataTransportService = dataTransportService;
    }

    public void unsetDataTransportService(DataTransportService dataTransportService) {
        this.dataTransportService = null;
    }

    public synchronized void setH2DbService(H2DbService dbService) {
        this.dbService = dbService;
        startDbStore();
        signalPublisher();
    }

    public synchronized void unsetH2DbService(H2DbService dbService) {
        this.dbService = null;
        disconnect();
        this.store.stop();
    }

    public void setCloudConnectionStatusService(CloudConnectionStatusService cloudConnectionStatusService) {
        this.cloudConnectionStatusService = cloudConnectionStatusService;
    }

    public void unsetCloudConnectionStatusService(CloudConnectionStatusService cloudConnectionStatusService) {
        this.cloudConnectionStatusService = null;
    }

    public void setWatchdogService(WatchdogService watchdogService) {
        this.watchdogService = watchdogService;
    }

    public void unsetWatchdogService(WatchdogService watchdogService) {
        this.watchdogService = null;
    }

    @Override
    public void addDataServiceListener(DataServiceListener listener) {
        this.dataServiceListeners.add(listener);
    }

    @Override
    public void removeDataServiceListener(DataServiceListener listener) {
        this.dataServiceListeners.remove(listener);
    }

    @Override
    public void onConnectionEstablished(boolean newSession) {

        logger.info("Notified connected");
        this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.ON);

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
            if (this.dataServiceOptions.isPublishInFlightMessages()) {
                logger.info("New session established. Unpublishing all in-flight messages. Disregarding the QoS level, "
                        + "this may cause duplicate messages.");
                try {
                    this.store.unpublishAllInFlighMessages();
                    this.inFlightMsgIds.clear();
                } catch (KuraStoreException e) {
                    logger.error("Failed to unpublish in-flight messages", e);
                }
            } else {
                logger.info("New session established. Dropping all in-flight messages.");
                try {
                    this.store.dropAllInFlightMessages();
                    this.inFlightMsgIds.clear();
                } catch (KuraStoreException e) {
                    logger.error("Failed to drop in-flight messages", e);
                }
            }
        }

        // Notify the listeners
        this.dataServiceListeners.onConnectionEstablished();

        signalPublisher();
    }

    @Override
    public void onDisconnecting() {
        logger.info("Notified disconnecting");

        // Notify the listeners
        this.dataServiceListeners.onDisconnecting();
    }

    @Override
    public void onDisconnected() {
        logger.info("Notified disconnected");
        this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.OFF);

        // Notify the listeners
        this.dataServiceListeners.onDisconnected();
    }

    @Override
    public void onConfigurationUpdating(boolean wasConnected) {
        logger.info("Notified DataTransportService configuration updating...");
        stopConnectionMonitorTask();
        disconnect(0);
    }

    @Override
    public void onConfigurationUpdated(boolean wasConnected) {
        logger.info("Notified DataTransportService configuration updated.");
        boolean autoConnect = startConnectionMonitorTask();
        if (!autoConnect && wasConnected) {
            try {
                connect();
            } catch (KuraConnectException e) {
                logger.error("Error during re-connect after configuration update.", e);
            }
        }
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        logger.info("connectionLost");

        stopConnectionMonitorTask(); // Just in case...
        startConnectionMonitorTask();

        // Notify the listeners
        this.dataServiceListeners.onConnectionLost(cause);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {

        logger.debug("Message arrived on topic: {}", topic);

        // Notify the listeners
        this.dataServiceListeners.onMessageArrived(topic, payload, qos, retained);

        signalPublisher();
    }

    @Override
    // It's very important that the publishInternal and messageConfirmed methods are synchronized
    public synchronized void onMessageConfirmed(DataTransportToken token) {

        logger.debug("Confirmed message with MQTT message ID: {} on session ID: {}", token.getMessageId(),
                token.getSessionId());

        Integer messageId = this.inFlightMsgIds.remove(token);
        if (messageId == null) {
            logger.info(
                    "Confirmed message published with MQTT message ID: {} not tracked in the map of in-flight messages",
                    token.getMessageId());
        } else {

            DataMessage confirmedMessage = null;
            try {
                logger.info("Confirmed message ID: {} to store", messageId);
                this.store.confirmed(messageId);
                confirmedMessage = this.store.get(messageId);
            } catch (KuraStoreException e) {
                logger.error("Cannot confirm message to store", e);
            }

            // Notify the listeners
            if (confirmedMessage != null) {
                String topic = confirmedMessage.getTopic();
                this.dataServiceListeners.onMessageConfirmed(messageId, topic);
            } else {
                logger.error("Confirmed Message with ID {} could not be loaded from the DataStore.", messageId);
            }
        }

        if (this.inFlightMsgIds.size() < this.dataServiceOptions.getMaxInFlightMessages()) {
            handleInFlightDecongestion();
        }

        signalPublisher();
    }

    @Override
    public void connect() throws KuraConnectException {
        stopConnectionMonitorTask();
        if (this.dbService == null) {
            throw new KuraConnectException("H2DbService instance not attached, not connecting");
        }

        if (!this.dataTransportService.isConnected()) {
            this.dataTransportService.connect();
        }
    }

    @Override
    public boolean isConnected() {
        return this.dataTransportService.isConnected();
    }

    @Override
    public boolean isAutoConnectEnabled() {
        return this.dataServiceOptions.isAutoConnect();
    }

    @Override
    public int getRetryInterval() {
        return this.dataServiceOptions.getConnectDelay();
    }

    @Override
    public void disconnect(long quiesceTimeout) {
        stopConnectionMonitorTask();
        this.dataTransportService.disconnect(quiesceTimeout);
    }

    @Override
    public void subscribe(String topic, int qos) throws KuraException {
        this.dataTransportService.subscribe(topic, qos);
    }

    @Override
    public void unsubscribe(String topic) throws KuraException {
        this.dataTransportService.unsubscribe(topic);
    }

    @Override
    public int publish(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraStoreException {

        logger.info("Storing message on topic: {}, priority: {}", topic, priority);
        
        DataMessage dataMsg = this.store.store(topic, payload, qos, retain, priority);
        logger.info("Stored message on topic: {}, priority: {}", topic, priority);

        signalPublisher();

        return dataMsg.getId();
    }

    @Override
    public List<Integer> getUnpublishedMessageIds(String topicRegex) throws KuraStoreException {
        List<DataMessage> messages = this.store.allUnpublishedMessagesNoPayload();
        return buildMessageIds(messages, topicRegex);
    }

    @Override
    public List<Integer> getInFlightMessageIds(String topicRegex) throws KuraStoreException {
        List<DataMessage> messages = this.store.allInFlightMessagesNoPayload();
        return buildMessageIds(messages, topicRegex);
    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds(String topicRegex) throws KuraStoreException {
        List<DataMessage> messages = this.store.allDroppedInFlightMessagesNoPayload();
        return buildMessageIds(messages, topicRegex);
    }

    private void signalPublisher() {
        this.lock.lock();
        this.notifyPending = true;
        this.lockCondition.signal();
        this.lock.unlock();
    }

    private boolean startConnectionMonitorTask() {
        if (this.connectionMonitorFuture != null && !this.connectionMonitorFuture.isDone()) {
            logger.error("Reconnect task already running");
            throw new IllegalStateException("Reconnect task already running");
        }

        //
        // Establish a reconnect Thread based on the reconnect interval
        boolean autoConnect = this.dataServiceOptions.isAutoConnect();
        int reconnectInterval = this.dataServiceOptions.getConnectDelay();
        if (autoConnect) {

            if (this.dataServiceOptions.isConnectionRecoveryEnabled()) {
                this.watchdogService.registerCriticalComponent(this);
                this.watchdogService.checkin(this);
                this.connectionAttempts = new AtomicInteger(0);
            }

            // Change notification status to slow blinking when connection is expected to happen in the future
            this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.SLOW_BLINKING);
            // add a delay on the reconnect
            int maxDelay = reconnectInterval / 5;
            maxDelay = maxDelay > 0 ? maxDelay : 1;
            int initialDelay = new Random().nextInt(maxDelay);

            logger.info("Starting reconnect task with initial delay {}", initialDelay);
            this.connectionMonitorFuture = this.connectionMonitorExecutor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("DataServiceImpl:ReconnectTask:"
                            + DataServiceImpl.this.dataServiceOptions.getKuraServicePid());
                    boolean connected = false;
                    try {
                        if (DataServiceImpl.this.dbService == null) {
                            logger.warn("H2DbService instance not attached, not connecting");
                            return;
                        }
                        logger.info("Connecting...");
                        if (DataServiceImpl.this.dataTransportService.isConnected()) {
                            logger.info("Already connected. Reconnect task will be terminated.");

                        } else {
                            DataServiceImpl.this.dataTransportService.connect();
                            logger.info("Connected. Reconnect task will be terminated.");
                        }
                        connected = true;
                    } catch (KuraConnectException e) {
                        logger.warn("Connect failed", e);

                        if (DataServiceImpl.this.dataServiceOptions.isConnectionRecoveryEnabled()) {
                            if (isAuthenticationException(e) || DataServiceImpl.this.connectionAttempts
                                    .getAndIncrement() < DataServiceImpl.this.dataServiceOptions
                                            .getRecoveryMaximumAllowedFailures()) {
                                logger.info("Checkin done.");
                                DataServiceImpl.this.watchdogService.checkin(DataServiceImpl.this);
                            } else {
                                logger.info("Maximum number of connection attempts reached. Requested reboot...");
                            }
                        }
                    } finally {
                        if (connected) {
                            unregisterAsCriticalComponent();
                            // Throwing an exception will suppress subsequent executions of this periodic task.
                            throw new RuntimeException("Connected. Reconnect task will be terminated.");
                        }
                    }
                }

                private boolean isAuthenticationException(KuraConnectException e) {
                    boolean authenticationException = false;
                    if (e.getCause() instanceof MqttException) {
                        MqttException mqttException = (MqttException) e.getCause();
                        if (mqttException.getReasonCode() == MqttException.REASON_CODE_FAILED_AUTHENTICATION
                                || mqttException.getReasonCode() == MqttException.REASON_CODE_INVALID_CLIENT_ID
                                || mqttException.getReasonCode() == MqttException.REASON_CODE_NOT_AUTHORIZED) {
                            logger.info("Authentication exception encountered.");
                            authenticationException = true;
                        }
                    }
                    return authenticationException;
                }
            }, initialDelay, reconnectInterval, TimeUnit.SECONDS);
        } else {
            // Change notification status to off. Connection is not expected to happen in the future
            this.cloudConnectionStatusService.updateStatus(this, CloudConnectionStatusEnum.OFF);
            unregisterAsCriticalComponent();
        }
        return autoConnect;
    }

    private void createThrottle() {
        if (this.dataServiceOptions.isRateLimitEnabled()) {
            int publishRate = this.dataServiceOptions.getRateLimitAverageRate();
            int burstLength = this.dataServiceOptions.getRateLimitBurstSize();

            long publishPeriod = this.dataServiceOptions.getRateLimitTimeUnit() / publishRate;

            logger.info("Get Throttle with burst length {} and send a message every {} nanoseconds", burstLength,
                    publishPeriod);
            this.throttle = new TokenBucket(burstLength, publishPeriod);
        }
    }

    private void stopConnectionMonitorTask() {
        if (this.connectionMonitorFuture != null && !this.connectionMonitorFuture.isDone()) {

            logger.info("Reconnect task running. Stopping it");

            this.connectionMonitorFuture.cancel(true);
        }
        unregisterAsCriticalComponent();
    }

    private void unregisterAsCriticalComponent() {
        this.watchdogService.unregisterCriticalComponent(this);
    }

    private void disconnect() {
        long millis = this.dataServiceOptions.getDisconnectDelay() * 1000L;
        this.dataTransportService.disconnect(millis);
    }

    private void submitPublishingWork() {
        this.publisherEnabled.set(true);

        this.publisherExecutor.execute(new PublishManager());
    }

    // It's very important that the publishInternal and messageConfirmed methods are synchronized
    private synchronized void publishInternal(DataMessage message) throws KuraException {

        String topic = message.getTopic();
        byte[] payload = message.getPayload();
        int qos = message.getQos();
        boolean retain = message.isRetain();
        int msgId = message.getId();

        logger.debug("Publishing message with ID: {} on topic: {}, priority: {}", msgId, topic, message.getPriority());

        DataTransportToken token = DataServiceImpl.this.dataTransportService.publish(topic, payload, qos, retain);

        if (token == null) {
            DataServiceImpl.this.store.published(msgId);
            logger.debug("Published message with ID: {}", msgId);
        } else {

            // Check if the token is already tracked in the map (in which case we are in trouble)
            Integer trackedMsgId = DataServiceImpl.this.inFlightMsgIds.get(token);
            if (trackedMsgId != null) {
                logger.error("Token already tracked: {} - {}", token.getSessionId(), token.getMessageId());
            }

            DataServiceImpl.this.inFlightMsgIds.put(token, msgId);
            DataServiceImpl.this.store.published(msgId, token.getMessageId(), token.getSessionId());
            logger.debug("Published message with ID: {} and MQTT message ID: {}", msgId, token.getMessageId());
        }
    }

    private List<Integer> buildMessageIds(List<DataMessage> messages, String topicRegex) {
        Pattern topicPattern = Pattern.compile(topicRegex);
        List<Integer> ids = new ArrayList<>();

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

    private void handleInFlightDecongestion() {
        if (this.congestionFuture != null && !this.congestionFuture.isDone()) {
            this.congestionFuture.cancel(true);
        }
    }

    @Override
    public int getNotificationPriority() {
        return CloudConnectionStatusService.PRIORITY_LOW;
    }

    @Override
    public CloudConnectionStatusEnum getNotificationStatus() {
        return this.notificationStatus;
    }

    @Override
    public void setNotificationStatus(CloudConnectionStatusEnum status) {
        this.notificationStatus = status;
    }

    private final class PublishManager implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("DataServiceImpl:Submit");
            while (DataServiceImpl.this.publisherEnabled.get()) {
                long sleepingTime = -1;
                boolean messagePublished = false;

                if (DataServiceImpl.this.dataTransportService.isConnected()) {
                    try {
                        DataMessage message = DataServiceImpl.this.store.getNextMessage();

                        if (message != null) {
                            checkInFlightMessages(message);

                            if (DataServiceImpl.this.dataServiceOptions.isRateLimitEnabled()
                                    && message.getPriority() >= 5) {
                                messagePublished = publishMessageTokenBucket(message);
                                sleepingTime = DataServiceImpl.this.throttle.getTokenWaitTime();
                            } else {
                                publishMessageUnbound(message);
                                messagePublished = true;
                            }
                        }
                    } catch (KuraNotConnectedException e) {
                        logger.info("DataPublisherService is not connected");
                    } catch (KuraTooManyInflightMessagesException e) {
                        logger.info("Too many in-flight messages");
                        handleInFlightCongestion();
                    } catch (Exception e) {
                        logger.error("Probably an unrecoverable exception", e);
                    }
                } else {
                    logger.info("DataPublisherService not connected");
                }

                if (!messagePublished) {
                    suspendPublisher(sleepingTime, TimeUnit.NANOSECONDS);
                }
            }
            logger.debug("Exited publisher loop.");
        }

        private void checkInFlightMessages(DataMessage message) throws KuraTooManyInflightMessagesException {
            if (message.getQos() > 0 && DataServiceImpl.this.inFlightMsgIds
                    .size() >= DataServiceImpl.this.dataServiceOptions.getMaxInFlightMessages()) {
                logger.warn("The configured maximum number of in-flight messages has been reached");
                throw new KuraTooManyInflightMessagesException("Too many in-flight messages");
            }
        }

        private void suspendPublisher(long timeout, TimeUnit timeUnit) {
            if (!DataServiceImpl.this.publisherEnabled.get()) {
                return;
            }
            try {
                DataServiceImpl.this.lock.lock();
                if (!DataServiceImpl.this.notifyPending) {
                    if (timeout == -1) {
                        logger.debug("Suspending publishing thread indefinitely");
                        DataServiceImpl.this.lockCondition.await();
                    } else {
                        logger.debug("Suspending publishing thread for {} nanoseconds", timeout);
                        DataServiceImpl.this.lockCondition.await(timeout, timeUnit);
                    }
                }
                DataServiceImpl.this.notifyPending = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                DataServiceImpl.this.lock.unlock();
            }
        }

        private void publishMessageUnbound(DataMessage message) throws KuraException {
            publishInternal(message);
            // Notify the listeners
            DataServiceImpl.this.dataServiceListeners.onMessagePublished(message.getId(), message.getTopic());
        }

        private boolean publishMessageTokenBucket(DataMessage message) throws KuraException {
            boolean tokenAvailable = DataServiceImpl.this.throttle.getToken();

            if (tokenAvailable) {
                publishMessageUnbound(message);
                return true;
            }
            return false;
        }

        private void handleInFlightCongestion() {
            int timeout = DataServiceImpl.this.dataServiceOptions.getInFlightMessagesCongestionTimeout();

            // Do not schedule more that one task at a time
            if (timeout != 0 && (DataServiceImpl.this.congestionFuture == null
                    || DataServiceImpl.this.congestionFuture.isDone())) {
                logger.warn("In-flight message congestion timeout started");
                DataServiceImpl.this.congestionFuture = DataServiceImpl.this.congestionExecutor.schedule(() -> {
                    Thread.currentThread().setName("DataServiceImpl:InFlightCongestion");
                    logger.warn("In-flight message congestion timeout elapsed. Disconnecting and reconnecting again");
                    disconnect();
                    startConnectionMonitorTask();
                }, timeout, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public String getCriticalComponentName() {
        return "DataServiceImpl";
    }

    @Override
    public int getCriticalComponentTimeout() {
        return this.dataServiceOptions.getCriticalComponentTimeout();
    }

    public Map<String, String> getConnectionInfo() {
        Map<String, String> result = new HashMap<>();
        result.put("Broker URL", this.dataTransportService.getBrokerUrl());
        result.put("Account", this.dataTransportService.getAccountName());
        result.put("Username", this.dataTransportService.getUsername());
        result.put("Client ID", this.dataTransportService.getClientId());
        return result;
    }
}
