/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import java.io.IOException;

import org.eclipse.kura.KuraInvalidMetricTypeException;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * Encodes an KuraPayload class using the Google ProtoBuf binary format.
 */
public class CloudPayloadProtoBufEncoderImpl implements CloudPayloadEncoder {

    private static final Logger s_logger = LoggerFactory.getLogger(CloudPayloadProtoBufEncoderImpl.class);

    private final KuraPayload m_kuraPayload;

    public CloudPayloadProtoBufEncoderImpl(KuraPayload kuraPayload) {
        this.m_kuraPayload = kuraPayload;
    }

    /**
     * Conversion method to serialize an KuraPayload instance into a byte array.
     *
     * @return
     */
    @Override
    public byte[] getBytes() throws IOException {
        // Build the message
        KuraPayloadProto.KuraPayload.Builder protoMsg = KuraPayloadProto.KuraPayload.newBuilder();

        // set the timestamp
        if (this.m_kuraPayload.getTimestamp() != null) {
            protoMsg.setTimestamp(this.m_kuraPayload.getTimestamp().getTime());
        }

        // set the position
        if (this.m_kuraPayload.getPosition() != null) {
            protoMsg.setPosition(buildPositionProtoBuf());
        }

        // set the metrics
        for (String name : this.m_kuraPayload.metricNames()) {

            // build a metric
            Object value = this.m_kuraPayload.getMetric(name);
            try {
                KuraPayloadProto.KuraPayload.KuraMetric.Builder metricB = KuraPayloadProto.KuraPayload.KuraMetric
                        .newBuilder();
                metricB.setName(name);

                boolean result = setProtoKuraMetricValue(metricB, value);
                if (result) {
                    metricB.build();

                    // add it to the message
                    protoMsg.addMetric(metricB);
                }
            } catch (KuraInvalidMetricTypeException eihte) {
                try {
                    s_logger.error("During serialization, ignoring metric named: {}. Unrecognized value type: {}.",
                            name, value.getClass().getName());
                } catch (NullPointerException npe) {
                    s_logger.error("During serialization, ignoring metric named: {}. The value is null.", name);
                }
                throw new RuntimeException(eihte);
            }
        }

        // set the body
        if (this.m_kuraPayload.getBody() != null) {
            protoMsg.setBody(ByteString.copyFrom(this.m_kuraPayload.getBody()));
        }

        return protoMsg.build().toByteArray();
    }

    //
    // Helper methods to convert the KuraMetrics
    //
    private KuraPayloadProto.KuraPayload.KuraPosition buildPositionProtoBuf() {
        KuraPayloadProto.KuraPayload.KuraPosition.Builder protoPos = KuraPayloadProto.KuraPayload.KuraPosition
                .newBuilder();

        KuraPosition position = this.m_kuraPayload.getPosition();
        if (position.getLatitude() != null) {
            protoPos.setLatitude(position.getLatitude());
        }
        if (position.getLongitude() != null) {
            protoPos.setLongitude(position.getLongitude());
        }
        if (position.getAltitude() != null) {
            protoPos.setAltitude(position.getAltitude());
        }
        if (position.getPrecision() != null) {
            protoPos.setPrecision(position.getPrecision());
        }
        if (position.getHeading() != null) {
            protoPos.setHeading(position.getHeading());
        }
        if (position.getSpeed() != null) {
            protoPos.setSpeed(position.getSpeed());
        }
        if (position.getTimestamp() != null) {
            protoPos.setTimestamp(position.getTimestamp().getTime());
        }
        if (position.getSatellites() != null) {
            protoPos.setSatellites(position.getSatellites());
        }
        if (position.getStatus() != null) {
            protoPos.setStatus(position.getStatus());
        }
        return protoPos.build();
    }

    private static boolean setProtoKuraMetricValue(KuraPayloadProto.KuraPayload.KuraMetric.Builder metric, Object o)
            throws KuraInvalidMetricTypeException {

        if (o instanceof String) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.STRING);
            metric.setStringValue((String) o);
        } else if (o instanceof Double) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.DOUBLE);
            metric.setDoubleValue((Double) o);
        } else if (o instanceof Integer) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.INT32);
            metric.setIntValue((Integer) o);
        } else if (o instanceof Float) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.FLOAT);
            metric.setFloatValue((Float) o);
        } else if (o instanceof Long) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.INT64);
            metric.setLongValue((Long) o);
        } else if (o instanceof Boolean) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.BOOL);
            metric.setBoolValue((Boolean) o);
        } else if (o instanceof byte[]) {
            metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.BYTES);
            metric.setBytesValue(ByteString.copyFrom((byte[]) o));
        } else if (o == null) {
            s_logger.warn("Received a metric with a null value!");
            return false;
        } else {
            throw new KuraInvalidMetricTypeException(o.getClass().getName());
        }
        return true;
    }
}
