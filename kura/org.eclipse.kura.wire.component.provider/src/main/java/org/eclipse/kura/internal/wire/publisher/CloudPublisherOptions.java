/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class CloudPublisherOptions is responsible to provide all the required
 * options for the Cloud Publisher Wire Component
 */
final class CloudPublisherOptions {

    private static final Logger logger = LogManager.getLogger();

    private static final String CLOUD_SERVICE_PID = "cloud.service.pid";

    /** The Constant denoting the publisher application. */
    private static final String CONF_APPLICATION = "publish.application";

    /** The Constant denoting if publishing has to be performed on control topics. */
    private static final String CONF_PUBLISH_CONTROL_MESSAGE = "publish.control.messages";

    private static final String CONF_PRIORITY = "publish.priority";

    private static final String CONF_QOS = "publish.qos";

    /** The Constant denoting MQTT retain */
    private static final String CONF_RETAIN = "publish.retain";

    /** The Constant denoting MQTT topic. */
    private static final String CONF_TOPIC = "publish.topic";

    private static final String CONF_POSITION = "publish.position";

    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";

    /** The Constant application to perform (either publish or subscribe). */
    private static final String DEFAULT_APPLICATION = "W1";

    private static final boolean DEFAULT_CONTROL_MESSAGE = false;

    private static final int DEFAULT_PRIORITY = 7;

    private static final int DEFAULT_QOS = 0;

    /** The Constant denoting default MQTT retain. */
    private static final boolean DEFAULT_RETAIN = false;

    /** The Constant denoting default MQTT topic. */
    private static final String DEFAULT_TOPIC = "A1/$assetName";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new cloud publisher options.
     *
     * @param properties
     *            the properties
     */
    CloudPublisherOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = properties;
    }

    /**
     * Returns the topic to be used for message publishing.
     *
     * @return the publishing application topic
     */
    String getPublishingApplication() {
        String publishingApp = DEFAULT_APPLICATION;
        final Object app = this.properties.get(CONF_APPLICATION);
        if (nonNull(app) && app instanceof String) {
            publishingApp = String.valueOf(app);
        }
        return publishingApp;
    }

    /**
     * Returns the priority to be used for message publishing.
     *
     * @return the publishing priority
     */
    int getPublishingPriority() {
        int publishingPriority = DEFAULT_PRIORITY;
        final Object priority = this.properties.get(CONF_PRIORITY);
        if (nonNull(priority) && priority instanceof Integer) {
            publishingPriority = (Integer) priority;
        }
        return publishingPriority;
    }

    /**
     * Returns the QoS to be used for message publishing.
     *
     * @return the publishing QoS
     */
    int getPublishingQos() {
        int publishingQos = DEFAULT_QOS;
        final Object qos = this.properties.get(CONF_QOS);
        if (nonNull(qos) && qos instanceof Integer) {
            publishingQos = (Integer) qos;
        }
        return publishingQos;
    }

    /**
     * Returns the retain to be used for message publishing.
     *
     * @return the publishing retain
     */
    boolean getPublishingRetain() {
        boolean publishingRetain = DEFAULT_RETAIN;
        final Object retain = this.properties.get(CONF_RETAIN);
        if (nonNull(retain) && retain instanceof Boolean) {
            publishingRetain = (Boolean) retain;
        }
        return publishingRetain;
    }

    /**
     * Returns the topic to be used for message publishing.
     *
     * @return the publishing topic
     */
    String getPublishingTopic() {
        String publishingTopic = DEFAULT_TOPIC;
        final Object topic = this.properties.get(CONF_TOPIC);
        if (nonNull(topic) && topic instanceof String) {
            publishingTopic = String.valueOf(topic);
        }
        return publishingTopic;
    }

    /**
     * Returns the kura.service.pid of the cloud service to be used to publish the generated messages
     *
     * @return the kura.service.pid of the cloud service to be used.
     */
    String getCloudServicePid() {
        String cloudServicePid = DEFAULT_CLOUD_SERVICE_PID;
        Object configCloudServicePid = this.properties.get(CLOUD_SERVICE_PID);
        if (nonNull(configCloudServicePid) && configCloudServicePid instanceof String) {
            cloudServicePid = (String) configCloudServicePid;
        }
        return cloudServicePid;
    }

    /**
     * Returns if the messages have to be published as control messages or simple data messages.
     *
     * @return true if messages have to be published as control messages
     */
    boolean isControlMessage() {
        boolean isControlMessage = DEFAULT_CONTROL_MESSAGE;
        final Object configurationIsControlMessage = this.properties.get(CONF_PUBLISH_CONTROL_MESSAGE);
        if (nonNull(configurationIsControlMessage) && configurationIsControlMessage instanceof Boolean) {
            isControlMessage = (Boolean) configurationIsControlMessage;
        }
        return isControlMessage;
    }

    /**
     * Returns if the messages have to be enriched with gateway's position and which type of position is needed
     *
     * @return true if messages have to be enriched with gateway's position
     */
    PositionType getPositionType() {
        String positionTypeString = "";
        final Object configurationPositionType = this.properties.get(CONF_POSITION);
        if (nonNull(configurationPositionType) && configurationPositionType instanceof String) {
            positionTypeString = (String) configurationPositionType;
        }

        PositionType result = PositionType.NONE;
        try {
            result = PositionType.getEncoding(positionTypeString);
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot parse the provided position type.", e);
        }
        return result;
    }
}