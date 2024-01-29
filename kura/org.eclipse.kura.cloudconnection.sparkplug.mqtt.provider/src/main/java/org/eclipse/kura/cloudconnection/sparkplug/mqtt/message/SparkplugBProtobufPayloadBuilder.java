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

import java.math.BigInteger;
import java.util.Date;

import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class SparkplugBProtobufPayloadBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugBProtobufPayloadBuilder.class);

    public static final String BDSEQ_METRIC_NAME = "bdSeq";

    private Payload.Builder payloadBuilder = Payload.newBuilder();

    public SparkplugBProtobufPayloadBuilder withMetric(String name, Object value, long timestamp) {
        DataType sparkplugDataType = DataType.Unknown;

        if (value instanceof Boolean) {
            sparkplugDataType = DataType.Boolean;
        }

        if (value instanceof byte[]) {
            sparkplugDataType = DataType.Bytes;
        }

        if (value instanceof Double) {
            sparkplugDataType = DataType.Double;
        }

        if (value instanceof Float) {
            sparkplugDataType = DataType.Float;
        }

        if (value instanceof Byte) {
            sparkplugDataType = DataType.Int8;
        }

        if (value instanceof Short) {
            sparkplugDataType = DataType.Int16;
        }

        if (value instanceof Integer) {
            sparkplugDataType = DataType.Int32;
        }

        if (value instanceof Long) {
            sparkplugDataType = DataType.Int64;
        }

        if (value instanceof String) {
            sparkplugDataType = DataType.String;
        }

        if (value instanceof Date) {
            sparkplugDataType = DataType.DateTime;
        }

        if (value instanceof BigInteger) {
            sparkplugDataType = DataType.UInt64;
        }

        logger.debug("Converting Java Type: {} to Sparkplug.DataType: {}", value.getClass().getName(),
                sparkplugDataType);

        return this.withMetric(name, value, sparkplugDataType, timestamp);
    }

    public SparkplugBProtobufPayloadBuilder withMetric(String name, Object value, DataType dataType, long timestamp) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setDatatype(dataType.getNumber());
        metricBuilder.setTimestamp(timestamp);

        switch (dataType) {
        case Boolean:
            metricBuilder.setBooleanValue((Boolean) value);
            break;
        case Bytes:
            metricBuilder.setBytesValue(ByteString.copyFrom((byte[]) value));
            break;
        case Double:
            metricBuilder.setDoubleValue((Double) value);
            break;
        case Float:
            metricBuilder.setFloatValue((Float) value);
            break;
        case Int8:
            metricBuilder.setIntValue((Byte) value);
            break;
        case Int16:
            metricBuilder.setIntValue((Short) value);
            break;
        case Int32:
            metricBuilder.setIntValue((Integer) value);
            break;
        case Int64:
            metricBuilder.setLongValue((Long) value);
            break;
        case String:
        case Text:
        case UUID:
            metricBuilder.setStringValue((String) value);
            break;
        case DateTime:
            metricBuilder.setLongValue(((Date) value).getTime());
            break;
        case UInt8:
            metricBuilder.setIntValue(Short.toUnsignedInt((Short) value));
            break;
        case UInt16:
            metricBuilder.setIntValue((int) Integer.toUnsignedLong((Integer) value));
            break;
        case UInt32:
            metricBuilder.setLongValue(Long.parseUnsignedLong(Long.toUnsignedString((Long) value)));
            break;
        case UInt64:
            metricBuilder.setLongValue(((BigInteger) value).longValue());
            break;
        case DataSet:
        case Template:
        case PropertySet:
        case PropertySetList:
        case File:
        case BooleanArray:
        case DateTimeArray:
        case UInt8Array:
        case UInt64Array:
        case UInt32Array:
        case UInt16Array:
        case StringArray:
        case Int8Array:
        case Int64Array:
        case Int32Array:
        case Int16Array:
        case FloatArray:
        case DoubleArray:
        case Unknown:
        default:
            throw new UnsupportedOperationException("DataType " + dataType.toString() + " not implemented");
        }

        this.payloadBuilder.addMetrics(metricBuilder.build());

        return this;
    }

    public SparkplugBProtobufPayloadBuilder withBdSeq(long bdSeq, long timestamp) {
        Payload.Metric.Builder bdSeqMetric = Payload.Metric.newBuilder();
        bdSeqMetric.setName(BDSEQ_METRIC_NAME);
        bdSeqMetric.setLongValue(bdSeq);
        bdSeqMetric.setDatatype(DataType.Int64.getNumber());
        bdSeqMetric.setTimestamp(timestamp);

        this.payloadBuilder.addMetrics(bdSeqMetric.build());
        return this;
    }

    public SparkplugBProtobufPayloadBuilder withSeq(long seq) {
        this.payloadBuilder.setSeq(seq);
        return this;
    }

    public SparkplugBProtobufPayloadBuilder withTimestamp(long timestamp) {
        this.payloadBuilder.setTimestamp(timestamp);
        return this;
    }

    public SparkplugBProtobufPayloadBuilder withBody(byte[] body) {
        this.payloadBuilder.setBody(ByteString.copyFrom(body));
        return this;
    }

    public Payload buildPayload() {
        return this.payloadBuilder.build();
    }

    public byte[] build() {
        return this.buildPayload().toByteArray();
    }

}
