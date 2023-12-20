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

import java.math.BigInteger;
import java.util.Date;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;

import com.google.protobuf.ByteString;

public class SparkplugBProtobufPayloadBuilder {

    public static final String BDSEQ_METRIC_NAME = "bdSeq";

    private Payload.Builder payloadBuilder = Payload.newBuilder();

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

    public <T> SparkplugBProtobufPayloadBuilder withMetric(String name, TypedValue<T> value, long timestamp) {
        DataType sparkplugDataType;

        switch (value.getType()) {
        case BOOLEAN:
            sparkplugDataType = DataType.Boolean;
            break;
        case BYTE_ARRAY:
            sparkplugDataType = DataType.Bytes;
            break;
        case DOUBLE:
            sparkplugDataType = DataType.Double;
            break;
        case FLOAT:
            sparkplugDataType = DataType.Float;
            break;
        case INTEGER:
            sparkplugDataType = DataType.Int32;
            break;
        case LONG:
            sparkplugDataType = DataType.Int64;
            break;
        case STRING:
            sparkplugDataType = DataType.String;
            break;
        default:
            sparkplugDataType = DataType.Unknown;
            break;
        }

        return this.withMetric(name, value.getValue(), sparkplugDataType, timestamp);
    }

    public <T> SparkplugBProtobufPayloadBuilder withMetric(String name, TypedValue<T> value) {
        return this.withMetric(name, value, new Date().getTime());
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

    public Payload buildPayload() {
        return this.payloadBuilder.build();
    }

    public byte[] build() {
        return this.buildPayload().toByteArray();
    }

}
