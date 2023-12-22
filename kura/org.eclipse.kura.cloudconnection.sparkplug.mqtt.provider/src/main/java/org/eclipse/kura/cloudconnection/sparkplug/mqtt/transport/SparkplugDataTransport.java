/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkplugDataTransport implements ConfigurableComponent, DataTransportService, MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugDataTransport.class);

    private String kuraServicePid;
    private String sessionId;
    private SparkplugDataTransportOptions options;
    private MqttAsyncClient client;
    private boolean isInitialized = false;
    private Set<DataTransportListener> dataTransportListeners = new HashSet<>();

    /*
     * Activation APIs
     */

    public void setMqttAsyncClient(MqttAsyncClient client) {
        this.client = client;
    }

    public void activate(Map<String, Object> properties) {
        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        logger.info("{} - Activating", this.kuraServicePid);

        update(properties);

        logger.info("{} - Activated", this.kuraServicePid);
    }

    public void update(Map<String, Object> properties) {
        logger.info("{} - Updating", this.kuraServicePid);

        boolean wasConnected = isConnected();

        this.dataTransportListeners.forEach(listener -> callSafely(listener::onConfigurationUpdating, wasConnected));

        try {
            this.options = new SparkplugDataTransportOptions(properties);

            this.client = new MqttAsyncClient(this.options.getPrimaryServerURI(), this.options.getClientId(),
                    new MemoryPersistence());
            this.client.setCallback(this);

            this.sessionId = getBrokerUrl() + "-" + getClientId();

            if (wasConnected) {
                disconnect(0);
                connect();
            }
        } catch (KuraException ke) {
            logger.error("{} - Error in configuration properties", this.kuraServicePid, ke);
        } catch (MqttException me) {
            logger.error("{} - Error initializing MQTT client", this.kuraServicePid, me);
        }

        this.dataTransportListeners.forEach(listener -> callSafely(listener::onConfigurationUpdated, wasConnected));

        logger.info("{} - Updated", this.kuraServicePid);
    }

    public void initSparkplugParameters(String groupId, String nodeId, long bdSeq) {
        String topic = SparkplugTopics.getNodeDeathTopic(groupId, nodeId);
        byte[] payload = SparkplugPayloads.getNodeDeathPayload(bdSeq);
        this.options.getMqttConnectOptions().setWill(topic, payload, 1, false);

        this.isInitialized = Objects.nonNull(this.client);
    }

    public void deactivate() {
        logger.info("{} - Deactivating", this.kuraServicePid);

        if (isConnected()) {
            disconnect(0);
        }

        logger.info("{} - Deactivated", this.kuraServicePid);
    }

    /*
     * DataTransportService APIs
     */

    @Override
    public void connect() throws KuraConnectException {
        if (isConnected()) {
            throw new IllegalStateException("MQTT client is already connected");
        }

        if (!this.isInitialized) {
            throw new IllegalStateException("MQTT client has not been initialized correctly");
        }

        try {
            IMqttToken token = this.client.connect(this.options.getMqttConnectOptions());
            token.waitForCompletion(this.options.getConnectionTimeoutMs());

            this.dataTransportListeners.forEach(listener -> callSafely(listener::onConnectionEstablished, true));

            logger.info("{} - Connected", this.kuraServicePid);
        } catch (MqttException e) {
            throw new KuraConnectException(e, "Error connecting MQTT client");
        }
    }

    @Override
    public boolean isConnected() {
        if (this.isInitialized) {
            return this.client.isConnected();
        }
        return false;
    }

    @Override
    public String getBrokerUrl() {
        if (this.isInitialized) {
            return isConnected() ? this.client.getCurrentServerURI() : this.client.getServerURI();
        }
        return "";
    }

    @Override
    public String getAccountName() {
        return "";
    }

    @Override
    public String getUsername() {
        if (this.isInitialized) {
            return this.options.getUsername();
        }
        return "";
    }

    @Override
    public String getClientId() {
        if (this.isInitialized) {
            return this.options.getClientId();
        }
        return "";
    }

    @Override
    public void disconnect(long quiesceTimeout) {
        this.dataTransportListeners.forEach(listener -> callSafely(listener::onDisconnecting));

        if (isConnected()) {
            try {
                IMqttToken token = this.client.disconnect(quiesceTimeout);
                token.waitForCompletion(this.options.getConnectionTimeoutMs());

                logger.info("{} - Disconnected", this.kuraServicePid);
            } catch (MqttException e) {
                logger.error("{} - Error disconnecting", this.kuraServicePid, e);
            }
        } else {
            logger.warn("{} - Already disconnected", this.kuraServicePid);
        }

        this.dataTransportListeners.forEach(listener -> callSafely(listener::onDisconnected));
    }

    @Override
    public void subscribe(String topic, int qos) throws KuraException {
        checkConnectedAndInitialized();

        try {
            IMqttToken token = this.client.subscribe(topic, qos);
            token.waitForCompletion(this.options.getConnectionTimeoutMs());

            logger.info("{} - Subscribed to topic {} with QoS {}", this.kuraServicePid, topic, qos);
        } catch (MqttException e) {
            logger.error("{} - Error subscribing", this.kuraServicePid, e);
        }

    }

    @Override
    public void unsubscribe(String topic) throws KuraException {
        checkConnectedAndInitialized();

        try {
            IMqttToken token = this.client.unsubscribe(topic);
            token.waitForCompletion(this.options.getConnectionTimeoutMs());

            logger.info("{} - Unsubscribed from topic {}", this.kuraServicePid, topic);
        } catch (MqttException e) {
            logger.error("{} - Error unsubscribing", this.kuraServicePid, e);
        }
    }

    @Override
    public DataTransportToken publish(String topic, byte[] payload, int qos, boolean retain) throws KuraException {
        checkConnectedAndInitialized();

        logger.info("{} - Publishing message on topic {} with QoS {} and retain {}", this.kuraServicePid, topic, qos,
                retain);

        try {
            IMqttDeliveryToken deliveryToken = this.client.publish(topic, payload, qos, retain);

            if (qos > 0) {
                return new DataTransportToken(deliveryToken.getMessageId(), this.sessionId);
            }
        } catch (MqttException pe) {
            logger.error("{} - Error publishing", this.kuraServicePid, pe);
        }

        return null;
    }

    @Override
    public void addDataTransportListener(DataTransportListener listener) {
        logger.debug("{} - Adding DataTransportListener {}", this.kuraServicePid, listener.getClass().getName());
        this.dataTransportListeners.add(listener);
    }

    @Override
    public void removeDataTransportListener(DataTransportListener listener) {
        logger.debug("{} - Removing DataTransportListener {}", this.kuraServicePid, listener.getClass().getName());
        this.dataTransportListeners.remove(listener);
    }

    /*
     * MqttCallback APIs
     */

    @Override
    public void connectionLost(Throwable arg0) {
        logger.info("{} - Connection lost", this.kuraServicePid);
        this.dataTransportListeners.forEach(listener -> callSafely(listener::onConnectionLost, arg0));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
        try {
            if (deliveryToken.getMessage().getQos() > 0) {
                DataTransportToken dataTransportToken = new DataTransportToken(deliveryToken.getMessageId(),
                        this.sessionId);

                this.dataTransportListeners
                        .forEach(listener -> callSafely(listener::onMessageConfirmed, dataTransportToken));
            }
        } catch (MqttException e) {
            logger.error("{} - Error processing MQTTDeliveryToken", this.kuraServicePid, e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.debug("{} - Message arrived on topic {} with QoS {}", this.kuraServicePid, topic, message.getQos());
        
        this.dataTransportListeners.forEach(listener -> {
            try {
                listener.onMessageArrived(topic, message.getPayload(), message.getQos(), message.isRetained());
            } catch (Exception e) {
                logger.error("{} - Error processing onMessageArrived for listener {}", this.kuraServicePid,
                        listener.getClass().getName(), e);
            }
        });
    }

    /*
     * Utils
     */

    private void checkConnectedAndInitialized() throws KuraNotConnectedException {
        if (!this.isInitialized) {
            throw new IllegalStateException("MQTT client has not been initialized correctly");
        }

        if (!isConnected()) {
            throw new KuraNotConnectedException("MQTT client is not connected");
        }
    }

    private void callSafely(Runnable f) {
        try {
            f.run();
        } catch (Exception e) {
            logger.error("{} - An error occured in listener {}", this.kuraServicePid, f.getClass().getName(), e);
        }
    }

    private <T> void callSafely(Consumer<T> f, T argument) {
        try {
            f.accept(argument);
        } catch (Exception e) {
            logger.error("{} - An error occured in listener {}", this.kuraServicePid, f.getClass().getName(), e);
        }
    }

}
