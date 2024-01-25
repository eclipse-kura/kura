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
import java.util.Map;
import java.util.Map.Entry;

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

    public static byte[] getSparkplugDevicePayload(long seq, Map<String, Object> metrics) {
        long timestamp = new Date().getTime();
        
        SparkplugBProtobufPayloadBuilder payloadBuilder = new SparkplugBProtobufPayloadBuilder();

        for (Entry<String, Object> metric : metrics.entrySet()) {
            payloadBuilder.withMetric(metric.getKey(), metric.getValue(), timestamp);
        }

        payloadBuilder.withSeq(seq);
        payloadBuilder.withTimestamp(timestamp);

        return payloadBuilder.build();
    }

}
