/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.message;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Metric;

import com.google.protobuf.InvalidProtocolBufferException;

public class SparkplugPayloads {

    public static final String NODE_CONTROL_REBIRTH_METRIC_NAME = "Node Control/Rebirth";

    private SparkplugPayloads() {
    }

    public static byte[] getNodeDeathPayload(long bdSeq) {
        return new SparkplugBProtobufPayloadBuilder().withBdSeq(bdSeq, new Date().getTime()).build();
    }

    public static byte[] getNodeBirthPayload(long bdSeq, long seq) {
        long timestamp = new Date().getTime();

        SparkplugBProtobufPayloadBuilder payloadBuilder = new SparkplugBProtobufPayloadBuilder();
        payloadBuilder.withBdSeq(bdSeq, timestamp);
        payloadBuilder.withMetric(NODE_CONTROL_REBIRTH_METRIC_NAME, false, DataType.Boolean, timestamp);
        payloadBuilder.withSeq(seq);
        payloadBuilder.withTimestamp(timestamp);

        return payloadBuilder.build();
    }

    public static boolean getBooleanMetric(String metricName, byte[] rawPayload)
            throws InvalidProtocolBufferException, NoSuchFieldException {
        Payload payload = Payload.parseFrom(rawPayload);
        for (Metric metric : payload.getMetricsList()) {
            if (metric.getName().equals(metricName)) {
                return metric.getBooleanValue();
            }
        }
        
        throw new NoSuchFieldException("Metric " + metricName + " not found in payload");
    }

    public static byte[] getSparkplugDevicePayload(final long seq, final KuraPayload kuraPayload) {
        SparkplugBProtobufPayloadBuilder payloadBuilder = new SparkplugBProtobufPayloadBuilder();

        byte[] payloadBody = kuraPayload.getBody();
        if (Objects.nonNull(payloadBody)) {
            payloadBuilder.withBody(payloadBody);
        }

        Date kuraTimestamp = kuraPayload.getTimestamp();
        long timestamp = Objects.nonNull(kuraTimestamp) ? kuraTimestamp.getTime() : new Date().getTime();
        payloadBuilder.withTimestamp(timestamp);

        for (Entry<String, Object> metric : kuraPayload.metrics().entrySet()) {
            payloadBuilder.withMetric(metric.getKey(), metric.getValue(), timestamp);
        }

        KuraPosition position = kuraPayload.getPosition();
        if (Objects.nonNull(position)) {
            addMetricIfNonNull(payloadBuilder, "kura.position.altitude", position.getAltitude(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.latitude", position.getLatitude(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.longitude", position.getLongitude(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.heading", position.getHeading(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.precision", position.getPrecision(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.satellites", position.getSatellites(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.speed", position.getSpeed(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.status", position.getStatus(), timestamp);
            addMetricIfNonNull(payloadBuilder, "kura.position.timestamp", position.getTimestamp(), timestamp);
        }

        payloadBuilder.withSeq(seq);
        payloadBuilder.withTimestamp(timestamp);

        return payloadBuilder.build();
    }

    private static void addMetricIfNonNull(SparkplugBProtobufPayloadBuilder payloadBuilder, String name, Object value,
            long timestamp) {
        if (Objects.nonNull(value)) {
            payloadBuilder.withMetric(name, value, timestamp);
        }
    }

}
