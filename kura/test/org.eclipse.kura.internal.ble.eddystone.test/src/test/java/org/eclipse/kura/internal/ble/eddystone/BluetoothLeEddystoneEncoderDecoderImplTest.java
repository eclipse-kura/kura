/*******************************************************************************
 * Copyright (c) 2018, 2019 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.eddystone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.junit.Test;

public class BluetoothLeEddystoneEncoderDecoderImplTest {

    @Test
    public void testBasicEncoder() {
        BluetoothLeEddystoneEncoderImpl svc = new BluetoothLeEddystoneEncoderImpl();

        assertEquals(BluetoothLeEddystone.class, svc.getBeaconType());

        // nothing to check, here
        svc.activate(null);
        svc.deactivate(null);
    }

    @Test
    public void testEncodeUID() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = null;

        byte[] encoded = null;
        try {
            encoded = encoder.encode(beacon);
        } catch (NullPointerException e) {
            // expected
        }

        beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);
        beacon.setFrameType(EddystoneFrameType.UID.name());
        byte[] namespace = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        beacon.setNamespace(namespace);
        byte[] instance = { 0, 1, 2, 3, 4, 5 };
        beacon.setInstance(instance);

        encoded = encoder.encode(beacon);

        byte[] expected = { 31, 2, 1, 31, 3, 3, (byte) 0xAA, (byte) 0xFE, 23, 22, (byte) 0xAA, (byte) 0xFE, 0, 50, 0, 1,
                2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 0, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeUIDTxPowerAboveMax() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = null;

        byte[] encoded = null;
        try {
            encoded = encoder.encode(beacon);
        } catch (NullPointerException e) {
            // expected
        }

        beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 180);
        beacon.setFrameType(EddystoneFrameType.UID.name());
        byte[] namespace = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        beacon.setNamespace(namespace);
        byte[] instance = { 0, 1, 2, 3, 4, 5 };
        beacon.setInstance(instance);

        encoded = encoder.encode(beacon);

        byte[] expected = { 31, 2, 1, 31, 3, 3, (byte) 0xAA, (byte) 0xFE, 23, 22, (byte) 0xAA, (byte) 0xFE, 0, 126, 0,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 0, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeUIDTxPowerBelowMin() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = null;

        byte[] encoded = null;
        try {
            encoded = encoder.encode(beacon);
        } catch (NullPointerException e) {
            // expected
        }

        beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) -190);
        beacon.setFrameType(EddystoneFrameType.UID.name());
        byte[] namespace = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        beacon.setNamespace(namespace);
        byte[] instance = { 0, 1, 2, 3, 4, 5 };
        beacon.setInstance(instance);

        encoded = encoder.encode(beacon);

        byte[] expected = { 31, 2, 1, 31, 3, 3, (byte) 0xAA, (byte) 0xFE, 23, 22, (byte) 0xAA, (byte) 0xFE, 0, -127, 0,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 0, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeURLTooLong() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);
        beacon.setFrameType(EddystoneFrameType.URL.name());
        String scheme = "http://";
        beacon.setUrlScheme(scheme);
        String url = "http://www.eurotech.com";
        beacon.setUrl(url);

        byte[] encoded = encoder.encode(beacon);

        assertEquals(32, encoded.length);

        byte[] expected = new byte[32];
        Arrays.fill(expected, (byte) 0);

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeURL() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);
        beacon.setFrameType(EddystoneFrameType.URL.name());
        String scheme = "http://";
        beacon.setUrlScheme(scheme);
        String url = "http://eurotech.com";
        beacon.setUrl(url);

        byte[] encoded = encoder.encode(beacon);

        byte[] expected = { 30, 2, 1, 31, 3, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 16, 50, 2,
                104, 116, 116, 112, 58, 47, 47, 101, 117, 114, 111, 116, 101, 99, 104, 7, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeURLTxPowerAboveMax() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 190);
        beacon.setFrameType(EddystoneFrameType.URL.name());
        String scheme = "http://";
        beacon.setUrlScheme(scheme);
        String url = "http://eurotech.com";
        beacon.setUrl(url);

        byte[] encoded = encoder.encode(beacon);

        byte[] expected = { 30, 2, 1, 31, 3, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 16, 126, 2,
                104, 116, 116, 112, 58, 47, 47, 101, 117, 114, 111, 116, 101, 99, 104, 7, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeURLTxPowerBellowMin() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) -175);
        beacon.setFrameType(EddystoneFrameType.URL.name());
        String scheme = "http://";
        beacon.setUrlScheme(scheme);
        String url = "http://eurotech.com";
        beacon.setUrl(url);

        byte[] encoded = encoder.encode(beacon);

        byte[] expected = { 30, 2, 1, 31, 3, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 16, -127, 2,
                104, 116, 116, 112, 58, 47, 47, 101, 117, 114, 111, 116, 101, 99, 104, 7, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeTLM() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);
        beacon.setFrameType(EddystoneFrameType.TLM.name());

        byte[] encoded = encoder.encode(beacon);

        byte[] expected = { 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeEID() {
        BluetoothLeEddystoneEncoderImpl encoder = new BluetoothLeEddystoneEncoderImpl();

        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);
        beacon.setFrameType(EddystoneFrameType.EID.name());

        byte[] encoded = encoder.encode(beacon);

        byte[] expected = { 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testBasicDecoder() {
        BluetoothLeEddystoneDecoderImpl svc = new BluetoothLeEddystoneDecoderImpl();

        assertEquals(BluetoothLeEddystone.class, svc.getBeaconType());

        // nothing to check, here
        svc.activate(null);
        svc.deactivate(null);
    }

    @Test
    public void testDecodeUID() {
        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);

        byte[] encoded = { 1, 0x1F, 32, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 0, 50, 0, 1, 2,
                3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        byte[] namespace = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        byte[] instance = { 0, 1, 2, 3, 4, 5 };

        assertEquals(50, decoded.getTxPower());
        assertEquals(EddystoneFrameType.UID.name(), decoded.getFrameType());
        assertArrayEquals(namespace, decoded.getNamespace());
        assertArrayEquals(instance, decoded.getInstance());
    }

    @Test
    public void testDecodeURL() {
        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);

        byte[] encoded = { 1, 0x1F, 32, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 16, 50, 2, 104,
                116, 116, 112, 58, 47, 47, 101, 117, 114, 111, 116, 101, 99, 104, 7 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertEquals(50, decoded.getTxPower());
        assertEquals(EddystoneFrameType.URL.name(), decoded.getFrameType());
        assertEquals("http://", decoded.getUrlScheme());
        assertEquals("http://eurotech.com", decoded.getUrl());
    }

    @Test
    public void testDecodeURL0() {
        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);

        byte[] encoded = { 1, 0x1F, 32, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 16, 50, 2, 104,
                116, 116, 112, 58, 47, 47, 101, 117, 114, 111, 116, 101, 99, 104, 7, 0 }; // the last 0 is .com/

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertEquals(50, decoded.getTxPower());
        assertEquals(EddystoneFrameType.URL.name(), decoded.getFrameType());
        assertEquals("http://", decoded.getUrlScheme());
        assertEquals("http://eurotech.com.com/", decoded.getUrl());
    }

    @Test
    public void testDecodeURL7() {
        BluetoothLeEddystone beacon = new BluetoothLeEddystone();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setTxPower((short) 50);

        byte[] encoded = { 1, 0x1F, 32, 3, (byte) 0xAA, (byte) 0xFE, 22, 22, (byte) 0xAA, (byte) 0xFE, 16, 50, 2, 104,
                116, 116, 112, 58, 47, 47, 101, 117, 114, 111, 116, 101, 99, 104, 7, 7 }; // the last 7 is .com

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertEquals(50, decoded.getTxPower());
        assertEquals(EddystoneFrameType.URL.name(), decoded.getFrameType());
        assertEquals("http://", decoded.getUrlScheme());
        assertEquals("http://eurotech.com.com", decoded.getUrl());
    }

    @Test
    public void testDecodeStopAt0() {
        byte[] encoded = { 1, 0x1F, 26, 1, 76, 0, 2, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 18,
                0, 5, 0, 2, 50, 0, 0 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

}
