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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.device.SparkplugDevice;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugMessageType;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.subscriber.SparkplugSubscriber;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils.InvocationUtils;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

public class SparkplugCloudEndpoint
        implements ConfigurableComponent, CloudEndpoint, CloudConnectionManager, DataServiceListener {

    public static final String PLACEHOLDER_GROUP_ID = "placeholder.group.id";
    public static final String PLACEHOLDER_NODE_ID = "placeholder.node.id";

    private static final Logger logger = LoggerFactory.getLogger(SparkplugCloudEndpoint.class);

    private Set<CloudConnectionListener> cloudConnectionListeners = new HashSet<>();
    private Set<CloudDeliveryListener> cloudDeliveryListeners = new HashSet<>();
    private String kuraServicePid;
    private SeqCounter seqCounter = new SeqCounter();
    private SubscriptionsMap subscriptions = new SubscriptionsMap();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    /*
     * Activation APIs
     */

    private DataService dataService;
    private EventAdmin eventAdmin;

    public void setDataService(final DataService dataService) {
        this.dataService = dataService;
    }

    public void setEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void activate(final Map<String, Object> properties) {
        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        logger.info("{} - Activating", this.kuraServicePid);

        this.dataService.addDataServiceListener(this);
        update();

        logger.info("{} - Activated", this.kuraServicePid);
    }

    public void update() {
        logger.info("{} - Updating", this.kuraServicePid);

        this.seqCounter = new SeqCounter();
        logger.debug("{} - seq number reset to {}", this.kuraServicePid, this.seqCounter.getCurrent());

        logger.info("{} - Updated", this.kuraServicePid);
    }

    public void deactivate() {
        logger.info("{} - Deactivating", this.kuraServicePid);

        try {
            disconnect();
        } catch (KuraDisconnectException e) {
            logger.info("{} - Error disconnecting", this.kuraServicePid, e);
        }

        this.executorService.shutdownNow();

        logger.info("{} - Deactivated", this.kuraServicePid);
    }

    /*
     * CloudEndpoint APIs
     */

    @Override
    public String publish(final KuraMessage message) throws KuraException {
        Map<String, Object> messageProperties = message.getProperties();
        if (!messageProperties.containsKey(SparkplugDevice.KEY_MESSAGE_TYPE)
                || !messageProperties.containsKey(SparkplugDevice.KEY_DEVICE_ID)) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "KuraMessage has a missing property between message.type and device.id");
        }

        SparkplugMessageType type = (SparkplugMessageType) messageProperties.get(SparkplugDevice.KEY_MESSAGE_TYPE);
        String deviceId = (String) messageProperties.get(SparkplugDevice.KEY_DEVICE_ID);

        logger.debug("{} - Sending message with seq: {}", this.kuraServicePid, this.seqCounter.getCurrent());

        byte[] sparkplugPayload = SparkplugPayloads.getSparkplugDevicePayload(this.seqCounter.getCurrent(),
                message.getPayload());

        this.seqCounter.next();

        if (type == SparkplugMessageType.DBIRTH) {
            return publishInternal(
                    SparkplugTopics.getDeviceBirthTopic(PLACEHOLDER_GROUP_ID, PLACEHOLDER_NODE_ID, deviceId),
                    sparkplugPayload, 0, false, 0);
        }

        if (type == SparkplugMessageType.DDATA) {
            return publishInternal(
                    SparkplugTopics.getDeviceDataTopic(PLACEHOLDER_GROUP_ID, PLACEHOLDER_NODE_ID, deviceId),
                    sparkplugPayload, 0, false, 7);
        }

        return null;
    }

    private String publishInternal(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        int id = this.dataService.publish(topic, payload, qos, retain, priority);

        if (qos == 0) {
            return null;
        }

        return String.valueOf(id);
    }

    @Override
    public void registerSubscriber(Map<String, Object> subscriptionProperties,
            CloudSubscriberListener cloudSubscriberListener) {
        String topicFilter = (String) subscriptionProperties.get(SparkplugSubscriber.KEY_TOPIC_FILTER);
        int qos = (int) subscriptionProperties.get(SparkplugSubscriber.KEY_QOS);

        this.subscriptions.add(topicFilter, qos, cloudSubscriberListener);
        subscribeIfConnected(topicFilter, qos);

        logger.info("{} - Added subscription for {}", this.kuraServicePid,
                cloudSubscriberListener.getClass().getSimpleName());
    }

    @Override
    public void unregisterSubscriber(CloudSubscriberListener cloudSubscriberListener) {
        this.subscriptions.remove(cloudSubscriberListener).forEach(this::unsubscribeIfConnected);

        logger.info("{} - Removed subscription for {}", this.kuraServicePid,
                cloudSubscriberListener.getClass().getSimpleName());
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        logger.debug("{} - Adding CloudDeliveryListener {}", this.kuraServicePid,
                cloudDeliveryListener.getClass().getName());
        this.cloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        logger.debug("{} - Removing CloudDeliveryListener {}", this.kuraServicePid,
                cloudDeliveryListener.getClass().getName());
        this.cloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    /*
     * CloudConnectionManager APIs
     */

    @Override
    public void connect() throws KuraConnectException {
        this.dataService.connect();
    }

    @Override
    public void disconnect() throws KuraDisconnectException {
        this.dataService.disconnect(0);
    }

    @Override
    public boolean isConnected() {
        return this.dataService.isConnected();
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        logger.debug("{} - Adding CloudConnectionListener {}", this.kuraServicePid,
                cloudConnectionListener.getClass().getName());
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        logger.debug("{} - Removing CloudConnectionListener {}", this.kuraServicePid,
                cloudConnectionListener.getClass().getName());
        this.cloudConnectionListeners.remove(cloudConnectionListener);
    }

    /*
     * DataServiceListener APIs
     */

    @Override
    public void onConnectionEstablished() {
        logger.debug("{} - Connection estabilished", this.kuraServicePid);

        this.cloudConnectionListeners
                .forEach(listener -> InvocationUtils.callSafely(listener::onConnectionEstablished));
        postConnectionChangeEvent(true);

        this.seqCounter = new SeqCounter();

        this.subscriptions.getSubscriptionRecords()
                .forEach(subscription -> subscribeIfConnected(subscription.getTopicFilter(), subscription.getQos()));
    }

    @Override
    public void onDisconnecting() {
        // nothing to do
    }

    @Override
    public void onDisconnected() {
        logger.debug("{} - Disconnected", this.kuraServicePid);
        this.cloudConnectionListeners.forEach(listener -> InvocationUtils.callSafely(listener::onDisconnected));
        postConnectionChangeEvent(false);
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        logger.debug("{} - Connection lost", this.kuraServicePid);
        this.cloudConnectionListeners.forEach(listener -> InvocationUtils.callSafely(listener::onConnectionLost));
        postConnectionChangeEvent(false);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        logger.debug("{} - Message arrived on topic {}, forwarding to registered subscribers", this.kuraServicePid,
                topic);

        for (CloudSubscriberListener listener : this.subscriptions.getMatchingListeners(topic, qos)) {
            try {
                KuraMessage message = new KuraMessage(SparkplugPayloads.getKuraPayload(payload));

                this.executorService.execute(() -> InvocationUtils.callSafely(listener::onMessageArrived, message));
            } catch (InvalidProtocolBufferException e) {
                logger.error("{} - Error parsing received SparkplugPayload to KuraPayload", this.kuraServicePid, e);
            }
        }
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        // nothing to do
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        logger.debug("{} - Message with ID {} confirmed", this.kuraServicePid, messageId);
        this.cloudDeliveryListeners.forEach(
                listener -> InvocationUtils.callSafely(listener::onMessageConfirmed, String.valueOf(messageId)));
    }

    /*
     * Utilities
     */

    private void postConnectionChangeEvent(final boolean isConnected) {
        logger.debug("{} - Posting connection changed event", this.kuraServicePid);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put("cloud.service.pid", this.kuraServicePid);

        Event event = isConnected ? new CloudConnectionEstablishedEvent(eventProperties)
                : new CloudConnectionLostEvent(eventProperties);

        this.eventAdmin.postEvent(event);
    }

    private synchronized void subscribeIfConnected(String topicFilter, int qos) {
        try {
            if (isConnected()) {
                this.dataService.subscribe(topicFilter, qos);
            }
        } catch (KuraException e) {
            logger.error("{} - Error subscribing to topic " + topicFilter + " with QoS " + qos, this.kuraServicePid, e);
        }
    }

    private synchronized void unsubscribeIfConnected(String topic) {
        try {
            if (isConnected()) {
                this.dataService.unsubscribe(topic);
            }
        } catch (KuraException e) {
            logger.error("{} - Error unsubscribing from topic {}", this.kuraServicePid, topic);
        }
    }

}
