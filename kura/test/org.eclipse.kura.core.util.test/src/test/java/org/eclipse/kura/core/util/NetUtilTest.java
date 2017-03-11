/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.util;

import static org.junit.Assert.*;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetUtilTest {

	@Test
	public void testHardwareAddressToString() {
		byte[] input = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB};
		String result = NetUtil.hardwareAddressToString(input);
		assertEquals("01:23:45:67:89:AB", result);
	}

	@Test
	public void testHardwareAddressToStringNull() {
		byte[] input = null;
		String result = NetUtil.hardwareAddressToString(input);
		assertEquals("N/A", result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToStringEmptyValue() {
		NetUtil.hardwareAddressToString(new byte[]{});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToStringNotEnoughElements() {
		byte[] input = {0x01, 0x23, 0x45, 0x67, (byte) 0x89};
		NetUtil.hardwareAddressToString(input);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToStringTooManyElements() {
		byte[] input = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, 0x42};
		NetUtil.hardwareAddressToString(input);
	}

	@Test
	public void testHardwareAddressToBytes() {
		byte[] result = NetUtil.hardwareAddressToBytes("01:23:45:67:89:AB");
		byte[] expected = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB};
		assertArrayEquals(expected, result);
	}

	@Test
	public void testHardwareAddressToBytesNullOrEmpty() {
		byte[] expected = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

		byte[] result = NetUtil.hardwareAddressToBytes(null);
		assertArrayEquals(expected, result);

		result = NetUtil.hardwareAddressToBytes("");
		assertArrayEquals(expected, result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToBytesInvalidValue1() {
		NetUtil.hardwareAddressToBytes("g1:23:45:67:89:AB");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToBytesInvalidValue2() {
		NetUtil.hardwareAddressToBytes("01::45:67:89:AB");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToBytesInvalidValueNotEnoughElements() {
		NetUtil.hardwareAddressToBytes("01:23:45:67:89");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToBytesTooManyElements() {
		NetUtil.hardwareAddressToBytes("01:23:45:67:89:AB:42");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHardwareAddressToBytesOutOfRangeValue() {
		NetUtil.hardwareAddressToBytes("101:23:45:67:89:AB");
	}
}
