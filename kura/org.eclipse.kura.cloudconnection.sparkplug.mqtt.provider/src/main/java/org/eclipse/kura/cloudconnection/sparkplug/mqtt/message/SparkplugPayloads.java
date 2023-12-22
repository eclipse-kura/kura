/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;

public class SparkplugPayloads {

    private SparkplugPayloads() {
    }

    public static byte[] getNodeDeathPayload(long bdSeq) {
        return new SparkplugBProtobufPayloadBuilder().withBdSeq(bdSeq, new Date().getTime()).build();
    }

    public static byte[] getNodeBirthPayload(long bdSeq, long seq) {
        long timestamp = new Date().getTime();

        Payload.Builder protoMsg = Payload.newBuilder();

        Payload.Metric.Builder bdSeqMetric = Payload.Metric.newBuilder();
        bdSeqMetric.setName("bdSeq");
        bdSeqMetric.setLongValue(bdSeq);
        bdSeqMetric.setDatatype(DataType.Int64.getNumber());
        bdSeqMetric.setTimestamp(timestamp);
        protoMsg.addMetrics(bdSeqMetric.build());

        Payload.Metric.Builder rebirthMetric = Payload.Metric.newBuilder();
        rebirthMetric.setName("Node Control/Rebirth");
        rebirthMetric.setBooleanValue(false);
        rebirthMetric.setDatatype(DataType.Boolean_VALUE);
        protoMsg.addMetrics(rebirthMetric.build());

        protoMsg.setSeq(seq);
        protoMsg.setTimestamp(timestamp);

        return protoMsg.build().toByteArray();
    }

}
