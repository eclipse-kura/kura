/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.nm.NetworkProperties;
import org.eclipse.kura.nm.SemanticVersion;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.Test;
import org.mockito.Mockito;

public class NMSettingsConverterTest {

    private final static String PEM_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDFWXYxR1zfnzpeO1771SosgCRhzyANqqxH600iLajJww+o1QeKR5n08INKBBNRRW6bCJpPNA5XNLl9ucnu/Bl2CIZ/NeAyFHtau+8kYrkT5wp/g2FCKIPqNAOUik2N7rEPB6FPm0FTWjlBUz2qRIQ7Szdqbw6ZXgK2Zn15MPb+CLjum2biqv1YPxaFnrPhHO2APVSu+xYEB90byFgGWEfL8qY+BAycVmNxPzq4C3LRdJwvCUvsMhcnNhpN0ZHg0ujAFEeQLZXm3SXZGvAQat5IAZLHxIQbUSeJjt1H2yWwkxrNoSMpwOGyAiUTiPAKwpfT2ab1cJZXILWF1+QmNC3tAgMBAAECggEABCT7CHMqDiU9y9KANl1HIfwc1PWk6OSQykndKtLmOvdUx+kcVTdoJoLpfT6l7dawLl/Xj3ILePLXP3ST6jjRVYpl+l9opPjO09kV5feCQ7kNP+ovknzYzkC/EhSsoEbAWqGbjET2Gll+MAIsdhbVAi5mhA3Nb4caNgHIyxsTMXHidl/BwaxkLyv4RWOiPxQPA1XFCTGX9b3KcIDte8hRvEuK7mD6V6VKMm0ArxJgJXtOQ/dhH4Jhra/RH3Y3NjszgP2OW18z71/Yeud18ykNNgzrX2EkXAYXulfa9O4Yfi/k3TttP3QxItbRD+VetZCvQj3jHaG1Ly3dJRGhC2xaOQKBgQD+R7g8FHxjihpJt3bSZNClftRl3bY/4jRV7MBXNC7XA9zj3zNp5R6JCV9PAI5CyY1lhOq+pHfDggufcQZlSC6h4n6b0rj+b2vxbNBy8efQUDtgQw7QfunPGYs+OHPpNK7JGYUezbIw0PV7ahxiD6ncG6DibnNInEeyBC5AJ803LwKBgQDGryr1wOE15Q/lH+XPPf+cclDC/vKpp3Fm4btzSWrOyQWh+yxKGd5dmeRk0h2cDp92jOAHNjLVA0ejvcQUIwew+DYRnXJe/YDvOijFWW+LDRdm/oPcqtjzrfFd1ROQRSeEB0R/BF4m1EDcLligs3N3pWiBEWs/HYdJuIRhK2PlowKBgDh4AOgGvKD2WGQqhA6xKMy3379Hf2OsfmbejtBO3GAPkYxhUu+fXCqelDXdL7qRO/9hhygTKi2WwbIEzaDMaN62h9te7opCgDw7KAd+xTYzuxvjiHSw2oeNaqjErKkLdA1gx3lRwNKqdPmVVPxJ8jTZRd9DHALyAdH8r7C7pg0tAoGBAJIc1/cK9ZRw9BOINbUG3yfqWcJNQ5/IZ/lFIFlUMJwJ8X6B/Lwx8fnb5r7OVsAhcNv6Ffa3wQIt+01LjRtR96IJp5mktCtvOpazqrAXaZRU+FTh748khZAO52YeANkkQj8yKQlP6P2dMmW6H6tuzQe8OPJSIRC1YnywmYnsIvcJAoGAHLoik8+9ej4zcFnO2xTve6cvEym/sISnlE5GLC2sYomG6cxDnMq4DWL3tVBbBnOCkany+p0oWuhSGsbnEWVDHvA3Wo3uuY7NdL3iTPhIHbZ+0AkgjHT99LYZHr4lhTJP8XU6UKT15aDJyljvRFuWrcHKrQ18VSiIwOQoQzbAVAc=";

    private Map<String, Variant<?>> resultMap;

    private Map<String, Map<String, Variant<?>>> internalComparatorAllSettingsMap = new HashMap<>();
    private Map<String, Map<String, Variant<?>>> resultAllSettingsMap = new HashMap<>();

    private Map<String, Object> internetNetworkPropertiesInstanciationMap = new HashMap<>();

    private NetworkProperties networkProperties;
    private Connection mockedConnection;

    private Exception occurredException;
    private SemanticVersion nmVersion = SemanticVersion.parse("1.40");

    private static final List<Byte> IP6_BYTE_ARRAY_ADDRESS = Arrays
            .asList(new Byte[] { 32, 1, 72, 96, 72, 96, 0, 0, 0, 0, 0, 0, 0, 0, -120, 68 });

