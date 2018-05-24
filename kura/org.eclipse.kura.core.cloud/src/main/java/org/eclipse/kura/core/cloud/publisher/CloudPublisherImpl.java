/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.cloud.publisher;

import static java.util.Objects.nonNull;
import static org.eclipse.kura.core.message.MessageConstants.APP_ID;
import static org.eclipse.kura.core.message.MessageConstants.APP_TOPIC;
import static org.eclipse.kura.core.message.MessageConstants.CONTROL;
import static org.eclipse.kura.core.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.core.message.MessageConstants.QOS;
import static org.eclipse.kura.core.message.MessageConstants.RETAIN;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.cloud.CloudPublisherDeliveryListener;
import org.eclipse.kura.core.cloud.CloudServiceImpl;
import org.eclipse.kura.core.message.MessageType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudPublisherImpl
        implements CloudPublisher, ConfigurableComponent, CloudConnectionListener, CloudPublisherDeliveryListener {

    private final class CloudConnectionManagerTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudConnectionManager, CloudConnectionManager> {

        @Override
        public CloudConnectionManager addingService(final ServiceReference<CloudConnectionManager> reference) {
            CloudConnectionManager tempCloudService = CloudPublisherImpl.this.bundleContext.getService(reference);

            if (tempCloudService instanceof CloudServiceImpl) {
                CloudPublisherImpl.this.cloudServiceImpl = (CloudServiceImpl) tempCloudService;
                CloudPublisherImpl.this.cloudServiceImpl.registerCloudConnectionListener(CloudPublisherImpl.this);
                CloudPublisherImpl.this.cloudServiceImpl
                        .registerCloudPublisherDeliveryListener(CloudPublisherImpl.this);
                return tempCloudService;
            } else {
                CloudPublisherImpl.this.bundleContext.ungetService(reference);
            }

            return null;
        }

        @Override
        public void removedService(final ServiceReference<CloudConnectionManager> reference,
                final CloudConnectionManager service) {
            CloudPublisherImpl.this.cloudServiceImpl.unregisterCloudConnectionListener(CloudPublisherImpl.this);
            CloudPublisherImpl.this.cloudServiceImpl.unregisterCloudPublisherDeliveryListener(CloudPublisherImpl.this);
            CloudPublisherImpl.this.cloudServiceImpl = null;
        }

        @Override
        public void modifiedService(ServiceReference<CloudConnectionManager> reference,
                CloudConnectionManager service) {
            // Not needed
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CloudPublisherImpl.class);

    private static final String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
    private static final Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);

    private final Set<CloudConnectionListener> cloudConnectionListeners = new CopyOnWriteArraySet<>();
    private final Set<CloudDeliveryListener> cloudDeliveryListeners = new CopyOnWriteArraySet<>();

    private ServiceTrackerCustomizer<CloudConnectionManager, CloudConnectionManager> cloudConnectionManagerTrackerCustomizer;
    private ServiceTracker<CloudConnectionManager, CloudConnectionManager> cloudConnectionManagerTracker;

    private CloudPublisherOptions cloudPublisherOptions;
    private CloudServiceImpl cloudServiceImpl;
    private BundleContext bundleContext;

    private final ExecutorService worker = Executors.newCachedThreadPool();

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating Cloud Publisher...");
        this.bundleContext = componentContext.getBundleContext();

        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        this.cloudConnectionManagerTrackerCustomizer = new CloudConnectionManagerTrackerCustomizer();
        initCloudConnectionManagerTracking();

        logger.debug("Activating Cloud Publisher... Done");
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("Updating Cloud Publisher...");

        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        if (nonNull(this.cloudConnectionManagerTracker)) {
            this.cloudConnectionManagerTracker.close();
        }
        initCloudConnectionManagerTracking();

        logger.debug("Updating Cloud Publisher... Done");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating Cloud Publisher...");

        if (nonNull(this.cloudConnectionManagerTracker)) {
            this.cloudConnectionManagerTracker.close();
        }

        this.worker.shutdown();
        logger.debug("Deactivating Cloud Publisher... Done");
    }

    @Override
    public String publish(KuraMessage message) throws KuraException {
        if (this.cloudServiceImpl == null) {
            logger.info("Null cloud service");
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE);
        }

        if (message == null) {
            logger.warn("Received null message!");
            throw new IllegalArgumentException();
        }

        String appTopic = fillAppTopicPlaceholders(this.cloudPublisherOptions.getAppTopic(), message);

        int qos = this.cloudPublisherOptions.getQos();
        boolean retain = this.cloudPublisherOptions.isRetain();
        int priority = this.cloudPublisherOptions.getPriority();
        boolean isControl = MessageType.CONTROL.equals(this.cloudPublisherOptions.getMessageType());

        Map<String, Object> publishMessageProps = new HashMap<>();
        publishMessageProps.put(APP_TOPIC.name(), appTopic);
        publishMessageProps.put(APP_ID.name(), this.cloudPublisherOptions.getAppId());
        publishMessageProps.put(QOS.name(), qos);
        publishMessageProps.put(RETAIN.name(), retain);
        publishMessageProps.put(PRIORITY.name(), priority);
        publishMessageProps.put(CONTROL.name(), isControl);

        KuraMessage publishMessage = new KuraMessage(message.getPayload(), publishMessageProps);

        return this.cloudServiceImpl.publish(publishMessage);
    }

    private String fillAppTopicPlaceholders(String appTopic, KuraMessage message) {
        Matcher matcher = TOPIC_PATTERN.matcher(appTopic);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            Map<String, Object> properties = message.getProperties();
            if (properties.containsKey(matcher.group(1))) {
                String replacement = matcher.group(0);

                Object value = properties.get(matcher.group(1));
                if (replacement != null) {
                    matcher.appendReplacement(buffer, value.toString());
                }
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private void initCloudConnectionManagerTracking() {
        String selectedCloudServicePid = this.cloudPublisherOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudConnectionManager.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.cloudConnectionManagerTracker = new ServiceTracker<>(this.bundleContext, filter,
                this.cloudConnectionManagerTrackerCustomizer);
        this.cloudConnectionManagerTracker.open();
    }

    @Override
    public void onDisconnected() {
        this.cloudConnectionListeners.forEach(listener -> this.worker.execute(listener::onDisconnected));
    }

    @Override
    public void onConnectionLost() {
        this.cloudConnectionListeners.forEach(listener -> this.worker.execute(listener::onConnectionLost));
    }

    @Override
    public void onConnectionEstablished() {
        this.cloudConnectionListeners.forEach(listener -> this.worker.execute(listener::onConnectionEstablished));
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.remove(cloudConnectionListener);
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryistener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    @Override
    public void onMessageConfirmed(String messageId, String topic) {
        if (topic.contains(this.cloudPublisherOptions.getAppId())) {
            this.cloudDeliveryListeners.forEach(listener -> this.worker.execute(() -> listener.onMessageConfirmed(messageId)));
        }
    }

}
