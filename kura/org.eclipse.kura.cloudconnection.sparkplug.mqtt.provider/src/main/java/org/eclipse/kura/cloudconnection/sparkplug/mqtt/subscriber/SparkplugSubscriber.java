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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.subscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils.InvocationUtils;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils.SparkplugCloudEndpointTracker;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkplugSubscriber
        implements ConfigurableComponent, CloudSubscriber, CloudSubscriberListener, CloudConnectionListener {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugSubscriber.class);

    public static final String KEY_TOPIC_FILTER = "topic.filter";
    public static final String KEY_QOS = "qos";

    private Optional<SparkplugCloudEndpoint> sparkplugCloudEndpoint = Optional.empty();
    private SparkplugCloudEndpointTracker endpointTracker;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Set<CloudSubscriberListener> cloudSubscriberListeners = new CopyOnWriteArraySet<>();
    private Set<CloudConnectionListener> cloudConnectionListeners = new CopyOnWriteArraySet<>();

    private String kuraServicePid;
    private String topicFilter;
    private int qos;

    /*
     * Activation APIs
     */

    public void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws InvalidSyntaxException {
        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);

        logger.info("{} - Activating", this.kuraServicePid);

        String endpointPid = (String) properties
                .get(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value());

        this.endpointTracker = new SparkplugCloudEndpointTracker(componentContext.getBundleContext(),
                this::setSparkplugCloudEndpoint, this::unsetSparkplugCloudEndpoint, endpointPid);
        update(properties);

        logger.info("{} - Activated", this.kuraServicePid);
    }

    public void update(final Map<String, Object> properties) throws InvalidSyntaxException {
        logger.info("{} - Updating", this.kuraServicePid);

        this.topicFilter = (String) properties.get(KEY_TOPIC_FILTER);
        this.qos = (int) properties.get(KEY_QOS);
        MqttTopic.validate(this.topicFilter, true);

        this.endpointTracker.stopEndpointTracker();
        this.endpointTracker.startEndpointTracker();

        logger.info("{} - Updated", this.kuraServicePid);
    }

    public void deactivate() {
        logger.info("{} - Deactivating", this.kuraServicePid);

        this.endpointTracker.stopEndpointTracker();

        logger.debug("{} - Shutting down executor service", this.kuraServicePid);
        this.executorService.shutdownNow();

        logger.info("{} - Deactivated", this.kuraServicePid);
    }

    /*
     * CloudSubscriber APIs
     */

    @Override
    public void registerCloudSubscriberListener(CloudSubscriberListener listener) {
        this.cloudSubscriberListeners.add(listener);
    }

    @Override
    public void unregisterCloudSubscriberListener(CloudSubscriberListener listener) {
        this.cloudSubscriberListeners.remove(listener);
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.remove(cloudConnectionListener);
    }

    /*
     * CloudSubscriberListener APIs
     */

    @Override
    public void onMessageArrived(KuraMessage message) {
        this.cloudSubscriberListeners.forEach(listener -> this.executorService
                .execute(() -> InvocationUtils.callSafely(listener::onMessageArrived, message)));
    }

    /*
     * CloudConnectionListener APIs
     */

    @Override
    public void onDisconnected() {
        this.cloudConnectionListeners.forEach(
                listener -> this.executorService.execute(() -> InvocationUtils.callSafely(listener::onDisconnected)));
    }

    @Override
    public void onConnectionLost() {
        this.cloudConnectionListeners.forEach(
                listener -> this.executorService.execute(() -> InvocationUtils.callSafely(listener::onConnectionLost)));
    }

    @Override
    public void onConnectionEstablished() {
        this.cloudConnectionListeners.forEach(listener -> this.executorService
                .execute(() -> InvocationUtils.callSafely(listener::onConnectionEstablished)));
    }

    /*
     * Utils
     */

    private synchronized void setSparkplugCloudEndpoint(SparkplugCloudEndpoint endpoint) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY_TOPIC_FILTER, this.topicFilter);
        properties.put(KEY_QOS, this.qos);

        this.sparkplugCloudEndpoint = Optional.of(endpoint);
        this.sparkplugCloudEndpoint.get().registerSubscriber(properties, this);
    }

    private synchronized void unsetSparkplugCloudEndpoint(SparkplugCloudEndpoint endpoint) {
        if (this.sparkplugCloudEndpoint.isPresent() && this.sparkplugCloudEndpoint.get() == endpoint) {
            this.sparkplugCloudEndpoint.get().unregisterSubscriber(this);
            this.sparkplugCloudEndpoint = Optional.empty();
        }
    }

}
