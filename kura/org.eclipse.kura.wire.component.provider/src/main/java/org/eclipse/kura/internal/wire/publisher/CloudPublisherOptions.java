/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.util.base.ThrowableUtil;

/**
 * The Class CloudPublisherOptions is responsible to provide all the required
 * options for the Cloud Publisher Wire Component
 */
final class CloudPublisherOptions {

    /**
     * The different Auto Connect Modes.
     */
    enum AutoConnectMode {

        /** The autoconnect mode off. */
        AUTOCONNECT_MODE_OFF(0),
        /** The autoconnect mode on and off. */
        AUTOCONNECT_MODE_ON_AND_OFF(0),
        /** The autoconnect mode on and stay. */
        AUTOCONNECT_MODE_ON_AND_STAY(0),
        /** The AUTOCONNECT ON and STAY for 1 min. */
        AUTOCONNECT_MODE_ON_AND_STAY_1_MIN(1),
        /** The AUTOCONNECT ON and STAY for 10 min. */
        AUTOCONNECT_MODE_ON_AND_STAY_10_MIN(10),
        /** The AUTOCONNECT ON and STAY for 15 min. */
        AUTOCONNECT_MODE_ON_AND_STAY_15_MIN(15),
        /** The AUTOCONNECT ON and STAY for 30 min. */
        AUTOCONNECT_MODE_ON_AND_STAY_30_MIN(30),
        /** The AUTOCONNECT ON and STAY for 5 min. */
        AUTOCONNECT_MODE_ON_AND_STAY_5_MIN(5),
        /** The AUTOCONNECT ON and STAY for 60 min. */
        AUTOCONNECT_MODE_ON_AND_STAY_60_MIN(60);

        /** The disconnect delay. */
        private int disconnectDelay;

        /**
         * Instantiates a new auto connect mode.
         *
         * @param disconnectDelay
         *            the disconnect delay
         */
        AutoConnectMode(final int disconnectDelay) {
            this.disconnectDelay = disconnectDelay;
        }

        /**
         * Gets the disconnect delay.
         *
         * @return the disconnect delay
         */
        int getDisconnectDelay() {
            return this.disconnectDelay;
        }
    }

    /** The Constant denoting the publisher application. */
    private static final String CONF_APPLICATION = "publish.application";

    /** The Constant denoting autoconnect mode. */
    private static final String CONF_AUTOCONNECT_MODE = "autoconnect.mode";

    /** The Constant denoting message type. */
    private static final String CONF_MESSAGE_TYPE = "publish.message.type";

    /** The Constant denoting priority. */
    private static final String CONF_PRIORITY = "publish.priority";

    /** The Constant denoting QoS. */
    private static final String CONF_QOS = "publish.qos";

    /** The Constant denoting quiesce timeout. */
    private static final String CONF_QUIESCE_TIMEOUT = "disconnect.quiesce.timeout";

    /** The Constant denoting MQTT retain */
    private static final String CONF_RETAIN = "publish.retain";

    /** The Constant denoting MQTT topic. */
    private static final String CONF_TOPIC = "publish.topic";

    /** The Constant application to perform (either publish or subscribe). */
    private static final String DEFAULT_APPLICATION = "PUB";

    /** The Constant denoting default auto connect mode. */
    private static final AutoConnectMode DEFAULT_AUTOCONNECT_MODE = AutoConnectMode.AUTOCONNECT_MODE_ON_AND_OFF;

    /** The Constant denoting default message type : Kura Payload. */
    private static final int DEFAULT_MESSAGE_TYPE = 1;

    /** The Constant denoting default priority. */
    private static final int DEFAULT_PRIORITY = 7;

    /** The Constant denoting default QoS. */
    private static final int DEFAULT_QOS = 0;

    /** The Constant DEFAULT_QUIECE_TIMEOUT. */
    private static final int DEFAULT_QUIECE_TIMEOUT = 1000;

    /** The Constant denoting default MQTT retain. */
    private static final boolean DEFAULT_RETAIN = false;

    /** The Constant denoting default MQTT topic. */
    private static final String DEFAULT_TOPIC = "EVENT";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new cloud publisher options.
     *
     * @param properties
     *            the properties
     */
    CloudPublisherOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the auto connect mode.
     *
     * @return the auto connect mode
     */
    AutoConnectMode getAutoConnectMode() {
        AutoConnectMode autoConnectMode = DEFAULT_AUTOCONNECT_MODE;
        final Object mode = this.properties.get(CONF_AUTOCONNECT_MODE);
        if ((this.properties != null) && this.properties.containsKey(CONF_AUTOCONNECT_MODE) && (mode != null)
                && (mode instanceof String)) {
            final String autoconnectModeValue = String.valueOf(mode);
            try {
                autoConnectMode = AutoConnectMode.valueOf(autoconnectModeValue);
            } catch (final IllegalArgumentException iea) {
                throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ThrowableUtil.stackTraceAsString(iea));
            }
        }
        return autoConnectMode;
    }

    /**
     * Returns the message type to be used for wrapping wire records.
     *
     * @return the type of the encoding message type
     */
    int getMessageType() {
        int messageType = DEFAULT_MESSAGE_TYPE;
        final Object type = this.properties.get(CONF_MESSAGE_TYPE);
        if ((this.properties != null) && this.properties.containsKey(CONF_MESSAGE_TYPE) && (type != null)
                && (type instanceof Integer)) {
            messageType = (Integer) type;
        }
        return messageType;
    }

    /**
     * Returns the topic to be used for message publishing.
     *
     * @return the publishing application topic
     */
    String getPublishingApplication() {
        String publishingApp = DEFAULT_APPLICATION;
        final Object app = this.properties.get(CONF_APPLICATION);
        if ((this.properties != null) && this.properties.containsKey(CONF_APPLICATION) && (app != null)
                && (app instanceof String)) {
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
        if ((this.properties != null) && this.properties.containsKey(CONF_PRIORITY) && (priority != null)
                && (priority instanceof Integer)) {
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
        if ((this.properties != null) && this.properties.containsKey(CONF_QOS) && (qos != null)
                && (qos instanceof Integer)) {
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
        if ((this.properties != null) && this.properties.containsKey(CONF_RETAIN) && (retain != null)
                && (retain instanceof Integer)) {
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
        if ((this.properties != null) && this.properties.containsKey(CONF_TOPIC) && (topic != null)
                && (topic instanceof String)) {
            publishingTopic = String.valueOf(topic);
        }
        return publishingTopic;
    }

    /**
     * Returns the Quiesce Timeout.
     *
     * @return the connect quiesce timeout
     */
    int getQuiesceTimeout() {
        int quieceTimeout = DEFAULT_QUIECE_TIMEOUT;
        final Object timeout = this.properties.get(CONF_QUIESCE_TIMEOUT);
        if ((this.properties != null) && this.properties.containsKey(CONF_QUIESCE_TIMEOUT) && (timeout != null)
                && (timeout instanceof Integer)) {
            quieceTimeout = (Integer) timeout;
        }
        return quieceTimeout;
    }

}