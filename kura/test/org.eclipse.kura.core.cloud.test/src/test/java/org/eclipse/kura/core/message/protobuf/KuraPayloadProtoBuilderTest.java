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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.Builder;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraMetric;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraMetric.ValueType;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraPosition;
import org.junit.Test;

import com.google.protobuf.ByteString;

public class KuraPayloadProtoBuilderTest {

    @Test
    public void testBuilderMerge() {
        // test that merging an existing KuraPayload in a Builder works

        String metricName = "metric.name";
        String metric2Name = "metric2.name";
        long time = 1503300000000L;
        double lon = 14.0;
        double lat = 46.0;
        String bodyTxt = "test";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(metric);
        KuraPosition pos = KuraPosition.newBuilder().setLongitude(lon).setLatitude(lat).setPrecision(0.01).build();
        builder.setPosition(pos);
        builder.setTimestamp(time);
        builder.setBody(ByteString.copyFromUtf8(bodyTxt));

        assertTrue(builder.isInitialized());
        assertTrue(builder.hasPosition());
        assertTrue(builder.hasTimestamp());

        KuraPayload payload = builder.build();

        Builder builder2 = KuraPayload.newBuilder();
        KuraMetric metric2 = KuraMetric.newBuilder().setName(metric2Name).setType(ValueType.DOUBLE)
                .setDoubleValue(3.1415926).build();
        builder2.addMetric(metric2);
        builder2.setBody(ByteString.copyFromUtf8("test2"));

        builder2.mergeFrom(payload);

        KuraPayload payload2 = builder2.build();

        final KuraPosition pos2 = payload2.getPosition();
        assertNotNull(pos2);
        assertEquals(lon, pos2.getLongitude(), 0.000001);
        assertEquals(lat, pos2.getLatitude(), 0.000001);

        final List<KuraMetric> metrics = payload2.getMetricList();
        assertNotNull(metrics);
        assertEquals(2, metrics.size());

        assertEquals(metric2Name, metrics.get(0).getName());
        assertEquals(metricName, metrics.get(1).getName());

        assertEquals(time, payload2.getTimestamp());

        assertEquals(bodyTxt, payload2.getBody().toStringUtf8());
    }

    @Test
    public void testBuilderMergeNoMetric() {
        // test that merging works if the new Builder has no metric, yet

        String metricName = "metric.name";
        double lon = 13.0;
        double lat = 46.0;

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(0, metric);
        KuraPosition pos = KuraPosition.newBuilder().setLongitude(lon).setLatitude(lat).setPrecision(0.01).build();
        builder.setPosition(pos);

        assertTrue(builder.isInitialized());

        KuraPayload payload = builder.build();

        Builder builder2 = KuraPayload.newBuilder();
        KuraPosition pos2 = KuraPosition.newBuilder().setLongitude(13.0).setLatitude(40.0).setPrecision(0.01).build();
        builder2.setPosition(pos2);
        builder2.setBody(ByteString.copyFromUtf8("test2"));

        builder2.mergeFrom(payload);

        KuraPayload payload2 = builder2.build();

        final List<KuraMetric> metrics = payload2.getMetricList();
        assertNotNull(metrics);
        assertEquals(1, metrics.size());

        assertEquals(metricName, metrics.get(0).getName());

        assertEquals("test2", payload2.getBody().toStringUtf8());

        assertEquals(pos, payload2.getPosition());
    }

    @Test
    public void testBuilderClear() {
        // test various clear() methods

        String metricName = "metric.name";
        double lon = 14.0;
        double lat = 46.0;
        String bodyTxt = "test";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(metric);
        KuraPosition pos = KuraPosition.newBuilder().setLongitude(lon).setLatitude(lat).setPrecision(0.01).build();
        builder.setPosition(pos);
        builder.setTimestamp(1503300000000L);
        builder.setBody(ByteString.copyFromUtf8(bodyTxt));

        assertTrue(builder.isInitialized());
        assertTrue(builder.hasPosition());
        assertTrue(builder.hasTimestamp());
        assertTrue(builder.hasBody());
        assertEquals(1, builder.getMetricCount());

        builder.clearBody();
        builder.clearMetric();
        builder.clearPosition();
        builder.clearTimestamp();

        assertTrue(builder.isInitialized());
        assertFalse(builder.hasPosition());
        assertFalse(builder.hasTimestamp());
        assertFalse(builder.hasBody());
        assertEquals(0, builder.getMetricCount());
    }

    @Test
    public void testBuilderClearAll() {
        // test the clear() method

        String metricName = "metric.name";
        long time = 1503300000000L;
        double lon = 14.0;
        double lat = 46.0;
        String bodyTxt = "test";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(metric);
        KuraPosition pos = KuraPosition.newBuilder().setLongitude(lon).setLatitude(lat).setPrecision(0.01).build();
        builder.setPosition(pos);
        builder.setTimestamp(time);
        builder.setBody(ByteString.copyFromUtf8(bodyTxt));

        assertTrue(builder.isInitialized());
        assertTrue(builder.hasPosition());
        assertTrue(builder.hasTimestamp());
        assertTrue(builder.hasBody());
        assertEquals(1, builder.getMetricCount());

        builder.clear();

        assertTrue(builder.isInitialized());
        assertFalse(builder.hasPosition());
        assertFalse(builder.hasTimestamp());
        assertFalse(builder.hasBody());
        assertEquals(0, builder.getMetricCount());
    }

