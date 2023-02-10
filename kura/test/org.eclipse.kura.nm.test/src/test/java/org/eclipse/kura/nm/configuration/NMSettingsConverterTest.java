/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.nm.NMDeviceType;
import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.Test;

public class NMSettingsConverterTest {

	Map<String, Variant<?>> internalComparatorMap;
	Map<String, Variant<?>> resultMap;

	Map<String, Map<String, Variant<?>>> internalComparatorAllSettingsMap;
	Map<String, Map<String, Variant<?>>> resultAllSettingsMap;

	Map<String, Object> internetNetworkPropertiesInstanciationMap;

	NetworkProperties networkProperties;

	Boolean hasIllegalArgumentExceptionBeenThrown = false;
	Boolean hasAGenericExecptionBeenThrown = false;

	@Test
	public void shouldThrowErrorWhenBuildSettingsWithEmptyMap() {
		givenEmptyMaps();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), "wlan0",
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void shouldThrowErrorWhenBuildIpv4SettingsWithEmptyMap() {
		givenEmptyMaps();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void shouldThrowErrorWhenBuildIpv6SettingsWithEmptyMap() {
		givenEmptyMaps();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv6SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void shouldThrowErrorWhenBuild80211WirelessSettingsWithEmptyMap() {
		givenEmptyMaps();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void shouldThrowErrorWhenBuild80211WirelessSecuritySettingsWithEmptyMap() {
		givenEmptyMaps();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSecuritySettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void shouldBuildIpv4SettingsWithExpectedInputsAndDhcpEnabledForWAN() {
		givenEmptyMaps();
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", true, "netIPv4StatusEnabledWAN");
		givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", true,
				"netIPv4StatusEnabledWAN");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void shouldBuildIpv4SettingsWithExpectedInputsAndDhcpDisabledForWAN() {
		givenEmptyMaps();
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusEnabledWAN");
		givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false,
				"netIPv4StatusEnabledWAN");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();

	}

	@Test
	public void shouldBuildIpv4SettingsWithExpectedInputsAndDhcpEnabledForLAN() {
		givenEmptyMaps();
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", true, "netIPv4StatusEnabledLAN");
		givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", true,
				"netIPv4StatusEnabledLAN");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void shouldBuildIpv4SettingsWithExpectedInputsAndDhcpDisabledForLAN() {
		givenEmptyMaps();
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusEnabledLAN");
		givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false,
				"netIPv4StatusEnabledLAN");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void shouldBuildIpv4SettingsWithExpectedInputsAndDhcpDisabledForOther() {
		givenEmptyMaps();
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusUnmanaged");
		givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false,
				"netIPv4StatusUnmanaged");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void shouldBuild80211WirelessSettingsWithExpectedInputs() {
		givenEmptyMaps();
		givenValid80211WirelessSettingsWithInterfaceNameAndSsid("wlan0", "testssid", "INFRA", true);
		givenExpectedValid80211WirelessSettingsWithInterfaceNameAndSsid("wlan0", "testssid", "infrastructure", true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void shouldBuild80211WirelessSecurityWithExpectedInputs() {
		givenEmptyMaps();
		givenValid80211WirelessSecuritySettingsWithInterfaceNameAndSsid("wlan0", "ssidtest", "propMode", true, true);
		givenExpected80211WirelessSecuritySettingsWithInterfaceNameAndSsid("wlan0", "ssidtest", "propMode", true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSecuritySettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void shouldBuildSettingsWithExpectedInputs() {
		givenEmptyMaps();
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusUnmanaged");
		givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false,
				"netIPv4StatusUnmanaged");
		givenValid80211WirelessSettingsWithInterfaceNameAndSsid("wlan0", "testssid", "INFRA", true);
		givenExpectedValid80211WirelessSettingsWithInterfaceNameAndSsid("wlan0", "testssid", "infrastructure", true);
		givenValid80211WirelessSecuritySettingsWithInterfaceNameAndSsid("wlan0", "ssidtest", "propMode", true, true);
		givenExpected80211WirelessSecuritySettingsWithInterfaceNameAndSsid("wlan0", "ssidtest", "propMode", true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), "wlan0",
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	// given

	public void givenEmptyMaps() {
		internalComparatorMap = new HashMap<>();
		resultMap = new HashMap<>();
		internalComparatorAllSettingsMap = new HashMap<>();
		resultAllSettingsMap = new HashMap<>();
		internetNetworkPropertiesInstanciationMap = new HashMap<>();
	}

	public void givenNetworkPropsCreatedWithTheMap(Map<String, Object> properties) {
		this.networkProperties = new NetworkProperties(properties);
	}

	public void givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(String inter, Boolean dhcpStatus,
			String netStatus) {
		internetNetworkPropertiesInstanciationMap
				.put(String.format("net.interface.%s.config.dhcpClient4.enabled", inter), dhcpStatus);
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.ip4.status", inter),
				netStatus);
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.ip4.address", inter),
				"192.168.0.12");
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.ip4.prefix", inter),
				(short) 25);
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.ip4.dnsServers", inter),
				"1.1.1.1");
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.ip4.gateway", inter),
				"192.168.0.1");
	}

	public void givenExpectedValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(String inter,
			Boolean dhcpStatus, String netStatus) {

		if (!dhcpStatus) {
			internalComparatorMap.put("method", new Variant<>("manual"));

			Map<String, Variant<?>> addressEntry = new HashMap<>();
			addressEntry.put("address", new Variant<>("192.168.0.12"));
			addressEntry.put("prefix", new Variant<>(new UInt32(25)));

			List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
			internalComparatorMap.put("address-data", new Variant<>(addressData, "aa{sv}"));

		} else {
			internalComparatorMap.put("method", new Variant<>("auto"));
		}

		if (netStatus.equals("netIPv4StatusEnabledLAN")) {
			internalComparatorMap.put("ignore-auto-dns", new Variant<>(true));
			internalComparatorMap.put("ignore-auto-routes", new Variant<>(true));
		} else if (netStatus.equals("netIPv4StatusEnabledWAN")) {
			Optional<List<String>> dnsServers = Optional.of(Arrays.asList("1.1.1.1"));
			if (dnsServers.isPresent()) {
				internalComparatorMap.put("dns", new Variant<>(Arrays.asList(new UInt32(16843009)), "au"));
				internalComparatorMap.put("ignore-auto-dns", new Variant<>(true));
			}
			Optional<String> gateway = Optional.of("192.168.0.1");
			if (gateway.isPresent()) {
				internalComparatorMap.put("gateway", new Variant<>(gateway.get()));
			}
		}

	}

	public void givenValid80211WirelessSettingsWithInterfaceNameAndSsid(String inter, String ssid, String propMode,
			Boolean channelEnabled) {
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.wifi.mode", inter),
				propMode);
		internetNetworkPropertiesInstanciationMap
				.put(String.format("net.interface.%s.config.wifi.%s.ssid", inter, propMode.toLowerCase()), ssid);
		internetNetworkPropertiesInstanciationMap.put(
				String.format("net.interface.%s.config.wifi.%s.radioMode", inter, propMode.toLowerCase()),
				"RADIO_MODE_80211a");
		if (channelEnabled) {
			internetNetworkPropertiesInstanciationMap
					.put(String.format("net.interface.%s.config.wifi.%s.channel", inter, propMode.toLowerCase()), "10");
		}
	}

	public void givenExpectedValid80211WirelessSettingsWithInterfaceNameAndSsid(String inter, String ssid,
			String propMode, Boolean channelEnabled) {
		internalComparatorMap.put("mode", new Variant<>(propMode));
		// TODO: investigate this .getBytes discrepancy
		internalComparatorMap.put("ssid", new Variant<>(ssid.getBytes(StandardCharsets.UTF_8)));
		internalComparatorMap.put("band", new Variant<>("a"));
		if (channelEnabled) {
			internalComparatorMap.put("channel", new Variant<>(new UInt32(Short.parseShort("10"))));
		}
	}

	public void givenValid80211WirelessSecuritySettingsWithInterfaceNameAndSsid(String inter, String ssid,
			String propMode, Boolean groupEnabled, Boolean ciphersEnabled) {
		internetNetworkPropertiesInstanciationMap.put(String.format("net.interface.%s.config.wifi.mode", inter),
				propMode);
		internetNetworkPropertiesInstanciationMap.put(
				String.format("net.interface.%s.config.wifi.%s.passphrase", inter, propMode.toLowerCase()),
				new Password("test"));
		internetNetworkPropertiesInstanciationMap.put(
				String.format("net.interface.%s.config.wifi.%s.securityType", inter, propMode.toLowerCase()),
				"SECURITY_WPA_WPA2"); // wpa
		if (groupEnabled) {
			internetNetworkPropertiesInstanciationMap.put(
					String.format("net.interface.%s.config.wifi.%s.groupCiphers", inter, propMode.toLowerCase()),
					"CCMP"); // option
		}
		if (ciphersEnabled) {
			internetNetworkPropertiesInstanciationMap.put(
					String.format("net.interface.%s.config.wifi.%s.pairwiseCiphers", inter, propMode.toLowerCase()),
					"CCMP"); // ccmp
		}
	}

	public void givenExpected80211WirelessSecuritySettingsWithInterfaceNameAndSsid(String inter, String ssid,
			String propMode, Boolean groupEnabled, Boolean ciphersEnabled) {

		internalComparatorMap.put("psk", new Variant<>(new Password("test").toString()));
		internalComparatorMap.put("key-mgmt", new Variant<>("wpa-psk"));

		if (groupEnabled) {
			internalComparatorMap.put("group", new Variant<>(Arrays.asList("ccmp"), "as"));
		}

		if (ciphersEnabled) {
			internalComparatorMap.put("pairwise", new Variant<>(Arrays.asList("ccmp"), "as"));
		}

	}

	// when

	public void whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties properties,
			Optional<Connection> oldConnection, String iface, NMDeviceType deviceType) {
		try {
			this.resultAllSettingsMap = NMSettingsConverter.buildSettings(properties, oldConnection, iface, deviceType);
		} catch (IllegalArgumentException e) {
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props, String iface) {
		try {
			this.resultMap = NMSettingsConverter.buildIpv4Settings(props, iface);
		} catch (IllegalArgumentException e) {
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuildIpv6SettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props, String iface) {
		try {
			this.resultMap = NMSettingsConverter.buildIpv6Settings(props, iface);
		} catch (IllegalArgumentException e) {
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuild80211WirelessSettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props,
			String iface) {
		try {
			this.resultMap = NMSettingsConverter.build80211WirelessSettings(props, iface);
		} catch (IllegalArgumentException e) {
			hasIllegalArgumentExceptionBeenThrown = true;
		}
	}

	public void whenBuild80211WirelessSecuritySettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props,
			String iface) {
		try {

		} catch (IllegalArgumentException e) {
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			hasAGenericExecptionBeenThrown = true;
		}
		this.resultMap = NMSettingsConverter.build80211WirelessSecuritySettings(props, iface);
	}

	// then

	public void thenMapResultShouldEqualInternalMap() {
		assertEquals(this.internalComparatorMap, this.resultMap);
	}

	public void thenMapResultShouldEqualInternalBuildMap() {
		assertEquals(this.internalComparatorAllSettingsMap, this.resultAllSettingsMap);
	}
	
	public void thenIllegalArgumentExceptionHasBeenThrown() {
		assertTrue(this.hasIllegalArgumentExceptionBeenThrown);
	}
	
	public void thenNoExceptionsHaveBeenThrown() {
		assertFalse(this.hasIllegalArgumentExceptionBeenThrown);
		assertFalse(this.hasAGenericExecptionBeenThrown);
	}

}
