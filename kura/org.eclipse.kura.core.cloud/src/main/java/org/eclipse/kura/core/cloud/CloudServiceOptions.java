/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.cloud;

import java.util.Map;

import org.eclipse.kura.cloud.CloudPayloadEncoding;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServiceOptions {

    private static final Logger logger = LoggerFactory.getLogger(CloudServiceOptions.class);

    private static final String TOPIC_SEPARATOR = "/";
    private static final String TOPIC_ACCOUNT_TOKEN = "#account-name";
    private static final String TOPIC_CLIENT_ID_TOKEN = "#client-id";
    private static final String TOPIC_BIRTH_SUFFIX = "MQTT/BIRTH";
    private static final String TOPIC_DISCONNECT_SUFFIX = "MQTT/DC";
    private static final String TOPIC_APPS_SUFFIX = "MQTT/APPS";
    private static final String TOPIC_CONTROL_PREFIX = "topic.control-prefix";
    private static final String TOPIC_CONTROL_PREFIX_DEFAULT = "$EDC";
    private static final String TOPIC_WILD_CARD = "#";

    private static final String DEVICE_DISPLAY_NAME = "device.display-name";
    private static final String DEVICE_CUSTOM_NAME = "device.custom-name";
    private static final String ENCODE_GZIP = "encode.gzip";
    private static final String REPUB_BIRTH_ON_GPS_LOCK = "republish.mqtt.birth.cert.on.gps.lock";
    private static final String REPUB_BIRTH_ON_MODEM_DETECT = "republish.mqtt.birth.cert.on.modem.detect";
    private static final String REPUB_BIRTH_ON_TAMPER_EVENT = "republish.mqtt.birth.cert.on.tamper.event";
    private static final String ENABLE_DFLT_SUBSCRIPTIONS = "enable.default.subscriptions";
    private static final String PAYLOAD_ENCODING = "payload.encoding";

    private static final int LIFECYCLE_PRIORITY = 0;
    private static final boolean LIFECYCLE_RETAIN = false;

    private final Map<String, Object> properties;
    private final SystemService systemService;

    public CloudServiceOptions(Map<String, Object> properties, SystemService systemService) {
        this.properties = properties;
        this.systemService = systemService;
    }

    /**
     * Returns the display name for the device.
     *
     * @return a String value.
     */
    public String getDeviceDisplayName() {
        String displayName = "";
        if (this.properties == null) {
            return displayName;
        }
        String deviceDisplayNameOption = (String) this.properties.get(DEVICE_DISPLAY_NAME);

        // Use the device name from SystemService. This should be kura.device.name from
        // the properties file.
        if ("device-name".equals(deviceDisplayNameOption)) {
            displayName = this.systemService.getDeviceName();
        }
        // Try to get the device hostname
        else if ("hostname".equals(deviceDisplayNameOption)) {
            displayName = this.systemService.getHostname();
        }
        // Return the custom field defined by the user
        else if ("custom".equals(deviceDisplayNameOption)
                && this.properties.get(DEVICE_CUSTOM_NAME) instanceof String) {
            displayName = (String) this.properties.get(DEVICE_CUSTOM_NAME);
        }
        // Return empty string to the server
        else if ("server".equals(deviceDisplayNameOption)) {
            displayName = "";
        }

        return displayName;
    }

    /**
     * Returns true if the current CloudService configuration
     * specifies Gzip compression enabled for outgoing payloads.
     *
     * @return a boolean value.
     */
    public boolean getEncodeGzip() {
        boolean encodeGzip = false;
        if (this.properties != null && this.properties.get(ENCODE_GZIP) instanceof Boolean) {
            encodeGzip = (Boolean) this.properties.get(ENCODE_GZIP);
        }
        return encodeGzip;
    }

    /**
     * Returns true if the current CloudService configuration
     * specifies the cloud client should republish the MQTT birth
     * certificate on GPS lock events.
     *
     * @return a boolean value.
     */
    public boolean getRepubBirthCertOnGpsLock() {
        boolean repubBirth = false;
        if (this.properties != null && this.properties.get(REPUB_BIRTH_ON_GPS_LOCK) instanceof Boolean) {
            repubBirth = (Boolean) this.properties.get(REPUB_BIRTH_ON_GPS_LOCK);
        }
        return repubBirth;
    }

    /**
     * Returns true if the current CloudService configuration
     * specifies the cloud client should republish the MQTT birth
     * certificate on modem detection events.
     *
     * @return a boolean value.
     */
    public boolean getRepubBirthCertOnModemDetection() {
        boolean repubBirth = false;
        if (this.properties != null && this.properties.get(REPUB_BIRTH_ON_MODEM_DETECT) instanceof Boolean) {
            repubBirth = (Boolean) this.properties.get(REPUB_BIRTH_ON_MODEM_DETECT);
        }
        return repubBirth;
    }

    public boolean getRepubBirthCertOnTamperEvent() {
        boolean repubBirth = true;
        if (this.properties != null && this.properties.get(REPUB_BIRTH_ON_TAMPER_EVENT) instanceof Boolean) {
            repubBirth = (Boolean) this.properties.get(REPUB_BIRTH_ON_TAMPER_EVENT);
        }
        return repubBirth;
    }

    /**
     * Returns the prefix to be used when publishing messages to control topics.
     *
     * @return a String value.
     */
    public String getTopicControlPrefix() {
        String prefix = TOPIC_CONTROL_PREFIX_DEFAULT;
        if (this.properties != null && this.properties.get(TOPIC_CONTROL_PREFIX) instanceof String) {
            prefix = (String) this.properties.get(TOPIC_CONTROL_PREFIX);
        }
        return prefix;
    }

    /**
     * Returns true if the current CloudService configuration
     * specifies that the cloud client should perform default subscriptions.
     *
     * @return a boolean value.
     */
    public boolean getEnableDefaultSubscriptions() {
        boolean enable = true;
        if (this.properties != null && this.properties.get(ENABLE_DFLT_SUBSCRIPTIONS) instanceof Boolean) {
            enable = (Boolean) this.properties.get(ENABLE_DFLT_SUBSCRIPTIONS);
        }
        return enable;
    }

    /**
     * This method parses the Cloud Service configuration and returns the selected cloud payload encoding.
     * By default, this method returns {@link CloudPayloadEncoding} {@code KURA_PROTOBUF}.
     *
     * @return a boolean value.
     */
    public CloudPayloadEncoding getPayloadEncoding() {
        CloudPayloadEncoding result = CloudPayloadEncoding.KURA_PROTOBUF;
        String encodingString = "";
        if (this.properties != null && this.properties.get(PAYLOAD_ENCODING) != null
                && this.properties.get(PAYLOAD_ENCODING) instanceof String) {
            encodingString = (String) this.properties.get(PAYLOAD_ENCODING);
        }
        try {
            result = CloudPayloadEncoding.getEncoding(encodingString);
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot parse the provided payload encoding.", e);
        }

        return result;
    }

    public static String getTopicSeparator() {
        return TOPIC_SEPARATOR;
    }

    public static String getTopicAccountToken() {
        return TOPIC_ACCOUNT_TOKEN;
    }

    public static String getTopicClientIdToken() {
        return TOPIC_CLIENT_ID_TOKEN;
    }

    public static String getTopicBirthSuffix() {
        return TOPIC_BIRTH_SUFFIX;
    }

    public static String getTopicDisconnectSuffix() {
        return TOPIC_DISCONNECT_SUFFIX;
    }

    public static String getTopicAppsSuffix() {
        return TOPIC_APPS_SUFFIX;
    }

    public static String getTopicWildCard() {
        return TOPIC_WILD_CARD;
    }

    public static int getLifeCycleMessagePriority() {
        return LIFECYCLE_PRIORITY;
    }

    public static boolean getLifeCycleMessageRetain() {
        return LIFECYCLE_RETAIN;
    }
}
