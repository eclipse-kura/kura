/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import java.util.Map;

/**
 * The Class CloudSubscriberOptions is responsible to provide all the required
 * options for the Cloud Subscriber Wire Component
 */
final class CloudSubscriberOptions {

    private static final String CLOUD_SERVICE_PID = "cloud.service.pid";

    /** The Constant denoting QoS. */
    private static final String CONF_QOS = "subscribe.qos";

    /** The Constant denoting MQTT deviceId. */
    private static final String CONF_DEVICE_ID_TOPIC = "subscribe.deviceId";

    /** The Constant denoting MQTT app topic. */
    private static final String CONF_APP_TOPIC = "subscribe.appTopic";

    /** The Constant application to perform (either publish or subscribe). */
    private static final String DEFAULT_APPLICATION = "WIRE-SUB-V1";

    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";

    /** The Constant denoting default QoS. */
    private static final int DEFAULT_QOS = 0;

    /** The Constant denoting default MQTT deviceId. */
    private static final String DEFAULT_DEVICE_ID_TOPIC = "EVENT";

    /** The Constant denoting default MQTT app topic. */
    private static final String DEFAULT_APP_TOPIC = "EVENT";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new cloud subscriber options.
     *
     * @param properties
     *            the properties
     */
    CloudSubscriberOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the QoS to be used for message subscription.
     *
     * @return the subscribing QoS
     */
    int getSubscribingQos() {
        int subscribingQos = DEFAULT_QOS;
        final Object qos = this.properties.get(CONF_QOS);
        if ((this.properties != null) && this.properties.containsKey(CONF_QOS) && (qos != null)
                && (qos instanceof Integer)) {
            subscribingQos = (Integer) qos;
        }
        return subscribingQos;
    }

    /**
     * Returns the topic to be used for message subscription.
     *
     * @return the subscribing topic
     */
    String getSubscribingAppTopic() {
        String subscribingAppTopic = DEFAULT_APP_TOPIC;
        final Object appTopic = this.properties.get(CONF_APP_TOPIC);
        if ((this.properties != null) && this.properties.containsKey(CONF_APP_TOPIC) && (appTopic != null)
                && (appTopic instanceof String)) {
            subscribingAppTopic = String.valueOf(appTopic);
        }
        return subscribingAppTopic;
    }

    /**
     * Returns the deviceId to be used for message subscription.
     *
     * @return the deviceId
     */
    String getSubscribingDeviceId() {
        String subscribingDeviceId = DEFAULT_DEVICE_ID_TOPIC;
        final Object deviceId = this.properties.get(CONF_DEVICE_ID_TOPIC);
        if ((this.properties != null) && this.properties.containsKey(CONF_APP_TOPIC) && (deviceId != null)
                && (deviceId instanceof String)) {
            subscribingDeviceId = String.valueOf(deviceId);
        }
        return subscribingDeviceId;
    }

    /**
     * Returns the topic to be used for message subscription.
     *
     * @return the subscription application name
     */
    String getSubscribingApplication() {
        return DEFAULT_APPLICATION;
    }

    /**
     * Returns the kura.service.pid of the cloud service to be used to publish the generated messages
     *
     * @return the kura.service.pid of the cloud service to be used.
     */
    String getCloudServicePid() {
        String cloudServicePid = DEFAULT_CLOUD_SERVICE_PID;
        Object configCloudServicePid = this.properties.get(CLOUD_SERVICE_PID);
        if ((this.properties != null) && this.properties.containsKey(CLOUD_SERVICE_PID) && configCloudServicePid != null
                && configCloudServicePid instanceof String) {
            cloudServicePid = (String) configCloudServicePid;
        }
        return cloudServicePid;
    }

}