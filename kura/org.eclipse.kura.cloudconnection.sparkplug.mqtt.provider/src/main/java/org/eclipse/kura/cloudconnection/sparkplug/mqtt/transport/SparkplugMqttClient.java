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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.net.SocketFactory;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils.InvocationUtils;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.ssl.SslManagerService;
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

    private SslManagerService sslManagerService;

    private SessionStatus sessionStatus = new Terminated();

    /*
     * State management
     */

    private abstract class SessionStatus {

        public abstract SessionStatus establishSession(boolean shouldConnectClient) throws KuraConnectException;

        public abstract SessionStatus terminateSession(boolean shouldDisconnectClient, long quiesceTimeout);

        public abstract SessionStatus confirmSession();

        SessionStatus toEstablishing(boolean shouldConnectClient) throws KuraConnectException {
            try {
                if (shouldConnectClient) {
                    newClientConnection();
                }

                subscribe(SparkplugTopics.getNodeCommandTopic(SparkplugMqttClient.this.groupId,
                        SparkplugMqttClient.this.nodeId), 1);

                if (SparkplugMqttClient.this.primaryHostId.isPresent()) {
                    subscribe(SparkplugTopics.getStateTopic(SparkplugMqttClient.this.primaryHostId.get()), 1);
                } else {
                    return toEstablished();
                }
            } catch (MqttException | GeneralSecurityException | IOException e) {
                SparkplugMqttClient.this.bdSeqCounter = new BdSeqCounter();
                throw new KuraConnectException(e);
            }

            return new Establishing();
        }

        SessionStatus toTerminated(boolean shouldDisconnectClient, long quiesceTimeout) {
            try {
                SparkplugMqttClient.this.listeners
                        .forEach(listener -> InvocationUtils.callSafely(listener::onDisconnecting));

                if (SparkplugMqttClient.this.sessionStatus instanceof Established) {
                    sendEdgeNodeDeath();
                }

                if (shouldDisconnectClient) {
                    disconnectClient(quiesceTimeout);
                }

                SparkplugMqttClient.this.listeners
                        .forEach(listener -> InvocationUtils.callSafely(listener::onDisconnected));
            } catch (MqttException e) {
                logger.error("Error terminating Sparkplug Edge Node session", e);
                return SparkplugMqttClient.this.sessionStatus;
            }

            return new Terminated();
        }

        SessionStatus toEstablished() {
            sendEdgeNodeBirth();
            SparkplugMqttClient.this.listeners
                    .forEach(listener -> InvocationUtils.callSafely(listener::onConnectionEstablished, true));
            return new Established();
        }

        private void newClientConnection() throws MqttException, GeneralSecurityException, IOException {
            SparkplugMqttClient.this.bdSeqCounter.next();
            setWillMessage();
            logger.debug("bdSeq: {}", SparkplugMqttClient.this.bdSeqCounter.getCurrent());

            try {
                long randomDelay = SparkplugMqttClient.this.randomDelayGenerator.nextInt(5000);
                logger.info("Randomly delaying connect by {} ms", randomDelay);
                Thread.sleep(randomDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            SparkplugMqttClient.this.client = new MqttAsyncClient(getNextServer(), SparkplugMqttClient.this.clientId,
                    new MemoryPersistence());
            SparkplugMqttClient.this.client.setCallback(SparkplugMqttClient.this.callback);

            IMqttToken token = SparkplugMqttClient.this.client.connect(SparkplugMqttClient.this.options);
            token.waitForCompletion(SparkplugMqttClient.this.connectionTimeoutMs);

            logger.debug("Client connected");
        }

        private void disconnectClient(long quiesceTimeout) throws MqttException {
            if (SparkplugMqttClient.this.client.isConnected()) {
                IMqttToken token = SparkplugMqttClient.this.client.disconnect(quiesceTimeout);
                token.waitForCompletion(SparkplugMqttClient.this.connectionTimeoutMs);
            }

            logger.debug("Client disconnected");
        }

        private void setWillMessage() {
            String topic = SparkplugTopics.getNodeDeathTopic(SparkplugMqttClient.this.groupId,
                    SparkplugMqttClient.this.nodeId);
            byte[] payload = SparkplugPayloads.getNodeDeathPayload(SparkplugMqttClient.this.bdSeqCounter.getCurrent());
            SparkplugMqttClient.this.options.setWill(topic, payload, 1, false);
        }

        private void sendEdgeNodeBirth() {
            String topic = SparkplugTopics.getNodeBirthTopic(SparkplugMqttClient.this.groupId,
                    SparkplugMqttClient.this.nodeId);
            byte[] payload = SparkplugPayloads.getNodeBirthPayload(SparkplugMqttClient.this.bdSeqCounter.getCurrent(),
                    0);
            publish(topic, payload, 0, false);
            logger.debug("Published Edge Node BIRTH with bdSeq {}", SparkplugMqttClient.this.bdSeqCounter.getCurrent());
        }

        private void sendEdgeNodeDeath() {
            String topic = SparkplugTopics.getNodeDeathTopic(SparkplugMqttClient.this.groupId,
                    SparkplugMqttClient.this.nodeId);
            byte[] payload = SparkplugPayloads.getNodeDeathPayload(SparkplugMqttClient.this.bdSeqCounter.getCurrent());
            publish(topic, payload, 0, false);
            logger.debug("Published Edge Node DEATH with bdSeq {}", SparkplugMqttClient.this.bdSeqCounter.getCurrent());
        }

        private String getNextServer() throws GeneralSecurityException, IOException {
            String server;
            if (SparkplugMqttClient.this.serversIterator.hasNext()) {
                server = SparkplugMqttClient.this.serversIterator.next();
            } else {
                SparkplugMqttClient.this.serversIterator = SparkplugMqttClient.this.servers.iterator();
                server = SparkplugMqttClient.this.serversIterator.next();
            }

            setSocketFactory(server);

            logger.info("Selecting next server {} from {}", server, SparkplugMqttClient.this.servers);
            return server;
        }

        private void setSocketFactory(String server) throws GeneralSecurityException, IOException {
            if (server.startsWith("ssl")) {
                SparkplugMqttClient.this.options
                        .setSocketFactory(SparkplugMqttClient.this.sslManagerService.getSSLSocketFactory());
            } else {
                SparkplugMqttClient.this.options.setSocketFactory(SocketFactory.getDefault());
            }
        }

    }

    private class Terminated extends SessionStatus {

        @Override
        public SessionStatus establishSession(boolean shouldConnectClient) throws KuraConnectException {
            return toEstablishing(shouldConnectClient);
        }

        @Override
        public SessionStatus terminateSession(boolean shouldDisconnectClient, long quiesceTimeout) {
            return this;
        }

        @Override
        public SessionStatus confirmSession() {
            return this;
        }

    }

    private class Establishing extends SessionStatus {

        @Override
        public SessionStatus establishSession(boolean shouldConnectClient) throws KuraConnectException {
            return this;
        }

        @Override
        public SessionStatus terminateSession(boolean shouldDisconnectClient, long quiesceTimeout) {
            return toTerminated(shouldDisconnectClient, quiesceTimeout);
        }

        @Override
        public SessionStatus confirmSession() {
            return toEstablished();
        }

    }

    private class Established extends SessionStatus {

        @Override
        public SessionStatus establishSession(boolean shouldConnectClient) throws KuraConnectException {
            return this;
        }

        @Override
        public SessionStatus terminateSession(boolean shouldDisconnectClient, long quiesceTimeout) {
            return toTerminated(shouldDisconnectClient, quiesceTimeout);
        }

        @Override
        public SessionStatus confirmSession() {
            return toEstablished();
        }

    }

    /*
     * Public methods
     */

    public SparkplugMqttClient(SparkplugDataTransportOptions options, MqttCallback callback,
            Set<DataTransportListener> listeners, SslManagerService sslManagerService) {
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

        this.sslManagerService = sslManagerService;

        logger.info(
                "Sparkplug MQTT client updated" + "\n\tServers: {}" + "\n\tClient ID: {}" + "\n\tGroup ID: {}"
                        + "\n\tNode ID: {}" + "\n\tPrimary Host Application ID: {}" + "\n\tConnection Timeout (ms): {}",
                this.servers, this.clientId, this.groupId, this.nodeId, this.primaryHostId, this.connectionTimeoutMs);
    }

    public synchronized boolean isSessionEstablished() {
        return this.sessionStatus instanceof Established && Objects.nonNull(this.client)
                && this.client.isConnected();
    }

    public synchronized void handleConnectionLost() {
        doSessionTransition(new Terminated());
    }

    public synchronized void establishSession(boolean shouldConnectClient) throws KuraConnectException {
        logger.debug("Requested session establishment");
        doSessionTransition(this.sessionStatus.establishSession(shouldConnectClient));
    }
    
    public synchronized void terminateSession(boolean shouldDisconnectClient, long quiesceTimeout) {
        logger.debug("Requested session termination");
        doSessionTransition(this.sessionStatus.terminateSession(shouldDisconnectClient, quiesceTimeout));
    }

    public synchronized void confirmSession() {
        logger.debug("Requested session confirmation");
        doSessionTransition(this.sessionStatus.confirmSession());
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
        return isSessionEstablished() ? this.client.getCurrentServerURI() : this.servers.toString();
    }

    public synchronized Runnable getMessageDispatcher(String topic, MqttMessage message) {
        return () -> dispatchMessage(topic, message);
    }

    /*
     * Private methods
     */

    private void doSessionTransition(SessionStatus newStatus) {
        String from = this.sessionStatus.getClass().getSimpleName();
        String to = newStatus.getClass().getSimpleName();

        if (!from.equals(to)) {
            logger.info("Sparkplug session: {} -> {}", from, to);
            this.sessionStatus = newStatus;
        }
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
                establishSession(true);
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
                establishSession(false);
            }
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error processing payload for NCMD message", e);
        } catch (NoSuchFieldException e) {
            logger.debug("NMCD message ignored, it does not contain any Node Control/Rebirth metric");
        }
    }

}
