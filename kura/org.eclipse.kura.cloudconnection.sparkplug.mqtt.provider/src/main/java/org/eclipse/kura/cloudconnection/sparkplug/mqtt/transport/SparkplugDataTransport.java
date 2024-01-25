/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkplugDataTransport implements ConfigurableComponent, DataTransportService, MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugDataTransport.class);

    private String kuraServicePid;
    private String sessionId;
    private SparkplugMqttClient client;
    private SparkplugDataTransportOptions options;
    private Set<DataTransportListener> dataTransportListeners = new HashSet<>();
    private ExecutorService executorService;

    /*
     * Activation APIs
     */

    public void activate(Map<String, Object> properties) {
        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        logger.info("{} - Activating", this.kuraServicePid);

        update(properties);

        logger.info("{} - Activated", this.kuraServicePid);
    }

    public void update(Map<String, Object> properties) {
        logger.info("{} - Updating", this.kuraServicePid);

        boolean wasConnected = isConnected();

        this.dataTransportListeners
                .forEach(listener -> callSafely(listener::onConfigurationUpdating, wasConnected));

        if (wasConnected) {
            disconnect(0);
        }

        try {
            this.options = new SparkplugDataTransportOptions(properties);
            this.sessionId = getBrokerUrl() + "-" + getClientId();
            this.client = new SparkplugMqttClient(this.options, this, this.dataTransportListeners);

            if (wasConnected) {
                connect();
            }
        } catch (KuraException ke) {
            logger.error("{} - Error in configuration properties", this.kuraServicePid, ke);
        }

        this.dataTransportListeners
                .forEach(listener -> callSafely(listener::onConfigurationUpdated, wasConnected));

        logger.info("{} - Updated", this.kuraServicePid);
    }

    public void deactivate() {
        logger.info("{} - Deactivating", this.kuraServicePid);

        disconnect(0);

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

        this.client.estabilishSession(true);

        stopExecutorService();
        this.executorService = Executors.newSingleThreadExecutor();
        logger.debug("{} - Initialized message dispatcher executor", this.kuraServicePid);
    }

    @Override
    public boolean isConnected() {
        return Objects.nonNull(this.client) && this.client.isSessionEstabilished();
    }

    @Override
    public String getBrokerUrl() {
        return Objects.nonNull(this.client) ? this.client.getConnectedServer() : "";
    }

    @Override
    public synchronized String getAccountName() {
        return "";
    }

    @Override
    public String getUsername() {
        return this.options.getUsername();
    }

    @Override
    public String getClientId() {
        return this.options.getClientId();
    }

    @Override
    public void disconnect(long quiesceTimeout) {
        stopExecutorService();
        this.client.terminateSession(true, quiesceTimeout);
    }

    @Override
    public void subscribe(String topic, int qos) throws KuraException {
        checkConnected();

        this.client.subscribe(topic, qos);
    }

    @Override
    public void unsubscribe(String topic) throws KuraException {
        checkConnected();

        this.client.unsubscribe(topic);
    }

    @Override
    public DataTransportToken publish(String completeTopic, byte[] payload, int qos, boolean retain)
            throws KuraException {
        checkConnected();

        String topic = completeTopic.replace(SparkplugCloudEndpoint.PLACEHOLDER_GROUP_ID, this.options.getGroupId())
                .replace(SparkplugCloudEndpoint.PLACEHOLDER_NODE_ID, this.options.getNodeId());

        IMqttDeliveryToken deliveryToken = this.client.publish(topic, payload, qos, retain);

        if (qos > 0) {
            return new DataTransportToken(deliveryToken.getMessageId(), this.sessionId);
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
    public void messageArrived(String topic, MqttMessage message) {
        logger.debug("{} - Message arrived on topic {} with QoS {}", this.kuraServicePid, topic, message.getQos());

        this.executorService.submit(() -> this.dataTransportListeners.forEach(listener -> listener
                .onMessageArrived(topic, message.getPayload(), message.getQos(), message.isRetained())));
        this.executorService.submit(this.client.getMessageDispatcher(topic, message));
    }

    /*
     * Utils
     */

    private void checkConnected() throws KuraNotConnectedException {
        if (!isConnected()) {
            throw new KuraNotConnectedException("MQTT client is not connected");
        }
    }

    static void callSafely(Runnable f) {
        try {
            f.run();
        } catch (Exception e) {
            logger.error("An error occured in listener {}", f.getClass().getName(), e);
        }
    }

    static <T> void callSafely(Consumer<T> f, T argument) {
        try {
            f.accept(argument);
        } catch (Exception e) {
            logger.error("An error occured in listener {}", f.getClass().getName(), e);
        }
    }

    private void stopExecutorService() {
        if (Objects.nonNull(this.executorService)) {
            logger.debug("{} - Shutting down message dispatcher executor", this.kuraServicePid);
            this.executorService.shutdownNow();
        }
    }

}
