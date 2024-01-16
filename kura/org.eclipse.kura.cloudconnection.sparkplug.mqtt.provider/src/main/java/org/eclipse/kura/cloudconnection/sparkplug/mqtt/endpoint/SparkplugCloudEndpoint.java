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
import java.util.function.Consumer;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkplugCloudEndpoint
        implements ConfigurableComponent, CloudEndpoint, CloudConnectionManager, DataServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugCloudEndpoint.class);

    private Set<CloudConnectionListener> cloudConnectionListeners = new HashSet<>();
    private Set<CloudDeliveryListener> cloudDeliveryListeners = new HashSet<>();
    private String kuraServicePid;

    /*
     * Activation APIs
     */

    private DataService dataService;
    private EventAdmin eventAdmin;

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void activate(Map<String, Object> properties) {
        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        logger.info("{} - Activating", this.kuraServicePid);

        this.dataService.addDataServiceListener(this);
        update(properties);

        logger.info("{} - Activated", this.kuraServicePid);
    }

    public void update(Map<String, Object> properties) {
        logger.info("{} - Updating", this.kuraServicePid);

        logger.info("{} - Updated", this.kuraServicePid);
    }

    public void deactivate() {
        logger.info("{} - Deactivating", this.kuraServicePid);

        logger.info("{} - Deactivated", this.kuraServicePid);
    }

    /*
     * CloudEndpoint APIs
     */

    @Override
    public String publish(KuraMessage message) throws KuraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerSubscriber(Map<String, Object> subscriptionProperties,
            CloudSubscriberListener cloudSubscriberListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterSubscriber(CloudSubscriberListener cloudSubscriberListener) {
        // TODO Auto-generated method stub

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

        sendExampleDeviceBirth();

        this.cloudConnectionListeners.forEach(listener -> callSafely(listener::onConnectionEstablished));
        postConnectionChangeEvent(true);

        // TO DO: init subscriptions
    }

    private void sendExampleDeviceBirth() {
        try {
            this.dataService.subscribe(SparkplugTopics.getDeviceCommandTopic("g1", "n1", "d1"), 1);

            Map<String, TypedValue<?>> metrics = new HashMap<>();
            TypedValue<String> value = new StringValue("test.value");
            metrics.put("test.key", value);

            String topic = SparkplugTopics.getDeviceBirthTopic("g1", "n1", "d1");
            byte[] payload = SparkplugPayloads.getDeviceBirthPayload(1, metrics);

            this.dataService.publish(topic, payload, 0, false, 7);
        } catch (KuraException e) {
            logger.error("Error in example", e);
        }
    }

    @Override
    public void onDisconnecting() {
        // nothing to do
    }

    @Override
    public void onDisconnected() {
        logger.debug("{} - Disconnected", this.kuraServicePid);
        this.cloudConnectionListeners.forEach(listener -> callSafely(listener::onDisconnected));
        postConnectionChangeEvent(false);
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        logger.debug("{} - Connection lost", this.kuraServicePid);
        this.cloudConnectionListeners.forEach(listener -> callSafely(listener::onConnectionLost));
        postConnectionChangeEvent(false);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        logger.debug("{} - Message arrived, forwarding to registered subscribers", this.kuraServicePid);
        // TODO
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        // nothing to do
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        logger.debug("{} - Message with ID {} confirmed", this.kuraServicePid, messageId);
        this.cloudDeliveryListeners
                .forEach(listener -> callSafely(listener::onMessageConfirmed, String.valueOf(messageId)));
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

    private void callSafely(Runnable f) {
        try {
            f.run();
        } catch (Exception e) {
            logger.warn("{} - An error occured in listener {}", this.kuraServicePid, f.getClass().getName(), e);
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
