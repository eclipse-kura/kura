/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;

public class SparkplugMqttClient {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugMqttClient.class);

    private List<String> servers;
    private Iterator<String> serversIterator;
    private String clientId;
    private MqttConnectOptions options;
    private MqttCallback callback;
    private Set<DataTransportListener> listeners;

    private String groupId;
    private String nodeId;
    private Optional<String> primaryHostId;
    private long connectionTimeoutMs;

    private MqttAsyncClient client;
    private BdSeqCounter bdSeqCounter = new BdSeqCounter();
    private long lastStateTimestamp = 0;
    private Random randomDelayGenerator = new Random();

    public enum SessionStatus {
        TERMINATED,
        ESTABILISHING,
        ESTABILISHED
    }

    private SessionStatus sessionStatus = SessionStatus.TERMINATED;

    public SparkplugMqttClient(SparkplugDataTransportOptions options, MqttCallback callback,
            Set<DataTransportListener> listeners) {
        this.servers = options.getServers();
        this.serversIterator = this.servers.iterator();
        this.clientId = options.getClientId();
        this.options = options.getMqttConnectOptions();
        this.callback = callback;
        this.listeners = listeners;

        this.groupId = options.getGroupId();
        this.nodeId = options.getNodeId();
        this.primaryHostId = options.getPrimaryHostApplicationId();
        this.connectionTimeoutMs = options.getConnectionTimeoutMs();

        logger.info(
                "Sparkplug MQTT client updated" + "\n\tServers: {}" + "\n\tClient ID: {}" + "\n\tGroup ID: {}"
                        + "\n\tNode ID: {}" + "\n\tPrimary Host Application ID: {}" + "\n\tConnection Timeout (ms): {}"
                        + "\n\tbdSeq: {}",
                this.servers, this.clientId, this.groupId, this.nodeId, this.primaryHostId, this.connectionTimeoutMs,
                this.bdSeqCounter.getCurrent());
    }

    public synchronized boolean isSessionEstabilished() {
        return this.sessionStatus == SessionStatus.ESTABILISHED && Objects.nonNull(this.client)
                && this.client.isConnected();
    }

    public synchronized void estabilishSession(boolean shouldConnectClient) throws KuraConnectException {
        if (this.sessionStatus == SessionStatus.TERMINATED) {
            try {
                updateSessionStatus(SessionStatus.TERMINATED, SessionStatus.ESTABILISHING);

                if (shouldConnectClient) {
                    newClientConnection();
                }

                subscribe(SparkplugTopics.getNodeCommandTopic(this.groupId, this.nodeId), 1);

                if (this.primaryHostId.isPresent()) {
                    subscribe(SparkplugTopics.getStateTopic(this.primaryHostId.get()), 1);
                } else {
                    confirmSession();
                }
            } catch (MqttException e) {
                this.sessionStatus = SessionStatus.TERMINATED;
                throw new KuraConnectException(e);
            }
        } else {
            logInvalidStateTransition(this.sessionStatus, SessionStatus.ESTABILISHING);
        }
    }

    public synchronized void terminateSession(boolean shouldDisconnectClient, long quiesceTimeout) {
        if (this.sessionStatus == SessionStatus.ESTABILISHED || this.sessionStatus == SessionStatus.ESTABILISHING) {
            try {
                this.listeners.forEach(listener -> SparkplugDataTransport.callSafely(listener::onDisconnecting));

                if (this.sessionStatus == SessionStatus.ESTABILISHED) {
                    sendEdgeNodeDeath();
                }

                if (shouldDisconnectClient) {
                    disconnectClient(quiesceTimeout);
                }

                updateSessionStatus(this.sessionStatus, SessionStatus.TERMINATED);
                this.listeners.forEach(listener -> SparkplugDataTransport.callSafely(listener::onDisconnected));
            } catch (MqttException e) {
                logger.error("Error terminating Sparkplug Edge Node session", e);
            }
        } else {
            logInvalidStateTransition(this.sessionStatus, SessionStatus.TERMINATED);
        }
    }

    public synchronized void confirmSession() {
        if (this.sessionStatus == SessionStatus.ESTABILISHING) {
            sendEdgeNodeBirth();
            updateSessionStatus(SessionStatus.ESTABILISHING, SessionStatus.ESTABILISHED);
            this.listeners
                    .forEach(listener -> SparkplugDataTransport.callSafely(listener::onConnectionEstablished, true));
        } else {
            logInvalidStateTransition(this.sessionStatus, SessionStatus.ESTABILISHED);
        }
    }

    public synchronized IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean isRetained) {
        try {
            logger.info("Publishing message on topic {} with QoS {} and retain {}", topic, qos, isRetained);
            return this.client.publish(topic, payload, qos, isRetained);
        } catch (MqttException e) {
            logger.error("Error publishing to topic {} with QoS {}", topic, qos, e);
        }

        return null;
    }

    public synchronized void subscribe(String topic, int qos) {
        try {
            IMqttToken token = this.client.subscribe(topic, qos);
            token.waitForCompletion(this.connectionTimeoutMs);
            logger.info("Subscribed to topic {} with QoS {}", topic, qos);
        } catch (MqttException e) {
            logger.error("Error subscribing to topic {} with QoS {}", topic, qos, e);
        }
    }
    
    public synchronized void unsubscribe(String topic) {
        try {
            IMqttToken token = this.client.unsubscribe(topic);
            token.waitForCompletion(this.connectionTimeoutMs);
            logger.info("Unsubscribed from topic {}", topic);
        } catch (MqttException e) {
            logger.error("Error unsubscribing from topic {}", topic, e);
        }
    }

    public synchronized String getConnectedServer() {
        return isSessionEstabilished() ? this.client.getCurrentServerURI() : this.servers.toString();
    }

    public synchronized Runnable getMessageDispatcher(String topic, MqttMessage message) {
        return () -> dispatchMessage(topic, message);
    }

    /*
     * Private methods
     */

    private String getNextServer() {
        String server;
        if (this.serversIterator.hasNext()) {
            server = this.serversIterator.next();
        } else {
            this.serversIterator = this.servers.iterator();
            server = this.serversIterator.next();
        }

        logger.info("Selecting next server {} from {}", server, this.servers);
        return server;
    }

    private void setWillMessage() {
        String topic = SparkplugTopics.getNodeDeathTopic(this.groupId, this.nodeId);
        byte[] payload = SparkplugPayloads.getNodeDeathPayload(this.bdSeqCounter.getCurrent());
        this.options.setWill(topic, payload, 1, false);
    }

    private void sendEdgeNodeBirth() {
        String topic = SparkplugTopics.getNodeBirthTopic(this.groupId, this.nodeId);
        byte[] payload = SparkplugPayloads.getNodeBirthPayload(this.bdSeqCounter.getCurrent(), 0);
        publish(topic, payload, 0, false);
        logger.debug("Published Edge Node BIRTH with bdSeq {}", this.bdSeqCounter.getCurrent());
    }

    private void sendEdgeNodeDeath() {
        String topic = SparkplugTopics.getNodeDeathTopic(this.groupId, this.nodeId);
        byte[] payload = SparkplugPayloads.getNodeDeathPayload(this.bdSeqCounter.getCurrent());
        publish(topic, payload, 0, false);
        logger.debug("Published Edge Node DEATH with bdSeq {}", this.bdSeqCounter.getCurrent());
    }

    private void newClientConnection() throws MqttException {
        this.bdSeqCounter.next();
        setWillMessage();

        try {
            long randomDelay = this.randomDelayGenerator.nextInt(5000);
            logger.info("Randomly delaying connect by {} ms", randomDelay);
            Thread.sleep(randomDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        this.client = new MqttAsyncClient(getNextServer(), this.clientId, new MemoryPersistence());
        this.client.setCallback(this.callback);

        IMqttToken token = this.client.connect(this.options);
        token.waitForCompletion(this.connectionTimeoutMs);

        logger.debug("Client connected");
    }

    private void disconnectClient(long quiesceTimeout) throws MqttException {
        IMqttToken token = this.client.disconnect(quiesceTimeout);
        token.waitForCompletion(this.connectionTimeoutMs);

        logger.debug("Client disconnected");
    }

    private void updateSessionStatus(SessionStatus from, SessionStatus to) {
        logger.info("Sparkplug Session: {} -> {}", from, to);
        this.sessionStatus = to;
    }

    private void logInvalidStateTransition(SessionStatus from, SessionStatus to) {
        logger.warn("Invalid state transition {} -> {}, ignoring request", from, to);
    }

    private synchronized void dispatchMessage(String topic, MqttMessage message) {
        boolean isValidStateMessage = this.primaryHostId.isPresent()
                && topic.equals(SparkplugTopics.getStateTopic(this.primaryHostId.get()));
        boolean isValidNcmdMessage = topic.equals(SparkplugTopics.getNodeCommandTopic(this.groupId, this.nodeId));

        try {
            if (isValidStateMessage) {
                dispatchStateMessage(message.getPayload());
            } else if (isValidNcmdMessage) {
                dispatchNcmdMessage(message.getPayload());
            }
        } catch (Exception e) {
            logger.error("Error dispatching arrived message", e);
        }
    }

    private void dispatchStateMessage(byte[] payload) throws KuraConnectException {
        logger.debug("Handling STATE message");

        JsonElement json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8));
        boolean isOnline = json.getAsJsonObject().get("online").getAsBoolean();
        long timestamp = json.getAsJsonObject().get("timestamp").getAsLong();

        if (this.lastStateTimestamp <= timestamp) {
            this.lastStateTimestamp = timestamp;

            if (isOnline) {
                logger.info("Primary Host Application is online");
                confirmSession();
            } else {
                logger.info("Primary Host Application is offline");
                terminateSession(true, 0);
                estabilishSession(true);
            }
        }
    }

    private void dispatchNcmdMessage(byte[] payload) throws KuraConnectException {
        logger.debug("Handling NCMD message");

        try {
            boolean nodeRebirth = SparkplugPayloads.getBooleanMetric(SparkplugPayloads.NODE_CONTROL_REBIRTH_METRIC_NAME,
                    payload);

            if (nodeRebirth) {
                logger.debug("{} requested", SparkplugPayloads.NODE_CONTROL_REBIRTH_METRIC_NAME);

                terminateSession(false, 0);
                estabilishSession(false);
            }
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error processing payload for NCMD message", e);
        } catch (NoSuchFieldException e) {
            logger.debug("NMCD message ignored, it does not contain any Node Control/Rebirth metric");
        }
    }

}
