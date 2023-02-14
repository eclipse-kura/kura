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

	String netInterface;
	String kuraIP4Status;
	Boolean KuraDhcpStatus;

	@Test
	public void buildSettingsShouldThrowWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI);
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void buildIpv4SettingsShouldThrowWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void buildIpv6SettingsShouldThrowErrorWhenWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void build80211WirelessSettingsShouldThrowErrorWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void build80211WirelessSecuritySettingsShouldThrowWhenGivenEmptyMap() {
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");
		thenIllegalArgumentExceptionHasBeenThrown();
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForWan() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusEnabledWAN");
		givenDhcpStatus(true);

		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);

		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
		thenNoExceptionsHaveBeenThrown();

		thenResultingMapContains("method", "auto");
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndEnabledForWan() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusEnabledWAN");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingMapContains("method", "manual");
		thenResultingMapContains("address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingMapContains("dns", new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
		thenResultingMapContains("ignore-auto-dns", true);
		thenResultingMapContains("gateway", "192.168.0.1");

	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForLan() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusEnabledLAN");
		givenDhcpStatus(true);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingMapContains("method", "auto");
		thenResultingMapContains("ignore-auto-dns", true);
		thenResultingMapContains("ignore-auto-routes", true);
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndEnabledForLan() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusEnabledLAN");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingMapContains("method", "manual");
		thenResultingMapContains("address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingMapContains("ignore-auto-dns", true);
		thenResultingMapContains("ignore-auto-routes", true);
	}

	@Test
	public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndUnmanaged() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusUnmanaged");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingMapContains("method", "manual");
		thenResultingMapContains("address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
	}

	@Test
	public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndSetToInfraAndWithChannelField() {
		givenNetInterface("wlan0");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ssid", "ssidtest");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.radioMode", "RADIO_MODE_80211a");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.channel", "10");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");
	
		thenNoExceptionsHaveBeenThrown();		
		thenResultingMapContains("mode", "infrastructure");
		thenResultingMapContainsBytes("ssid", "ssidtest");
		thenResultingMapContains("band", "a");
		thenResultingMapContains("channel", new UInt32(Short.parseShort("10")));
	}

	@Test
	public void build80211WirelessSecuritySettingsShouldWorkWhenGivenExpectedMap() {
		givenNetInterface("wlan0");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.passphrase",new Password("test"));
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.securityType","SECURITY_WPA_WPA2");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.groupCiphers", "CCMP");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.pairwiseCiphers", "CCMP");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingMapContains("psk", new Password("test").toString());
		thenResultingMapContains("key-mgmt", "wpa-psk");
		thenResultingMapContains("group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
		thenResultingMapContains("pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiUnmanged() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusUnmanaged");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ssid", "ssidtest");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.radioMode", "RADIO_MODE_80211a");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.channel", "10");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.passphrase",new Password("test"));
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.securityType","SECURITY_WPA_WPA2");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.groupCiphers", "CCMP");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.pairwiseCiphers", "CCMP");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
		thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
		thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
		thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
		thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
		thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
		thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
		thenResultingBuildAllMapContains("802-11-wireless-security", "group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
		thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLan() {

		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusManagedLan");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ssid", "ssidtest");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.radioMode", "RADIO_MODE_80211a");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.channel", "10");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.passphrase",new Password("test"));
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.securityType","SECURITY_WPA_WPA2");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.groupCiphers", "CCMP");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.pairwiseCiphers", "CCMP");
		
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
		thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
		thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
		thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
		thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
		thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
		thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
		thenResultingBuildAllMapContains("802-11-wireless-security", "group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
		thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedConfiguredForInputsWiFiWan() {

		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusManagedWan");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ssid", "ssidtest");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.radioMode", "RADIO_MODE_80211a");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.channel", "10");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.passphrase",new Password("test"));
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.securityType","SECURITY_WPA_WPA2");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.groupCiphers", "CCMP");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.pairwiseCiphers", "CCMP");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
		thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
		thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
		thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
		thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
		thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
		thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
		thenResultingBuildAllMapContains("802-11-wireless-security", "group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
		thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanAndHiddenSsid() {

		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusManagedLan");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ssid", "ssidtest");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.radioMode", "RADIO_MODE_80211a");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.channel", "10");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ignoreSSID", true);
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.passphrase",new Password("test"));
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.securityType","SECURITY_WPA_WPA2");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.groupCiphers", "CCMP");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.pairwiseCiphers", "CCMP");		
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
		thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
		thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
		thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
		thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
		thenResultingBuildAllMapContains("802-11-wireless", "hidden", true);
		thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
		thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
		thenResultingBuildAllMapContains("802-11-wireless-security", "group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
		thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiWanAndHiddenSsid() {
		givenNetInterface("wlan0");
		givenIP4Status("netIPv4StatusManagedWan");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ssid", "ssidtest");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.radioMode", "RADIO_MODE_80211a");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.channel", "10");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.ignoreSSID", true);
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.mode", "INFRA");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.passphrase",new Password("test"));
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.securityType","SECURITY_WPA_WPA2");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.groupCiphers", "CCMP");
		givenMapWith("net.interface." + this.netInterface + ".config.wifi.infra.pairwiseCiphers", "CCMP");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_WIFI);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
		thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
		thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
		thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
		thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
		thenResultingBuildAllMapContains("802-11-wireless", "hidden", true);
		thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
		thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
		thenResultingBuildAllMapContains("802-11-wireless-security", "group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
		thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndUnmanaged() {
		givenNetInterface("eth0");
		givenIP4Status("netIPv4StatusUnmanaged");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndLan() {
		givenNetInterface("eth0");
		givenIP4Status("netIPv4StatusManagedLan");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
		
		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
		
		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
	}

	@Test
	public void buildSettingsShouldWorkWithExpectedInputsEthernetAndWan() {
		givenNetInterface("eth0");
		givenIP4Status("netIPv4StatusManagedWan");
		givenDhcpStatus(false);
		givenMapWith("net.interface." + this.netInterface + ".config.dhcpClient4.enabled", this.KuraDhcpStatus);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.status", this.kuraIP4Status);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.address", "192.168.0.12");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.prefix", (short) 25);
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.dnsServers", "1.1.1.1");
		givenMapWith("net.interface." + this.netInterface + ".config.ip4.gateway", "192.168.0.1");
		givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

		whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), netInterface,
				NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

		thenNoExceptionsHaveBeenThrown();
		thenResultingBuildAllMapContains("ipv6", "method", "disabled");
		thenResultingBuildAllMapContains("ipv4", "method", "manual");
		thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
		thenResultingBuildAllMapContains("connection", "id", "kura-" + this.netInterface + "-connection");
		thenResultingBuildAllMapContains("connection", "interface-name", this.netInterface);
		thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
	}

	// given

	public void givenNetInterface(String netInterface) {
		this.netInterface = netInterface;
	}

	public void givenIP4Status(String kuraIP4Status) {
		this.kuraIP4Status = kuraIP4Status;
	}

	public void givenDhcpStatus(Boolean KuraDhcpStatus) {
		this.KuraDhcpStatus = KuraDhcpStatus;
	}

	public void givenNetworkPropsCreatedWithTheMap(Map<String, Object> properties) {
		this.networkProperties = new NetworkProperties(properties);
	}

	public void givenMapWith(String key, Object value) {
		this.internetNetworkPropertiesInstanciationMap.put(key, value);
	}

	// when

	public void whenBuildSettingsIsRunWith(NetworkProperties properties, Optional<Connection> oldConnection,
			String iface, NMDeviceType deviceType) {
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

	public void whenBuildIpv4SettingsIsRunWith(NetworkProperties props, String iface) {
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

	public void whenBuildIpv6SettingsIsRunWith(NetworkProperties props, String iface) {
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

	public void whenBuild80211WirelessSettingsIsRunWith(NetworkProperties props, String iface) {
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

	public void whenBuild80211WirelessSecuritySettingsIsRunWith(NetworkProperties props, String iface) {
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
	public void thenResultingMapContains(String key, Object value) {
		assertEquals(value, this.resultMap.get(key).getValue());
	}
	
	public void thenResultingMapContainsBytes(String key, Object value) {
		assertEquals(value, new String((byte[]) this.resultMap.get(key).getValue(), StandardCharsets.UTF_8));
	}

	public void thenResultingBuildAllMapContains(String key, String subKey, Object value) {
		assertEquals(value, this.resultAllSettingsMap.get(key).get(subKey).getValue());
	}
	
	public void thenResultingBuildAllMapContainsBytes(String key, String subKey, Object value) {
		assertEquals(value, new String((byte[]) this.resultAllSettingsMap.get(key).get(subKey).getValue(), StandardCharsets.UTF_8));
	}

	public void thenIllegalArgumentExceptionHasBeenThrown() {
		assertTrue(this.hasIllegalArgumentExceptionBeenThrown);
	}

	public void thenNoExceptionsHaveBeenThrown() {
		assertFalse(this.hasIllegalArgumentExceptionBeenThrown);
		assertFalse(this.hasAGenericExecptionBeenThrown);
	}

	//helper classes
	public Object buildAddressDataWith(String ipAddr, UInt32 prefix) {
		
		Map<String, Variant<?>> addressEntry = new HashMap<>();
		addressEntry.put("address", new Variant<>(ipAddr));
		addressEntry.put("prefix", new Variant<>(prefix));
		
		List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
		
		Variant<?> dataVarient = new Variant<>(addressData, "aa{sv}");
		
		return dataVarient.getValue();
	}
}