    @Test(expected = NullPointerException.class)
    public void testBuilderSetPositionNull() {
        // test setting position to null

        Builder builder = KuraPayload.newBuilder();
        builder.setPosition((KuraPosition) null);
    }

    @Test
    public void testBuilderSetPosition() {
        // test setting position builder

        double lon = 14.0;
        double lat = 46.0;

        Builder builder = KuraPayload.newBuilder();
        org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraPosition.Builder posBuilder = KuraPosition
                .newBuilder().setLongitude(lon).setLatitude(lat).setPrecision(0.01);
        builder.setPosition(posBuilder);

        assertTrue(builder.hasPosition());

        assertEquals(posBuilder.build(), builder.getPosition());
    }

    @Test
    public void testBuilderMergeWithMetricBuilder() {
        // test that merging an existing KuraPayload in a Builder works

        String metricName = "metric.name";
        String metric2Name = "metric2.name";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(metric);

        KuraPayload payload = builder.build();

        Builder builder2 = KuraPayload.newBuilder();
        org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraMetric.Builder mBuilder = builder2
                .addMetricBuilder();

        assertEquals(1, builder2.getMetricCount()); // it's not valid, but it's still 'there'

        mBuilder.setName(metric2Name).setType(ValueType.DOUBLE).setDoubleValue(3.1415926);

        builder2.mergeFrom(payload);

        KuraPayload payload2 = builder2.build();

        final List<KuraMetric> metrics = payload2.getMetricList();
        assertNotNull(metrics);
        assertEquals(2, metrics.size());
        assertEquals(metric2Name, metrics.get(0).getName());
        assertEquals(metricName, metrics.get(1).getName());
    }

    @Test
    public void testBuilderInsertMetric() {
        // test that inserting a metric works

        String metricName = "metric.name";
        String metric2Name = "metric2.name";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(metric);

        metric = KuraMetric.newBuilder().setName(metric2Name).setType(ValueType.INT32).setIntValue(123).build();
        builder.addMetric(0, metric);

        KuraPayload payload = builder.build();

        final List<KuraMetric> metrics = payload.getMetricList();
        assertNotNull(metrics);
        assertEquals(2, metrics.size());
        assertEquals(metric2Name, metrics.get(0).getName());
        assertEquals(metricName, metrics.get(1).getName());
    }

    @Test
    public void testBuilderInsertMetricBuilder() {
        // test that inserting a metric builder works

        String metricName = "metric.name";
        String metric2Name = "metric2.name";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric.Builder metricBuilder = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value");
        builder.addMetric(metricBuilder);

        metricBuilder = KuraMetric.newBuilder().setName(metric2Name).setType(ValueType.INT32).setIntValue(123);
        builder.addMetric(0, metricBuilder);

        KuraPayload payload = builder.build();

        final List<KuraMetric> metrics = payload.getMetricList();
        assertNotNull(metrics);
        assertEquals(2, metrics.size());
        assertEquals(metric2Name, metrics.get(0).getName());
        assertEquals(metricName, metrics.get(1).getName());
    }

    @Test
    public void testBuilderSetMetric() {
        // test that overwriting a metric works

        String metricName = "metric.name";
        String metric2Name = "metric2.name";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric metric = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value").build();
        builder.addMetric(metric);

        metric = KuraMetric.newBuilder().setName(metric2Name).setType(ValueType.INT32).setIntValue(123).build();
        builder.setMetric(0, metric);

        KuraPayload payload = builder.build();

        final List<KuraMetric> metrics = payload.getMetricList();
        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        assertEquals(metric2Name, metrics.get(0).getName());
    }

    @Test
    public void testBuilderSetMetricWithBuilder() {
        // test that overwriting a metric with a builder works

        String metricName = "metric.name";
        String metric2Name = "metric2.name";

        Builder builder = KuraPayload.newBuilder();
        KuraMetric.Builder metricBuilder = KuraMetric.newBuilder().setName(metricName).setType(ValueType.STRING)
                .setStringValue("metric.value");
        builder.addMetric(metricBuilder);

        metricBuilder = KuraMetric.newBuilder().setName(metric2Name).setType(ValueType.INT32).setIntValue(123);
        builder.setMetric(0, metricBuilder);

        KuraPayload payload = builder.build();

        final List<KuraMetric> metrics = payload.getMetricList();
        assertNotNull(metrics);
        assertEquals(1, metrics.size());
        assertEquals(metric2Name, metrics.get(0).getName());
    }

    @Test
    public void testBuilderGetPositionBuilder() {
        // test if position builder is returned

        Builder builder = KuraPayload.newBuilder();
        org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraPosition.Builder positionBuilder = builder
                .getPositionBuilder();

        assertNotNull(positionBuilder);

        assertTrue(builder.hasPosition());
    }

}
