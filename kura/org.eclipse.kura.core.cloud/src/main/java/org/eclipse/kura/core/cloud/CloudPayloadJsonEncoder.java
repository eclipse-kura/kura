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

import java.util.Base64;
import java.util.Date;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class CloudPayloadJsonEncoder {

    private CloudPayloadJsonEncoder() {
    }

    public static JsonObject toJson(KuraPayload kuraPayload) {
        JsonObject json = Json.object();

        encodeTimestamp(kuraPayload, json);

        encodePosition(kuraPayload, json);

        encodeMetrics(kuraPayload, json);

        encodeBody(kuraPayload, json);

        return json;
    }

    private static void encodeBody(KuraPayload kuraPayload, JsonObject json) {
        byte[] body = kuraPayload.getBody();
        if (body != null) {
            json.add("body", Base64.getEncoder().encodeToString(body));
        }
    }

    private static void encodeMetrics(KuraPayload kuraPayload, JsonObject json) {
        for (String name : kuraPayload.metricNames()) {
            Object object = kuraPayload.getMetric(name);
            if (object instanceof Boolean) {
                json.add(name, (Boolean) object);
            } else if (object instanceof Double) {
                json.add(name, (Double) object);
            } else if (object instanceof Float) {
                json.add(name, (Float) object);
            } else if (object instanceof Integer) {
                json.add(name, (Integer) object);
            } else if (object instanceof Long) {
                json.add(name, (Long) object);
            } else if (object instanceof String) {
                json.add(name, (String) object);
            } else if (object instanceof Character) {
                json.add(name, (Character) object);
            } else if (object instanceof Byte) {
                json.add(name, (Byte) object);
            } else if (object instanceof Short) {
                json.add(name, (Short) object);
            } else if (object instanceof byte[]) {
                json.add(name, Base64.getEncoder().encodeToString((byte[]) object));
            } else {
                json.add(name, object.toString());
            }
        }
    }

    private static void encodePosition(KuraPayload kuraPayload, JsonObject json) {
        KuraPosition position = kuraPayload.getPosition();
        if (position != null) {

            JsonObject jsonPosition = Json.object();
            if (position.getLatitude() != null) {
                jsonPosition.add("lat", position.getLatitude());
            }
            if (position.getLongitude() != null) {
                jsonPosition.add("lat", position.getLongitude());
            }
            if (position.getAltitude() != null) {
                jsonPosition.add("alt", position.getAltitude());
            }
            if (position.getHeading() != null) {
                jsonPosition.add("head", position.getHeading());
            }
            if (position.getPrecision() != null) {
                jsonPosition.add("prec", position.getPrecision());
            }
            if (position.getSatellites() != null) {
                jsonPosition.add("sat", position.getSatellites());
            }
            if (position.getSpeed() != null) {
                jsonPosition.add("speed", position.getSpeed());
            }
            if (position.getTimestamp() != null) {
                jsonPosition.add("ts", position.getTimestamp().getTime());
            }
            if (position.getStatus() != null) {
                jsonPosition.add("status", position.getStatus());
            }

            json.add("pos", jsonPosition);
        }
    }

    private static void encodeTimestamp(KuraPayload kuraPayload, JsonObject json) {
        Date timestamp = kuraPayload.getTimestamp();
        if (timestamp != null) {
            json.add("ts", timestamp.getTime());
        }
    }
}