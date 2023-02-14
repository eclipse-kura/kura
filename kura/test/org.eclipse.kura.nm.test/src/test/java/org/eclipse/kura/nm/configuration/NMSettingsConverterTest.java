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

	Map<String, Variant<?>> internalComparatorMap = new HashMap<>();
	Map<String, Variant<?>> resultMap;

	Map<String, Map<String, Variant<?>>> internalComparatorAllSettingsMap = new HashMap<>();
	Map<String, Map<String, Variant<?>>> resultAllSettingsMap = new HashMap<>();

	Map<String, Object> internetNetworkPropertiesInstanciationMap = new HashMap<>();

	NetworkProperties networkProperties;

	Boolean hasIllegalArgumentExceptionBeenThrown = false;
	Boolean hasAGenericExecptionBeenThrown = false;

	@Test
	public void buildSettingsShouldThrowWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), "wlan0",
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void buildIpv4SettingsShouldThrowWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void buildIpv6SettingsShouldThrowErrorWhenWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv6SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void build80211WirelessSettingsShouldThrowErrorWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void build80211WirelessSecuritySettingsShouldThrowWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSecuritySettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForWan() {
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", true, "netIPv4StatusEnabledWAN");
		givenValidBuildIpv4ConfigWithDhcpEnabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToWan("wlan0");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndEnabledForWan() {
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusEnabledWAN");
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToWan("wlan0");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();

	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForLan() {
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", true, "netIPv4StatusEnabledLAN");
		givenValidBuildIpv4ConfigWithDhcpEnabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToLan("wlan0");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndEnabledForLan() {
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusEnabledLAN");
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToLan("wlan0");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndUnmanaged() {
		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus("wlan0", false, "netIPv4StatusUnmanaged");
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndSetToInfraAndWithChannelField() {
		givenValid80211WirelessSettingsWithTheFollowingParameters("wlan0", "testssid", "INFRA", true, false);
		givenExpectedValid80211WirelessSettingsWithTheFollowingParameters("wlan0", "testssid", "infrastructure", true,
				false);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultFromWifiSettingsShouldEqualInternalMapForWifiSettings();
	}

	@Test
	public void build80211WirelessSecuritySettingsShouldWorkWhenGivenExpectedMap() {
		givenValid80211WirelessSecuritySettingsWithTheFollowingParameters("wlan0", "ssidtest", "propmode", true, true);
		givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters("wlan0", "ssidtest", "propmode", true,
				true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSecuritySettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiUnmanged() {

		String netInterface = "wlan0";
		String kuraNetType = "netIPv4StatusUnmanaged";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true, false);
		givenExpectedValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, false);
		givenValid80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true,
				true);
		givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-11-wireless");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLan() {

		String netInterface = "wlan0";
		String kuraNetType = "netIPv4StatusManagedLan";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToLan(netInterface);
		givenValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true, false);
		givenExpectedValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, false);
		givenValid80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true,
				true);
		givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-11-wireless");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedConfiguredForInputsWiFiWan() {

		String netInterface = "wlan0";
		String kuraNetType = "netIPv4StatusManagedWan";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToWan(netInterface);
		givenValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true, false);
		givenExpectedValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, false);
		givenValid80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true,
				true);
		givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-11-wireless");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanAndHiddenSsid() {

		String netInterface = "wlan0";
		String kuraNetType = "netIPv4StatusManagedLan";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToLan(netInterface);
		givenValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true, true);
		givenExpectedValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenValid80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true,
				true);
		givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-11-wireless");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiWanAndHiddenSsid() {

		String netInterface = "wlan0";
		String kuraNetType = "netIPv4StatusManagedWan";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToWan(netInterface);
		givenValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true, true);
		givenExpectedValid80211WirelessSettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenValid80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "INFRA", true,
				true);
		givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters(netInterface, "ssidtest", "infrastructure",
				true, true);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-11-wireless");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndUnmanaged() {

		String netInterface = "eth0";
		String kuraNetType = "netIPv4StatusUnmanaged";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-3-ethernet");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndLan() {

		String netInterface = "eth0";
		String kuraNetType = "netIPv4StatusManagedLan";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToLan(netInterface);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-3-ethernet");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsEthernetAndWan() {

		String netInterface = "eth0";
		String kuraNetType = "netIPv4StatusManagedWan";

		givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(netInterface, false, kuraNetType);
		givenValidBuildIpv4ConfigWithDhcpDisabled();
		givenValidBuildIpv6Config();
		givenExpectedValidWifiConfigurationSetToWan(netInterface);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		givenExpectedBuildAllConnectionField(netInterface, "802-3-ethernet");
		whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
		thenNoExceptionsHaveBeenThrown();
		thenMapResultShouldEqualInternalBuildMap();
	}

	// given

	public void givenNetworkPropsCreatedWithTheMap(Map<String, Object> properties) {
		this.networkProperties = new NetworkProperties(properties);
	}

	public void givenValidWifiConfigurationWithInterfaceNameAndDhcpBoolAndNetStatus(String inter, Boolean dhcpStatus,
			String netStatus) {
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.dhcpClient4.enabled",
				dhcpStatus);
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.ip4.status", netStatus);
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.ip4.address", "192.168.0.12");
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.ip4.prefix", (short) 25);
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.ip4.dnsServers", "1.1.1.1");
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.ip4.gateway", "192.168.0.1");
	}

	public void givenExpectedValidWifiConfigurationSetToLan(String inter) {

		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		internalMethodHashMap.put("ignore-auto-dns", new Variant<>(true));
		internalMethodHashMap.put("ignore-auto-routes", new Variant<>(true));

		this.internalComparatorMap.putAll(internalMethodHashMap);

	}

	public void givenExpectedValidWifiConfigurationSetToWan(String inter) {

		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		Optional<List<String>> dnsServers = Optional.of(Arrays.asList("1.1.1.1"));
		if (dnsServers.isPresent()) {
			internalMethodHashMap.put("dns", new Variant<>(Arrays.asList(new UInt32(16843009)), "au"));
			internalMethodHashMap.put("ignore-auto-dns", new Variant<>(true));
		}

		Optional<String> gateway = Optional.of("192.168.0.1");
		if (gateway.isPresent()) {
			internalMethodHashMap.put("gateway", new Variant<>(gateway.get()));
		}

		this.internalComparatorMap.putAll(internalMethodHashMap);

	}

	public void givenValidBuildIpv4ConfigWithDhcpEnabled() {
		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		internalMethodHashMap.put("method", new Variant<>("auto"));

		this.internalComparatorMap.putAll(internalMethodHashMap);
		this.internalComparatorAllSettingsMap.put("ipv4", internalMethodHashMap);
	}

	public void givenValidBuildIpv4ConfigWithDhcpDisabled() {
		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		internalMethodHashMap.put("method", new Variant<>("manual"));

		Map<String, Variant<?>> addressEntry = new HashMap<>();
		addressEntry.put("address", new Variant<>("192.168.0.12"));
		addressEntry.put("prefix", new Variant<>(new UInt32(25)));

		List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
		internalMethodHashMap.put("address-data", new Variant<>(addressData, "aa{sv}"));

		this.internalComparatorMap.putAll(internalMethodHashMap);
		this.internalComparatorAllSettingsMap.put("ipv4", internalMethodHashMap);
	}

	public void givenValidBuildIpv6Config() {
		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();
		internalMethodHashMap.put("method", new Variant<>("disabled"));
		this.internalComparatorAllSettingsMap.put("ipv6", internalMethodHashMap);
	}

	public void givenValid80211WirelessSettingsWithTheFollowingParameters(String inter, String ssid, String propMode,
			Boolean channelEnabled, Boolean isHidden) {
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

		if (isHidden) {
			internetNetworkPropertiesInstanciationMap.put(
					String.format("net.interface.%s.config.wifi.%s.ignoreSSID", inter, propMode.toLowerCase()),
					isHidden);
		}

	}

	public void givenExpectedValid80211WirelessSettingsWithTheFollowingParameters(String inter, String ssid,
			String propMode, Boolean channelEnabled, Boolean isHidden) {

		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		internalMethodHashMap.put("mode", new Variant<>(propMode));
		internalMethodHashMap.put("ssid", new Variant<>(ssid.getBytes(StandardCharsets.UTF_8)));
		internalMethodHashMap.put("band", new Variant<>("a"));

		if (channelEnabled) {
			internalMethodHashMap.put("channel", new Variant<>(new UInt32(Short.parseShort("10"))));
		}

		if (isHidden) {
			internalMethodHashMap.put("hidden", new Variant<>(isHidden));
		}

		this.internalComparatorAllSettingsMap.put("802-11-wireless", internalMethodHashMap);
		this.internalComparatorMap.putAll(internalMethodHashMap);
	}

	public void givenValid80211WirelessSecuritySettingsWithTheFollowingParameters(String inter, String ssid,
			String propMode, Boolean groupEnabled, Boolean ciphersEnabled) {
		internetNetworkPropertiesInstanciationMap.put("net.interface." + inter + ".config.wifi.mode", propMode);
		internetNetworkPropertiesInstanciationMap.put(
				"net.interface." + inter + ".config.wifi." + propMode.toLowerCase() + ".passphrase",
				new Password("test"));
		internetNetworkPropertiesInstanciationMap.put(
				"net.interface." + inter + ".config.wifi." + propMode.toLowerCase() + ".securityType",
				"SECURITY_WPA_WPA2");
		if (groupEnabled) {
			internetNetworkPropertiesInstanciationMap
					.put("net.interface." + inter + ".config.wifi." + propMode.toLowerCase() + ".groupCiphers", "CCMP");
		}
		if (ciphersEnabled) {
			internetNetworkPropertiesInstanciationMap.put(
					"net.interface." + inter + ".config.wifi." + propMode.toLowerCase() + ".pairwiseCiphers", "CCMP");
		}
	}

	public void givenExpected80211WirelessSecuritySettingsWithTheFollowingParameters(String inter, String ssid,
			String propMode, Boolean groupEnabled, Boolean ciphersEnabled) {

		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		internalMethodHashMap.put("psk", new Variant<>(new Password("test").toString()));
		internalMethodHashMap.put("key-mgmt", new Variant<>("wpa-psk"));

		if (groupEnabled) {
			internalMethodHashMap.put("group", new Variant<>(Arrays.asList("ccmp"), "as"));
		}

		if (ciphersEnabled) {
			internalMethodHashMap.put("pairwise", new Variant<>(Arrays.asList("ccmp"), "as"));
		}

		this.internalComparatorAllSettingsMap.put("802-11-wireless-security", internalMethodHashMap);
		this.internalComparatorMap.putAll(internalMethodHashMap);

	}

	public void givenExpectedBuildAllConnectionField(String inter, String interfaceType) {
		Map<String, Variant<?>> internalMethodHashMap = new HashMap<>();

		internalMethodHashMap.put("id", new Variant<>(String.format("kura-%s-connection", inter)));
		internalMethodHashMap.put("interface-name", new Variant<>(inter));
		internalMethodHashMap.put("type", new Variant<>(interfaceType));

		this.internalComparatorAllSettingsMap.put("connection", internalMethodHashMap);
	}

	// when

	public void whenBuildSettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties properties,
			Optional<Connection> oldConnection, String iface, NMDeviceType deviceType) {
		try {
			this.resultAllSettingsMap = NMSettingsConverter.buildSettings(properties, oldConnection, iface, deviceType);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			e.printStackTrace();
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuildIpv4SettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props, String iface) {
		try {
			this.resultMap = NMSettingsConverter.buildIpv4Settings(props, iface);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			e.printStackTrace();
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuildIpv6SettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props, String iface) {
		try {
			this.resultMap = NMSettingsConverter.buildIpv6Settings(props, iface);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			e.printStackTrace();
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuild80211WirelessSettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props,
			String iface) {
		try {
			this.resultMap = NMSettingsConverter.build80211WirelessSettings(props, iface);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			e.printStackTrace();
			hasAGenericExecptionBeenThrown = true;
		}
	}

	public void whenBuild80211WirelessSecuritySettingsIsRunWithNetworkPropsAndIfaceString(NetworkProperties props,
			String iface) {
		try {
			this.resultMap = NMSettingsConverter.build80211WirelessSecuritySettings(props, iface);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			hasIllegalArgumentExceptionBeenThrown = true;
		} catch (Exception e) {
			e.printStackTrace();
			hasAGenericExecptionBeenThrown = true;
		}
	}

	// then

	public void thenMapResultShouldEqualInternalMap() {
		assertEquals(this.internalComparatorMap, this.resultMap);
	}

	public void thenMapResultFromWifiSettingsShouldEqualInternalMapForWifiSettings() {
		// Workaround to compare String.getBytes(StandardCharsets.UTF_8)

		String internalSsid = new String((byte[]) internalComparatorMap.get("ssid").getValue(), StandardCharsets.UTF_8);
		String resultSsid = new String((byte[]) this.resultMap.get("ssid").getValue(), StandardCharsets.UTF_8);

		assertEquals(internalSsid, resultSsid);

		// Remove SSID fields from Maps before comparison
		this.internalComparatorMap.put("ssid", new Variant<>(""));
		this.resultMap.put("ssid", new Variant<>(""));

		assertEquals(this.internalComparatorMap, this.resultMap);
	}

	public void thenMapResultShouldEqualInternalBuildMap() {

		// Workaround to compare String.getBytes(StandardCharsets.UTF_8)
		if (this.internalComparatorAllSettingsMap.containsKey("802-11-wireless")) {
			String internalSsid = new String(
					(byte[]) internalComparatorAllSettingsMap.get("802-11-wireless").get("ssid").getValue(),
					StandardCharsets.UTF_8);
			String resultSsid = new String(
					(byte[]) this.resultAllSettingsMap.get("802-11-wireless").get("ssid").getValue(),
					StandardCharsets.UTF_8);

			internalComparatorAllSettingsMap.get("802-11-wireless").put("ssid", new Variant<>("wpa-psk"));
			resultAllSettingsMap.get("802-11-wireless").put("ssid", new Variant<>("wpa-psk"));

			assertEquals(internalSsid, resultSsid);
		}
		// Remove SSID fields from Maps before comparison

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
