/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.publisher;

import static java.util.Objects.nonNull;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.FULL_TOPIC;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.QOS;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageConstants.RETAIN;

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
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudConnectionManagerImpl;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudConnectionManagerOptions;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPublisherDeliveryListener;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageType;
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

            if (tempCloudService instanceof CloudConnectionManagerImpl) {
                CloudPublisherImpl.this.cloudConnectionImpl = (CloudConnectionManagerImpl) tempCloudService;
                CloudPublisherImpl.this.cloudConnectionImpl.registerCloudConnectionListener(CloudPublisherImpl.this);
                CloudPublisherImpl.this.cloudConnectionImpl
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
            CloudPublisherImpl.this.cloudConnectionImpl.unregisterCloudConnectionListener(CloudPublisherImpl.this);
            CloudPublisherImpl.this.cloudConnectionImpl
                    .unregisterCloudPublisherDeliveryListener(CloudPublisherImpl.this);
            CloudPublisherImpl.this.cloudConnectionImpl = null;
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
    private CloudConnectionManagerImpl cloudConnectionImpl;
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
        if (this.cloudConnectionImpl == null) {
            logger.info("Null cloud service");
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE);
        }

        if (message == null) {
            logger.warn("Received null message!");
            throw new IllegalArgumentException();
        }

        MessageType messageType = this.cloudPublisherOptions.getMessageType();

        String fullTopic = encodeFullTopic(message, messageType.getTopicPrefix());

        int qos = messageType.getQos();
        boolean retain = false;
        int priority = messageType.getPriority();

        Map<String, Object> publishMessageProps = new HashMap<>();
        publishMessageProps.put(FULL_TOPIC.name(), fullTopic);
        publishMessageProps.put(QOS.name(), qos);
        publishMessageProps.put(RETAIN.name(), retain);
        publishMessageProps.put(PRIORITY.name(), priority);

        KuraMessage publishMessage = new KuraMessage(message.getPayload(), publishMessageProps);

        return this.cloudConnectionImpl.publish(publishMessage);
    }

    private String encodeFullTopic(KuraMessage message, String topicPrefix) {
        String fullTopic = encodeTopic(topicPrefix, this.cloudPublisherOptions.getSemanticTopic());
        return fillTopicPlaceholders(fullTopic, message);
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

    private String encodeTopic(String topicPrefix, String semanticTopic) {
        CloudConnectionManagerOptions options = this.cloudConnectionImpl.getCloudServiceOptions();
        String deviceId = options.getTopicClientIdToken();
        String topicSeparator = options.getTopicSeparator();
        String accountName = options.getTopicAccountToken();

        StringBuilder sb = new StringBuilder();

        sb.append(topicPrefix).append(topicSeparator).append(accountName).append(topicSeparator).append(deviceId);

        if (semanticTopic != null && !semanticTopic.isEmpty()) {
            sb.append(topicSeparator).append(semanticTopic);
        }

        return sb.toString();
    }

    private String fillTopicPlaceholders(String semanticTopic, KuraMessage message) {
        Matcher matcher = TOPIC_PATTERN.matcher(semanticTopic);
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

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.remove(cloudConnectionListener);
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
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryistener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    @Override
    public void onMessageConfirmed(String messageId, String topic) {
        CloudConnectionManagerOptions options = this.cloudConnectionImpl.getCloudServiceOptions();
        String topicSeparator = options.getTopicSeparator();

        String[] semanticTopicElements = this.cloudPublisherOptions.getSemanticTopic().split(topicSeparator);

        int index = getSemanticTopicComparisonOffset(semanticTopicElements);
        String semanticTopicComparisonElement = semanticTopicElements[index];

        String[] messageSemanticTopicElements = getMessageSemanticTopicElements(topic, topicSeparator,
                semanticTopicComparisonElement);

        index = 0;
        for (String semanticTopicElement : semanticTopicElements) {
            if (!semanticTopicElement.equals(messageSemanticTopicElements[index])) {
                return;
            }
            index++;
        }

        this.cloudDeliveryListeners
                .forEach(deliveryListener -> this.worker.execute(() -> deliveryListener.onMessageConfirmed(messageId)));
    }

    private String[] getMessageSemanticTopicElements(String topic, String topicSeparator,
            String semanticTopicComparisonElement) {
        int messagePostfixTopicOffset = topic.indexOf(semanticTopicComparisonElement);
        String messagePostfixTopic = topic.substring(messagePostfixTopicOffset, topic.length());
        return messagePostfixTopic.split(topicSeparator);
    }

    private int getSemanticTopicComparisonOffset(String[] semanticTopicElements) {
        int index = 0;
        for (String semanticTopicElement : semanticTopicElements) {
            if (semanticTopicElement.startsWith("$")) {
                index++;
            } else {
                break;
            }
        }
        return index;
    }
}
