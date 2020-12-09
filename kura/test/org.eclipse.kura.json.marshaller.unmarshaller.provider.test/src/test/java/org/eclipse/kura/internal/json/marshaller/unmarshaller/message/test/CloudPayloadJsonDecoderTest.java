/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Base64;
import java.util.Date;
import java.util.Map.Entry;

import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonDecoder;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.ParseException;

public class CloudPayloadJsonDecoderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testStringToJson() {
        String stringToConvert = "{\"ts\":1490196349868,\"metric.string\":\"string.value\",\"metric.long\":9223372036854774999,\"metric.char\":\"a\",\"metric.string.oneof\":\"string.value.option.1\",\"metric.password\":\"xea2sebrvKJQEW1YRDEEGg==\",\"metric.float\":32766.98,\"metric.integer.fixed\":101,\"metric.byte\":119,\"metric.boolean\":false,\"temperature\":127.19863,\"metric.integer\":2147483599,\"metric.short\":32759,\"metric.double\":4.29496729599998E9}";
        KuraPayload kuraPayload = CloudPayloadJsonDecoder.buildFromString(stringToConvert);
        assertNotNull(kuraPayload);
    }
    
    @Test(expected = ParseException.class)
    public void testStringToJsonEmptyStringFailure() {
        String stringToConvert = "";
        CloudPayloadJsonDecoder.buildFromString(stringToConvert);
    }

    @Test(expected = ParseException.class)
    public void testStringToJsonIncompleteFailure() {
        String stringToConvert = "{\"TIMESTAMP\":,\"POSITION\":{\"LATITUDE\":10,\"LONGITUDE\":20,\"ALTITUDE\":200,\"HEADING\":30,\"PRECISION\":1,\"SATELLITES\":3,\"SPEED\":50,\"TIMESTAMP\":123456789,\"STATUS\":0},\"metric.name\":{\"type\":\"STRING\",\"value\":\"metric.value\"},\"metric.int\":{\"type\":\"INTEGER\",\"value\":1},\"metric.character\":{\"type\":\"CHARACTER\",\"value\":99},\"metric.float\":{\"type\":\"FLOAT\",\"value\":1.2},\"metric.bytearray\":{\"type\":\"BYTEARRAY\",\"value\":\"VGVzdA==\"},\"metric.double\":{\"type\":\"DOUBLE\",\"value\":1.7976931348623157E308}}";
        CloudPayloadJsonDecoder.buildFromString(stringToConvert);
    }

    @Test
    public void testStringToJsonUnparsableMetric() {
        String stringToConvert = "{\"SENTON\":1490275324619,\"METRICS\":{\"metric.name\":{\"metric.value\":\"value\"}}}";

        KuraPayload payload = CloudPayloadJsonDecoder.buildFromString(stringToConvert);

        assertNull(payload.getTimestamp());

        assertNull(payload.getPosition());

        assertNotNull(payload.metrics());
        assertEquals(0, payload.metrics().size());

        assertNotNull(payload.getBody());
    }

    @Test
    public void testToJsonUnparsablePosition() {
        String stringToConvert = "{\"SENTON\":1490275324619,\"POSITION\":{\"LATITUDE\":10,\"LON\":20}}";

        KuraPayload payload = CloudPayloadJsonDecoder.buildFromString(stringToConvert);

        assertNull(payload.getTimestamp());

        assertNull(payload.getPosition());

        assertNotNull(payload.metrics());
        assertEquals(0, payload.metrics().size());

        assertNotNull(payload.getBody());
    }

    @Test
    public void testByteToJsonComplete() {
        String stringToConvert = ("{\"SENTON\":1490275324619,"
                + "\"POSITION\":{\"LATITUDE\":10,\"LONGITUDE\":20,\"ALTITUDE\":200,\"HEADING\":30,\"PRECISION\":1,\"SATELLITES\":3,\"SPEED\":50,\"TIMESTAMP\":123456789,\"STATUS\":0},"
                + "\"METRICS\":{\"metric.name\":\"metric.value\",\"metric.int\":1,\"metric.long\":12345,\"metric.boolean\":true,\"metric.character\":\"c\",\"metric.float\":1.2,\"metric.bytearray\":\"VGVzdA==\",\"metric.double\":1.7976931348623157E308},"
                + "\"BODY\":\"" + Base64.getEncoder().encodeToString("test".getBytes()) + "\"}");

        KuraPayload payload = CloudPayloadJsonDecoder.buildFromString(stringToConvert);

        assertEquals(1490275324619L, payload.getTimestamp().getTime());

        KuraPosition position = payload.getPosition();
        assertNotNull(position);
        assertTrue(position.getAltitude() == 200);
        assertTrue(position.getLatitude() == 10);
        assertTrue(position.getLongitude() == 20);
        assertTrue(position.getHeading() == 30);
        assertTrue(position.getPrecision() == 1);
        assertTrue(position.getSatellites() == 3);
        assertTrue(position.getSpeed() == 50);
        assertTrue(position.getStatus() == 0);
        assertTrue(position.getTimestamp().getTime() == 123456789L);

        assertNotNull(payload.metrics());
        assertEquals(8, payload.metrics().size());
        assertEquals("metric.value", payload.getMetric("metric.name"));
        assertEquals(1L, payload.getMetric("metric.int")); // all fixed point numbers decode as long
        assertEquals(Double.MAX_VALUE, payload.getMetric("metric.double"));
        assertEquals(1.2, payload.getMetric("metric.float")); // all floating point numbers decode as double
        // byte[] -> base64 string -> String: it will decode as a String
        // assertArrayEquals("Test".getBytes(), (byte[]) decodedPayload.getMetric("metric.bytearray"));
        assertEquals("VGVzdA==", payload.getMetric("metric.bytearray"));
        // assertEquals('c', decodedPayload.getMetric("metric.character"));
        assertEquals(true, payload.getMetric("metric.boolean"));
        assertEquals(12345L, payload.getMetric("metric.long"));

        byte[] body = payload.getBody();
        assertNotNull(body);
        assertArrayEquals("test".getBytes(), body);
    }

    // this test is prepared for one of the new format possibilities with better decoding options - currently invalid
    // @Test
    public void testFromJson() {
        String stringToConvert = "{\"TIMESTAMP\":1490275324619,\"metric.name\":{\"type\":\"STRING\",\"value\":\"metric.value\"},\"metric.int\":{\"type\":\"INTEGER\",\"value\":1},\"metric.character\":{\"type\":\"CHARACTER\",\"value\":99},\"metric.float\":{\"type\":\"FLOAT\",\"value\":1.2},\"metric.bytearray\":{\"type\":\"BYTEARRAY\",\"value\":\"VGVzdA==\"},\"metric.double\":{\"type\":\"DOUBLE\",\"value\":1.7976931348623157E308}}";
        KuraPayload payload = CloudPayloadJsonDecoder.buildFromString(stringToConvert);
        assertNotNull(payload);

        // timestamp
        Date timestamp = payload.getTimestamp();
        assertNotNull(timestamp);
        long expectedTimestamp = 1490275324619L;
        assertEquals(expectedTimestamp, timestamp.getTime());

        assertNull(payload.getPosition());
        assertNull(payload.getBody());

        // Metrics
        assertNotNull(payload.metrics());
        for (Entry<String, Object> entry : payload.metrics().entrySet()) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testFromJsonNullInput() {
        CloudPayloadJsonDecoder.buildFromString(null);
    }

    @Test
    public void testFromJsonUnknownJson() {
        String stringToConvert = "{\"stuff\": {\"onetype\": [{\"id\":1,\"name\":\"John Doe\"},{\"id\":2,\"name\":\"Don Joeh\"}],\"othertype\": {\"id\":2,\"company\":\"ACME\"}}, \"otherstuff\": {\"thing\": [[1,42],[2,2]]}}";
        KuraPayload payload = CloudPayloadJsonDecoder.buildFromString(stringToConvert);
        assertNotNull(payload);

        // timestamp
        Date timestamp = payload.getTimestamp();
        assertNull(timestamp);

        assertNull(payload.getPosition());

        byte[] body = payload.getBody();
        assertNotNull(body);
        assertFalse(body.length == 0);

        assertNotNull(payload.metrics());
        assertTrue(payload.metrics().isEmpty());
    }
}
