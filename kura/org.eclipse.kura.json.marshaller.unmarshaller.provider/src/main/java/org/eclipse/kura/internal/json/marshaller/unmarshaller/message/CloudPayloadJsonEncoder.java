/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller.message;

import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.BODY;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.METRICS;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.POSITION;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.SENTON;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.ALTITUDE;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.HEADING;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.LATITUDE;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.LONGITUDE;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.PRECISION;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.SATELLITES;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.SPEED;
import static org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonFields.CloudPayloadJsonPositionFields.STATUS;

import java.util.Base64;
import java.util.Date;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * This class provides a set of methods that allow to encode the {@link KuraPayload} into a byte[] message.
 *
 */
public class CloudPayloadJsonEncoder {

    private static final Logger logger = LoggerFactory.getLogger(CloudPayloadJsonEncoder.class);

    private CloudPayloadJsonEncoder() {
    }

    /**
     * This static method takes a {@link KuraPayload} and converts it into a {@code String}
     *
     * @param kuraPayload
     *            a {@link KuraPayload} object that has to be converted.
     * @return a String that maps the received {@link KuraPayload} object
     * @throws IllegalArgumentException
     *             if the conversion fails
     */
    public static String marshal(KuraPayload kuraPayload) {
        JsonObject json = Json.object();

        encodeTimestamp(kuraPayload, json);

        encodePosition(kuraPayload, json);

        encodeMetrics(kuraPayload, json);

        encodeBody(kuraPayload, json);

        return json.toString();
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
                encodeDoubleMetric(jsonMetrics, name, object);
            } else if (object instanceof Float) {
                encodeFloatMetric(jsonMetrics, name, object);
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

    private static void encodeFloatMetric(JsonObject jsonMetrics, String name, Object object) {
        encodeFloatProperty(jsonMetrics, name, object, "discarding non finite float metric: {}={}");
    }

    private static void encodeDoubleMetric(JsonObject jsonMetrics, String name, Object object) {
        encodeDoubleProperty(jsonMetrics, name, object, "discarding non finite double metric: {}={}");
    }

    private static void encodePosition(KuraPayload kuraPayload, JsonObject json) {
        KuraPosition position = kuraPayload.getPosition();

        if (position == null) {
            return;
        }

        JsonObject jsonPosition = Json.object();

        encodePositionDouble(jsonPosition, LATITUDE.value(), position.getLatitude());

        encodePositionDouble(jsonPosition, LONGITUDE.value(), position.getLongitude());

        encodePositionDouble(jsonPosition, ALTITUDE.value(), position.getAltitude());

        encodePositionDouble(jsonPosition, HEADING.value(), position.getHeading());

        encodePositionDouble(jsonPosition, PRECISION.value(), position.getPrecision());

        if (position.getSatellites() != null) {
            jsonPosition.add(SATELLITES.value(), position.getSatellites());
        }

        encodePositionDouble(jsonPosition, SPEED.value(), position.getSpeed());

        if (position.getTimestamp() != null) {
            jsonPosition.add(CloudPayloadJsonFields.CloudPayloadJsonPositionFields.TIMESTAMP.value(),
                    position.getTimestamp().getTime());
        }
        if (position.getStatus() != null) {
            jsonPosition.add(STATUS.value(), position.getStatus());
        }

        json.add(POSITION.value(), jsonPosition);
    }

    private static void encodePositionDouble(final JsonObject object, final String metric, final Double value) {
        if (value != null) {
            encodeDoubleProperty(object, metric, value, "discarding non finite double metric: position.{}={}");
        }
    }

    private static void encodeTimestamp(KuraPayload kuraPayload, JsonObject json) {
        Date timestamp = kuraPayload.getTimestamp();
        if (timestamp != null) {
            json.add(SENTON.value(), timestamp.getTime());
        }
    }

    private static void encodeFloatProperty(JsonObject object, String name, Object value, String errorMessageFormat) {
        final Float floatValue = (Float) value;

        if (Float.isFinite(floatValue)) {
            object.add(name, floatValue);

        } else {
            logger.warn(errorMessageFormat, name, floatValue);
        }
    }

    private static void encodeDoubleProperty(JsonObject object, String name, Object value, String errorMessageFormat) {
        final Double doubleValue = (Double) value;

        if (Double.isFinite(doubleValue)) {
            object.add(name, doubleValue);

        } else {
            logger.warn(errorMessageFormat, name, doubleValue);
        }
    }
}