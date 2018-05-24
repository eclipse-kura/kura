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

import static java.util.Objects.isNull;
import static org.eclipse.kura.core.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.core.message.MessageConstants.QOS;
import static org.eclipse.kura.core.message.MessageConstants.RETAIN;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.eclipse.kura.core.cloud.CloudServiceImpl;
import org.eclipse.kura.core.cloud.CloudServiceOptions;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationPublisherImpl implements CloudNotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPublisherImpl.class);

    private static final String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
    private static final Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);

    private static final int DFLT_PUB_QOS = 0;
    private static final boolean DFLT_RETAIN = false;
    private static final int DFLT_PRIORITY = 1;

    private static final String MESSAGE_TYPE_KEY = "messageType";

    private static final String REQUESTOR_CLIENT_ID_KEY = "requestorClientId";

    private static final String APP_ID_KEY = "appId";

    private final CloudServiceImpl cloudServiceImpl;

    public NotificationPublisherImpl(CloudServiceImpl cloudServiceImpl) {
        this.cloudServiceImpl = cloudServiceImpl;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating Cloud Notification Publisher...");

        logger.debug("Activating Cloud Notification Publisher... Done");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating Cloud Notification Publisher...");

        logger.debug("Deactivating Cloud Notification Publisher... Done");
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        // Not needed
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        // Not needed
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        // Not needed
    }

    @Override
    public void unregisterCloudDeliveryistener(CloudDeliveryListener cloudDeliveryListener) {
        // Not needed
    }

    @Override
    public String publish(KuraMessage message) throws KuraException {
        if (this.cloudServiceImpl == null) {
            logger.warn("Null cloud connection");
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE);
        }

        if (message == null) {
            logger.warn("Received null message!");
            throw new IllegalArgumentException();
        }

        String fullTopic = encodeFullTopic(message);

        Map<String, Object> publishMessageProps = new HashMap<>();
        publishMessageProps.put("fullTopic", fullTopic);
        publishMessageProps.put(QOS.name(), DFLT_PUB_QOS);
        publishMessageProps.put(RETAIN.name(), DFLT_RETAIN);
        publishMessageProps.put(PRIORITY.name(), DFLT_PRIORITY);

        KuraMessage publishMessage = new KuraMessage(message.getPayload(), publishMessageProps);

        return this.cloudServiceImpl.publish(publishMessage);
    }

    private String encodeFullTopic(KuraMessage message) {
        String appId = (String) message.getProperties().get(APP_ID_KEY);
        String messageType = (String) message.getProperties().get(MESSAGE_TYPE_KEY);
        String requestorClientId = (String) message.getProperties().get(REQUESTOR_CLIENT_ID_KEY);

        if (isNull(appId) || isNull(messageType) || isNull(requestorClientId)) {
            throw new IllegalArgumentException("Incomplete properties in received message.");
        }

        String fullTopic = encodeTopic(appId, messageType, requestorClientId);
        return fillAppTopicPlaceholders(fullTopic, message);
    }

    private String encodeTopic(String appId, String messageType, String requestorClientId) {

        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        String deviceId = options.getTopicClientIdToken();
        String topicSeparator = options.getTopicSeparator();

        StringBuilder sb = new StringBuilder();

        sb.append(options.getTopicControlPrefix()).append(topicSeparator);

        sb.append(options.getTopicAccountToken()).append(topicSeparator).append(requestorClientId)
                .append(topicSeparator).append(appId);

        sb.append(topicSeparator).append("NOTIFY").append(topicSeparator).append(deviceId).append(topicSeparator)
                .append(messageType);

        return sb.toString();
    }

    private String fillAppTopicPlaceholders(String fullTopic, KuraMessage message) {

        Matcher matcher = TOPIC_PATTERN.matcher(fullTopic);
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

}
