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
 ******************************************************************************/
package org.eclipse.kura.net.configuration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.junit.Test;

public class NetworkConfigurationServiceCommonTest {

    private final Map<String, Object> properties = new HashMap<>();
    private Tocd tocd;
    private List<AD> ads;
    private Map<String, Object> returnedProperties = new HashMap<>();

    @Test
    public void componentDefinitionShouldHaveBasicPropertiesTest() throws KuraException {
        givenPropertiesWithoutInterfaces();
        whenTocdIsRetrieved();
        thenComponentDefinitionHasBasicProperties();
    }

    @Test
    public void componentDefinitionShouldHaveCorrectNumberOfPropertiesTest() throws KuraException {
        givenFullProperties();
        whenTocdIsRetrieved();
        thenComponentDefinitionHasCorrectNumberOfResources();
    }

    @Test
    public void componentDefinitionShouldHaveCorrectPropertiesTest() throws KuraException {
        givenFullProperties();
        whenTocdIsRetrieved();
        thenComponentDefinitionHasCorrectProperties();
    }

    @Test
    public void componentDefinitionShouldHaveWifiPropertiesTest() throws KuraException {
        givenFullProperties();
        whenTocdIsRetrieved();
        thenComponentDefinitionHasWifiProperties();
    }

    @Test
    public void componentDefinitionShouldHaveModemPropertiesTest() throws KuraException {
        givenFullProperties();
        whenTocdIsRetrieved();
        thenComponentDefinitionHasModemProperties();
    }

    @Test
    public void pppNumberShouldBeAnInteger() throws KuraException {
        givenFullProperties();
        whenTocdIsRetrieved();
        thenPppNumIsInteger();
    }

    @Test
    public void shouldWrapStringPasswords() throws KuraException {
        givenFullProperties();
        givenConfigurationProperty("net.interface.1-4.config.password", "foo");
        givenConfigurationProperty("net.interface.wlp1s0.config.wifi.master.passphrase", "bar");
        givenConfigurationProperty("net.interface.wlp1s0.config.wifi.infra.passphrase", "baz");

        whenConfigurationPropertiesAreRetrieved();

        thenReturnedPropertyEqualsPassword("net.interface.1-4.config.password",
                new Password(new char[] { 'f', 'o', 'o' }));
        thenReturnedPropertyEqualsPassword("net.interface.wlp1s0.config.wifi.master.passphrase",
                new Password(new char[] { 'b', 'a', 'r' }));
        thenReturnedPropertyEqualsPassword("net.interface.wlp1s0.config.wifi.infra.passphrase",
                new Password(new char[] { 'b', 'a', 'z' }));

    }

    @Test
    public void shouldNotChangeWrappedPasswords() throws KuraException {
        givenFullProperties();
        givenConfigurationProperty("net.interface.1-4.config.password", new Password(new char[] { 'f', 'o', 'o' }));
        givenConfigurationProperty("net.interface.wlp1s0.config.wifi.master.passphrase",
                new Password(new char[] { 'b', 'a', 'r' }));
        givenConfigurationProperty("net.interface.wlp1s0.config.wifi.infra.passphrase",
                new Password(new char[] { 'b', 'a', 'z' }));

        whenConfigurationPropertiesAreRetrieved();

        thenReturnedPropertyEqualsPassword("net.interface.1-4.config.password",
                new Password(new char[] { 'f', 'o', 'o' }));
        thenReturnedPropertyEqualsPassword("net.interface.wlp1s0.config.wifi.master.passphrase",
                new Password(new char[] { 'b', 'a', 'r' }));
        thenReturnedPropertyEqualsPassword("net.interface.wlp1s0.config.wifi.infra.passphrase",
                new Password(new char[] { 'b', 'a', 'z' }));

    }

    @Test
    public void shouldNotChangeOtherProperties() throws KuraException {
        givenFullProperties();
        whenConfigurationPropertiesAreRetrieved();

        thenReturnedPropertyEquals("net.interface.lo.config.ip4.status", "netIPv4StatusEnabledLAN");
        thenReturnedPropertyEquals("net.interface.1-4.config.resetTimeout", 5);
        thenReturnedPropertyEquals("net.interface.1-4.config.ip4.prefix", -1);
    }

    private void givenPropertiesWithoutInterfaces() {
        this.properties.clear();
        this.properties.put("net.interfaces", "");
    }

