/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import java.io.IOException;
import java.util.Date;

import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.KuraInvalidMetricTypeException;
import org.eclipse.kura.core.util.GZipUtil;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.protobuf.KuraPayloadProto;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class CloudPayloadProtoBufDecoderImpl {

    private static final Logger logger = LoggerFactory.getLogger(CloudPayloadProtoBufDecoderImpl.class);

    private byte[] bytes;

    public CloudPayloadProtoBufDecoderImpl(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Factory method to build an KuraPayload instance from a byte array.
     *
     * @param bytes
     * @return
     * @throws InvalidProtocolBufferException
     * @throws IOException
     */
    public KuraPayload buildFromByteArray() throws IOException {
        // Check if a compressed payload and try to decompress it
        if (GZipUtil.isCompressed(this.bytes)) {
            try {
                this.bytes = GZipUtil.decompress(this.bytes);
            } catch (IOException e) {
                logger.info("Decompression failed");
                // do not rethrow the exception here as isCompressed may return some false positives
            }
        }

        // build the KuraPayloadProto.KuraPayload
        KuraPayloadProto.KuraPayload protoMsg = null;
        try {
            protoMsg = KuraPayloadProto.KuraPayload.parseFrom(this.bytes);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new KuraInvalidMessageException(ipbe);
        }

        // build the KuraPayload
        KuraPayload kuraMsg = new KuraPayload();

        // set the timestamp
        if (protoMsg.hasTimestamp()) {
            kuraMsg.setTimestamp(new Date(protoMsg.getTimestamp()));
        }

        // set the position
        if (protoMsg.hasPosition()) {
            kuraMsg.setPosition(buildFromProtoBuf(protoMsg.getPosition()));
        }

        // set the metrics
        for (int i = 0; i < protoMsg.getMetricCount(); i++) {
            String name = protoMsg.getMetric(i).getName();
            try {
                Object value = getProtoKuraMetricValue(protoMsg.getMetric(i), protoMsg.getMetric(i).getType());
                kuraMsg.addMetric(name, value);
            } catch (KuraInvalidMetricTypeException ihte) {
                logger.warn("During deserialization, ignoring metric named: {}. Unrecognized value type: {}", name,
                        protoMsg.getMetric(i).getType(), ihte);
            }
        }

        // set the body
        if (protoMsg.hasBody()) {
            kuraMsg.setBody(protoMsg.getBody().toByteArray());
        }

        return kuraMsg;
    }

    private KuraPosition buildFromProtoBuf(KuraPayloadProto.KuraPayload.KuraPosition protoPosition) {
        KuraPosition position = new KuraPosition();

        if (protoPosition.hasLatitude()) {
            position.setLatitude(protoPosition.getLatitude());
        }
        if (protoPosition.hasLongitude()) {
            position.setLongitude(protoPosition.getLongitude());
        }
        if (protoPosition.hasAltitude()) {
            position.setAltitude(protoPosition.getAltitude());
        }
        if (protoPosition.hasPrecision()) {
            position.setPrecision(protoPosition.getPrecision());
        }
        if (protoPosition.hasHeading()) {
            position.setHeading(protoPosition.getHeading());
        }
        if (protoPosition.hasSpeed()) {
            position.setSpeed(protoPosition.getSpeed());
        }
        if (protoPosition.hasSatellites()) {
            position.setSatellites(protoPosition.getSatellites());
        }
        if (protoPosition.hasStatus()) {
            position.setStatus(protoPosition.getStatus());
        }
        if (protoPosition.hasTimestamp()) {
            position.setTimestamp(new Date(protoPosition.getTimestamp()));
        }
        return position;
    }

    private Object getProtoKuraMetricValue(KuraPayloadProto.KuraPayload.KuraMetric metric,
            KuraPayloadProto.KuraPayload.KuraMetric.ValueType type) throws KuraInvalidMetricTypeException {
        switch (type) {

        case DOUBLE:
            return metric.getDoubleValue();

        case FLOAT:
            return metric.getFloatValue();

        case INT64:
            return metric.getLongValue();

        case INT32:
            return metric.getIntValue();

        case BOOL:
            return metric.getBoolValue();

        case STRING:
            return metric.getStringValue();

        case BYTES:
            ByteString bs = metric.getBytesValue();
            return bs.toByteArray();

        default:
            throw new KuraInvalidMetricTypeException(type);
        }
    }
}
