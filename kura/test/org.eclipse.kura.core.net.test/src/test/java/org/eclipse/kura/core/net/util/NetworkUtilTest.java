/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetworkUtilTest {

    @Test
    public void testCalculateNetwork() throws IllegalArgumentException {
        String result = NetworkUtil.calculateNetwork("192.168.1.123", "255.255.255.0");
        assertEquals("192.168.1.0", result);

        result = NetworkUtil.calculateNetwork("10.100.250.1", "255.0.0.0");
        assertEquals("10.0.0.0", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkNullAddress() {
        NetworkUtil.calculateNetwork(null, "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkEmptyAddress() {
        NetworkUtil.calculateNetwork("", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkTooShortAddress() {
        NetworkUtil.calculateNetwork("192.168.1", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkTooLongAddress() {
        NetworkUtil.calculateNetwork("192.168.1.123.1", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkInvalidAddress() {
        NetworkUtil.calculateNetwork("256.168.1.123", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkNullSubnet() {
        NetworkUtil.calculateNetwork("192.168.1.123", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkEmptySubnet() {
        NetworkUtil.calculateNetwork("192.168.1.123", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkTooShortSubnet() {
        NetworkUtil.calculateNetwork("192.168.1.123", "255.255.255");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkTooLongSubnet() {
        NetworkUtil.calculateNetwork("192.168.1.123", "255.255.255.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateNetworkInvalidSubnet() {
        NetworkUtil.calculateNetwork("192.168.1.123", "256.255.255.0");
    }

    @Test
    public void testCalculateBroadcast() throws IllegalArgumentException {
        String result = NetworkUtil.calculateBroadcast("192.168.1.123", "255.255.255.0");
        assertEquals("192.168.1.255", result);

        result = NetworkUtil.calculateBroadcast("10.100.250.1", "255.0.0.0");
        assertEquals("10.255.255.255", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastNullAddress() {
        NetworkUtil.calculateBroadcast(null, "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastEmptyAddress() {
        NetworkUtil.calculateBroadcast("", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastTooShortAddress() {
        NetworkUtil.calculateBroadcast("192.168.1", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastTooLongAddress() {
        NetworkUtil.calculateBroadcast("192.168.1.123.1", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastInvalidAddress() {
        NetworkUtil.calculateBroadcast("256.168.1.123", "255.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastNullSubnet() {
        NetworkUtil.calculateBroadcast("192.168.1.123", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastEmptySubnet() {
        NetworkUtil.calculateBroadcast("192.168.1.123", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastTooShortSubnet() {
        NetworkUtil.calculateBroadcast("192.168.1.123", "255.255.255");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastTooLongSubnet() {
        NetworkUtil.calculateBroadcast("192.168.1.123", "255.255.255.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBroadcastInvalidSubnet() {
        NetworkUtil.calculateBroadcast("192.168.1.123", "256.255.255.0");
    }

    @Test
    public void testGetNetmaskStringForm() throws IllegalArgumentException {
        String result = NetworkUtil.getNetmaskStringForm(32);
        assertEquals("255.255.255.255", result);

        result = NetworkUtil.getNetmaskStringForm(24);
        assertEquals("255.255.255.0", result);

        result = NetworkUtil.getNetmaskStringForm(17);
        assertEquals("255.255.128.0", result);

        result = NetworkUtil.getNetmaskStringForm(1);
        assertEquals("128.0.0.0", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskStringFormPrefixAboveUpperLimit() {
        NetworkUtil.getNetmaskStringForm(33);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskStringFormPrefixBelowLowerLimit() {
        NetworkUtil.getNetmaskStringForm(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskStringFormNegativePrefix() {
        NetworkUtil.getNetmaskStringForm(-1);
    }

    @Test
    public void testGetNetmaskShortForm() throws IllegalArgumentException {
        short result = NetworkUtil.getNetmaskShortForm("255.255.255.255");
        assertEquals(32, result);

        result = NetworkUtil.getNetmaskShortForm("255.255.0.0");
        assertEquals(16, result);

        result = NetworkUtil.getNetmaskShortForm("255.128.0.0");
        assertEquals(9, result);

        result = NetworkUtil.getNetmaskShortForm("128.0.0.0");
        assertEquals(1, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskShortFormNullSubnet() {
        NetworkUtil.getNetmaskShortForm(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskShortFormEmptySubnet() {
        NetworkUtil.getNetmaskShortForm("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskShortFormTooShortSubnet() {
        NetworkUtil.getNetmaskShortForm("255.255.255");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskShortFormTooLongSubnet() {
        NetworkUtil.getNetmaskShortForm("255.255.255.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskShortFormInvalidSubnet1() {
        NetworkUtil.getNetmaskShortForm("256.255.255.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNetmaskShortFormInvalidSubnet2() {
        NetworkUtil.getNetmaskShortForm("255.255.127.0");
    }

    @Test
    public void testDottedQuad() {
        String result = NetworkUtil.dottedQuad(0xFFFFFFFF);
        assertEquals("255.255.255.255", result);

        result = NetworkUtil.dottedQuad(0xFFFFFF00);
        assertEquals("255.255.255.0", result);

        result = NetworkUtil.dottedQuad(0xFF120000);
        assertEquals("255.18.0.0", result);

        result = NetworkUtil.dottedQuad(0x80000000);
        assertEquals("128.0.0.0", result);

        result = NetworkUtil.dottedQuad(0x00000000);
        assertEquals("0.0.0.0", result);
    }

    @Test
    public void testConvertIp4Address() throws IllegalArgumentException {
        int result = NetworkUtil.convertIp4Address("255.255.255.255");
        assertEquals(0xFFFFFFFF, result);

        result = NetworkUtil.convertIp4Address("255.255.255.0");
        assertEquals(0xFFFFFF00, result);

        result = NetworkUtil.convertIp4Address("255.18.0.0");
        assertEquals(0xFF120000, result);

        result = NetworkUtil.convertIp4Address("128.0.0.0");
        assertEquals(0x80000000, result);

        result = NetworkUtil.convertIp4Address("0.0.0.0");
        assertEquals(0x00000000, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIp4AddressNullAddress() {
        NetworkUtil.convertIp4Address(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIp4AddressEmptyAddress() {
        NetworkUtil.convertIp4Address("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIp4AddressTooShortAddress() {
        NetworkUtil.convertIp4Address("192.168.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIp4AddressTooLongAddress() {
        NetworkUtil.convertIp4Address("192.168.1.123.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIp4AddressInvalidAddress() {
        NetworkUtil.convertIp4Address("256.168.1.123");
    }

    @Test
    public void testPackIp4AddressBytes() throws IllegalArgumentException {
        int result = NetworkUtil.packIp4AddressBytes(new short[] { 255, 255, 255, 255 });
        assertEquals(0xFFFFFFFF, result);

        result = NetworkUtil.packIp4AddressBytes(new short[] { 255, 255, 255, 0 });
        assertEquals(0xFFFFFF00, result);

        result = NetworkUtil.packIp4AddressBytes(new short[] { 255, 255, 18, 0 });
        assertEquals(0xFFFF1200, result);

        result = NetworkUtil.packIp4AddressBytes(new short[] { 128, 0, 0, 0 });
        assertEquals(0x80000000, result);

        result = NetworkUtil.packIp4AddressBytes(new short[] { 0, 0, 0, 0 });
        assertEquals(0x00000000, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPackIp4AddressBytesNullValue() {
        NetworkUtil.packIp4AddressBytes(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPackIp4AddressBytesEmptyValue() {
        NetworkUtil.packIp4AddressBytes(new short[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPackIp4AddressBytesTooShortValue() {
        NetworkUtil.packIp4AddressBytes(new short[] { 192, 168, 1 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPackIp4AddressBytesTooLongValue() {
        NetworkUtil.packIp4AddressBytes(new short[] { 192, 168, 1, 123, 1 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPackIp4AddressBytesInvalidValue1() {
        NetworkUtil.packIp4AddressBytes(new short[] { 256, 168, 1, 123 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPackIp4AddressBytesInvalidValue2() {
        NetworkUtil.packIp4AddressBytes(new short[] { -1, 168, 1, 123 });
    }

    @Test
    public void testUnpackIP4AddressInt() {
        short[] result = NetworkUtil.unpackIP4AddressInt(0xFFFFFFFF);
        assertArrayEquals(new short[] { 255, 255, 255, 255 }, result);

        result = NetworkUtil.unpackIP4AddressInt(0xFFFFFF00);
        assertArrayEquals(new short[] { 255, 255, 255, 0 }, result);

        result = NetworkUtil.unpackIP4AddressInt(0xFFFF1200);
        assertArrayEquals(new short[] { 255, 255, 18, 0 }, result);

        result = NetworkUtil.unpackIP4AddressInt(0x80000000);
        assertArrayEquals(new short[] { 128, 0, 0, 0 }, result);

        result = NetworkUtil.unpackIP4AddressInt(0x00000000);
        assertArrayEquals(new short[] { 0, 0, 0, 0 }, result);
    }

    @Test
    public void testConvertIP6AddressStringFullFormat() throws IllegalArgumentException {
        byte[] result = NetworkUtil.convertIP6Address("2001:db8:85a3:0:0:8a2e:370:7334");
        byte[] expected = { 0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0x00, 0x00, 0x00, 0x00,
                (byte) 0x8a, 0x2e, 0x03, 0x70, 0x73, 0x34 };
        assertArrayEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringNullValue() {
        String input = null;
        NetworkUtil.convertIP6Address(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringEmptyValue() {
        NetworkUtil.convertIP6Address("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringInvalidValue() {
        NetworkUtil.convertIP6Address("g001:db8:85a3:0:0:8a2e:370:7334");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringOutOfRangeValue1() {
        NetworkUtil.convertIP6Address("12345:db8:85a3:0:0:8a2e:370:7334");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringOutOfRangeValue2() {
        NetworkUtil.convertIP6Address("-1:db8:85a3:0:0:8a2e:370:7334");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringNotEnoughGroups() {
        NetworkUtil.convertIP6Address("2001:db8:85a3:0:0:8a2e:370");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressStringToManyGroups() {
        NetworkUtil.convertIP6Address("2001:db8:85a3:0:0:8a2e:370:7334:7334");
    }

    @Test
    public void testConvertIP6AddressByteArray() throws IllegalArgumentException {
        byte[] input = { 0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0x00, 0x00, 0x00, 0x00, (byte) 0x8a,
                0x2e, 0x03, 0x70, 0x73, 0x34 };
        String result = NetworkUtil.convertIP6Address(input);
        assertEquals("2001:db8:85a3:0:0:8a2e:370:7334", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressByteArrayNullValue() {
        byte[] input = null;
        NetworkUtil.convertIP6Address(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressByteArrayEmptyValue() {
        NetworkUtil.convertIP6Address(new byte[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressByteArrayNotEnoughElements() {
        byte[] input = { 0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0x00, 0x00, 0x00, 0x00, (byte) 0x8a,
                0x2e, 0x03, 0x70, 0x73 };
        NetworkUtil.convertIP6Address(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertIP6AddressByteArrayTooManyElements() {
        byte[] input = { 0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0x00, 0x00, 0x00, 0x00, (byte) 0x8a,
                0x2e, 0x03, 0x70, 0x73, 0x34, 0x34 };
        NetworkUtil.convertIP6Address(input);
    }

    @Test
    public void testMacToString() throws IllegalArgumentException {
        byte[] input = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB };
        String result = NetworkUtil.macToString(input);
        assertEquals("01:23:45:67:89:AB", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToStringNullValue() {
        NetworkUtil.macToString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToStringEmptyValue() {
        NetworkUtil.macToString(new byte[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToStringNotEnoughElements() {
        byte[] input = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89 };
        NetworkUtil.macToString(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToStringTooManyElements() {
        byte[] input = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, 0x42 };
        NetworkUtil.macToString(input);
    }

    @Test
    public void testMacToBytes() throws IllegalArgumentException {
        byte[] result = NetworkUtil.macToBytes("01:23:45:67:89:AB");
        byte[] expected = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB };
        assertArrayEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesNullValue() {
        NetworkUtil.macToBytes(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesEmptyValue() {
        NetworkUtil.macToBytes("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesInvalidValue1() {
        NetworkUtil.macToBytes("g1:23:45:67:89:AB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesInvalidValue2() {
        NetworkUtil.macToBytes("01::45:67:89:AB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesNotEnoughElements() {
        NetworkUtil.macToBytes("01:23:45:67:89");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesTooManyElements() {
        NetworkUtil.macToBytes("01:23:45:67:89:AB:42");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMacToBytesOutOfRangeValue() {
        NetworkUtil.macToBytes("101:23:45:67:89:AB");
    }
}