    private void givenFullProperties() {
        this.properties.clear();
        this.properties.put("net.interfaces", "enp5s0,lo,eno1,wlp1s0,1-4");
        this.properties.put("net.interface.lo.config.ip4.status", "netIPv4StatusEnabledLAN");
        this.properties.put("net.interface.lo.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.1-4.config.resetTimeout", 5);
        this.properties.put("net.interface.eno1.config.ip4.status", "netIPv4StatusEnabledLAN");
        this.properties.put("net.interface.enp5s0.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.1-4.config.ip4.prefix", -1);
        this.properties.put("net.interface.1-4.config.idle", 95);
        this.properties.put("net.interface.wlp1s0.type", "WIFI");
        this.properties.put("net.interface.eno1.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.wlp1s0.config.ip4.gateway", "");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.driver", "nl80211");
        this.properties.put("net.interface.eno1.config.ip4.address", "172.16.0.1");
        this.properties.put("net.interface.1-4.config.ip4.address", "");
        this.properties.put("net.interface.eno1.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.ssid", "");
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.groupCiphers", "CCMP_TKIP");
        this.properties.put("net.interface.lo.config.ip4.gateway", "");
        this.properties.put("net.interface.1-4.config.enabled", false);
        this.properties.put("net.interface.lo.config.ip6.dnsServers", "");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.bgscan", "");
        this.properties.put("net.interface.1-4.config.ip4.status", "netIPv4StatusDisabled");
        this.properties.put("net.interface.enp5s0.type", "ETHERNET");
        this.properties.put("net.interface.1-4.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.eno1.config.ip6.dnsServers", "");
        this.properties.put("net.interface.1-4.config.ip4.gateway", "");
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.rangeStart", "172.16.1.100");
        this.properties.put("net.interface.1-4.config.password", "");
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.maxLeaseTime", 7200);
        this.properties.put("net.interface.eno1.config.ip4.prefix", 24);
        this.properties.put("net.interface.wlp1s0.config.wifi.master.pingAccessPoint", false);
        this.properties.put("net.interface.1-4.config.activeFilter", "inbound");
        this.properties.put("net.interface.enp5s0.config.ip6.dnsServers", "");
        this.properties.put("net.interface.1-4.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.channel", 1);
        this.properties.put("net.interface.wlp1s0.config.wifi.master.passphrase", "qwerty=");
        this.properties.put("net.interface.1-4.config.lcpEchoFailure", 0);
        this.properties.put("net.interface.eno1.config.dhcpServer4.rangeEnd", "172.16.0.110");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.groupCiphers", "CCMP_TKIP");
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.bgscan", "");
        this.properties.put("net.interface.1-4.config.ipAddress", "");
        this.properties.put("net.interface.eno1.config.dhcpServer4.defaultLeaseTime", 7200);
        this.properties.put("net.interface.eno1.config.dhcpServer4.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.passphrase", "");
        this.properties.put("net.interface.1-4.config.persist", true);
        this.properties.put("net.interface.1-4.config.diversityEnabled", false);
        this.properties.put("net.interface.eno1.config.dhcpServer4.prefix", 24);
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.defaultLeaseTime", 7200);
        this.properties.put("net.interface.lo.config.ip4.prefix", 8);
        this.properties.put("net.interface.1-4.config.authType", "NONE");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.ssid", "kura_gateway_0");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.securityType", "SECURITY_WPA2");
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.rangeEnd", "172.16.1.110");
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.ignoreSSID", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.master.mode", "MASTER");
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.prefix", 24);
        this.properties.put("net.interface.1-4.config.ip4.dnsServers", "");
        this.properties.put("net.interface.lo.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.mode", "MASTER");
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.mode", "INFRA");
        this.properties.put("net.interface.eno1.config.ip4.dnsServers", "");
        this.properties.put("net.interface.lo.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.pingAccessPoint", false);
        this.properties.put("net.interface.1-4.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.nat.enabled", false);
        this.properties.put("net.interface.eno1.config.dhcpServer4.passDns", false);
        this.properties.put("net.interface.lo.config.ip4.dnsServers", "");
        this.properties.put("net.interface.eno1.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.ip4.status", "netIPv4StatusDisabled");
        this.properties.put("net.interface.enp5s0.config.ip4.dnsServers", "");
        this.properties.put("net.interface.wlp1s0.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.1-4.config.ip6.dnsServers", "");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.channel", 1);
        this.properties.put("net.interface.enp5s0.config.dhcpClient4.enabled", true);
        this.properties.put("net.interface.1-4.type", "MODEM");
        this.properties.put("net.interface.enp5s0.config.ip4.status", "netIPv4StatusEnabledWAN");
        this.properties.put("net.interface.eno1.type", "ETHERNET");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.radioMode", "RADIO_MODE_80211g");
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.driver", "nl80211");
        this.properties.put("net.interface.enp5s0.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.eno1.config.dhcpServer4.rangeStart", "172.16.0.100");
        this.properties.put("net.interface.wlp1s0.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.enabled", false);
        this.properties.put("net.interface.1-4.config.gpsEnabled", false);
        this.properties.put("net.interface.wlp1s0.config.wifi.master.ignoreSSID", false);
        this.properties.put("net.interface.wlp1s0.config.ip4.address", "172.16.1.1");
        this.properties.put("net.interface.wlp1s0.config.ip6.dnsServers", "");
        this.properties.put("net.interface.lo.type", "LOOPBACK");
        this.properties.put("net.interface.lo.ip4.gateway", "127.0.0.1");
        this.properties.put("net.interface.wlp1s0.config.wifi.master.pairwiseCiphers", "CCMP");
        this.properties.put("net.interface.wlp1s0.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.wlp1s0.config.dhcpServer4.passDns", false);
        this.properties.put("net.interface.lo.config.ip4.address", "127.0.0.1");
        this.properties.put("net.interface.1-4.config.maxFail", 5);
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.securityType", "SECURITY_NONE");
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.radioMode", "RADIO_MODE_80211b");
        this.properties.put("net.interface.eno1.config.nat.enabled", false);
        this.properties.put("net.interface.eno1.config.ip4.gateway", "");
        this.properties.put("net.interface.wlp1s0.config.ip4.dnsServers", "");
        this.properties.put("net.interface.eno1.config.dhcpServer4.maxLeaseTime", 7200);
        this.properties.put("net.interface.1-4.config.lcpEchoInterval", 0);
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.pairwiseCiphers", "CCMP_TKIP");
        this.properties.put("net.interface.wlp1s0.config.ip4.prefix", 24);
        this.properties.put("net.interface.1-4.config.pdpType", "IP");
    }

    private void givenConfigurationProperty(final String key, final Object value) {
        this.properties.put(key, value);
    }

    private void whenTocdIsRetrieved() throws KuraException {
        this.ads = null;
        this.tocd = NetworkConfigurationServiceCommon.getDefinition(this.properties, Optional.empty());
        this.ads = this.tocd.getAD();
    }

    private void whenConfigurationPropertiesAreRetrieved() throws KuraException {
        this.returnedProperties = NetworkConfigurationServiceCommon
                .getConfiguration("foo", this.properties, Optional.empty()).getConfigurationProperties();
    }

    private void thenComponentDefinitionHasBasicProperties() {
        assertEquals(NetworkConfigurationServiceCommon.PID, this.tocd.getId());
        assertEquals("NetworkConfigurationService", this.tocd.getName());
        assertEquals("Network Configuration Service", this.tocd.getDescription());
        assertNotNull(this.ads);
        assertEquals(1, this.ads.size());
    }

    private void thenComponentDefinitionHasCorrectNumberOfResources() {
        assertNotNull(this.ads);
        assertEquals(145, this.ads.size());
    }

    private void thenReturnedPropertyEquals(final String key, final Object value) {
        assertEquals(value, this.returnedProperties.get(key));
    }

    private void thenReturnedPropertyEqualsPassword(final String key, final Password value) {
        assertEquals(Password.class,
                Optional.ofNullable(this.returnedProperties.get(key)).map(Object::getClass).orElse(null));
        assertArrayEquals(value.getPassword(), ((Password) this.returnedProperties.get(key)).getPassword());
    }

    private void thenComponentDefinitionHasCorrectProperties() {
        int adsConfigured = 0;
        for (AD ad : this.ads) {
            if ("net.interface.eno1.config.dhcpClient4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpClient4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.enp5s0.config.dhcpClient4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.enp5s0.config.dhcpClient4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpClient4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpClient4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.defaultLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.defaultLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.defaultLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.defaultLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.maxLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.maxLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.maxLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.maxLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.passDns".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.passDns", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.passDns".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.passDns", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.rangeEnd".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.rangeEnd", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.rangeEnd".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.rangeEnd", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.dhcpServer4.rangeStart".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.dhcpServer4.rangeStart", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.dhcpServer4.rangeStart".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.dhcpServer4.rangeStart", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.enp5s0.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.enp5s0.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.ip4.gateway".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.ip4.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.enp5s0.config.ip4.gateway".equals(ad.getId())) {
                assertEquals("net.interface.enp5s0.config.ip4.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.ip4.gateway".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.ip4.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eno1.config.nat.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eno1.config.nat.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.nat.enabled".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.nat.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.channel".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.channel", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.master.channel".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.master.channel", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.groupCiphers".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.groupCiphers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.pairwiseCiphers".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.pairwiseCiphers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.passphrase".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.passphrase", ad.getName());
                assertEquals("PASSWORD", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.master.passphrase".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.master.passphrase", ad.getName());
                assertEquals("PASSWORD", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.radioMode".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.radioMode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.master.radioMode".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.master.radioMode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.securityType".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.securityType", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.master.securityType".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.master.securityType", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.infra.ssid".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.infra.ssid", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.master.ssid".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.master.ssid", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp1s0.config.wifi.mode".equals(ad.getId())) {
                assertEquals("net.interface.wlp1s0.config.wifi.mode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interfaces".equals(ad.getId())) {
                assertEquals("net.interfaces", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }
        }
        assertEquals(40, adsConfigured);
    }

    private void thenComponentDefinitionHasWifiProperties() {
        assertEquals(47, this.ads.stream().filter(ad -> ad.getName().contains("wlp1s0")).count());
    }

    private void thenComponentDefinitionHasModemProperties() {
        assertEquals(36, this.ads.stream().filter(ad -> ad.getName().contains("1-4")).count());
    }

    private void thenPppNumIsInteger() {
        Optional<AD> adOptional = this.ads.stream().filter(ad -> ad.getName().equals("net.interface.1-4.config.pppNum"))
                .findFirst();
        assertTrue(adOptional.isPresent());
        assertEquals(Scalar.INTEGER, adOptional.get().getType());
    }

}