    @Test
    public void buildSettingsShouldThrowWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);
        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildIpv4SettingsShouldThrowWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void build80211WirelessSettingsShouldThrowErrorWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");
        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldThrowWhenGivenEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");
        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildIpv4SettingsShouldThrowWithUnmanagedStatus() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusUnmanaged");

        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv4SettingsShouldThrowWithUnknownStatus() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "myNewAnDifferentUnknownStatus");

        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv4SettingsShouldWorkWhenDisabled() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "disabled");
        thenResultingMapNotContains("ignore-auto-dns");
        thenResultingMapNotContains("ignore-auto-routes");
        thenResultingMapNotContains("route-metric");
        thenResultingMapNotContains("address-data");
        thenResultingMapNotContains("dns");
        thenResultingMapNotContains("ignore-auto-dns");
        thenResultingMapNotContains("gateway");
    }

    @Test
    public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForWan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");

        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");
        thenNoExceptionOccurred();

        thenResultingMapContains("method", "auto");
        thenResultingMapNotContains("ignore-auto-dns");
        thenResultingMapNotContains("ignore-auto-routes");
        thenResultingMapNotContains("route-metric");
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

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "manual");
        thenResultingMapContains("address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingMapContains("dns", new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("gateway", "192.168.0.1");
        thenResultingMapNotContains("route-metric");
    }

    @Test
    public void buildIpv4SettingsShouldWorkWhenGivenExpectedMapAndDhcpIsTrueAndEnabledForLan() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("ignore-auto-routes", true);
        thenResultingMapNotContains("route-metric");
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

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "manual");
        thenResultingMapContains("address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("ignore-auto-routes", true);
        thenResultingMapNotContains("dns");
        thenResultingMapNotContains("route-metric");
    }

    @Test
    public void buildIpv4SettingsShouldPopulateWanPriorityProperty() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.wan.priority", new Integer(30));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("route-metric", new Variant<>(new Long(30)).getValue());
    }

    @Test
    public void buildIpv4SettingsShouldNotPopulateWanPriorityPropertyIfNotWAN() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.wan.priority", new Integer(30));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv4SettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("ignore-auto-routes", true);
        thenResultingMapNotContains("route-metric");
    }

    @Test
    public void buildIpv6SettingsShouldThrowWhitWrongMethod() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "WrongMethod");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv6SettingsShouldNotSetIgnoreAutoDNSWhenGivenExpectedMapWithAutoEnableAndWAN() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapNotContains("ignore-auto-dns");
    }

    @Test
    public void buildIpv6SettingsShouldSetWhenGivenExpectedMapWithAutoEnableAndWAN() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithDhcpEnableAndWAN() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodDhcp");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "dhcp");
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithManualEnableAndEnabledForWan() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "manual");
        thenResultingMapContains("address-data", buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingMapContains("dns", Arrays.asList(IP6_BYTE_ARRAY_ADDRESS));
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("gateway", "fe80::eed:f0a1:d03a:1");

    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithAutoEnableAndEnabledForLan() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("ignore-auto-routes", true);
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithManualEnableAndEnabledForLan() {

        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "manual");
        thenResultingMapContains("address-data", buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingMapContains("ignore-auto-dns", true);
        thenResultingMapContains("ignore-auto-routes", true);
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithAutoEnableAndWANEUI64Disabled() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.addr.gen.mode", "netIPv6AddressGenModeEUI64");
        givenMapWith("net.interface.wlan0.config.ip6.privacy", "netIPv6PrivacyDisabled");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("addr-gen-mode", 0);
        thenResultingMapContains("ip6-privacy", 0);
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithAutoEnableAndWANStablePrivacyPreferPublicAddress() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.addr.gen.mode", "netIPv6AddressGenModeStablePrivacy");
        givenMapWith("net.interface.wlan0.config.ip6.privacy", "netIPv6PrivacyEnabledPubAdd");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("addr-gen-mode", 1);
        thenResultingMapContains("ip6-privacy", 1);
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithAutoEnableAndWANStablePrivacyPreferTempAddress() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.addr.gen.mode", "netIPv6AddressGenModeStablePrivacy");
        givenMapWith("net.interface.wlan0.config.ip6.privacy", "netIPv6PrivacyEnabledTempAdd");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "auto");
        thenResultingMapContains("addr-gen-mode", 1);
        thenResultingMapContains("ip6-privacy", 2);
    }

    @Test
    public void buildIpv6SettingsShouldWorkWhenGivenExpectedMapWithDisabled() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.addr.gen.mode", "netIPv6AddressGenModeStablePrivacy");
        givenMapWith("net.interface.wlan0.config.ip6.privacy", "netIPv6PrivacyEnabledTempAdd");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("method", "disabled");
        thenResultingMapNotContains("addr-gen-mode");
        thenResultingMapNotContains("ip6-privacy");
    }

    @Test
    public void buildIpv6SettingsShouldThrowWhenGivenExpectedMapWithWrongAddressGenMode() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.addr.gen.mode", "WrongGenAddress");
        givenMapWith("net.interface.wlan0.config.ip6.privacy", "netIPv6PrivacyEnabledTempAdd");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv6SettingsShouldThrowWhenGivenExpectedMapWithWrongPrivacy() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.addr.gen.mode", "netIPv6AddressGenModeStablePrivacy");
        givenMapWith("net.interface.wlan0.config.ip6.privacy", "WrongPrivacy");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv6SettingsShouldThrowIllegalArgumentExceptionWhenGivenExpectedMapWithManualEnableAndUnmanaged() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv6SettingsShouldThrowIllegalArgumentExceptionWhenGivenExpectedMapWithManualEnableAndUnknown() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnknown");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildIpv6SettingsShouldHaveMtuWhenSupported() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.mtu", 2345);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        givenNetworkManagerVersion("1.40.18");

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("mtu", new UInt32(2345));
    }

    @Test
    public void buildIpv6SettingsShouldNotHaveMtuWhenNotSupported() {
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.mtu", 3456);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        givenNetworkManagerVersion("1.22.10");

        whenBuildIpv6SettingsIsRunWith(this.networkProperties, "wlan0", this.nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapNotContains("mtu");
    }

    @Test
    public void buildEthernetSettingsShouldHavePromiscWhenSupported() {
        givenMapWith("net.interface.eth0.config.promisc", -1);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        givenNetworkManagerVersion("1.32");

        whenBuildEthernetSettingsIsRunWith(networkProperties, "eth0", nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapContains("accept-all-mac-addresses", -1);
    }

    @Test
    public void buildEthernetSettingsShouldNotHavePromiscWhenNotSupported() {
        givenMapWith("net.interface.eth0.config.promisc", 1);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);
        givenNetworkManagerVersion("1.31");

        whenBuildEthernetSettingsIsRunWith(networkProperties, "eth0", nmVersion);

        thenNoExceptionOccurred();
        thenResultingMapNotContains("accept-all-mac-addresses");
    }

    @Test
    public void build8021xSettingsShouldThrowIfIsEmpty() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void build8021xSettingsShouldThrowIfEapIsInvalid() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "invalid eap value");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void build8021xSettingsShouldThrowIfInnerAuthIsInvalid() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "invalid eap value");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "invalid Inner Auth value");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void build8021xSettingsShouldWorkWithTtlsAndMschapV2() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTtls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthMschapv2");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "example-user-name");
        givenMapWith("net.interface.wlan0.config.802-1x.password", new Password("secure-test-password-123!@#"));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "ttls" }).getValue());
        thenResultingMapContains("phase2-auth", "mschapv2");
        thenResultingMapContains("identity", "example-user-name");
        thenResultingMapContains("password", "secure-test-password-123!@#");
        thenResultingMapNotContains("anonymous-identity");
        thenResultingMapNotContains("ca-cert");
        thenResultingMapNotContains("ca-cert-password");

    }

    @Test
    public void build8021xSettingsShouldWorkWithTtlsAndMschapV2AndOptionalParams() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTtls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthMschapv2");
        givenMapWith("net.interface.wlan0.config.802-1x.anonymous-identity", "anonymous-identity-test-var");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                buildMockedCertificateWithCert("binary ca cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "example-user-name");
        givenMapWith("net.interface.wlan0.config.802-1x.password", new Password("secure-test-password-123!@#"));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "ttls" }).getValue());
        thenResultingMapContains("phase2-auth", "mschapv2");
        thenResultingMapContains("anonymous-identity", "anonymous-identity-test-var");
        thenResultingMapContainsBytes("ca-cert", "binary ca cert");
        thenResultingMapContains("identity", "example-user-name");
        thenResultingMapContains("password", "secure-test-password-123!@#");

        thenResultingMapNotContains("ca-cert-password");
    }

    @Test
    public void build8021xSettingsShouldWorkWithPeapAndMschapV2() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapPeap");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthMschapv2");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "example-user-name");
        givenMapWith("net.interface.wlan0.config.802-1x.password", new Password("secure-test-password-123!@#"));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "peap" }).getValue());
        thenResultingMapContains("phase2-auth", "mschapv2");
        thenResultingMapContains("identity", "example-user-name");
        thenResultingMapContains("password", "secure-test-password-123!@#");

        thenResultingMapNotContains("anonymous-identity");
        thenResultingMapNotContains("ca-cert");
        thenResultingMapNotContains("ca-cert-password");
    }

    @Test
    public void build8021xSettingsShouldWorkWithPeapAndMschapV2AndCertificates() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapPeap");
        givenMapWith("net.interface.wlan0.config.802-1x.anonymous-identity", "anonymous-identity-test-var");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                buildMockedCertificateWithCert("binary ca cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthMschapv2");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "example-user-name");
        givenMapWith("net.interface.wlan0.config.802-1x.password", new Password("secure-test-password-123!@#"));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "peap" }).getValue());
        thenResultingMapContains("anonymous-identity", "anonymous-identity-test-var");
        thenResultingMapContainsBytes("ca-cert", "binary ca cert");
        thenResultingMapContains("phase2-auth", "mschapv2");
        thenResultingMapContains("identity", "example-user-name");
        thenResultingMapContains("password", "secure-test-password-123!@#");

        thenResultingMapNotContains("ca-cert-password");
    }

    @Test
    public void build8021xSettingsShouldThrowIfTlsIsEmpty() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void build8021xSettingsShouldWorkWithTls() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthNone");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "username@email.com");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                buildMockedCertificateWithCert("binary ca cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.client-cert-name",
                buildMockedCertificateWithCert("binary client cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.private-key-name", buildMockPrivateKeyWith(PEM_PRIVATE_KEY));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "tls" }).getValue());
        thenResultingMapNotContains("phase2-auth");
        thenResultingMapContains("identity", "username@email.com");
        thenResultingMapContainsBytes("ca-cert", "binary ca cert");
        thenResultingMapContainsBytes("client-cert", "binary client cert");
        thenResultingMapContains("private-key-password", "sOPM6ph9zBENU0rrOiZhIAk8wn26W8qj0r+DBVu6Zbk=");
        thenResultingMapContainsEncryptedPrivateKey("private-key", "sOPM6ph9zBENU0rrOiZhIAk8wn26W8qj0r+DBVu6Zbk=",
                PEM_PRIVATE_KEY);

        thenResultingMapNotContains("ca-cert-password");
        thenResultingMapNotContains("client-cert-password");
    }

    @Test
    public void build8021xSettingsShouldThrowWithTlsWithNullPrivateKey() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthNone");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "username@email.com");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                buildMockedCertificateWithCert("binary ca cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.client-cert-name",
                buildMockedCertificateWithCert("binary client cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.private-key-name", null);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void build8021xSettingsShouldThrowWithTlsWithWrongTypePrivateKey() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthNone");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "username@email.com");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                buildMockedCertificateWithCert("binary ca cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.client-cert-name",
                buildMockedCertificateWithCert("binary client cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.private-key-name", "");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void build8021xSettingsShouldWorkWithTlsWithNullCACert() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthNone");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "username@email.com");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name", null);
        givenMapWith("net.interface.wlan0.config.802-1x.client-cert-name",
                buildMockedCertificateWithCert("binary client cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.private-key-name", buildMockPrivateKeyWith(PEM_PRIVATE_KEY));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "tls" }).getValue());
        thenResultingMapContains("identity", "username@email.com");
        thenResultingMapContainsBytes("client-cert", "binary client cert");
        thenResultingMapContains("private-key-password", "sOPM6ph9zBENU0rrOiZhIAk8wn26W8qj0r+DBVu6Zbk=");
        thenResultingMapContainsEncryptedPrivateKey("private-key", "sOPM6ph9zBENU0rrOiZhIAk8wn26W8qj0r+DBVu6Zbk=",
                PEM_PRIVATE_KEY);

        thenResultingMapNotContains("phase2-auth");
        thenResultingMapNotContains("ca-cert");
        thenResultingMapNotContains("ca-cert-password");
        thenResultingMapNotContains("client-cert-password");
    }

    @Test
    public void build8021xSettingsShouldWorkWithTlsWithWrongTypeCACert() {
        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthNone");
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "username@email.com");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                new Password("When I grow up I want to be a certificate"));
        givenMapWith("net.interface.wlan0.config.802-1x.client-cert-name",
                buildMockedCertificateWithCert("binary client cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.private-key-name", buildMockPrivateKeyWith(PEM_PRIVATE_KEY));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild8021xSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();

        thenResultingMapContainsArray("eap", new Variant<>(new String[] { "tls" }).getValue());
        thenResultingMapContains("identity", "username@email.com");
        thenResultingMapContainsBytes("client-cert", "binary client cert");
        thenResultingMapContains("private-key-password", "sOPM6ph9zBENU0rrOiZhIAk8wn26W8qj0r+DBVu6Zbk=");
        thenResultingMapContainsEncryptedPrivateKey("private-key", "sOPM6ph9zBENU0rrOiZhIAk8wn26W8qj0r+DBVu6Zbk=",
                PEM_PRIVATE_KEY);

        thenResultingMapNotContains("phase2-auth");
        thenResultingMapNotContains("ca-cert");
        thenResultingMapNotContains("ca-cert-password");
        thenResultingMapNotContains("client-cert-password");
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndSetToInfraAndWithChannelField() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWhenGivenExpectedMapAndBandBg() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211b");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void build80211WirelessSettingsShouldWorkWithChannel0And2Ghz() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211b");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "0");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSettingsIsRunWith(this.networkProperties, "wlan0");

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void build80211WirelessSecuritySettingsShouldThrowWhenGivenSecurityTypeNone() {

        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "NONE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuild80211WirelessSecuritySettingsIsRunWith(this.networkProperties, "wlan0");

        thenExceptionOccurred(IllegalArgumentException.class);
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

        thenNoExceptionOccurred();
        thenResultingMapContains("key-mgmt", "none");
        thenResultingMapContains("wep-key-type", 1);
        thenResultingMapContains("wep-key0", "test");
        thenResultingMapNotContains("proto");
        thenResultingMapNotContains("wpa-psk");
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

        thenNoExceptionOccurred();
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("proto", new Variant<>(Arrays.asList("wpa"), "as").getValue());
        thenResultingMapNotContains("wep-key-type");
        thenResultingMapNotContains("wep-key0");
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

        thenNoExceptionOccurred();
        thenResultingMapContains("key-mgmt", "wpa-psk");
        thenResultingMapContains("proto", new Variant<>(Arrays.asList("rsn"), "as").getValue());
        thenResultingMapNotContains("wep-key-type");
        thenResultingMapNotContains("wep-key0");
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildGsmSettingsShouldThrowWithMissingRequiredArgument() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildGsmSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildGsmSettingsShouldWorkWhenGivenExpectedMap() {
        givenMapWith("net.interface.ttyACM0.config.apn", "mobile.provider.com");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildGsmSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
        thenResultingMapContains("apn", "mobile.provider.com");
        thenResultingMapNotContains("username");
        thenResultingMapNotContains("password");
    }

    @Test
    public void buildPPPSettingsShouldNotThrowWithEmptyMap() {
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionOccurred();
    }

    @Test
    public void buildPPPSettingsShouldWorkWhenGivenOptionalLcpParameters() {
        givenMapWith("net.interface.ttyACM0.config.lcpEchoInterval", 30);
        givenMapWith("net.interface.ttyACM0.config.lcpEchoFailure", 5);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionOccurred();
        thenResultingMapContains("lcp-echo-interval", 30);
        thenResultingMapContains("lcp-echo-failure", 5);
    }

    @Test
    public void buildPPPSettingsShouldWorkWithAuthTypeAuto() {
        givenMapWith("net.interface.ttyACM0.config.authType", "AUTO");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildPPPSettingsIsRunWith(this.networkProperties, "ttyACM0");

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenNoExceptionOccurred();
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildVlanSettingsShouldWorkWithRequiredSettings() {
        givenMapWith("net.interface.eth0.30.config.vlan.parent", "eth0");
        givenMapWith("net.interface.eth0.30.config.vlan.id", 30);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildVlanSettingsIsRunWith(this.networkProperties, "eth0.30");

        thenNoExceptionOccurred();
        thenResultingMapContains("parent", "eth0");
        thenResultingMapContains("id", new UInt32(30));
        thenResultingMapContains("flags", new UInt32(1));
        thenResultingMapContains("ingress-priority-map", new Variant<>(Arrays.asList(), "as").getValue());
        thenResultingMapContains("egress-priority-map", new Variant<>(Arrays.asList(), "as").getValue());
    }

    @Test
    public void buildVlanSettingsShouldWorkWithFullSettings() {
        givenMapWith("net.interface.eth1.40.config.vlan.parent", "eth1");
        givenMapWith("net.interface.eth1.40.config.vlan.id", 40);
        givenMapWith("net.interface.eth1.40.config.vlan.flags", 3);
        givenMapWith("net.interface.eth1.40.config.vlan.id", 40);
        givenMapWith("net.interface.eth1.40.config.vlan.ingress", "0:1,4:5");
        givenMapWith("net.interface.eth1.40.config.vlan.egress", "2:3");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildVlanSettingsIsRunWith(this.networkProperties, "eth1.40");

        thenNoExceptionOccurred();
        thenResultingMapContains("parent", "eth1");
        thenResultingMapContains("id", new UInt32(40));
        thenResultingMapContains("flags", new UInt32(3));
        thenResultingMapContains("ingress-priority-map", new Variant<>(Arrays.asList("0:1", "4:5"), "as").getValue());
        thenResultingMapContains("egress-priority-map", new Variant<>(Arrays.asList("2:3"), "as").getValue());
    }

    @Test
    public void buildVlanSettingsShouldThrowWhenMissingParent() {
        givenMapWith("net.interface.eth0.30.config.vlan.id", 30);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildVlanSettingsIsRunWith(this.networkProperties, "eth0.30");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildVlanSettingsShouldThrowWhenMissingVlanId() {
        givenMapWith("net.interface.eth0.30.config.vlan.parent", "eth0");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildVlanSettingsIsRunWith(this.networkProperties, "eth0.30");

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithWifi() {
        whenBuildConnectionSettings(Optional.empty(), "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionOccurred();
        thenResultingMapContains("type", "802-11-wireless");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithEthernet() {
        whenBuildConnectionSettings(Optional.empty(), "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingMapContains("type", "802-3-ethernet");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithUnsupported() {
        whenBuildConnectionSettings(Optional.empty(), "modem0", NMDeviceType.NM_DEVICE_TYPE_ADSL);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithVlan() {
        whenBuildConnectionSettings(Optional.empty(), "eth0.40", NMDeviceType.NM_DEVICE_TYPE_VLAN);

        thenNoExceptionOccurred();
        thenResultingMapContains("type", "vlan");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildConnectionSettingsShouldWorkWithWifiMockedConnection() {

        givenMapWith("connection", "test", new Variant<>("test"));
        givenMockConnection();

        whenBuildConnectionSettings(Optional.of(this.mockedConnection), "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingMapContains("test", "test");
        thenResultingMapContains("autoconnect-retries", 1);
    }

    @Test
    public void buildSettingsShouldThrowWithStatusUnmanagedIp4() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithStatusUnknownIp4() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "myAwesomeUnknownString");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
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
    public void buildSettingsShouldWorkWith8021x() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "SECURITY_WPA2_WPA3_ENTERPRISE");

        givenMapWith("net.interface.wlan0.config.802-1x.eap", "Kura8021xEapTtls");
        givenMapWith("net.interface.wlan0.config.802-1x.innerAuth", "Kura8021xInnerAuthMschapv2");
        givenMapWith("net.interface.wlan0.config.802-1x.anonymous-identity", "anonymous-identity-test-var");
        givenMapWith("net.interface.wlan0.config.802-1x.ca-cert-name",
                buildMockedCertificateWithCert("binary ca cert"));
        givenMapWith("net.interface.wlan0.config.802-1x.identity", "example-user-name");
        givenMapWith("net.interface.wlan0.config.802-1x.password", new Password("secure-test-password-123!@#"));
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless-security", "key-mgmt", "wpa-eap");

        thenResultingBuildAllMapContainsArray("802-1x", "eap", new Variant<>(new String[] { "ttls" }).getValue());
        thenResultingBuildAllMapContains("802-1x", "phase2-auth", "mschapv2");
        thenResultingBuildAllMapContains("802-1x", "anonymous-identity", "anonymous-identity-test-var");
        thenResultingBuildAllMapContainsBytes("802-1x", "ca-cert", "binary ca cert");
        thenResultingBuildAllMapContains("802-1x", "identity", "example-user-name");
        thenResultingBuildAllMapContains("802-1x", "password", "secure-test-password-123!@#");

        thenResultingBuildAllMapNotContains("802-1x", "ca-cert-password");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedConfiguredForInputsWiFiWanIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
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
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanAndHiddenSsidIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
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
    public void buildSettingsShouldNotSet80211WirelessSecuritySettingsIfSecurityTypeIsSetToNoneIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "NONE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapNotContains("802-11-wireless-security");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiWanAndHiddenSsidIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv4", "ignore-auto-routes");
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
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndUnmanagedIp4() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndLanIp4() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsEthernetAndWanIp4() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv4", "ignore-auto-routes");
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsVlanAndWanIp4() {
        givenMapWith("net.interface.myVlan.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.myVlan.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.myVlan.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.myVlan.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.myVlan.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.myVlan.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.myVlan.config.vlan.parent", "eth0");
        givenMapWith("net.interface.myVlan.config.vlan.id", 55);
        givenMapWith("net.interface.myVlan.config.vlan.flags", 2);
        givenMapWith("net.interface.myVlan.config.vlan.egress", "2:3");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "myVlan", "myVlan",
                NMDeviceType.NM_DEVICE_TYPE_VLAN);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv4", "ignore-auto-routes");
        thenResultingBuildAllMapContains("connection", "id", "kura-myVlan-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "myVlan");
        thenResultingBuildAllMapContains("connection", "type", "vlan");
        thenResultingBuildAllMapContains("vlan", "parent", "eth0");
        thenResultingBuildAllMapContains("vlan", "id", new UInt32(55));
        thenResultingBuildAllMapContains("vlan", "flags", new UInt32(2));
        thenResultingBuildAllMapContains("vlan", "ingress-priority-map",
                new Variant<>(Arrays.asList(), "as").getValue());
        thenResultingBuildAllMapContains("vlan", "egress-priority-map",
                new Variant<>(Arrays.asList("2:3"), "as").getValue());
    }

    @Test
    public void buildSettingsShouldWorkWithModemSettingsIp4() {
        givenMapWith("net.interface.1-1.1.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.1-1.1.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.1-1.1.config.apn", "mobile.test.com");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "1-1.1", "ttyACM0",
                NMDeviceType.NM_DEVICE_TYPE_MODEM);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
        thenResultingBuildAllMapContains("ipv4", "method", "auto");
        thenResultingBuildAllMapContains("connection", "id", "kura-ttyACM0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "ttyACM0");
        thenResultingBuildAllMapContains("connection", "type", "gsm");
        thenResultingBuildAllMapContains("connection", "autoconnect-retries", 1);
        thenResultingBuildAllMapContains("gsm", "apn", "mobile.test.com");
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullIpIp4() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip4.address", null);
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullPrefixIp4() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", null);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullStatusIp4() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", null);
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullWifiSsidIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullWifiPasswordIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowDhcpDisabledAndNullWifiSecurityTypeIp4() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithStatusUnmanagedIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithStatusUnknownIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnknown");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiUnmangedIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
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
    public void buildSettingsShouldWorkWithExpectedConfiguredForInputsWiFiWanIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
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
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanAndHiddenSsidIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
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
    public void buildSettingsShouldNotSet80211WirelessSecuritySettingsIfSecurityTypeIsSetToNoneIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "NONE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv6", "gateway");
        thenResultingBuildAllMapNotContains("ipv6", "dns");
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapNotContains("802-11-wireless-security");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiWanAndHiddenSsidIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv6", "ignore-auto-routes");
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
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
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndUnmanagedIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndLanIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-routes", true);
        thenResultingBuildAllMapNotContains("ipv6", "gateway");
        thenResultingBuildAllMapNotContains("ipv6", "dns");
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsEthernetAndWanIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv6", "gateway", "fe80::eed:f0a1:d03a:1");
        thenResultingBuildAllMapContains("ipv6", "dns", Arrays.asList(IP6_BYTE_ARRAY_ADDRESS));
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv6", "ignore-auto-routes");
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithModemSettingsIp6() {
        givenMapWith("net.interface.1-1.1.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.1-1.1.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.1-1.1.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.1-1.1.config.apn", "mobile.test.com");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "1-1.1", "ttyACM0",
                NMDeviceType.NM_DEVICE_TYPE_MODEM);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "auto");
        thenResultingBuildAllMapContains("ipv4", "method", "disabled");
        thenResultingBuildAllMapContains("connection", "id", "kura-ttyACM0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "ttyACM0");
        thenResultingBuildAllMapContains("connection", "type", "gsm");
        thenResultingBuildAllMapContains("connection", "autoconnect-retries", 1);
        thenResultingBuildAllMapContains("gsm", "apn", "mobile.test.com");
    }

    @Test
    public void buildSettingsShouldThrowWithManualAndNullIpIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv6StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", null);
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithManualAndNullPrefixIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv6StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", null);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithManualAndNullStatusIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv6StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", null);
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
    }

    @Test
    public void buildSettingsShouldThrowWithManualAndNullWifiSsidIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithManualAndNullWifiPasswordIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithManualAndNullWifiSecurityTypeIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithStatusUnmanagedIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithStatusUnknownIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.ip4.status", "myAwesomeUnknownString");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnknown");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiUnmangedIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
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
    public void buildSettingsShouldWorkWithExpectedConfiguredForInputsWiFiWanIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
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
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiLanAndHiddenSsidIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
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
    public void buildSettingsShouldNotSet80211WirelessSecuritySettingsIfSecurityTypeIsSetToNoneIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.wifi.mode", "INFRA");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ssid", "ssidtest");
        givenMapWith("net.interface.wlan0.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        givenMapWith("net.interface.wlan0.config.wifi.infra.channel", "10");
        givenMapWith("net.interface.wlan0.config.wifi.infra.securityType", "NONE");
        givenMapWith("net.interface.wlan0.config.wifi.infra.ignoreSSID", true);
        givenMapWith("net.interface.wlan0.config.wifi.infra.passphrase", new Password("test"));
        givenMapWith("net.interface.wlan0.config.wifi.infra.groupCiphers", "CCMP");
        givenMapWith("net.interface.wlan0.config.wifi.infra.pairwiseCiphers", "CCMP");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "wlan0", "wlan0",
                NMDeviceType.NM_DEVICE_TYPE_WIFI);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv6", "gateway");
        thenResultingBuildAllMapNotContains("ipv6", "dns");
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-routes", true);
        thenResultingBuildAllMapContains("connection", "id", "kura-wlan0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "wlan0");
        thenResultingBuildAllMapContains("connection", "type", "802-11-wireless");
        thenResultingBuildAllMapContains("802-11-wireless", "mode", "infrastructure");
        thenResultingBuildAllMapContainsBytes("802-11-wireless", "ssid", "ssidtest");
        thenResultingBuildAllMapContains("802-11-wireless", "band", "a");
        thenResultingBuildAllMapContains("802-11-wireless", "channel", new UInt32(Short.parseShort("10")));
        thenResultingBuildAllMapNotContains("802-11-wireless-security");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForWiFiWanAndHiddenSsidIp4AndIp6() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv4", "ignore-auto-routes");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapNotContains("ipv6", "ignore-auto-routes");
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
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
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndUnmanagedIp4AndIp6() {
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusUnmanaged");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusUnmanaged");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsConfiguredForEthernetAndLanIp4AndIp6() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledLAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-routes", true);
        thenResultingBuildAllMapNotContains("ipv4", "gateway");
        thenResultingBuildAllMapNotContains("ipv4", "dns");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-routes", true);
        thenResultingBuildAllMapNotContains("ipv6", "gateway");
        thenResultingBuildAllMapNotContains("ipv6", "dns");
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithExpectedInputsEthernetAndWanIp4AndIp6() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv4", "method", "manual");
        thenResultingBuildAllMapContains("ipv4", "address-data", buildAddressDataWith("192.168.0.12", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv4", "gateway", "192.168.0.1");
        thenResultingBuildAllMapContains("ipv4", "dns",
                new Variant<>(Arrays.asList(new UInt32(16843009)), "au").getValue());
        thenResultingBuildAllMapContains("ipv4", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv4", "ignore-auto-routes");
        thenResultingBuildAllMapContains("ipv6", "method", "manual");
        thenResultingBuildAllMapContains("ipv6", "address-data",
                buildAddressDataWith("fe80::eed:f0a1:d03a:1028", new UInt32(25)));
        thenResultingBuildAllMapContains("ipv6", "gateway", "fe80::eed:f0a1:d03a:1");
        thenResultingBuildAllMapContains("ipv6", "dns", Arrays.asList(IP6_BYTE_ARRAY_ADDRESS));
        thenResultingBuildAllMapContains("ipv6", "ignore-auto-dns", true);
        thenResultingBuildAllMapNotContains("ipv6", "ignore-auto-routes");
        thenResultingBuildAllMapContains("connection", "id", "kura-eth0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "eth0");
        thenResultingBuildAllMapContains("connection", "type", "802-3-ethernet");
    }

    @Test
    public void buildSettingsShouldWorkWithModemSettingsIp4AndIp6() {
        givenMapWith("net.interface.1-1.1.config.dhcpClient4.enabled", true);
        givenMapWith("net.interface.1-1.1.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.1-1.1.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.1-1.1.config.ip6.address.method", "netIPv6MethodAuto");
        givenMapWith("net.interface.1-1.1.config.apn", "mobile.test.com");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "1-1.1", "ttyACM0",
                NMDeviceType.NM_DEVICE_TYPE_MODEM);

        thenNoExceptionOccurred();
        thenResultingBuildAllMapContains("ipv6", "method", "auto");
        thenResultingBuildAllMapContains("ipv4", "method", "auto");
        thenResultingBuildAllMapContains("connection", "id", "kura-ttyACM0-connection");
        thenResultingBuildAllMapContains("connection", "interface-name", "ttyACM0");
        thenResultingBuildAllMapContains("connection", "type", "gsm");
        thenResultingBuildAllMapContains("connection", "autoconnect-retries", 1);
        thenResultingBuildAllMapContains("gsm", "apn", "mobile.test.com");
    }

    @Test
    public void buildSettingsShouldThrowWithManualIp6DhcpDisableIp4AndNullIp() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip4.address", null);
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", null);
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithManualIp6DhcpDisableIp4AndNullPrefix() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", null);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.eth0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", null);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldWorkWithManualIp6DhcpDisableIp4AndNullStatus() {
        givenMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.eth0.config.ip4.status", null);
        givenMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.eth0.config.ip4.status", "netIPv6StatusDisabled");
        givenMapWith("net.interface.eth0.config.ip6.status", null);
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.eth0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.eth0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.eth0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.eth0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
        givenNetworkPropsCreatedWithTheMap(this.internetNetworkPropertiesInstanciationMap);

        whenBuildSettingsIsRunWith(this.networkProperties, Optional.empty(), "eth0", "eth0",
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        thenResultingBuildAllMapContains("ipv6", "method", "disabled");
    }

    @Test
    public void buildSettingsShouldThrowWithManualIp6DhcpDisableIp4AndNullWifiSsid() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithManualIp6DhcpDisableIp4AndNullWifiPassword() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(NoSuchElementException.class);
    }

    @Test
    public void buildSettingsShouldThrowWithManualIp6DhcpDisableIp4AndNullWifiSecurityType() {
        givenMapWith("net.interface.wlan0.config.dhcpClient4.enabled", false);
        givenMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip4.address", "192.168.0.12");
        givenMapWith("net.interface.wlan0.config.ip4.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip4.dnsServers", "1.1.1.1");
        givenMapWith("net.interface.wlan0.config.ip4.gateway", "192.168.0.1");
        givenMapWith("net.interface.wlan0.config.ip6.status", "netIPv6StatusEnabledWAN");
        givenMapWith("net.interface.wlan0.config.ip6.address.method", "netIPv6MethodManual");
        givenMapWith("net.interface.wlan0.config.ip6.address", "fe80::eed:f0a1:d03a:1028");
        givenMapWith("net.interface.wlan0.config.ip6.prefix", (short) 25);
        givenMapWith("net.interface.wlan0.config.ip6.dnsServers", "2001:4860:4860:0:0:0:0:8844");
        givenMapWith("net.interface.wlan0.config.ip6.gateway", "fe80::eed:f0a1:d03a:1");
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

        thenExceptionOccurred(NoSuchElementException.class);
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

    public void givenNetworkManagerVersion(String nmVersion) {
        this.nmVersion = SemanticVersion.parse(nmVersion);
    }

    public void givenMockConnectionWithNullSettings() {
        this.mockedConnection = Mockito.mock(Connection.class);
        Mockito.when(this.mockedConnection.GetSettings()).thenReturn(null);
    }

    /*
     * When
     */

    public void whenBuildSettingsIsRunWith(NetworkProperties properties, Optional<Connection> oldConnection,
            String deviceId, String iface, NMDeviceType deviceType) {
        try {
            this.resultAllSettingsMap = NMSettingsConverter.buildSettings(properties, oldConnection, deviceId, iface,
                    deviceType, this.nmVersion);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    public void whenBuildIpv4SettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildIpv4Settings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    public void whenBuildIpv6SettingsIsRunWith(NetworkProperties props, String iface, SemanticVersion nmVersion) {
        try {
            this.resultMap = NMSettingsConverter.buildIpv6Settings(props, iface, nmVersion);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    public void whenBuild8021xSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.build8021xSettings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    public void whenBuild80211WirelessSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.build80211WirelessSettings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    public void whenBuildConnectionSettings(Optional<Connection> connection, String iface, NMDeviceType deviceType) {
        try {
            this.resultMap = NMSettingsConverter.buildConnectionSettings(connection, iface, deviceType);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    public void whenBuild80211WirelessSecuritySettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.build80211WirelessSecuritySettings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenBuildGsmSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildGsmSettings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenBuildPPPSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildPPPSettings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenBuildVlanSettingsIsRunWith(NetworkProperties props, String iface) {
        try {
            this.resultMap = NMSettingsConverter.buildVlanSettings(props, iface);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenBuildEthernetSettingsIsRunWith(NetworkProperties props, String iface, SemanticVersion nmVersion) {
        try {
            this.resultMap = NMSettingsConverter.buildEthernetSettings(props, iface, nmVersion);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    public void thenResultingMapContains(String key, Object value) {
        assertEquals(value, this.resultMap.get(key).getValue());
    }

    public void thenResultingMapContainsArray(String key, Object[] value) {
        assertArrayEquals(value, (Object[]) this.resultMap.get(key).getValue());
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

    public void thenResultingBuildAllMapContainsArray(String key, String subKey, Object[] value) {
        assertArrayEquals(value, (Object[]) this.resultAllSettingsMap.get(key).get(subKey).getValue());
    }

    public void thenResultingBuildAllMapNotContains(String key) {
        assertFalse(this.resultAllSettingsMap.containsKey(key));
    }

    public void thenResultingBuildAllMapNotContains(String key, String subKey) {
        Map<String, Variant<?>> innerMap = this.resultAllSettingsMap.get(key);
        assertFalse(innerMap.containsKey(subKey));
    }

    public void thenResultingBuildAllMapContainsBytes(String key, String subKey, Object value) {
        assertEquals(value,
                new String((byte[]) this.resultAllSettingsMap.get(key).get(subKey).getValue(), StandardCharsets.UTF_8));
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private void thenResultingMapContainsEncryptedPrivateKey(String key, String expectedPrivateKeyPassword,
            String expectedPemPrivateKeyContent) {
        byte[] encryptedKey = (byte[]) this.resultMap.get(key).getValue();
        byte[] decryptedKey = decryptKey(convertToDer(encryptedKey), expectedPrivateKeyPassword);
        assertEquals(expectedPemPrivateKeyContent, Base64.getEncoder().encodeToString(decryptedKey));
    }

    private byte[] decryptKey(byte[] encryptedKey, String expectedPrivateKeyPassword) {
        PBEKeySpec pbeSpec = new PBEKeySpec(expectedPrivateKeyPassword.toCharArray());
        try {
            EncryptedPrivateKeyInfo privateKeyInfo = new EncryptedPrivateKeyInfo(encryptedKey);
            SecretKeyFactory secretKeyFact = SecretKeyFactory.getInstance(privateKeyInfo.getAlgName());
            SecretKey secret = secretKeyFact.generateSecret(pbeSpec);
            PKCS8EncodedKeySpec keySpec = privateKeyInfo.getKeySpec(secret);
            KeyFactory keyFact = KeyFactory.getInstance("RSA");
            return keyFact.generatePrivate(keySpec).getEncoded();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] convertToDer(byte[] privateKeyPem) {
        String privateKeyString = new String(privateKeyPem, StandardCharsets.UTF_8);
        String privateKeyStringContent = privateKeyString.replace("\n", "")
                .replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "")
                .replace("-----END ENCRYPTED PRIVATE KEY-----", "");
        return Base64.getDecoder().decode(privateKeyStringContent.getBytes());
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

    public Certificate buildMockedCertificateWithCert(String certBytes) {
        Certificate cert = mock(Certificate.class);
        try {
            when(cert.getEncoded()).thenReturn(certBytes.getBytes());
        } catch (CertificateEncodingException e) {
            fail();
        }

        return cert;
    }

    public PrivateKey buildMockPrivateKeyWith(String privateKeyPEM) {
        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);

        PrivateKey key = mock(PrivateKey.class);
        when(key.getEncoded()).thenReturn(decoded);

        return key;
    }
}
