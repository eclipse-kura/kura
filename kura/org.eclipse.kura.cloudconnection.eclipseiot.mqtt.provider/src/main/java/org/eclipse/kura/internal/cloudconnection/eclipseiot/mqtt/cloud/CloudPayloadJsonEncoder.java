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
            json.add(BODY.value(), Base64.getEncoder().encodeToString(body));
        }
    }

    private static void encodeMetrics(KuraPayload kuraPayload, JsonObject json) {
        JsonObject jsonMetrics = Json.object();
        for (String name : kuraPayload.metricNames()) {
            Object object = kuraPayload.getMetric(name);
            if (object instanceof Boolean) {
                jsonMetrics.add(name, (Boolean) object);
            } else if (object instanceof Double) {
                jsonMetrics.add(name, (Double) object);
            } else if (object instanceof Float) {
                jsonMetrics.add(name, (Float) object);
            } else if (object instanceof Integer) {
                jsonMetrics.add(name, (Integer) object);
            } else if (object instanceof Long) {
                jsonMetrics.add(name, (Long) object);
            } else if (object instanceof String) {
                jsonMetrics.add(name, (String) object);
            } else if (object instanceof byte[]) {
                jsonMetrics.add(name, Base64.getEncoder().encodeToString((byte[]) object));
            } else {
                throw new IllegalArgumentException("Cannot encode this value: " + object.toString());
            }
        }
        json.add(METRICS.value(), jsonMetrics);
    }

    private static void encodePosition(KuraPayload kuraPayload, JsonObject json) {
        KuraPosition position = kuraPayload.getPosition();
        if (position != null) {

            JsonObject jsonPosition = Json.object();
            if (position.getLatitude() != null) {
                jsonPosition.add(LATITUDE.value(), position.getLatitude());
            }
            if (position.getLongitude() != null) {
                jsonPosition.add(LONGITUDE.value(), position.getLongitude());
            }
            if (position.getAltitude() != null) {
                jsonPosition.add(ALTITUDE.value(), position.getAltitude());
            }
            if (position.getHeading() != null) {
                jsonPosition.add(HEADING.value(), position.getHeading());
            }
            if (position.getPrecision() != null) {
                jsonPosition.add(PRECISION.value(), position.getPrecision());
            }
            if (position.getSatellites() != null) {
                jsonPosition.add(SATELLITES.value(), position.getSatellites());
            }
            if (position.getSpeed() != null) {
                jsonPosition.add(SPEED.value(), position.getSpeed());
            }
            if (position.getTimestamp() != null) {
                jsonPosition.add(CloudPayloadJsonFields.CloudPayloadJsonPositionFields.TIMESTAMP.value(),
                        position.getTimestamp().getTime());
            }
            if (position.getStatus() != null) {
                jsonPosition.add(STATUS.value(), position.getStatus());
            }

            json.add(POSITION.value(), jsonPosition);
        }
    }

    private static void encodeTimestamp(KuraPayload kuraPayload, JsonObject json) {
        Date timestamp = kuraPayload.getTimestamp();
        if (timestamp != null) {
            json.add(SENTON.value(), timestamp.getTime());
        }
    }
}