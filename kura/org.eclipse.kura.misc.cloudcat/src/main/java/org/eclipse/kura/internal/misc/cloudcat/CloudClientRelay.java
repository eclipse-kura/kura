/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.misc.cloudcat;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudClientRelay implements CloudClientListener {

    private static final Logger logger = LoggerFactory.getLogger(CloudClientRelay.class);

    private static final int DFLT_PRIORITY = 5;

    private static final String FORWARDED_MESSAGE_METRIC_NAME = "_fwd";

    private final CloudClient thisCloudClient;
    private final CloudClient otherCloudClient;
    private final List<CloudCatSubscription> dataSubscriptions;
    private final List<CloudCatSubscription> controlSubscriptions;

    public CloudClientRelay(CloudClient thisCloudClient, CloudClient otherCloudClient,
            List<CloudCatSubscription> dataSubscriptions, List<CloudCatSubscription> controlSubscriptions) {
        super();
        this.thisCloudClient = thisCloudClient;
        this.otherCloudClient = otherCloudClient;
        this.dataSubscriptions = dataSubscriptions;
        this.controlSubscriptions = controlSubscriptions;
    }

    private boolean isForwardedMessage(KuraPayload msg) {
        final Object isForwardedMessage = msg.getMetric(FORWARDED_MESSAGE_METRIC_NAME);
        return isForwardedMessage != null && isForwardedMessage instanceof Boolean && (Boolean) isForwardedMessage;
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        try {
            if (isForwardedMessage(msg)) {
                logger.debug("Received already forwarded message, discarding");
                return;
            }

            msg.addMetric(FORWARDED_MESSAGE_METRIC_NAME, true);
            this.otherCloudClient.controlPublish(appTopic, msg, qos, retain, DFLT_PRIORITY);
        } catch (KuraException e) {
            logger.warn("Failed to relay incoming control message from: {} to: {}", appTopic,
                    this.thisCloudClient.getApplicationId(), this.otherCloudClient.getApplicationId());
        }
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        try {
            if (isForwardedMessage(msg)) {
                logger.debug("Received already forwarded message, discarding");
                return;
            }

            msg.addMetric(FORWARDED_MESSAGE_METRIC_NAME, true);
            this.otherCloudClient.publish(appTopic, msg, qos, retain, DFLT_PRIORITY);
        } catch (KuraException e) {
            logger.warn("Failed to relay incoming data message from: {} to: {}", appTopic,
                    this.thisCloudClient.getApplicationId(), this.otherCloudClient.getApplicationId());
        }
    }

    @Override
    public void onConnectionLost() {
        // Ignore
    }

    @Override
    public void onConnectionEstablished() {
        // Assuming clean session
        subscribe();
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        // Ignore
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        // Ignore
    }

    public void subscribe() {
        for (CloudCatSubscription subscription : this.dataSubscriptions) {
            String topic = subscription.getTopic();
            int qos = subscription.getQos();

            try {
                this.thisCloudClient.subscribe(topic, qos);
            } catch (KuraException e) {
                logger.error("Failed to subscribe client: {} to data topic: '{}'",
                        this.thisCloudClient.getApplicationId(), topic, e);
            }
        }

        for (CloudCatSubscription subscription : this.controlSubscriptions) {
            String topic = subscription.getTopic();
            int qos = subscription.getQos();

            try {
                this.thisCloudClient.controlSubscribe(topic, qos);
            } catch (KuraException e) {
                logger.error("Failed to subscribe client: {} to control topic: '{}'",
                        this.thisCloudClient.getApplicationId(), topic, e);
            }
        }
    }

    public void unsubscribe() {
        for (CloudCatSubscription subscription : this.dataSubscriptions) {
            String topic = subscription.getTopic();

            try {
                this.thisCloudClient.unsubscribe(topic);
            } catch (KuraException e) {
                logger.error("Failed to unsubscribe client: {} from data topic: '{}'",
                        this.thisCloudClient.getApplicationId(), topic, e);
            }
        }

        for (CloudCatSubscription subscription : this.controlSubscriptions) {
            String topic = subscription.getTopic();

            try {
                this.thisCloudClient.controlUnsubscribe(topic);
            } catch (KuraException e) {
                logger.error("Failed to unsubscribe client: {} from control topic: '{}'",
                        this.thisCloudClient.getApplicationId(), topic, e);
            }
        }
    }

    public boolean isConnected() {
        return this.thisCloudClient.isConnected();
    }

    public void listen() {
        this.thisCloudClient.addCloudClientListener(this);
    }

    public void unlisten() {
        this.thisCloudClient.removeCloudClientListener(this);
    }
}
