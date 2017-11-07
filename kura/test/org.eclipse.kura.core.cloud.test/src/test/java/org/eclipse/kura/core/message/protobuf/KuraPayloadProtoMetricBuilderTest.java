/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.message.protobuf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraMetric;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraMetric.Builder;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraMetric.ValueType;
import org.junit.Test;

import com.google.protobuf.ByteString;

public class KuraPayloadProtoMetricBuilderTest {

    @Test
    public void testBuilderNameStringHandling() {
        Builder builder = KuraMetric.newBuilder();

        try {
            builder.setName(null);

            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        builder.setName("name");

        assertTrue(builder.hasName());

        assertEquals("name", builder.getName());

        assertEquals("name", builder.getNameBytes().toStringUtf8());

        builder.clearName();

        assertFalse(builder.hasName());
    }

    @Test
    public void testBuilderNameBytesHandling() {
        Builder builder = KuraMetric.newBuilder();
        ByteString byteString = ByteString.copyFromUtf8("name");

        try {
            builder.setNameBytes(null);

            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        builder.setNameBytes(byteString);

        assertTrue(builder.hasName());

        assertEquals(byteString, builder.getNameBytes());

        assertEquals("name", builder.getName());

        builder.clearName();

        assertFalse(builder.hasName());
    }

    @Test
    public void testBuilderTypeHandling() {
        Builder builder = KuraMetric.newBuilder();

        try {
            builder.setType(null);

            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        builder.setType(ValueType.BYTES);

        assertTrue(builder.hasType());

        assertEquals(ValueType.BYTES, builder.getType());

        builder.clearType();

        assertFalse(builder.hasType());
    }

    @Test
    public void testBuilderIntHandling() {
        Builder builder = KuraMetric.newBuilder();

        int value = 123;

        builder.setIntValue(value);

        assertTrue(builder.hasIntValue());

        assertEquals(value, builder.getIntValue());

        builder.clearIntValue();

        assertFalse(builder.hasIntValue());
    }

    @Test
    public void testBuilderLongHandling() {
        Builder builder = KuraMetric.newBuilder();

        long value = 12345678901234L;

        builder.setLongValue(value);

        assertTrue(builder.hasLongValue());

        assertEquals(value, builder.getLongValue());

        builder.clearLongValue();

        assertFalse(builder.hasLongValue());
    }

    @Test
    public void testBuilderFloatHandling() {
        Builder builder = KuraMetric.newBuilder();

        float value = 123.4f;

        builder.setFloatValue(value);

        assertTrue(builder.hasFloatValue());

        assertEquals(value, builder.getFloatValue(), 0.000001);

        builder.clearFloatValue();

        assertFalse(builder.hasFloatValue());
    }

    @Test
    public void testBuilderDoubleHandling() {
        Builder builder = KuraMetric.newBuilder();

        double value = 1234.5;

        builder.setDoubleValue(value);

        assertTrue(builder.hasDoubleValue());

        assertEquals(value, builder.getDoubleValue(), 0.000001);

        builder.clearDoubleValue();

        assertFalse(builder.hasDoubleValue());
    }

    @Test
    public void testBuilderStringHandling() {
        Builder builder = KuraMetric.newBuilder();

        String value = "test";

        try {
            builder.setStringValue(null);

            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        builder.setStringValue(value);

        assertTrue(builder.hasStringValue());

        assertEquals(value, builder.getStringValue());

        assertEquals(value, builder.getStringValueBytes().toStringUtf8());

        builder.clearStringValue();

        assertFalse(builder.hasStringValue());
    }

    @Test
    public void testBuilderStringBytesHandling() {
        Builder builder = KuraMetric.newBuilder();

        String value = "test";
        ByteString byteString = ByteString.copyFromUtf8(value);

        try {
            builder.setStringValueBytes(null);

            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        builder.setStringValueBytes(byteString);

        assertTrue(builder.hasStringValue());

        assertEquals(byteString, builder.getStringValueBytes());

        assertEquals(value, builder.getStringValue());

        builder.clearStringValue();

        assertFalse(builder.hasStringValue());
    }

    @Test
    public void testBuilderBytesHandling() {
        Builder builder = KuraMetric.newBuilder();

        ByteString value = ByteString.copyFromUtf8("test");

        try {
            builder.setBytesValue(null);

            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        builder.setBytesValue(value);

        assertTrue(builder.hasBytesValue());

        assertEquals(value, builder.getBytesValue());

        builder.clearBytesValue();

        assertFalse(builder.hasBytesValue());
    }

    @Test
    public void testBuilderBoolHandling() {
        Builder builder = KuraMetric.newBuilder();

        builder.setBoolValue(true);

        assertTrue(builder.hasBoolValue());

        assertTrue(builder.getBoolValue());

        builder.clearBoolValue();

        assertFalse(builder.hasBoolValue());
    }

    @Test
    public void testBuilderBool() {
        Builder builder = KuraMetric.newBuilder();
        builder.setName("name").setType(ValueType.BOOL).setBoolValue(true);
        KuraMetric metric = builder.build();

        assertEquals("name", metric.getName());
        assertEquals(ValueType.BOOL, metric.getType());
        assertTrue(metric.getBoolValue());
    }

    @Test
    public void testBuilderInitClear() {
        Builder builder = KuraMetric.newBuilder();

        assertFalse(builder.isInitialized());

        builder.setName("name");

        assertFalse(builder.isInitialized());

        builder.setType(ValueType.BOOL);

        assertTrue(builder.isInitialized());

        builder.setBoolValue(true);
        builder.setIntValue(123);
        builder.setLongValue(123456789123456L);
        builder.setFloatValue(123.4f);
        builder.setDoubleValue(1234.5);
        builder.setStringValue("test");
        builder.setBytesValue(ByteString.copyFromUtf8("test"));

        assertTrue(builder.hasName());
        assertTrue(builder.hasType());
        assertTrue(builder.hasBoolValue());
        assertTrue(builder.hasIntValue());
        assertTrue(builder.hasLongValue());
        assertTrue(builder.hasFloatValue());
        assertTrue(builder.hasDoubleValue());
        assertTrue(builder.hasStringValue());
        assertTrue(builder.hasBytesValue());

        builder.clear();

        assertFalse(builder.isInitialized());
        assertFalse(builder.hasName());
        assertFalse(builder.hasType());
        assertFalse(builder.hasBoolValue());
        assertFalse(builder.hasIntValue());
        assertFalse(builder.hasLongValue());
        assertFalse(builder.hasFloatValue());
        assertFalse(builder.hasDoubleValue());
        assertFalse(builder.hasStringValue());
        assertFalse(builder.hasBytesValue());
    }

    @Test
    public void testBuilderMerge() {
        Builder builder = KuraMetric.newBuilder();

        builder.setName("name");
        builder.setType(ValueType.BOOL);
        builder.setBoolValue(true);
        builder.setIntValue(123);
        builder.setLongValue(123456789123456L);
        builder.setFloatValue(123.4f);
        builder.setDoubleValue(1234.5);
        builder.setStringValue("test");
        builder.setBytesValue(ByteString.copyFromUtf8("test"));

        assertTrue(builder.hasName());
        assertTrue(builder.hasType());
        assertTrue(builder.hasBoolValue());
        assertTrue(builder.hasIntValue());
        assertTrue(builder.hasLongValue());
        assertTrue(builder.hasFloatValue());
        assertTrue(builder.hasDoubleValue());
        assertTrue(builder.hasStringValue());
        assertTrue(builder.hasBytesValue());

        Builder builder2 = KuraMetric.newBuilder();

        assertFalse(builder2.isInitialized());
        assertFalse(builder2.hasName());
        assertFalse(builder2.hasType());
        assertFalse(builder2.hasBoolValue());
        assertFalse(builder2.hasIntValue());
        assertFalse(builder2.hasLongValue());
        assertFalse(builder2.hasFloatValue());
        assertFalse(builder2.hasDoubleValue());
        assertFalse(builder2.hasStringValue());
        assertFalse(builder2.hasBytesValue());

        builder2.mergeFrom(builder.build());

        assertTrue(builder2.hasName());
        assertTrue(builder2.hasType());
        assertTrue(builder2.hasBoolValue());
        assertTrue(builder2.hasIntValue());
        assertTrue(builder2.hasLongValue());
        assertTrue(builder2.hasFloatValue());
        assertTrue(builder2.hasDoubleValue());
        assertTrue(builder2.hasStringValue());
        assertTrue(builder2.hasBytesValue());
    }
}
