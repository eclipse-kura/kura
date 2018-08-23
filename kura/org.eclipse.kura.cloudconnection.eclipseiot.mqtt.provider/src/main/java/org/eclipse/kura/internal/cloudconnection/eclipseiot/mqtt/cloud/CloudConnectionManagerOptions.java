/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudServiceLifecycleCertsPolicy.DISABLE_PUBLISHING;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudServiceLifecycleCertsPolicy.PUBLISH_BIRTH_CONNECT_RECONNECT;

import java.util.Map;

import org.eclipse.kura.cloud.CloudPayloadEncoding;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionManagerOptions {

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionManagerOptions.class);

    private static final String TOPIC_SEPARATOR = "/";
    private static final String TOPIC_ACCOUNT_TOKEN = "#account-name";
    private static final String TOPIC_CLIENT_ID_TOKEN = "#client-id";
    private static final String TOPIC_BIRTH_SUFFIX = "MQTT/BIRTH";
    private static final String TOPIC_DISCONNECT_SUFFIX = "MQTT/DC";
    private static final String TOPIC_APPS_SUFFIX = "MQTT/APPS";
    private static final String TOPIC_WILD_CARD = "#";

    private static final String DEVICE_DISPLAY_NAME = "device.display-name";
    private static final String DEVICE_CUSTOM_NAME = "device.custom-name";
    private static final String ENCODE_GZIP = "encode.gzip";
    private static final String REPUB_BIRTH_ON_GPS_LOCK = "republish.mqtt.birth.cert.on.gps.lock";
    private static final String REPUB_BIRTH_ON_MODEM_DETECT = "republish.mqtt.birth.cert.on.modem.detect";
    private static final String BIRTH_CERT_POLICY = "birth.cert.policy";
    private static final String PAYLOAD_ENCODING = "payload.encoding";

    private static final int LIFECYCLE_QOS = 1;
    private static final int LIFECYCLE_PRIORITY = 0;
    private static final boolean LIFECYCLE_RETAIN = false;

    private final Map<String, Object> properties;
    private final SystemService systemService;

    CloudConnectionManagerOptions(Map<String, Object> properties, SystemService systemService) {
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
        if (this.properties != null && this.properties.get(ENCODE_GZIP) != null
                && this.properties.get(ENCODE_GZIP) instanceof Boolean) {
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
        if (this.properties != null && this.properties.get(REPUB_BIRTH_ON_GPS_LOCK) != null
                && this.properties.get(REPUB_BIRTH_ON_GPS_LOCK) instanceof Boolean) {
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
        if (this.properties != null && this.properties.get(REPUB_BIRTH_ON_MODEM_DETECT) != null
                && this.properties.get(REPUB_BIRTH_ON_MODEM_DETECT) instanceof Boolean) {
            repubBirth = (Boolean) this.properties.get(REPUB_BIRTH_ON_MODEM_DETECT);
        }
        return repubBirth;
    }

    /**
     * This method parses the Cloud Service configuration and returns true if the Cloud Service instance should
     * not publish lifecycle messages.
     *
     * @return a boolean value.
     */
    public boolean isLifecycleCertsDisabled() {
        boolean birthPubDisabled = false;
        String birthPubPolicy = "";
        if (this.properties != null && this.properties.get(BIRTH_CERT_POLICY) != null
                && this.properties.get(BIRTH_CERT_POLICY) instanceof String) {
            birthPubPolicy = (String) this.properties.get(BIRTH_CERT_POLICY);
        }
        if (DISABLE_PUBLISHING.getValue().equals(birthPubPolicy)) {
            birthPubDisabled = true;
        }
        return birthPubDisabled;
    }

    /**
     * This method parses the Cloud Service configuration and returns true if the Cloud Service instance should
     * republish the birth message on a reconnection.
     * By default, this method returns true.
     *
     * @return a boolean value.
     */
    public boolean getRepubBirthCertOnReconnect() {
        boolean republishBirt = true;
        String birthPubPolicy = "";
        if (this.properties != null && this.properties.get(BIRTH_CERT_POLICY) != null
                && this.properties.get(BIRTH_CERT_POLICY) instanceof String) {
            birthPubPolicy = (String) this.properties.get(BIRTH_CERT_POLICY);
        }
        if (!PUBLISH_BIRTH_CONNECT_RECONNECT.getValue().equals(birthPubPolicy)) {
            republishBirt = false;
        }
        return republishBirt;
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

    public String getTopicSeparator() {
        return TOPIC_SEPARATOR;
    }

    public String getTopicAccountToken() {
        return TOPIC_ACCOUNT_TOKEN;
    }

    public String getTopicClientIdToken() {
        return TOPIC_CLIENT_ID_TOKEN;
    }

    public String getTopicBirthSuffix() {
        return TOPIC_BIRTH_SUFFIX;
    }

    public String getTopicDisconnectSuffix() {
        return TOPIC_DISCONNECT_SUFFIX;
    }

    public String getTopicAppsSuffix() {
        return TOPIC_APPS_SUFFIX;
    }

    public String getTopicWildCard() {
        return TOPIC_WILD_CARD;
    }

    public int getLifeCycleMessageQos() {
        return LIFECYCLE_QOS;
    }

    public int getLifeCycleMessagePriority() {
        return LIFECYCLE_PRIORITY;
    }

    public boolean getLifeCycleMessageRetain() {
        return LIFECYCLE_RETAIN;
    }
}
