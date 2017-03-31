/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.cloud;

import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.BODY;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.POSITION;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.TIMESTAMP;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.ALTITUDE;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.HEADING;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.LATITUDE;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.LONGITUDE;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.PRECISION;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.SATELLITES;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.SPEED;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.STATUS;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.BOOLEAN;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.BYTEARRAY;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.CHARACTER;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.DOUBLE;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.FLOAT;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.INTEGER;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.LONG;
import static org.eclipse.kura.core.cloud.CloudPayloadJsonTypes.STRING;

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
 * This class contains all the necessary metods that can be used to decode a Json payload into a {@link KuraPayload}.
 *
 */
public class CloudPayloadJsonDecoder {

    private static final Logger logger = LoggerFactory.getLogger(CloudPayloadJsonDecoder.class);

    private static final String JSON_METRIC_TYPE = "type";
    private static final String JSON_METRIC_VALUE = "value";

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
                if (TIMESTAMP.name().equalsIgnoreCase(name)) {
                    decodeTimestamp(payload, value);
                } else if (BODY.name().equalsIgnoreCase(name)) {
                    decodeBody(payload, value);
                } else if (POSITION.name().equalsIgnoreCase(name) && value.isObject()) {
                    decodePosition(payload, value.asObject());
                } else {
                    decodeMetric(payload, name, value);
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
            if (LATITUDE.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setLatitude(value.asDouble());
            } else if (LONGITUDE.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setLongitude(value.asDouble());
            } else if (ALTITUDE.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setAltitude(value.asDouble());
            } else if (HEADING.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setHeading(value.asDouble());
            } else if (PRECISION.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setPrecision(value.asDouble());
            } else if (SATELLITES.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setSatellites(value.asInt());
            } else if (SPEED.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setSpeed(value.asDouble());
            } else if (CloudPayloadJsonFields.CloudPayloadJsonPositionFields.TIMESTAMP.name().equalsIgnoreCase(name)
                    && value.isNumber()) {
                position.setTimestamp(new Date(value.asLong()));
            } else if (STATUS.name().equalsIgnoreCase(name) && value.isNumber()) {
                position.setStatus(value.asInt());
            } else {
                throw new IllegalArgumentException("Cannot parse position!");
            }
        }
    }

    private static void decodeMetric(KuraPayload payload, String name, JsonValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot parse metric!");
        }

        JsonObject valueObject = value.asObject();
        String jsonMetricType = valueObject.get(JSON_METRIC_TYPE).asString();
        JsonValue jsonMetricValue = valueObject.get(JSON_METRIC_VALUE);

        if (BOOLEAN.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, jsonMetricValue.asBoolean());
        } else if (DOUBLE.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, jsonMetricValue.asDouble());
        } else if (FLOAT.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, jsonMetricValue.asFloat());
        } else if (INTEGER.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, jsonMetricValue.asInt());
        } else if (LONG.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, jsonMetricValue.asLong());
        } else if (STRING.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, jsonMetricValue.asString());
        } else if (CHARACTER.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, (char) jsonMetricValue.asInt());
        } else if (BYTEARRAY.name().equalsIgnoreCase(jsonMetricType)) {
            payload.addMetric(name, Base64.getDecoder().decode(jsonMetricValue.asString()));
        }
    }
}
