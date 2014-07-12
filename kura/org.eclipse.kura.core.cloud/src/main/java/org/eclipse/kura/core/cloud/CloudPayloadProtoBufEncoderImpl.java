/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
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
public class CloudPayloadProtoBufEncoderImpl implements CloudPayloadEncoder 
{
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPayloadProtoBufEncoderImpl.class);
	
	private KuraPayload m_kuraPayload;
	
	public CloudPayloadProtoBufEncoderImpl(KuraPayload KuraPayload) {
		m_kuraPayload = KuraPayload;
	}
	
	/**
	 * Conversion method to serialize an KuraPayload instance into a byte array.
	 * @return
	 */
	public byte[] getBytes() throws IOException 
	{
		// Build the message
		KuraPayloadProto.KuraPayload.Builder protoMsg = KuraPayloadProto.KuraPayload.newBuilder();
		
		// set the timestamp
		if (m_kuraPayload.getTimestamp() != null) {
			protoMsg.setTimestamp(m_kuraPayload.getTimestamp().getTime());
		}
		
		// set the position
		if (m_kuraPayload.getPosition() != null) {
			protoMsg.setPosition(buildPositionProtoBuf());
		}
		
		// set the metrics
		for (String name : m_kuraPayload.metricNames()) {

			// build a metric
			Object value = m_kuraPayload.getMetric(name);
			try {
				KuraPayloadProto.KuraPayload.KuraMetric.Builder metricB = KuraPayloadProto.KuraPayload.KuraMetric.newBuilder();
				metricB.setName(name);
				
				setProtoKuraMetricValue(metricB, value);
				metricB.build();
				
				// add it to the message
				protoMsg.addMetric(metricB);
			}
			catch (KuraInvalidMetricTypeException eihte) {
				try {
					s_logger.error("During serialization, ignoring metric named: {}. Unrecognized value type: {}.", name, value.getClass().getName());
				} catch(NullPointerException npe) {
					s_logger.error("During serialization, ignoring metric named: {}. The value is null.", name);
				}
				throw new RuntimeException(eihte);
			}
		}

		// set the body
		if (m_kuraPayload.getBody() != null) {
			protoMsg.setBody(ByteString.copyFrom(m_kuraPayload.getBody()));
		}

		return protoMsg.build().toByteArray();
	}

	
	//
	// Helper methods to convert the KuraMetrics 
	//
	
	private KuraPayloadProto.KuraPayload.KuraPosition buildPositionProtoBuf() 
	{
		KuraPayloadProto.KuraPayload.KuraPosition.Builder protoPos = null;
		protoPos = KuraPayloadProto.KuraPayload.KuraPosition.newBuilder();
		
		KuraPosition position = m_kuraPayload.getPosition();
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
		if (position.getHeading()!= null) {
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
		if (position.getStatus()!= null) {
			protoPos.setStatus(position.getStatus());
		}
		return protoPos.build();
	}


	private static void setProtoKuraMetricValue(KuraPayloadProto.KuraPayload.KuraMetric.Builder metric, 
											   Object o)
		throws KuraInvalidMetricTypeException
	{

		if (o instanceof String){
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.STRING);
			metric.setStringValue((String)o);
		} 
		else if (o instanceof Double){
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.DOUBLE);
			metric.setDoubleValue((Double)o);
		}
		else if (o instanceof Integer){
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.INT32);
			metric.setIntValue((Integer)o);
		}
		else if (o instanceof Float){
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.FLOAT);
			metric.setFloatValue((Float)o);
		}
		else if (o instanceof Long){
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.INT64);
			metric.setLongValue((Long)o);
		}
		else if (o instanceof Boolean){
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.BOOL);
			metric.setBoolValue((Boolean)o);
		}
		else if (o instanceof byte[]) {
			metric.setType(KuraPayloadProto.KuraPayload.KuraMetric.ValueType.BYTES);
			metric.setBytesValue(ByteString.copyFrom((byte[])o));
		}
		else if (o == null) {
			throw new KuraInvalidMetricTypeException("null value");			
		}
		else {
			throw new KuraInvalidMetricTypeException(o.getClass().getName());			
		}
	}
}
