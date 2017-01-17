/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.EnumSet;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WifiAccessPointImplTest {

	@Test
	public void testWifiAccessPointImpl() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		assertEquals("ssid", ap.getSSID());
		assertNull(ap.getHardwareAddress());
		assertEquals(0, ap.getFrequency());
		assertNull(ap.getMode());
		assertNull(ap.getBitrate());
		assertEquals(0, ap.getStrength());
		assertNull(ap.getWpaSecurity());
		assertNull(ap.getRsnSecurity());
		assertNull(ap.getCapabilities());
	}

	@Test
	public void testGetSSID() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid1");
		assertEquals("ssid1", ap1.getSSID());

		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid2");
		assertEquals("ssid2", ap2.getSSID());

		WifiAccessPointImpl ap3 = new WifiAccessPointImpl(null);
		assertNull(ap3.getSSID());
	}

	@Test
	public void testHardwareAddress() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		byte[] mac1 = NetworkUtil.macToBytes("12:34:56:78:90:AB");
		ap.setHardwareAddress(mac1);
		assertArrayEquals(mac1, ap.getHardwareAddress());

		byte[] mac2 = NetworkUtil.macToBytes("11:22:33:44:55:66");
		ap.setHardwareAddress(mac2);
		assertArrayEquals(mac2, ap.getHardwareAddress());
	}

	@Test
	public void testFrequency() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setFrequency(42);
		assertEquals(42, ap.getFrequency());

		ap.setFrequency(100);
		assertEquals(100, ap.getFrequency());
	}

	@Test
	public void testMode() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setMode(WifiMode.ADHOC);
		assertEquals(WifiMode.ADHOC, ap.getMode());

		ap.setMode(WifiMode.INFRA);
		assertEquals(WifiMode.INFRA, ap.getMode());
	}

	@Test
	public void testBitrate() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ArrayList<Long> bitrate1 = new ArrayList<>();
		bitrate1.add((long) 1);
		bitrate1.add((long) 2);

		ap.setBitrate(bitrate1);
		assertEquals(bitrate1, ap.getBitrate());

		ArrayList<Long> bitrate2 = new ArrayList<>();
		bitrate2.add((long) 3);
		bitrate2.add((long) 4);

		ap.setBitrate(bitrate2);
		assertEquals(bitrate2, ap.getBitrate());
	}

	@Test
	public void testStrength() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setStrength(42);
		assertEquals(42, ap.getStrength());

		ap.setStrength(100);
		assertEquals(100, ap.getStrength());
	}

	@Test
	public void testWpaSecurity() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		EnumSet<WifiSecurity> ws1 = EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP);
		ap.setWpaSecurity(ws1);
		assertEquals(ws1, ap.getWpaSecurity());

		EnumSet<WifiSecurity> ws2 = EnumSet.of(WifiSecurity.GROUP_WEP104, WifiSecurity.GROUP_WEP40);
		ap.setWpaSecurity(ws2);
		assertEquals(ws2, ap.getWpaSecurity());
	}

	@Test
	public void testRsnSecurity() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		EnumSet<WifiSecurity> rs1 = EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP);
		ap.setRsnSecurity(rs1);
		assertEquals(rs1, ap.getRsnSecurity());

		EnumSet<WifiSecurity> rs2 = EnumSet.of(WifiSecurity.GROUP_WEP104, WifiSecurity.GROUP_WEP40);
		ap.setRsnSecurity(rs2);
		assertEquals(rs2, ap.getRsnSecurity());
	}

	@Test
	public void testCapabilities() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ArrayList<String> capabilities1 = new ArrayList<>();
		capabilities1.add("a");
		capabilities1.add("b");

		ap.setCapabilities(capabilities1);
		assertEquals(capabilities1, ap.getCapabilities());

		ArrayList<String> capabilities2 = new ArrayList<>();
		capabilities2.add("c");
		capabilities2.add("d");

		ap.setCapabilities(capabilities2);
		assertEquals(capabilities2, ap.getCapabilities());
	}

	@Test
	public void testToStringJustSSID() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithHardwareAddress() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		byte[] mac = NetworkUtil.macToBytes("12:34:56:78:90:AB");

		ap.setHardwareAddress(mac);

		String expected = "ssid=ssid :: hardwareAddress=12:34:56:78:90:AB :: frequency=0 :: mode=null :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithFrequency() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setFrequency(42);

		String expected = "ssid=ssid :: frequency=42 :: mode=null :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithMode() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setMode(WifiMode.ADHOC);

		String expected = "ssid=ssid :: frequency=0 :: mode=ADHOC :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithBitrate() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ArrayList<Long> bitrate = new ArrayList<Long>();
		bitrate.add((long) 1);
		bitrate.add((long) 2);

		ap.setBitrate(bitrate);

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: bitrate=1 2  :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithEmptyBitrate() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ArrayList<Long> bitrate = new ArrayList<>();
		ap.setBitrate(bitrate);

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithStrength() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setStrength(42);

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=42";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithWpaSecurity() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=0 :: wpaSecurity=GROUP_TKIP GROUP_CCMP ";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithEmptyWpaSecurity() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setWpaSecurity(EnumSet.noneOf(WifiSecurity.class));

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithRsnSecurity() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setRsnSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=0 :: rsnSecurity=GROUP_TKIP GROUP_CCMP ";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithEmptyRsnSecurity() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		ap.setRsnSecurity(EnumSet.noneOf(WifiSecurity.class));

		String expected = "ssid=ssid :: frequency=0 :: mode=null :: strength=0";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testToStringWithAllFields() {
		WifiAccessPointImpl ap = createWifiAccessPoint();

		String expected = "ssid=ssid :: hardwareAddress=12:34:56:78:90:AB :: frequency=42 :: mode=ADHOC"
				+ " :: bitrate=1 2  :: strength=42 :: wpaSecurity=GROUP_TKIP GROUP_CCMP "
				+ " :: rsnSecurity=GROUP_TKIP GROUP_CCMP ";

		assertEquals(expected, ap.toString());
	}

	@Test
	public void testEqualsObject() {
		WifiAccessPointImpl ap1 = createWifiAccessPoint();
		assertEquals(ap1, ap1);

		WifiAccessPointImpl ap2 = createWifiAccessPoint();
		assertEquals(ap1, ap2);

		ap1 = new WifiAccessPointImpl("ssid");
		ap2 = new WifiAccessPointImpl("ssid");
		assertEquals(ap1, ap2);
	}

	@Test
	public void testEqualsWithNull() {
		WifiAccessPointImpl ap1 = createWifiAccessPoint();
		WifiAccessPointImpl ap2 = null;

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsUnexpectedType() {
		WifiAccessPointImpl ap1 = createWifiAccessPoint();
		String str = "";

		assertNotEquals(ap1, str);
	}

	@Test
	public void testEqualsDifferentSSID() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid1");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid2");

		assertNotEquals(ap1, ap2);

		ap1 = new WifiAccessPointImpl(null);

		assertNotEquals(ap1, ap2);

		ap1.setStrength(42);
		ap2 = new WifiAccessPointImpl(null);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentHardwareAddress() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		byte[] mac1 = NetworkUtil.macToBytes("12:34:56:78:90:AB");
		byte[] mac2 = NetworkUtil.macToBytes("11:22:33:44:55:66");

		ap1.setHardwareAddress(mac1);
		ap2.setHardwareAddress(mac2);

		assertNotEquals(ap1, ap2);

		ap1.setHardwareAddress(null);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentFrequency() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ap1.setFrequency(42);
		ap2.setFrequency(100);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentMode() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ap1.setMode(WifiMode.ADHOC);
		ap2.setMode(WifiMode.INFRA);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentBitrate() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ArrayList<Long> bitrate1 = new ArrayList<>();
		bitrate1.add((long) 1);
		bitrate1.add((long) 2);

		ArrayList<Long> bitrate2 = new ArrayList<>();
		bitrate2.add((long) 3);
		bitrate2.add((long) 4);

		ap1.setBitrate(bitrate1);
		ap2.setBitrate(bitrate2);

		assertNotEquals(ap1, ap2);

		ap1.setBitrate(null);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentStrength() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ap1.setStrength(42);
		ap2.setStrength(100);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentWpaSecurity() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ap1.setWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));
		ap2.setWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_WEP104));

		assertNotEquals(ap1, ap2);

		ap1.setWpaSecurity(null);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentRsnSecurity() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ap1.setRsnSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));
		ap2.setRsnSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_WEP104));

		assertNotEquals(ap1, ap2);

		ap1.setRsnSecurity(null);

		assertNotEquals(ap1, ap2);
	}

	@Test
	public void testEqualsDifferentCapabilities() {
		WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid");
		WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid");

		ArrayList<String> capabilities1 = new ArrayList<>();
		capabilities1.add("a");
		capabilities1.add("b");

		ArrayList<String> capabilities2 = new ArrayList<>();
		capabilities2.add("c");
		capabilities2.add("d");

		ap1.setCapabilities(capabilities1);
		ap2.setCapabilities(capabilities2);

		assertNotEquals(ap1, ap2);

		ap1.setCapabilities(null);

		assertNotEquals(ap1, ap2);
	}

	WifiAccessPointImpl createWifiAccessPoint() {
		WifiAccessPointImpl ap = new WifiAccessPointImpl("ssid");

		byte[] mac = NetworkUtil.macToBytes("12:34:56:78:90:AB");

		ArrayList<Long> bitrate = new ArrayList<>();
		bitrate.add((long) 1);
		bitrate.add((long) 2);

		ArrayList<String> capabilities = new ArrayList<>();
		capabilities.add("a");
		capabilities.add("b");

		ap.setHardwareAddress(mac);
		ap.setFrequency(42);
		ap.setMode(WifiMode.ADHOC);
		ap.setBitrate(bitrate);
		ap.setStrength(42);
		ap.setWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));
		ap.setRsnSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));
		ap.setCapabilities(capabilities);

		return ap;
	}
}
