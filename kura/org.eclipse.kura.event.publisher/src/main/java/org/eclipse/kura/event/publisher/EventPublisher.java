/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.event.publisher;

import static org.eclipse.kura.event.publisher.EventPublisherConstants.CONTROL;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.FULL_TOPIC;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.PRIORITY;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.QOS;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.RETAIN;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.TOPIC_ACCOUNT_TOKEN;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.TOPIC_CLIENT_ID_TOKEN;
import static org.eclipse.kura.event.publisher.EventPublisherConstants.TOPIC_SEPARATOR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.event.publisher.helper.CloudEndpointServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPublisher
        implements CloudPublisher, ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    private static final String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
    private static final Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);

    private BundleContext bundleContext;

    private Set<CloudDeliveryListener> cloudDeliveryListeners = new HashSet<>();
    private Set<CloudConnectionListener> cloudConnectionListeners = new HashSet<>();

    private EventPublisherOptions options;
    private final ExecutorService worker = Executors.newCachedThreadPool();

    private CloudEndpointServiceHelper cloudHelper;

    /*
     * Activation APIs
     */

    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating ConfigurationChangePublisher...");

        this.bundleContext = componentContext.getBundleContext();

        updated(properties);

        logger.debug("Activating ConfigurationChangePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("Updating ConfigurationChangePublisher...");
        
        this.options = new EventPublisherOptions(properties);
        this.cloudHelper = new CloudEndpointServiceHelper(this.bundleContext, this.options.getCloudEndpointPid());

        logger.debug("Updating ConfigurationChangePublisher... Done.");
    }

    public void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating ConfigurationChangePublisher...");
        this.cloudHelper.close();
        logger.debug("Deactivating ConfigurationChangePublisher... Done.");
    }

    /*
     * CloudPublisher APIs
     */

    @Override
    public String publish(KuraMessage message) throws KuraException {
        if (message == null) {
            throw new IllegalArgumentException("Kura message cannot be null");
        }

        String resolvedAppTopic = fillAppTopicPlaceholders(this.options.getTopic(), message);
        String fullTopic = encodeFullTopic(resolvedAppTopic);

        Map<String, Object> publishMessageProps = new HashMap<>();
        publishMessageProps.put(FULL_TOPIC, fullTopic);
        publishMessageProps.put(QOS, this.options.getQos());
        publishMessageProps.put(RETAIN, this.options.isRetain());
        publishMessageProps.put(PRIORITY, this.options.getPriority());
        publishMessageProps.put(CONTROL, true);

        return this.cloudHelper.publish(new KuraMessage(message.getPayload(), publishMessageProps));
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
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    /*
     * CloudConnectionListener APIs
     */

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

    /*
     * CloudDeliveryListener APIs
     */

    @Override
    public void onMessageConfirmed(String messageId) {
        this.cloudDeliveryListeners
                .forEach(listener -> this.worker.execute(() -> listener.onMessageConfirmed(messageId)));
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

    private String encodeFullTopic(String appTopic) {
        String accountName = TOPIC_ACCOUNT_TOKEN;
        String clientId = TOPIC_CLIENT_ID_TOKEN;
        String topicSeparator = TOPIC_SEPARATOR;

        StringBuilder sb = new StringBuilder();

        Optional<String> topicPrefix = this.options.getTopicPrefix();
        if (topicPrefix.isPresent()) {
            sb.append(topicPrefix.get()).append(topicSeparator);
        }
        sb.append(accountName).append(topicSeparator);
        sb.append(clientId).append(topicSeparator).append(appTopic);

        return sb.toString();
    }
}
