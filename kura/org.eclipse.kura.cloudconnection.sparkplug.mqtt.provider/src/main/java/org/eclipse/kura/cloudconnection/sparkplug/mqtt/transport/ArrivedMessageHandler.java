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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;

public class ArrivedMessageHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ArrivedMessageHandler.class);

    private LinkedBlockingDeque<ArrivedMessage> arrivedMessagesQueue;
    private Set<DataTransportListener> listeners;
    private SparkplugMqttClient client;
    private String groupId;
    private String nodeId;
    private Optional<String> primaryHostId;
    private long lastStateTimestamp = 0;

    public ArrivedMessageHandler(LinkedBlockingDeque<ArrivedMessage> arrivedMessagesQueue,
            Set<DataTransportListener> listeners, SparkplugMqttClient client, String groupId, String nodeId,
            Optional<String> primaryHostId) {
        this.arrivedMessagesQueue = arrivedMessagesQueue;
        this.listeners = listeners;
        this.client = client;
        this.groupId = groupId;
        this.nodeId = nodeId;
        this.primaryHostId = primaryHostId;
    }

    @Override
    public void run() {
        logger.debug("Starting arrived messages queue consumer");

        try {
            while (!Thread.interrupted()) {
                ArrivedMessage msg = this.arrivedMessagesQueue.take();

                handleArrivedMessage(msg.getTopic(), msg.getMessage());
            }
        } catch (InterruptedException e) {
            logger.debug("Stopped arrived messages queue consumer");
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void handleArrivedMessage(String topic, MqttMessage message) {
        logger.debug("Processing arrived message with topic '{}' from queue. Elements remaining: {}", topic,
                this.arrivedMessagesQueue.size());

        boolean isValidStateMessage = this.primaryHostId.isPresent()
                && topic.equals(SparkplugTopics.getStateTopic(this.primaryHostId.get()));
        boolean isValidNcmdMessage = topic
                .equals(SparkplugTopics.getNodeCommandTopic(this.groupId, this.nodeId));

        if (isValidStateMessage) {
            handleStateMessage(message.getPayload());
        } else if (isValidNcmdMessage) {
            handleRebirthMessage(message.getPayload());
        } else {
            this.listeners.forEach(listener -> {
                try {
                    logger.debug("Forwarding to listeners");
                    listener.onMessageArrived(topic, message.getPayload(), message.getQos(), message.isRetained());
                } catch (Exception e) {
                    logger.error("Error processing onMessageArrived for listener {}", listener.getClass().getName(), e);
                }
            });
        }
    }

    private void handleStateMessage(byte[] payload) {
        logger.debug("Handling STATE message");

        JsonElement json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8));
        boolean isOnline = json.getAsJsonObject().get("online").getAsBoolean();
        long timestamp = json.getAsJsonObject().get("timestamp").getAsLong();

        if (this.lastStateTimestamp <= timestamp) {
            this.lastStateTimestamp = timestamp;

            if (isOnline && !this.client.isSessionEstabilished()) {
                logger.info("Primary Host Application is online");
                this.client.confirmSession();
            } else {
                logger.info("Primary Host Application is offline");
                if (this.client.isSessionEstabilished()) {
                    this.client.terminateSession(true, 0);
                }
                this.client.estabilishSession(true);
            }
        }
    }

    private void handleRebirthMessage(byte[] payload) {
        logger.debug("Handling NCMD message");

        try {
            boolean nodeRebirth = SparkplugPayloads.getBooleanMetric(SparkplugPayloads.NODE_CONTROL_REBIRTH_METRIC_NAME,
                    payload);

            if (nodeRebirth && this.client.isSessionEstabilished()) {
                logger.debug("{} requested", SparkplugPayloads.NODE_CONTROL_REBIRTH_METRIC_NAME);

                this.client.terminateSession(false, 0);
                this.client.estabilishSession(false);
            }
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error processing payload for NCMD message", e);
        } catch (NoSuchFieldException e) {
            logger.debug("NMCD message ignored, it does not contain any Node Control/Rebirth metric");
        }
    }

}
