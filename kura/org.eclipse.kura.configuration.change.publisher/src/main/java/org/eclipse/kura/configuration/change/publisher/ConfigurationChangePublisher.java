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
package org.eclipse.kura.configuration.change.publisher;

import static org.eclipse.kura.core.message.MessageConstants.CONTROL;
import static org.eclipse.kura.core.message.MessageConstants.FULL_TOPIC;
import static org.eclipse.kura.core.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.core.message.MessageConstants.QOS;
import static org.eclipse.kura.core.message.MessageConstants.RETAIN;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.eclipse.kura.configuration.change.publisher.helper.CloudEndpointServiceHelper;
import org.eclipse.kura.core.cloud.CloudPublisherDeliveryListener;
import org.eclipse.kura.core.cloud.CloudServiceOptions;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationChangePublisher
        implements CloudPublisher, ConfigurableComponent, CloudConnectionListener, CloudPublisherDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationChangePublisher.class);

    private static final String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
    private static final Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);

    private BundleContext bundleContext;

    private Set<CloudDeliveryListener> cloudDeliveryListeners = new HashSet<>();
    private Set<CloudConnectionListener> cloudConnectionListeners = new HashSet<>();

    private ConfigurationChangePublisherOptions options;
    private final ExecutorService worker = Executors.newCachedThreadPool();

    private CloudEndpointServiceHelper cloudHelper;

    /*
     * Activation APIs
     */

    public void activate(ComponentContext componentContext, Map<String, Object> properties)
            throws InvalidSyntaxException {
        logger.debug("Activating ConfigurationChangePublisher...");

        this.bundleContext = componentContext.getBundleContext();

        updated(properties);

        logger.debug("Activating ConfigurationChangePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) throws InvalidSyntaxException {
        logger.debug("Updating ConfigurationChangePublisher...");
        
        this.options = new ConfigurationChangePublisherOptions(properties);
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
        publishMessageProps.put(FULL_TOPIC.name(), fullTopic);
        publishMessageProps.put(QOS.name(), this.options.getQos());
        publishMessageProps.put(RETAIN.name(), this.options.isRetain());
        publishMessageProps.put(PRIORITY.name(), this.options.getPriority());
        publishMessageProps.put(CONTROL.name(), true);

        // return this.cloudHelper.publish(new KuraMessage(message.getPayload(), publishMessageProps));
        KuraPayload p = new KuraPayload();
        p.addMetric("test", "testvalue");
        return this.cloudHelper.publish(new KuraMessage(p, publishMessageProps));
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
     * CloudPublisherDeliveryListener APIs
     */

    @Override
    public void onMessageConfirmed(String messageId, String topic) {
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
        String accountName = CloudServiceOptions.getTopicAccountToken();
        String clientId = CloudServiceOptions.getTopicClientIdToken();
        String topicSeparator = CloudServiceOptions.getTopicSeparator();

        StringBuilder sb = new StringBuilder();

        sb.append(this.options.getTopicPrefix()).append(topicSeparator);
        sb.append(accountName).append(topicSeparator);
        sb.append(clientId).append(topicSeparator).append(appTopic);

        return sb.toString();
    }
}
