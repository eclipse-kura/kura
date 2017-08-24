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

import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraPosition;
import org.eclipse.kura.core.message.protobuf.KuraPayloadProto.KuraPayload.KuraPosition.Builder;
import org.junit.Test;

public class KuraPayloadProtoPositionBuilderTest {

    @Test
    public void testBuilderLatitude() {
        Builder builder = KuraPosition.newBuilder();

        double value = 46.0;
        builder.setLatitude(value);

        assertTrue(builder.hasLatitude());

        assertEquals(value, builder.getLatitude(), 0.000001);

        builder.clearLatitude();

        assertFalse(builder.hasLatitude());
    }

    @Test
    public void testBuilderLongitude() {
        Builder builder = KuraPosition.newBuilder();

        double value = 13.0;
        builder.setLongitude(value);

        assertTrue(builder.hasLongitude());

        assertEquals(value, builder.getLongitude(), 0.000001);

        builder.clearLongitude();

        assertFalse(builder.hasLongitude());
    }

    @Test
    public void testBuilderAltitude() {
        Builder builder = KuraPosition.newBuilder();

        double value = 300.0;
        builder.setAltitude(value);

        assertTrue(builder.hasAltitude());

        assertEquals(value, builder.getAltitude(), 0.000001);

        builder.clearAltitude();

        assertFalse(builder.hasAltitude());
    }

    @Test
    public void testBuilderHeading() {
        Builder builder = KuraPosition.newBuilder();

        double value = 110.0;
        builder.setHeading(value);

        assertTrue(builder.hasHeading());

        assertEquals(value, builder.getHeading(), 0.000001);

        builder.clearHeading();

        assertFalse(builder.hasHeading());
    }

    @Test
    public void testBuilderPrecision() {
        Builder builder = KuraPosition.newBuilder();

        double value = 0.000001;
        builder.setPrecision(value);

        assertTrue(builder.hasPrecision());

        assertEquals(value, builder.getPrecision(), 0.000001);

        builder.clearPrecision();

        assertFalse(builder.hasPrecision());
    }

    @Test
    public void testBuilderSatellites() {
        Builder builder = KuraPosition.newBuilder();

        int value = 6;
        builder.setSatellites(value);

        assertTrue(builder.hasSatellites());

        assertEquals(value, builder.getSatellites());

        builder.clearSatellites();

        assertFalse(builder.hasSatellites());
    }

    @Test
    public void testBuilderSpeed() {
        Builder builder = KuraPosition.newBuilder();

        double value = 10.0;
        builder.setSpeed(value);

        assertTrue(builder.hasSpeed());

        assertEquals(value, builder.getSpeed(), 0.000001);

        builder.clearSpeed();

        assertFalse(builder.hasSpeed());
    }

    @Test
    public void testBuilderStatus() {
        Builder builder = KuraPosition.newBuilder();

        int value = 4;
        builder.setStatus(value);

        assertTrue(builder.hasStatus());

        assertEquals(value, builder.getStatus());

        builder.clearStatus();

        assertFalse(builder.hasStatus());
    }

    @Test
    public void testBuilderTimestamp() {
        Builder builder = KuraPosition.newBuilder();

        long value = 1503300000000L;
        builder.setTimestamp(value);

        assertTrue(builder.hasTimestamp());

        assertEquals(value, builder.getTimestamp());

        builder.clearTimestamp();

        assertFalse(builder.hasTimestamp());
    }

    @Test
    public void testBuilderInitClear() {
        Builder builder = KuraPosition.newBuilder();

        assertFalse(builder.isInitialized());

        builder.setLatitude(46.0);

        assertFalse(builder.isInitialized());

        builder.setLongitude(13.0);

        assertTrue(builder.isInitialized());

        builder.setAltitude(300.0);
        builder.setHeading(110.0);
        builder.setPrecision(0.01);
        builder.setSatellites(6);
        builder.setSpeed(10);
        builder.setStatus(4);
        builder.setTimestamp(1503300000000L);

        assertTrue(builder.hasAltitude());
        assertTrue(builder.hasHeading());
        assertTrue(builder.hasLatitude());
        assertTrue(builder.hasLongitude());
        assertTrue(builder.hasPrecision());
        assertTrue(builder.hasSatellites());
        assertTrue(builder.hasSpeed());
        assertTrue(builder.hasStatus());
        assertTrue(builder.hasTimestamp());

        builder.clear();

        assertFalse(builder.isInitialized());
        assertFalse(builder.hasAltitude());
        assertFalse(builder.hasHeading());
        assertFalse(builder.hasLatitude());
        assertFalse(builder.hasLongitude());
        assertFalse(builder.hasPrecision());
        assertFalse(builder.hasSatellites());
        assertFalse(builder.hasSpeed());
        assertFalse(builder.hasStatus());
        assertFalse(builder.hasTimestamp());
    }

    @Test
    public void testBuilderMerge() {
        Builder builder = KuraPosition.newBuilder();

        double lat = 46.0;
        double lon = 13.0;
        double alt = 300.0;
        double head = 110.0;
        double prec = 0.01;
        int sat = 6;
        int speed = 10;
        int status = 4;
        long time = 1503300000000L;
        double eps = 0.000001;

        builder.setLatitude(lat);
        builder.setLongitude(lon);
        builder.setAltitude(alt);
        builder.setHeading(head);
        builder.setPrecision(prec);
        builder.setSatellites(sat);
        builder.setSpeed(speed);
        builder.setStatus(status);
        builder.setTimestamp(time);

        KuraPosition position = builder.build();
        assertEquals(lat, position.getLatitude(), eps);
        assertEquals(lon, position.getLongitude(), eps);
        assertEquals(alt, position.getAltitude(), eps);
        assertEquals(head, position.getHeading(), eps);
        assertEquals(prec, position.getPrecision(), eps);
        assertEquals(sat, position.getSatellites());
        assertEquals(speed, position.getSpeed(), eps);
        assertEquals(status, position.getStatus());
        assertEquals(time, position.getTimestamp());

        Builder builder2 = KuraPosition.newBuilder();

        assertFalse(builder2.isInitialized());
        assertFalse(builder2.hasAltitude());
        assertFalse(builder2.hasHeading());
        assertFalse(builder2.hasLatitude());
        assertFalse(builder2.hasLongitude());
        assertFalse(builder2.hasPrecision());
        assertFalse(builder2.hasSatellites());
        assertFalse(builder2.hasSpeed());
        assertFalse(builder2.hasStatus());
        assertFalse(builder2.hasTimestamp());

        builder2.mergeFrom(position);

        assertTrue(builder2.isInitialized());
        assertTrue(builder2.hasAltitude());
        assertTrue(builder2.hasHeading());
        assertTrue(builder2.hasLatitude());
        assertTrue(builder2.hasLongitude());
        assertTrue(builder2.hasPrecision());
        assertTrue(builder2.hasSatellites());
        assertTrue(builder2.hasSpeed());
        assertTrue(builder2.hasStatus());
        assertTrue(builder2.hasTimestamp());
    }
}
