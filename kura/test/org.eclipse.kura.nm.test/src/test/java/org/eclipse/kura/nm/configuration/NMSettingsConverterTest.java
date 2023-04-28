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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.nm.NMDeviceType;
import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.Test;
import org.mockito.Mockito;

public class NMSettingsConverterTest {

    Map<String, Variant<?>> internalComparatorMap = new HashMap<>();
    Map<String, Variant<?>> resultMap;

    Map<String, Map<String, Variant<?>>> internalComparatorAllSettingsMap = new HashMap<>();
    Map<String, Map<String, Variant<?>>> resultAllSettingsMap = new HashMap<>();

    Map<String, Object> internetNetworkPropertiesInstanciationMap = new HashMap<>();

    NetworkProperties networkProperties;
    Connection mockedConnection;

    Boolean hasNoSuchElementExceptionBeenThrown = false;
    Boolean hasAnIllegalArgumentExceptionThrown = false;
    Boolean hasAGenericExecptionBeenThrown = false;

    @Test
    public void buildSettingsShouldThrowWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);
        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildIpv4SettingsShouldThrowWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void build80211WirelessSettingsShouldThrowErrorWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");
        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldThrowWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");
        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForWan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");

        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
        thenNoExceptionsHaveBeenThrown();

        thenResultingMapContains("method", "auto");
    }

    @Test
    public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndEnabledForWan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
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
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("ignore-auto-routes", true);
    }

    @Test
    public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsFalseAndEnabledForLan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
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
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("method", "manual");
        thenResultingMapContains("address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndSetToInfraAndWithChannelField() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "a");
        thenResultingMapContains("channel", new UInt32(Short.parseShort("10")));
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndModeAp() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "MASTER");
        givenMapWith("net.interface.wlan0.config.wifi.master.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.master.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.master.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "ap");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "a");
        thenResultingMapContains("channel", new UInt32(Short.parseShort("10")));
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndModeMalformed() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "MALFORMED_VALUE");
        givenMapWith("net.interface.wlan0.config.wifi.master.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.master.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.master.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenIllegalArgumentExceptionThrown();
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndBandBg() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211b");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "bg");
        thenResultingMapContains("channel", new UInt32(10));
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndBandBgExt() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211nHT20");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "bg");
        thenResultingMapContains("channel", new UInt32(10));
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndMalformedBand() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "MALFORMED_VALUE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenIllegalArgumentExceptionThrown();
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWithChannel0And2Ghz() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211b");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "0");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "bg");
        thenResultingMapContains("channel", new UInt32(0));
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWithChannel0And5Ghz() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "0");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "a");
        thenResultingMapContains("channel", new UInt32(0));
    }

    @Test
    public void build80211WirelessSettingsAutomaticBandSelectionShouldWorkWithChannel0() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211nHT20");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "0");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("channel", new UInt32(0));
        thenResultingMapNotContains("band");
    }

    @Test
    public void build80211WirelessSettingsAutomaticBandSelectionShouldWorkWithChannel2Ghz() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211nHT20");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "bg");
        thenResultingMapContains("channel", new UInt32(1));
    }

    @Test
    public void build80211WirelessSettingsAutomaticBandSelectionShouldWorkWithChannel5Ghz() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211nHT20");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "44");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("mode", "infrastructure");
        thenResultingMapContainsBytes("ssid", "ssidtest");
        thenResultingMapContains("band", "a");
        thenResultingMapContains("channel", new UInt32(44));
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenExpectedMap() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("psk", new Password("test").toString());
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("group", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingMapContains("pairwise", new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingMapContains("proto", new Variant<>(Arrays.asList(), "as").getValue());
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenExpectedMapTkip() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "TKIP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "TKIP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("psk", new Password("test").toString());
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("group", new Variant<>(Arrays.asList("tkip"), "as").getValue());
        thenResultingMapContains("pairwise", new Variant<>(Arrays.asList("tkip"), "as").getValue());
        thenResultingMapContains("proto", new Variant<>(Arrays.asList(), "as").getValue());
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenExpectedMapCcmpTkip() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP_TKIP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP_TKIP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("psk", new Password("test").toString());
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("group", new Variant<>(Arrays.asList("tkip", "ccmp"), "as").getValue());
        thenResultingMapContains("pairwise", new Variant<>(Arrays.asList("tkip", "ccmp"), "as").getValue());
        thenResultingMapContains("proto", new Variant<>(Arrays.asList(), "as").getValue());
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldThrowWhenGivenMalformedCiphers() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "MALFORMED_VALUE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP_TKIP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenIllegalArgumentExceptionThrown();
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenSecurityTypeNone() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "NONE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("key-mgmt", "none");
        thenResultingMapNotContains("proto");
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenSecurityTypeWep() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WEP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("key-mgmt", "none");
        thenResultingMapNotContains("proto");
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenSecurityTypeWpa() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("proto", new Variant<>(Arrays.asList("wpa"), "as").getValue());
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldWorkWhenGivenSecurityTypeWpa2() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("proto", new Variant<>(Arrays.asList("rsn"), "as").getValue());
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldThrowWhenGivenMalformedSecurity() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "MALFORMED_VALUE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenIllegalArgumentExceptionThrown();
    }

    @Test
    public void buildGsmSettingsShouldThrowWithMissingRequiredArgument() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildGsmSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildGsmSettingsShouldWorkWhenGivenExpectedMap() {
        givenMapWith("net.interface.ttyACM0.config.apn", "mobile.provider.com");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildGsmSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("apn", "mobile.provider.com");
    }

    @Test
    public void buildGsmSettingsShouldWorkWhenGivenExpectedMapAndOptionalParameters() {
        givenMapWith("net.interface.ttyACM0.config.apn", "mobile.provider.com");
        givenMapWith("net.interface.ttyACM0.config.username", "username");
        givenMapWith("net.interface.ttyACM0.config.password", new Password("password"));
        givenMapWith("net.interface.ttyACM0.config.dialString", "unaStringaPerMe");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildGsmSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("apn", "mobile.provider.com");
        thenResultingMapContains("username", "username");
        thenResultingMapContains("password", "password");
        thenResultingMapContains("number", "unaStringaPerMe");
    }

    @Test
    public void buildGsmSettingsShouldWorkWithEmptyUsernamePassword() {
        givenMapWith("net.interface.ttyACM0.config.apn", "mobile.provider.com");
        givenMapWith("net.interface.ttyACM0.config.username", "");
        givenMapWith("net.interface.ttyACM0.config.password", new Password(""));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildGsmSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("apn", "mobile.provider.com");
        thenResultingMapNotContains("username");
        thenResultingMapNotContains("password");
    }

    @Test
    public void buildPPPSettingsShouldNotThrowWithEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
    }

    @Test
    public void buildPPPSettingsShouldWorkWhenGivenOptionalLcpParameters() {
        givenMapWith("net.interface.ttyACM0.config.lcpEchoInterval", 30);
        givenMapWith("net.interface.ttyACM0.config.lcpEchoFailure", 5);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("lcp-echo-interval", 30);
        thenResultingMapContains("lcp-echo-failure", 5);
    }

    @Test
    public void buildPPPSettingsShouldWorkWithAuthTypeAuto() {
        givenMapWith("net.interface.ttyACM0.config.authType", "AUTO");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapNotContains("refuse-eap");
        thenResultingMapNotContains("refuse-chap");
        thenResultingMapNotContains("refuse-pap");
        thenResultingMapNotContains("refuse-mschap");
        thenResultingMapNotContains("refuse-mschapv2");
    }

    @Test
    public void buildPPPSettingsShouldWorkWithAuthTypeNone() {
        givenMapWith("net.interface.ttyACM0.config.authType", "NONE");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("refuse-eap", true);
        thenResultingMapContains("refuse-chap", true);
        thenResultingMapContains("refuse-pap", true);
        thenResultingMapContains("refuse-mschap", true);
        thenResultingMapContains("refuse-mschapv2", true);
    }

    @Test
    public void buildPPPSettingsShouldWorkWithAuthTypeChap() {
        givenMapWith("net.interface.ttyACM0.config.authType", "CHAP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("refuse-eap", true);
        thenResultingMapContains("refuse-chap", false);
        thenResultingMapContains("refuse-pap", true);
        thenResultingMapContains("refuse-mschap", true);
        thenResultingMapContains("refuse-mschapv2", true);
    }

    @Test
    public void buildPPPSettingsShouldWorkWithAuthTypePap() {
        givenMapWith("net.interface.ttyACM0.config.authType", "PAP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("refuse-eap", true);
        thenResultingMapContains("refuse-chap", true);
        thenResultingMapContains("refuse-pap", false);
        thenResultingMapContains("refuse-mschap", true);
        thenResultingMapContains("refuse-mschapv2", true);
    }

    @Test
    public void buildPPPSettingsShouldThrowWithUnsupportedAuthType() {
        givenMapWith("net.interface.ttyACM0.config.authType", "ROFL");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenIllegalArgumentExceptionThrown();
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithWifi() {
        whenBuildConnectionSettings(Optional.empty(), "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("type", "802-11-wireless");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithEthernet() {
        whenBuildConnectionSettings(Optional.empty(), "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("type", "802-3-ethernet");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithUnsupported() {
        whenBuildConnectionSettings(Optional.empty(), "modem0", NMDeviceType.NM_DEVICE_TYPE_ADSL);

        thenIllegalArgumentExceptionThrown();
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithWifiMockedConnection() {

        givenMapWith("connection", "test", new Variant<>("test"));
        givenMockConnection();

        whenBuildConnectionSettings(Optional.of(this.mockedConnection), "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("test", "test");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiUnmanged() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
        thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
        thenResultingBuildAllMapContains("802-11-wireless-security", "group",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedLan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");

        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
        thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
        thenResultingBuildAllMapContains("802-11-wireless-security", "group",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedConfiguredForInputsWiFiWan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
        thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
        thenResultingBuildAllMapContains("802-11-wireless-security", "group",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanAndHiddenSsid() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedLan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapContains("802-11-wireless", "hidden", true);
        thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
        thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
        thenResultingBuildAllMapContains("802-11-wireless-security", "group",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiWanAndHiddenSsid() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapContains("802-11-wireless", "hidden", true);
        thenResultingBuildAllMapContains("802-11-wireless-security", "psk", new Password("test").toString());
        thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-psk");
        thenResultingBuildAllMapContains("802-11-wireless-security", "group",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
        thenResultingBuildAllMapContains("802-11-wireless-security", "pairwise",
                new Variant<>(Arrays.asList("ccmp"), "as").getValue());
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndUnmanaged() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndLan() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusManagedLan");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsEthernetAndWan() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithModemSettings() {
        givenMapWith("net.interface.1-1.1.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.1-1.1.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.1-1.1.config.apn", "mobile.test.com");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "1-1.1", "ttyACM0",
                NMDeviceType.NM_DEVICE_TYPE_MODEM);

        thenNoExceptionsHaveBeenThrown();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "auto");
        thenResultingBuildAllMapContains("connection", "id", "kura-ttyACM0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "ttyACM0");
        thenResultingBuildAllMapContains("connection", "type", "gsm");
        thenResultingBuildAllMapContains("connection", "autoconnect-retries", 1);
        thenResultingBuildAllMapContains("gsm", "apn", "mobile.test.com");
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullIp() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.eth0.config.ip4.address", null);
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullPrefix() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", null);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullStatus() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", null);
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullWifiSsid() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", null);
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullWifiPassword() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", null);
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", null);
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA_WPA2");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullWifiSecurityType() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusManagedWan");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", null);
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoSuchElementExceptionThrown();
    }

    @Test
    public void shouldSupportEmptyWanPriorityProperty() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapNotContains("route-metric");
    }

    @Test
    public void shouldPopulateWanPriorityProperty() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.wan.priority", new Integer(30));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapContains("route-metric", new Variant<>(new Long(30)).getValue());
    }

    @Test
    public void shouldNotPopulateWanPriorityPropertyIfNotWAN() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.wan.priority", new Integer(30));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionsHaveBeenThrown();
        thenResultingMapNotContains("route-metric");
    }

    /*
     * Given
     */

    public void givenNetworkPropsCreatedWithTheMap(Map<String, Object> properties) {
        this.networkProperties = new NetworkProperties(properties);
    }

    public void givenMapWith(String key, Object value) {
        this.internetNetworkPropertiesInstanciationMap.put(key, value);
    }

    public void givenMapWith(String key, String subKey, Variant<?> value) {

        if (this.internalComparatorAllSettingsMap.containsKey(key)) {
            this.internalComparatorAllSettingsMap.get(key).put(subKey, value);
        } else {
            this.internalComparatorAllSettingsMap.put(key, Collections.singletonMap(subKey, value));
        }
    }

    public void givenMockConnection() {
        this.mockedConnection = Mockito.mock(Connection.class);
        Mockito.when(this.mockedConnection.GetSettings()).thenReturn(this.internalComparatorAllSettingsMap);

    }

    /*
     * When
     */

    public void whenBuildSettingsIsRunWith(NetworkProperties properties, Optional<Connection> oldConnection,
            String deviceId, String iface, NMDeviceType deviceType) {
        try {
            this.resultAllSettingsMap = NMSettingsConverter.buildSettings(properties, oldConnection, deviceId, iface,
                    deviceType);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    public void whenBuildIpv4SettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildIpv4Settings(props, iface);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    public void whenBuildIpv6SettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildIpv6Settings(props, iface);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    public void whenBuild80211WirelessSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.build80211WirelessSettings(props, iface);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    public void whenBuildConnectionSettings(Optional<Connection> connection, String iface, NMDeviceType deviceType) {
        try {
            this.resultMap = NMSettingsConverter.buildConnectionSettings(connection, iface, deviceType);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    public void whenBuild80211WirelessSecuritySettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.build80211WirelessSecuritySettings(props, iface);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    private void whenBuildGsmSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildGsmSettings(props, iface);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    private void whenBuildPPPSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildPPPSettings(props, iface);
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasAnIllegalArgumentExceptionThrown = true;
        } catch (Exception e) {
            this.hasAGenericExecptionBeenThrown = true;
        }
    }

    /*
     * Then
     */

    public void thenResultingMapContains(String key, Object value) {
        assertEquals(value, this.resultMap.get(key).getValue());
    }

    public void thenResultingMapNotContains(String key) {
        assertFalse(this.resultMap.containsKey(key));
    }

    public void thenResultingMapContainsBytes(String key, Object value) {
        assertEquals(value, new String((byte[]) this.resultMap.get(key).getValue(), StandardCharsets.UTF_8));
    }

    public void thenResultingBuildAllMapContains(String key, String subKey, Object value) {
        assertEquals(value, this.resultAllSettingsMap.get(key).get(subKey).getValue());
    }

    public void thenResultingBuildAllMapContainsBytes(String key, String subKey, Object value) {
        assertEquals(value,
                new String((byte[]) this.resultAllSettingsMap.get(key).get(subKey).getValue(), StandardCharsets.UTF_8));
    }

    public void thenNoSuchElementExceptionThrown() {
        assertTrue(this.hasNoSuchElementExceptionBeenThrown);
    }

    public void thenIllegalArgumentExceptionThrown() {
        assertTrue(this.hasAnIllegalArgumentExceptionThrown);
    }

    public void thenNoExceptionsHaveBeenThrown() {
        assertFalse(this.hasNoSuchElementExceptionBeenThrown);
        assertFalse(this.hasAGenericExecptionBeenThrown);
        assertFalse(this.hasAnIllegalArgumentExceptionThrown);
    }

    /*
     * Helper Methods
     */

    public Object buildAddressDataWith(String ipAddr, UInt32 prefix) {

        Map<String, Variant<?>> addressEntry = new HashMap<>();
        addressEntry.put("address", new Variant<>(ipAddr));
        addressEntry.put("prefix", new Variant<>(prefix));

        List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);

        Variant<?> dataVariant = new Variant<>(addressData, "aa{sv}");

        return dataVariant.getValue();
    }
}
