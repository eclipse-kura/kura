/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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

import java.nio.charset.StandardCharsets;
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
     * Builds a {@link KuraPayload} from a Json string. The method will try to parse the received Json in order to
     * fill the corresponding {@link KuraPayload} fields.
     * If the mapping fails, the entire string, received as argument, will be placed in the body of the returned
     * {@link KuraPayload}.
     *
     * @param stringJson
     *            a Json encoded as a String.
     * @return a {@link KuraPayload} that directly maps the received array.
     */
    public static KuraPayload buildFromString(String stringJson) {
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
            payload.setBody(stringJson.getBytes(StandardCharsets.UTF_8));
        }
        return payload;
    }

    private static void decodeTimestamp(KuraPayload payload, JsonValue timestampValue) {
        if (timestampValue != null) {
            String timestampString = "";
            if (timestampValue.isNumber()) {
                timestampString = timestampValue.toString();
            } else if (timestampValue.isObject()) {
                timestampString = getTypedValueString(timestampValue);
            }
            if (!"".equals(timestampString)) {
                payload.setTimestamp(new Date(Long.parseLong(timestampString)));
            }
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
            String valueString = "";
            if (value.isNumber()) {
                valueString = value.toString();
            } else if (value.isObject()) {
                valueString = getTypedValueString(value);
            } else {
                throw new IllegalArgumentException(String.format("Cannot parse position: %s.", name));
            }

            if (LATITUDE.value().equalsIgnoreCase(name)) {
                position.setLatitude(Double.parseDouble(valueString));
            } else if (LONGITUDE.value().equalsIgnoreCase(name)) {
                position.setLongitude(Double.parseDouble(valueString));
            } else if (ALTITUDE.value().equalsIgnoreCase(name)) {
                position.setAltitude(Double.parseDouble(valueString));
            } else if (HEADING.value().equalsIgnoreCase(name)) {
                position.setHeading(Double.parseDouble(valueString));
            } else if (PRECISION.value().equalsIgnoreCase(name)) {
                position.setPrecision(Double.parseDouble(valueString));
            } else if (SATELLITES.value().equalsIgnoreCase(name)) {
                position.setSatellites(Integer.parseInt(valueString));
            } else if (SPEED.value().equalsIgnoreCase(name)) {
                position.setSpeed(Integer.parseInt(valueString));
            } else if (CloudPayloadJsonFields.CloudPayloadJsonPositionFields.TIMESTAMP.value().equalsIgnoreCase(name)) {
                position.setTimestamp(new Date(Long.parseLong(valueString)));
            } else if (STATUS.value().equalsIgnoreCase(name)) {
                position.setStatus(Integer.parseInt(valueString));
            } else {
                throw new IllegalArgumentException(String.format("Cannot parse position: %s.", name));
            }
        }
    }

    private static void decodeMetric(KuraPayload payload, JsonObject metricsObject) {
        if (metricsObject == null) {
            throw new IllegalArgumentException("Cannot parse metric object!");
        }

        for (JsonObject.Member member : metricsObject) {
            String name = member.getName();
            JsonValue value = member.getValue();

            Object javaValue = null;
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
            } else if (value.isObject()) {
                javaValue = getTypedValue(value);
            }
            if (javaValue == null) {
                throw new IllegalArgumentException(String.format("Unparsable metric %s", name));
            }
            payload.addMetric(name, javaValue);
        }
    }

    private static Object getTypedValue(JsonValue value) {
        Object javaValue = null;
        if (!value.isObject()) {
            throw new IllegalArgumentException(String.format("metric typed object %s is incorrect!", value));
        }
        JsonObject.Member member = value.asObject().iterator().next();
        String name = member.getName();
        JsonValue value0 = member.getValue();
        if (name.equalsIgnoreCase("string")) {
            javaValue = value0.asString();
        } else if (name.equalsIgnoreCase("double")) {
            javaValue = value0.asDouble();
        } else if (name.equalsIgnoreCase("float")) {
            javaValue = value0.asFloat();
        } else if (name.equalsIgnoreCase("int") || name.equalsIgnoreCase("int32") || name.equalsIgnoreCase("int64")) {
            javaValue = value0.asInt();
        } else if (name.equalsIgnoreCase("bool")) {
            javaValue = value0.asBoolean();
        } else if (name.equalsIgnoreCase("long")) {
            javaValue = value0.asLong();
        } else if (name.equalsIgnoreCase("bytes")) {
            javaValue = Base64.getDecoder().decode(value0.asString());
        } else {
            throw new IllegalArgumentException(String.format("metric typed object %s is incorrect!", value));
        }
        return javaValue;
    }

    private static String getTypedValueString(JsonValue value) {
        if (!value.isObject()) {
            throw new IllegalArgumentException(String.format("metric typed object %s is incorrect!", value));
        }
        JsonObject.Member member = value.asObject().iterator().next();
        JsonValue value0 = member.getValue();
        return value0.toString();
    }
}
