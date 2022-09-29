/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller.message.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonDecoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonEncoder;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CloudPayloadJsonEncoderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = NullPointerException.class)
    public void testToJsonNullKuraPayload() {
        CloudPayloadJsonEncoder.marshal(null);
    }

    @Test
    public void testToJsonEmptyKuraPayload() {
        KuraPayload payload = new KuraPayload();
        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertEquals(payload.getPosition(), decodedPayload.getPosition());
        assertEquals(payload.getBody(), decodedPayload.getBody());
        assertEquals(payload.getTimestamp(), decodedPayload.getTimestamp());
        assertEquals(payload.metrics(), decodedPayload.metrics());
    }

    @Test
    public void shouldDiscardNonFiniteFloatMetrics() {
        KuraPayload payload = new KuraPayload();

        payload.addMetric("positive.infinity", Float.POSITIVE_INFINITY);
        payload.addMetric("negative.infinity", Float.NEGATIVE_INFINITY);
        payload.addMetric("nan", Float.NaN);
        payload.addMetric("foo", "bar");

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertEquals(payload.getPosition(), decodedPayload.getPosition());
        assertEquals(payload.getBody(), decodedPayload.getBody());
        assertEquals(payload.getTimestamp(), decodedPayload.getTimestamp());

        assertNull(decodedPayload.metrics().get("positive.infinity"));
        assertNull(decodedPayload.metrics().get("negative.infinity"));
        assertNull(decodedPayload.metrics().get("nan"));
        assertEquals("bar", decodedPayload.getMetric("foo"));
    }

    @Test
    public void shouldDiscardPositiveInfinityInPositionFields() {
        KuraPayload payload = new KuraPayload();
        KuraPosition position = new KuraPosition();

        position.setAltitude(Double.POSITIVE_INFINITY);
        position.setHeading(Double.POSITIVE_INFINITY);
        position.setLatitude(Double.POSITIVE_INFINITY);
        position.setLongitude(Double.POSITIVE_INFINITY);
        position.setPrecision(Double.POSITIVE_INFINITY);
        position.setSpeed(Double.POSITIVE_INFINITY);
        position.setSatellites(40);

        payload.setPosition(position);

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertNull(decodedPayload.getPosition().getAltitude());
        assertNull(decodedPayload.getPosition().getHeading());
        assertNull(decodedPayload.getPosition().getLatitude());
        assertNull(decodedPayload.getPosition().getLongitude());
        assertNull(decodedPayload.getPosition().getPrecision());
        assertNull(decodedPayload.getPosition().getSpeed());
        assertEquals(40, (int) decodedPayload.getPosition().getSatellites());
    }

    @Test
    public void shouldDiscardNegativeInfinityInPositionFields() {
        KuraPayload payload = new KuraPayload();
        KuraPosition position = new KuraPosition();

        position.setAltitude(Double.NEGATIVE_INFINITY);
        position.setHeading(Double.NEGATIVE_INFINITY);
        position.setLatitude(Double.NEGATIVE_INFINITY);
        position.setLongitude(Double.NEGATIVE_INFINITY);
        position.setPrecision(Double.NEGATIVE_INFINITY);
        position.setSpeed(Double.NEGATIVE_INFINITY);
        position.setSatellites(40);

        payload.setPosition(position);

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertNull(decodedPayload.getPosition().getAltitude());
        assertNull(decodedPayload.getPosition().getHeading());
        assertNull(decodedPayload.getPosition().getLatitude());
        assertNull(decodedPayload.getPosition().getLongitude());
        assertNull(decodedPayload.getPosition().getPrecision());
        assertNull(decodedPayload.getPosition().getSpeed());
        assertEquals(40, (int) decodedPayload.getPosition().getSatellites());
    }

    @Test
    public void shouldDiscardNaNInfinityInPositionFields() {
        KuraPayload payload = new KuraPayload();
        KuraPosition position = new KuraPosition();

        position.setAltitude(Double.NaN);
        position.setHeading(Double.NaN);
        position.setLatitude(Double.NaN);
        position.setLongitude(Double.NaN);
        position.setPrecision(Double.NaN);
        position.setSpeed(Double.NaN);
        position.setSatellites(40);

        payload.setPosition(position);

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertNull(decodedPayload.getPosition().getAltitude());
        assertNull(decodedPayload.getPosition().getHeading());
        assertNull(decodedPayload.getPosition().getLatitude());
        assertNull(decodedPayload.getPosition().getLongitude());
        assertNull(decodedPayload.getPosition().getPrecision());
        assertNull(decodedPayload.getPosition().getSpeed());
        assertEquals(40, (int) decodedPayload.getPosition().getSatellites());
    }

    @Test
    public void shoyldDiscardNonFiniteDoubleMetrics() {
        KuraPayload payload = new KuraPayload();

        payload.addMetric("positive.infinity", Double.POSITIVE_INFINITY);
        payload.addMetric("negative.infinity", Double.NEGATIVE_INFINITY);
        payload.addMetric("nan", Double.NaN);
        payload.addMetric("foo", "bar");

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertEquals(payload.getPosition(), decodedPayload.getPosition());
        assertEquals(payload.getBody(), decodedPayload.getBody());
        assertEquals(payload.getTimestamp(), decodedPayload.getTimestamp());

        assertNull(decodedPayload.metrics().get("positive.infinity"));
        assertNull(decodedPayload.metrics().get("negative.infinity"));
        assertNull(decodedPayload.metrics().get("nan"));
        assertEquals("bar", decodedPayload.getMetric("foo"));
    }

    @Test
    public void shouldEncodeEmptyPosition() {
        KuraPayload payload = new KuraPayload();
        KuraPosition position = new KuraPosition();

        payload.setPosition(position);

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertNull(decodedPayload.getPosition().getAltitude());
        assertNull(decodedPayload.getPosition().getHeading());
        assertNull(decodedPayload.getPosition().getLatitude());
        assertNull(decodedPayload.getPosition().getLongitude());
        assertNull(decodedPayload.getPosition().getPrecision());
        assertNull(decodedPayload.getPosition().getSpeed());
    }

    @Test
    public void testToJsonKuraPayloadOnlyTimestamp() {
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());
        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertNull(decodedPayload.getPosition());
        assertNull(decodedPayload.getBody());
        assertEquals(payload.getTimestamp(), decodedPayload.getTimestamp());
        assertNotNull(decodedPayload.metrics());
        assertTrue(decodedPayload.metrics().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToJsonKuraPayloadChar() {
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());

        payload.addMetric("metric.name", "metric.value");
        payload.addMetric("metric.character", 'c'); // characters are not supported, yet

        payload.setBody("Test body".getBytes());

        CloudPayloadJsonEncoder.marshal(payload);
    }

    @Test
    public void testToJsonKuraPayload() {
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());
        KuraPosition position = new KuraPosition();
        position.setAltitude(200);
        position.setLatitude(10);
        position.setLongitude(20);
        position.setHeading(30);
        position.setPrecision(1);
        position.setSatellites(3);
        position.setSpeed(50);
        position.setStatus(0);
        position.setTimestamp(new Date(123456789L));

        payload.setPosition(position);

        payload.addMetric("metric.name", "metric.value");

        payload.addMetric("metric.int", 1);
        payload.addMetric("metric.double", Double.MAX_VALUE);
        payload.addMetric("metric.float", 1.2f);
        // payload.addMetric("metric.character", 'c'); // characters are not supported, yet
        payload.addMetric("metric.bytearray", "Test".getBytes());
        payload.addMetric("metric.boolean", true);
        payload.addMetric("metric.long", 12345L);

        payload.setBody("Test body".getBytes());

        String result = CloudPayloadJsonEncoder.marshal(payload);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        KuraPayload decodedPayload = CloudPayloadJsonDecoder.buildFromString(result);

        assertEquals(payload.getTimestamp(), decodedPayload.getTimestamp());

        KuraPosition decodedPosition = decodedPayload.getPosition();
        assertNotNull(decodedPosition);
        assertTrue(decodedPosition.getAltitude() == 200);
        assertTrue(decodedPosition.getLatitude() == 10);
        assertTrue(decodedPosition.getLongitude() == 20);
        assertTrue(decodedPosition.getHeading() == 30);
        assertTrue(decodedPosition.getPrecision() == 1);
        assertTrue(decodedPosition.getSatellites() == 3);
        assertTrue(decodedPosition.getSpeed() == 50);
        assertTrue(decodedPosition.getStatus() == 0);
        assertTrue(decodedPosition.getTimestamp().getTime() == 123456789L);

        assertNotNull(decodedPayload.metrics());
        assertTrue(decodedPayload.metrics().size() == 7);
        assertEquals("metric.value", decodedPayload.getMetric("metric.name"));
        assertEquals(1L, decodedPayload.getMetric("metric.int")); // all fixed point numbers decode as long
        assertEquals(Double.MAX_VALUE, decodedPayload.getMetric("metric.double"));
        assertEquals(1.2, decodedPayload.getMetric("metric.float")); // all floating point numbers decode as double
        // byte[] -> base64 string -> String: it will decode as a String
        // assertArrayEquals("Test".getBytes(), (byte[]) decodedPayload.getMetric("metric.bytearray"));
        assertEquals("VGVzdA==", decodedPayload.getMetric("metric.bytearray"));
        // assertEquals('c', decodedPayload.getMetric("metric.character"));
        assertEquals(true, decodedPayload.getMetric("metric.boolean"));
        assertEquals(12345L, decodedPayload.getMetric("metric.long"));

        Assert.assertArrayEquals("Test body".getBytes(), decodedPayload.getBody());
    }

}
