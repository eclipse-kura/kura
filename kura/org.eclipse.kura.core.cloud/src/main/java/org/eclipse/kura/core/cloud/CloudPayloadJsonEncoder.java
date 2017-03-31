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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * This class provides a set of methods that allow to encode the {@link KuraPayload} into a byte[] message.
 *
 */
public class CloudPayloadJsonEncoder {

    private static final String JSON_METRIC_VALUE = "value";
    private static final String JSON_METRIC_TYPE = "type";

    private CloudPayloadJsonEncoder() {
    }

    /**
     * This static method takes a {@link KuraPayload} and converts it into a {@code byte[]}
     *
     * @param kuraPayload
     *            a {@link KuraPayload} object that has to be converted.
     * @return a byte[] that maps the received {@link KuraPayload} object
     * @throws IllegalArgumentException
     *             if the conversion fails
     */
    public static byte[] getBytes(KuraPayload kuraPayload) {
        JsonObject json = Json.object();

        encodeTimestamp(kuraPayload, json);

        encodePosition(kuraPayload, json);

        encodeMetrics(kuraPayload, json);

        encodeBody(kuraPayload, json);

        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void encodeBody(KuraPayload kuraPayload, JsonObject json) {
        byte[] body = kuraPayload.getBody();
        if (body != null) {
            json.add(BODY.name(), Base64.getEncoder().encodeToString(body));
        }
    }

    private static void encodeMetrics(KuraPayload kuraPayload, JsonObject json) {
        for (String name : kuraPayload.metricNames()) {
            Object object = kuraPayload.getMetric(name);
            if (object instanceof Boolean) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, BOOLEAN.name());
                valueObject.add(JSON_METRIC_VALUE, (Boolean) object);
                json.add(name, valueObject);
            } else if (object instanceof Double) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, DOUBLE.name());
                valueObject.add(JSON_METRIC_VALUE, (Double) object);
                json.add(name, valueObject);
            } else if (object instanceof Float) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, FLOAT.name());
                valueObject.add(JSON_METRIC_VALUE, (Float) object);
                json.add(name, valueObject);
            } else if (object instanceof Integer) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, INTEGER.name());
                valueObject.add(JSON_METRIC_VALUE, (Integer) object);
                json.add(name, valueObject);
            } else if (object instanceof Long) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, LONG.name());
                valueObject.add(JSON_METRIC_VALUE, (Long) object);
                json.add(name, valueObject);
            } else if (object instanceof String) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, STRING.name());
                valueObject.add(JSON_METRIC_VALUE, (String) object);
                json.add(name, valueObject);
            } else if (object instanceof Character) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, CHARACTER.name());
                valueObject.add(JSON_METRIC_VALUE, (Character) object);
                json.add(name, valueObject);
            } else if (object instanceof byte[]) {
                JsonObject valueObject = Json.object();
                valueObject.add(JSON_METRIC_TYPE, BYTEARRAY.name());
                valueObject.add(JSON_METRIC_VALUE, Base64.getEncoder().encodeToString((byte[]) object));
                json.add(name, valueObject);
            } else {
                throw new IllegalArgumentException("Cannot encode this value: " + object.toString());
            }
        }
    }

    private static void encodePosition(KuraPayload kuraPayload, JsonObject json) {
        KuraPosition position = kuraPayload.getPosition();
        if (position != null) {

            JsonObject jsonPosition = Json.object();
            if (position.getLatitude() != null) {
                jsonPosition.add(LATITUDE.name(), position.getLatitude());
            }
            if (position.getLongitude() != null) {
                jsonPosition.add(LONGITUDE.name(), position.getLongitude());
            }
            if (position.getAltitude() != null) {
                jsonPosition.add(ALTITUDE.name(), position.getAltitude());
            }
            if (position.getHeading() != null) {
                jsonPosition.add(HEADING.name(), position.getHeading());
            }
            if (position.getPrecision() != null) {
                jsonPosition.add(PRECISION.name(), position.getPrecision());
            }
            if (position.getSatellites() != null) {
                jsonPosition.add(SATELLITES.name(), position.getSatellites());
            }
            if (position.getSpeed() != null) {
                jsonPosition.add(SPEED.name(), position.getSpeed());
            }
            if (position.getTimestamp() != null) {
                jsonPosition.add(CloudPayloadJsonFields.CloudPayloadJsonPositionFields.TIMESTAMP.name(),
                        position.getTimestamp().getTime());
            }
            if (position.getStatus() != null) {
                jsonPosition.add(STATUS.name(), position.getStatus());
            }

            json.add(POSITION.name(), jsonPosition);
        }
    }

    private static void encodeTimestamp(KuraPayload kuraPayload, JsonObject json) {
        Date timestamp = kuraPayload.getTimestamp();
        if (timestamp != null) {
            json.add(TIMESTAMP.name(), timestamp.getTime());
        }
    }
}