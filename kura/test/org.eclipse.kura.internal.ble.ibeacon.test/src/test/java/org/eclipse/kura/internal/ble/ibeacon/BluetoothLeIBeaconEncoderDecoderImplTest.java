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
package org.eclipse.kura.internal.ble.ibeacon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.junit.Test;

public class BluetoothLeIBeaconEncoderDecoderImplTest {

    @Test
    public void testBasicEncoder() {
        BluetoothLeIBeaconEncoderImpl svc = new BluetoothLeIBeaconEncoderImpl();

        assertEquals(BluetoothLeIBeacon.class, svc.getBeaconType());

        // nothing to check, here
        svc.activate(null);
        svc.deactivate(null);
    }

    @Test
    public void testBasicDecoder() {
        BluetoothLeIBeaconDecoderImpl svc = new BluetoothLeIBeaconDecoderImpl();

        assertEquals(BluetoothLeIBeacon.class, svc.getBeaconType());

        // nothing to check, here
        svc.activate(null);
        svc.deactivate(null);
    }

    @Test
    public void testEncodeDecode() {
        BluetoothLeIBeaconEncoderImpl encoder = new BluetoothLeIBeaconEncoderImpl();
        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();

        BluetoothLeIBeacon beacon = new BluetoothLeIBeacon();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setMajor((short) 5);
        beacon.setMinor((short) 2);
        beacon.setRssi(0);
        beacon.setTxPower((short) 50);
        beacon.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        byte[] encoded = encoder.encode(beacon);

        BluetoothLeIBeacon decoded = decoder.decode(Arrays.copyOfRange(encoded, 1, encoded.length));

        compare(beacon, decoded);
    }

    @Test
    public void testEncode() {
        BluetoothLeIBeaconEncoderImpl encoder = new BluetoothLeIBeaconEncoderImpl();

        BluetoothLeIBeacon beacon = null;

        byte[] encoded = null;
        try {
            encoded = encoder.encode(beacon);
        } catch (NullPointerException e) {
            // expected
        }

        beacon = new BluetoothLeIBeacon();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setMajor((short) 5);
        beacon.setMinor((short) 2);
        beacon.setRssi(10);
        beacon.setTxPower((short) 50);
        beacon.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        encoded = encoder.encode(beacon);

        byte[] expected = { 30, 2, 1, 0x1F, 0x1A, (byte) 0xFF, 76, 0, 2, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
                17, 17, 17, 17, 17, 0, 5, 0, 2, 50, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeTxPowerAboveMax() {
        BluetoothLeIBeaconEncoderImpl encoder = new BluetoothLeIBeaconEncoderImpl();

        BluetoothLeIBeacon beacon = null;

        byte[] encoded = null;
        try {
            encoded = encoder.encode(beacon);
        } catch (NullPointerException e) {
            // expected
        }

        beacon = new BluetoothLeIBeacon();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setMajor((short) 5);
        beacon.setMinor((short) 2);
        beacon.setRssi(10);
        beacon.setTxPower((short) 190);
        beacon.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        encoded = encoder.encode(beacon);

        byte[] expected = { 30, 2, 1, 0x1F, 0x1A, (byte) 0xFF, 76, 0, 2, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
                17, 17, 17, 17, 17, 0, 5, 0, 2, 126, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testEncodeTxPowerBelowMin() {
        BluetoothLeIBeaconEncoderImpl encoder = new BluetoothLeIBeaconEncoderImpl();

        BluetoothLeIBeacon beacon = null;

        byte[] encoded = null;
        try {
            encoded = encoder.encode(beacon);
        } catch (NullPointerException e) {
            // expected
        }

        beacon = new BluetoothLeIBeacon();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setMajor((short) 5);
        beacon.setMinor((short) 2);
        beacon.setRssi(10);
        beacon.setTxPower((short) -183);
        beacon.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        encoded = encoder.encode(beacon);

        byte[] expected = { 30, 2, 1, 0x1F, 0x1A, (byte) 0xFF, 76, 0, 2, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
                17, 17, 17, 17, 17, 0, 5, 0, 2, -127, 0 };

        assertArrayEquals(expected, encoded);
    }

    @Test
    public void testDecode() {
        BluetoothLeIBeacon beacon = new BluetoothLeIBeacon();
        beacon.setBrEdrSupported(true);
        beacon.setLeBrController(true);
        beacon.setLeBrHost(true);
        beacon.setLeGeneral(true);
        beacon.setLeLimited(true);
        beacon.setMajor((short) 5);
        beacon.setMinor((short) 2);
        beacon.setTxPower((short) 50);
        beacon.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111112"));

        byte[] encoded = { 1, 0x1F, 26, (byte) 0xFF, 76, 0, 2, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
                17, 17, 18, 0, 5, 0, 2, 50, 0 };

        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();
        BluetoothLeIBeacon decoded = decoder.decode(encoded);

        compare(beacon, decoded);
    }

    @Test
    public void testDecodeStopAt0() {
        byte[] encoded = { 1, 0x1F, 26, 1, 76, 0, 2, 21, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 18,
                0, 5, 0, 2, 50, 0, 0 };

        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();
        BluetoothLeIBeacon decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    // An AD length byte of 0xFF is sign-extended to -1 when widened to int,
    // so ptr += structSize + 1
    // must not become a no-op, or the parser loops forever without ever exiting the
    // array bounds.
    @Test(timeout = 2000)
    public void testDecodeDoesNotHangOnLengthByte0xFF() {
        byte[] encoded = { (byte) 0xFF, 0x00 };

        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();
        BluetoothLeIBeacon decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    @Test(timeout = 2000)
    public void testDecodeDoesNotHangOnLengthByte0xFFMidStream() {
        byte[] encoded = { 2, 1, 6, (byte) 0xFF, 0x00 };

        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();
        BluetoothLeIBeacon decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    // A truncated iBeacon prefix passes the manufacturer-specific-data and prefix checks
    // (Arrays.copyOfRange tolerates a short array), but the fixed-offset major/minor/txPower
    // reads that follow are raw array indexing and must not run past the array bounds.
    @Test
    public void testDecodeDoesNotThrowOnTruncatedIBeaconPrefix() {
        byte[] encoded = { 0x05, (byte) 0xFF, 0x4C, 0x00, 0x02, 0x15 };

        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();
        BluetoothLeIBeacon decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    // A non-zero AD length byte with nothing after it must not crash when the parser reads
    // the AD type byte at ptr + 1.
    @Test
    public void testDecodeDoesNotThrowOnLengthByteWithNoData() {
        byte[] encoded = { 0x01 };

        BluetoothLeIBeaconDecoderImpl decoder = new BluetoothLeIBeaconDecoderImpl();
        BluetoothLeIBeacon decoded = decoder.decode(encoded);

        assertNull(decoded);
    }

    private void compare(BluetoothLeIBeacon beacon, BluetoothLeIBeacon decoded) {
        assertEquals(beacon.getMajor(), decoded.getMajor());
        assertEquals(beacon.getMinor(), decoded.getMinor());
        assertEquals(beacon.getRssi(), decoded.getRssi());
        assertEquals(beacon.getTxPower(), decoded.getTxPower());
        assertEquals(beacon.getUuid(), decoded.getUuid());
        assertEquals(beacon.isBrEdrSupported(), decoded.isBrEdrSupported());
        assertEquals(beacon.isLeBrController(), decoded.isLeBrController());
        assertEquals(beacon.isLeBrHost(), decoded.isLeBrHost());
        assertEquals(beacon.isLeGeneral(), decoded.isLeGeneral());
        assertEquals(beacon.isLeLimited(), decoded.isLeLimited());
    }

}
