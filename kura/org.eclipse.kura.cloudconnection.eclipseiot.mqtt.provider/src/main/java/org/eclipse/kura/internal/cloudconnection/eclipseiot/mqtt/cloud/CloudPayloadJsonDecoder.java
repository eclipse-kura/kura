/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
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

import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.BODY;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.METRICS;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.POSITION;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.SENTON;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.ALTITUDE;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.HEADING;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.LATITUDE;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.LONGITUDE;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.PRECISION;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.SATELLITES;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.SPEED;
import static org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.STATUS;

import java.util.Base64;
import java.util.Date;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * This class contains all the necessary methods that can be used to decode a Json payload into a {@link KuraPayload}.
 *
 */
public class CloudPayloadJsonDecoder {

    private static final Logger logger = LoggerFactory.getLogger(CloudPayloadJsonDecoder.class);

    private CloudPayloadJsonDecoder() {
    }

    /**
     * Builds a {@link KuraPayload} from a Json byte array. The method will try to parse the received Json in order to
     * fill the corresponding {@link KuraPayload} fields.
     * If the mapping fails, the entire byte array, received as argument, will be placed in the body of the returned
     * {@link KuraPayload}.
     *
     * @param array
     *            a Json encoded as a byte array.
     * @return a {@link KuraPayload} that directly maps the received array.
     */
    public static KuraPayload buildFromByteArray(byte[] array) {
        String stringJson = new String(array);
        JsonObject json = Json.parse(stringJson).asObject();

        KuraPayload payload = new KuraPayload();

        try {
            for (JsonObject.Member member : json) {
                String name = member.getName();
                JsonValue value = member.getValue();
                if (SENTON.value().equalsIgnoreCase(name)) {
                    decodeTimestamp(payload, value);
                } else if (BODY.value().equalsIgnoreCase(name)) {
                    decodeBody(payload, value);
                } else if (POSITION.value().equalsIgnoreCase(name) && value.isObject()) {
                    decodePosition(payload, value.asObject());
                } else if (METRICS.value().equalsIgnoreCase(name) && value.isObject()) {
                    decodeMetric(payload, value.asObject());
                } else {
                    throw new IllegalArgumentException(String.format("Unrecognized value: %s", name));
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot parse Json", e);
            payload = new KuraPayload();
            payload.setBody(array);
        }
        return payload;
    }

    private static void decodeTimestamp(KuraPayload payload, JsonValue timestampValue) {
        if (timestampValue != null && timestampValue.isNumber()) {
            long timestamp = timestampValue.asLong();
            payload.setTimestamp(new Date(timestamp));
        }
    }

    private static void decodeBody(KuraPayload payload, JsonValue body) {
        if (body != null && body.isString()) {
            payload.setBody(Base64.getDecoder().decode(body.asString()));
        }
    }

    private static void decodePosition(KuraPayload payload, JsonObject positionObject) {
        KuraPosition position = new KuraPosition();

        payload.setPosition(position);
        for (JsonObject.Member member : positionObject) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (LATITUDE.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setLatitude(value.asDouble());
            } else if (LONGITUDE.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setLongitude(value.asDouble());
            } else if (ALTITUDE.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setAltitude(value.asDouble());
            } else if (HEADING.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setHeading(value.asDouble());
            } else if (PRECISION.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setPrecision(value.asDouble());
            } else if (SATELLITES.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setSatellites(value.asInt());
            } else if (SPEED.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setSpeed(value.asDouble());
            } else if (CloudPayloadJsonFields.CloudPayloadJsonPositionFields.TIMESTAMP.value().equalsIgnoreCase(name)
                    && value.isNumber()) {
                position.setTimestamp(new Date(value.asLong()));
            } else if (STATUS.value().equalsIgnoreCase(name) && value.isNumber()) {
                position.setStatus(value.asInt());
            } else {
                throw new IllegalArgumentException(String.format("Cannot parse position: %s.", name));
            }
        }
    }

    // FIXME doesn't properly decode characters, ints, floats and byte arrays - the supported format has no metadata
    private static void decodeMetric(KuraPayload payload, JsonObject metricsObject) {
        if (metricsObject == null) {
            throw new IllegalArgumentException("Cannot parse metric object!");
        }

        for (JsonObject.Member member : metricsObject) {
            String name = member.getName();
            JsonValue value = member.getValue();

            Object javaValue;
            if (value.isNumber()) {
                try {
                    javaValue = value.asLong();
                } catch (Exception e) {
                    javaValue = value.asDouble();
                }
            } else if (value.isBoolean()) {
                javaValue = value.asBoolean();
            } else if (value.isString()) {
                javaValue = value.asString();
            } else {
                throw new IllegalArgumentException(String.format("Unparsable metric %s", name));
            }
            payload.addMetric(name, javaValue);
        }
    }
}
