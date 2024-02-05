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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.device;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugMessageType;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils.InvocationUtils;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.utils.SparkplugCloudEndpointTracker;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkplugDevice
        implements CloudPublisher, ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugDevice.class);

    public static final String KEY_MESSAGE_TYPE = "message.type";
    public static final String KEY_DEVICE_ID = "device.id";

    private String deviceId;
    private SparkplugCloudEndpointTracker endpointTracker;
    private Optional<SparkplugCloudEndpoint> sparkplugCloudEndpoint = Optional.empty();
    private final Set<CloudConnectionListener> cloudConnectionListeners = new CopyOnWriteArraySet<>();
    private final Set<CloudDeliveryListener> cloudDeliveryListeners = new CopyOnWriteArraySet<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Set<String> deviceMetrics = new HashSet<>();

    /*
     * ConfigurableComponent APIs
     */

    public void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws InvalidSyntaxException {
        String endpointPid = (String) properties
                .get(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value());

        this.endpointTracker = new SparkplugCloudEndpointTracker(componentContext.getBundleContext(),
                this::setSparkplugCloudEndpoint, this::unsetSparkplugCloudEndpoint, endpointPid);
        this.endpointTracker.startEndpointTracker();

        update(properties);
    }

    public void update(final Map<String, Object> properties) {
        this.deviceId = (String) properties.get(KEY_DEVICE_ID);
        if (Objects.isNull(this.deviceId) || this.deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Property '" + KEY_DEVICE_ID + "' cannot be null or empty");
        }

        this.deviceMetrics.clear();

        logger.info("Sparkplug Device {} - Updated device ID", this.deviceId);
    }

    public void deactivate() {
        logger.info("Sparkplug Device {} - Deactivating", this.deviceId);

        this.endpointTracker.stopEndpointTracker();

        logger.debug("Sparkplug Device {} - Shutting down executor service", this.deviceId);
        this.executorService.shutdownNow();

        logger.info("Sparkplug Device {} - Deactivated", this.deviceId);
    }

    /*
     * CloudConnectionListener APIs
     */

    @Override
    public void onDisconnected() {
        this.deviceMetrics.clear();
        this.cloudConnectionListeners.forEach(listener -> this.executorService.execute(listener::onDisconnected));
    }

    @Override
    public void onConnectionLost() {
        this.deviceMetrics.clear();
        this.cloudConnectionListeners.forEach(listener -> this.executorService.execute(listener::onConnectionLost));
    }

    @Override
    public void onConnectionEstablished() {
        this.deviceMetrics.clear();
        this.cloudConnectionListeners
                .forEach(listener -> this.executorService.execute(listener::onConnectionEstablished));
    }

    /*
     * CloudPublisher APIs
     */

    @Override
    public synchronized String publish(final KuraMessage message) throws KuraException {
        if (!this.sparkplugCloudEndpoint.isPresent()) {
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, "Missing SparkplugCloudEndpoint reference");
        }

        final Map<String, Object> newMessageProperties = new HashMap<>();
        newMessageProperties.put(KEY_DEVICE_ID, this.deviceId);

        if (this.deviceMetrics.isEmpty() || !this.deviceMetrics.equals(message.getPayload().metricNames())) {
            this.deviceMetrics.clear();
            this.deviceMetrics.addAll(message.getPayload().metricNames());
            newMessageProperties.put(KEY_MESSAGE_TYPE, SparkplugMessageType.DBIRTH);
            logger.info("Sparkplug Device {} - Metrics set changed, publishing DBIRTH", this.deviceId);
        } else {
            newMessageProperties.put(KEY_MESSAGE_TYPE, SparkplugMessageType.DDATA);
        }

        return this.sparkplugCloudEndpoint.get().publish(new KuraMessage(message.getPayload(), newMessageProperties));
    }

    @Override
    public void registerCloudConnectionListener(final CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(final CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.remove(cloudConnectionListener);
    }

    @Override
    public void registerCloudDeliveryListener(final CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryListener(final CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    /*
     * CloudDeliveryListener APIs
     */

    @Override
    public void onMessageConfirmed(final String messageId) {
        this.cloudDeliveryListeners.forEach(listener -> this.executorService
                .execute(() -> InvocationUtils.callSafely(listener::onMessageConfirmed, messageId)));
    }

    /*
     * Utils
     */

    private synchronized void setSparkplugCloudEndpoint(SparkplugCloudEndpoint endpoint) {
        this.sparkplugCloudEndpoint = Optional.of(endpoint);
        this.sparkplugCloudEndpoint.get().registerCloudConnectionListener(this);
        this.sparkplugCloudEndpoint.get().registerCloudDeliveryListener(this);
    }

    private synchronized void unsetSparkplugCloudEndpoint(SparkplugCloudEndpoint endpoint) {
        if (this.sparkplugCloudEndpoint.isPresent() && this.sparkplugCloudEndpoint.get() == endpoint) {
            this.sparkplugCloudEndpoint.get().unregisterCloudConnectionListener(this);
            this.sparkplugCloudEndpoint.get().unregisterCloudDeliveryListener(this);
            this.sparkplugCloudEndpoint = Optional.empty();
        }
    }

}
