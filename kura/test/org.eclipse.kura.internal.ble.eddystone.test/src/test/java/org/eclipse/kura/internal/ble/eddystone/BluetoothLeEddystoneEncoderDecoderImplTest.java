/*******************************************************************************
 * Copyright (c) 2018, 2026 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
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

    // An AD length byte of 0xFF is sign-extended to -1 when widened to int,
    // so ptr += structSize + 1
    // must not become a no-op, or the parser loops forever without ever exiting the
    // array bounds.
    @Test(timeout = 2000)
    public void testDecodeDoesNotHangOnLengthByte0xFF() {
        byte[] encoded = { (byte) 0xFF, 0x00 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    @Test(timeout = 2000)
    public void testDecodeDoesNotHangOnLengthByte0xFFMidStream() {
        byte[] encoded = { 2, 1, 6, (byte) 0xFF, 0x00 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    // A truncated Eddystone UUID signature (length + service-UUID bytes only, no
    // frame-type or txPower bytes) passes the signature check, but the
    // frameType/txPower reads that
    // follow are raw array indexing and must not run past the array bounds.
    @Test
    public void testDecodeDoesNotThrowOnTruncatedEddystoneHeader() {
        byte[] encoded = { 0x03, 0x03, (byte) 0xAA, (byte) 0xFE };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    // The header (signature + frame-type + txPower) is present, but the UID frame's
    // namespace/instance bytes are truncated. Arrays.copyOfRange tolerates the
    // namespace read (zero-pads), but the instance range starts past the end of the
    // array and must not throw. An incomplete record is treated as no match, same as
    // a truncated header.
    @Test
    public void testDecodeDoesNotThrowOnTruncatedEddystoneUidBody() {
        byte[] encoded = { 0x0B, 0x03, (byte) 0xAA, (byte) 0xFE, 0, 0, 0, 0, 0x00, 10, 1, 2 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    // EddystoneFrameType.valueOf() returns null for a frame-type byte outside
    // {0x00,0x10,0x20,0x30,0x40}. Comparing that against the known frame types must
    // not NPE, and an unsupported frame type is treated as no match.
    @Test
    public void testDecodeDoesNotThrowOnUnknownFrameType() {
        byte[] encoded = { 0x09, 0x03, (byte) 0xAA, (byte) 0xFE, 0, 0, 0, 0, 0x50, 50 };

        BluetoothLeEddystoneDecoderImpl decoder = new BluetoothLeEddystoneDecoderImpl();
        BluetoothLeEddystone decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

}
