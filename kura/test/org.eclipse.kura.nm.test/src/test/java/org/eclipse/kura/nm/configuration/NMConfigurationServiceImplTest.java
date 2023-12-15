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
package org.eclipse.kura.nm.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.PrivateKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.configuration.NetworkConfigurationServiceCommon;
import org.eclipse.kura.nm.NMDbusConnector;
import org.eclipse.kura.nm.NetworkProperties;
import org.eclipse.kura.nm.configuration.writer.DhcpServerConfigWriter;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class NMConfigurationServiceImplTest {

    private NMConfigurationServiceImpl networkConfigurationService;
    private final Map<String, Object> properties = new HashMap<>();
    private ComponentConfiguration configuration;
    private Map<String, Object> retrievedProperties;
    private OCD ocd;
    private List<AD> ads;
    private final Object lock = new Object();
    private AtomicBoolean posted;
    private Event event;
    private final Set<String> dhcpConfigWriterInterfaces = new HashSet<>();
    private KeystoreService keystoreService = mock(KeystoreService.class);

    @Test
    public void shouldPostEventAfterActivationTest() throws InterruptedException, KuraException {
        givenPropertiesWithModifiedInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        thenEventIsPosted();
    }

    @Test
    public void shouldPostEventAfterUpdateTest() throws InterruptedException, KuraException {
        givenPropertiesWithModifiedInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsUpdated();
        thenEventIsPosted();
    }

    @Test
    public void shouldRemovedModifiedInterfacesProperty() throws InterruptedException, KuraException {
        givenPropertiesWithModifiedInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        thenModifiedInterfacesPropertyIsRemoved();
    }

    @Test
    public void componentDefinitionShouldHaveBasicPropertiesTest() throws KuraException {
        givenPropertiesWithoutInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasBasicProperties();
    }

    @Test
    public void componentDefinitionShouldHaveInterfaceTypesTest() throws KuraException {
        givenFullProperties();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasInterfaceTypes();
    }

    @Test
    public void componentDefinitionShouldHaveCorrectNumberOfPropertiesTest() throws KuraException {
        givenFullProperties();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasCorrectNumberOfResources();
    }

    @Test
    public void componentDefinitionShouldHaveCorrectPropertiesTest() throws KuraException {
        givenFullProperties();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasCorrectProperties();
    }

    @Test
    public void shouldMigratePppInterfaceNames() throws KuraException {
        givenPropertiesWithPppInterfaceNames();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        thenPropertiesNumberIsCorrect();
        thenPppInterfaceNamesAreReplaced();
        thenPropertiesNotContainPppNames();
        thenNetInterfacesPropertyIsCorrect();
    }

    @Test
    public void shouldStartConfigWriterIfEthernetInterfaceIsEnabledAndInDhcpServerMode() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "ETHERNET");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces("eno1");
    }

    @Test
    public void shouldStartConfigWriterIfEnterpriseWifi() throws KuraException {
        givenNetworkConfigurationService();
        givenEnterpriseWifiKeystore();
        givenFullProperties();
        givenWifiEnterprisePropertiesForInterfaceWlp2s0();

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces("wlp2s0");
    }

    @Test
    public void shouldStartConfigWriterIfWifiInterfaceIsEnabledAndInDhcpServerMode() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "WIFI");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces("eno1");
    }

    @Test
    public void shouldNotStartConfigWriterIfInterfaceTypeIsNotCorrect() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "LOOPBACK");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces();
    }

    @Test
    public void shouldNotStartConfigWriterIfDisabled() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusEnabledLAN");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", false);
        givenProperty("net.interface.eno1.type", "ETHERNET");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces();
    }

    @Test
    public void shouldNotStartConfigWriterIfInterfaceIsDisabled() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusDisabled");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "ETHERNET");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces();
    }

    @Test
    public void shouldNotStartConfigWriterIfInterfaceIsUnmanaged() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusUnmanaged");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "ETHERNET");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces();
    }

    @Test
    public void shouldNotStartConfigWriterIfInterfaceIsL2Only() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusL2Only");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "ETHERNET");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces();
    }

    @Test
    public void shouldNotStartConfigWriterIfInterfaceIsUnknown() throws KuraException {
        givenNetworkConfigurationService();
        givenFullProperties();
        givenProperty("net.interface.eno1.config.ip4.status", "netIPv4StatusUnknown");
        givenProperty("net.interface.eno1.config.dhcpServer4.enabled", true);
        givenProperty("net.interface.eno1.type", "ETHERNET");

        whenServiceIsActivated();

        thenDhcpConfigWriterIsCreatedForInterfaces();
    }

    private void givenPropertiesWithModifiedInterfaces() {
        this.properties.clear();
        this.properties.put("modified.interface.names", "eth0");
    }

    private void givenPropertiesWithoutInterfaces() {
        this.properties.clear();
        this.properties.put("net.interfaces", "");
    }

    private void givenProperty(final String key, final Object value) {
        this.properties.put(key, value);
    }

    private void thenDhcpConfigWriterIsCreatedForInterfaces(final String... interfaces) {
        assertEquals(new HashSet<>(Arrays.asList(interfaces)), this.dhcpConfigWriterInterfaces);
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
        this.properties.put("net.interface.wlp1s0.config.wifi.infra.channel", "1");
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
        this.properties.put("net.interface.wlp1s0.config.wifi.master.channel", "1");
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

    private void givenWifiEnterprisePropertiesForInterfaceWlp2s0() {
        this.properties.put("net.interfaces", "enp5s0,lo,eno1,wlp1s0,wlp2s0,1-4");

        this.properties.put("net.interface.wlp2s0.type", "WIFI");
        this.properties.put("net.interface.wlp2s0.config.ip4.gateway", "");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.driver", "nl80211");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.ssid", "testssid");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.groupCiphers", "CCMP_TKIP");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.bgscan", "");
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.rangeStart", "172.16.1.100");
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.maxLeaseTime", 7200);
        this.properties.put("net.interface.wlp2s0.config.wifi.master.pingAccessPoint", false);
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.channel", "1");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.passphrase", "qwerty=");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.groupCiphers", "CCMP_TKIP");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.bgscan", "");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.passphrase", "");
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.defaultLeaseTime", 7200);
        this.properties.put("net.interface.wlp2s0.config.wifi.master.ssid", "kura_gateway_0");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.securityType", "SECURITY_WPA2");
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.rangeEnd", "172.16.1.110");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.ignoreSSID", false);
        this.properties.put("net.interface.wlp2s0.config.wifi.master.mode", "MASTER");
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.prefix", 24);
        this.properties.put("net.interface.wlp2s0.config.wifi.mode", "INFRA");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.mode", "INFRA");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.pingAccessPoint", false);
        this.properties.put("net.interface.wlp2s0.config.nat.enabled", false);
        this.properties.put("net.interface.wlp2s0.config.ip4.status", "netIPv4StatusEnabledWAN");
        this.properties.put("net.interface.wlp2s0.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.channel", "1");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.radioMode", "RADIO_MODE_80211g");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.driver", "nl80211");
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.enabled", true);
        this.properties.put("net.interface.wlp2s0.config.wifi.master.ignoreSSID", false);
        this.properties.put("net.interface.wlp2s0.config.ip4.address", "172.16.1.1");
        this.properties.put("net.interface.wlp2s0.config.ip6.dnsServers", "");
        this.properties.put("net.interface.wlp2s0.config.wifi.master.pairwiseCiphers", "CCMP");
        this.properties.put("net.interface.wlp2s0.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.wlp2s0.config.dhcpServer4.passDns", false);
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.securityType", "SECURITY_WPA2_WPA3_ENTERPRISE");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.radioMode", "RADIO_MODE_80211b");
        this.properties.put("net.interface.wlp2s0.config.ip4.dnsServers", "");
        this.properties.put("net.interface.wlp2s0.config.wifi.infra.pairwiseCiphers", "CCMP_TKIP");
        this.properties.put("net.interface.wlp2s0.config.ip4.prefix", 24);
        this.properties.put("net.interface.wlp2s0.config.802-1x.eap", "Kura8021xEapTls");
        this.properties.put("net.interface.wlp2s0.config.802-1x.keystore.pid", "WifiKeystore");
        this.properties.put("net.interface.wlp2s0.config.802-1x.ca-cert-name", "caCert");
        this.properties.put("net.interface.wlp2s0.config.802-1x.client-cert-name", "privatekey");
        this.properties.put("net.interface.wlp2s0.config.802-1x.private-key-name", "privatekey");
    }

    private void givenEnterpriseWifiKeystore() {

        try {
            TrustedCertificateEntry trustedCertificateEntry = mock(TrustedCertificateEntry.class);
            Certificate certificate = mock(Certificate.class);

            PrivateKeyEntry privateKeyEntry = mock(PrivateKeyEntry.class);
            Certificate privateKeyCertificate = mock(Certificate.class);
            PrivateKey privateKey = mock(PrivateKey.class);

            when(trustedCertificateEntry.getTrustedCertificate()).thenReturn(certificate);
            when(privateKeyEntry.getCertificate()).thenReturn(privateKeyCertificate);
            when(privateKeyEntry.getPrivateKey()).thenReturn(privateKey);

            when(certificate.getEncoded()).thenReturn("ca-certificate".getBytes());
            when(privateKeyCertificate.getEncoded()).thenReturn("certificate-key".getBytes());
            when(privateKey.getEncoded()).thenReturn("privatekey".getBytes());

            this.keystoreService = mock(KeystoreService.class);

            when(this.keystoreService.getEntry("caCert")).thenReturn(trustedCertificateEntry);
            when(this.keystoreService.getEntry("privatekey")).thenReturn(privateKeyEntry);

            Map<String, Object> propertiesMap = new HashMap<>();
            propertiesMap.put(ConfigurationService.KURA_SERVICE_PID, "WifiKeystore");

            this.networkConfigurationService.setKeystoreService(keystoreService, propertiesMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    private void givenPropertiesWithPppInterfaceNames() {
        this.properties.clear();
        this.properties.put("net.interfaces", "enp5s0,lo,eno1,wlp1s0,ppp3");
        this.properties.put("net.interface.ppp3.config.resetTimeout", 5);
        this.properties.put("net.interface.ppp3.config.ip4.prefix", -1);
        this.properties.put("net.interface.ppp3.config.idle", 95);
        this.properties.put("net.interface.ppp3.config.ip4.address", "");
        this.properties.put("net.interface.ppp3.config.enabled", false);
        this.properties.put("net.interface.ppp3.config.ip4.status", "netIPv4StatusDisabled");
        this.properties.put("net.interface.ppp3.config.ip6.status", "netIPv6StatusDisabled");
        this.properties.put("net.interface.ppp3.config.ip4.gateway", "");
        this.properties.put("net.interface.ppp3.config.password", "");
        this.properties.put("net.interface.ppp3.config.activeFilter", "inbound");
        this.properties.put("net.interface.ppp3.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.ppp3.config.lcpEchoFailure", 0);
        this.properties.put("net.interface.ppp3.config.ipAddress", "");
        this.properties.put("net.interface.ppp3.config.persist", true);
        this.properties.put("net.interface.ppp3.config.diversityEnabled", false);
        this.properties.put("net.interface.ppp3.config.authType", "NONE");
        this.properties.put("net.interface.ppp3.config.ip4.dnsServers", "");
        this.properties.put("net.interface.ppp3.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.ppp3.config.ip6.dnsServers", "");
        this.properties.put("net.interface.ppp3.type", "MODEM");
        this.properties.put("net.interface.ppp3.config.gpsEnabled", false);
        this.properties.put("net.interface.ppp3.config.maxFail", 5);
        this.properties.put("net.interface.ppp3.config.lcpEchoInterval", 0);
        this.properties.put("net.interface.ppp3.config.pdpType", "IP");
        this.properties.put("net.interface.eno1.config.ip4.gateway", "");
        this.properties.put("net.interface.wlp1s0.config.ip4.dnsServers", "");
    }

    private void givenNetworkConfigurationService() throws KuraException {
        ComponentContext componentContextMock = mock(ComponentContext.class);
        BundleContext bundleCtxMock = mock(BundleContext.class);
        NMDbusConnector nmDbusConnectorMock = mock(NMDbusConnector.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleCtxMock);
        this.networkConfigurationService = new NMConfigurationServiceImpl(nmDbusConnectorMock) {

            @Override
            protected NetInterfaceType getNetworkTypeFromSystem(String interfaceName) throws KuraException {
                return guessNetworkType(interfaceName);
            }

            @Override
            protected DhcpServerConfigWriter buildDhcpServerConfigWriter(String interfaceName,
                    NetworkProperties properties) {
                dhcpConfigWriterInterfaces.add(interfaceName);
                return Mockito.mock(DhcpServerConfigWriter.class);
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        this.networkConfigurationService.setEventAdmin(eventAdminMock);

        NetworkService networkServiceMock = mock(NetworkService.class);
        when(networkServiceMock.getModemPppInterfaceName("1-4")).thenReturn("ppp3");
        when(networkServiceMock.getModemUsbPort("ppp3")).thenReturn("1-4");
        EthernetInterfaceImpl<NetInterfaceAddress> eth0 = new EthernetInterfaceImpl<>("eth0");
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<>();
        interfaces.add(eth0);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(interfaces);
        this.networkConfigurationService.setNetworkService(networkServiceMock);

        this.posted = new AtomicBoolean(false);

        doAnswer(invocation -> {
            this.event = invocation.getArgument(0, Event.class);
            assertEquals("org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC", this.event.getTopic());

            this.posted.set(true);

            synchronized (this.lock) {
                this.lock.notifyAll();
            }

            return null;
        }).when(eventAdminMock).postEvent(any());

        CommandExecutorService executorServiceMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        when(executorServiceMock.execute(any(Command.class))).thenReturn(status);

        this.networkConfigurationService.setExecutorService(executorServiceMock);

    }

    private void whenServiceIsActivated() {
        this.networkConfigurationService.activate(null, this.properties);
    }

    private void whenServiceIsUpdated() {
        this.networkConfigurationService.activate(null, this.properties);
    }

    private void whenComponentDefinitionIsRetrieved() throws KuraException {
        this.configuration = this.networkConfigurationService.getConfiguration();
        this.retrievedProperties = this.configuration.getConfigurationProperties();
        this.ocd = this.configuration.getDefinition();
        this.ads = this.ocd.getAD();
    }

    private void thenEventIsPosted() throws InterruptedException {
        synchronized (this.lock) {
            this.lock.wait(6000); // > 5s wait is necessary
        }

        assertTrue(this.posted.get());
    }

    private void thenModifiedInterfacesPropertyIsRemoved() {
        assertNull(this.event.getProperty("modified.interface.names"));
    }

    private void thenComponentDefinitionHasBasicProperties() {
        assertEquals(NetworkConfigurationServiceCommon.PID, this.configuration.getPid());
        assertNotNull(this.properties);
        assertEquals(1, this.retrievedProperties.size());
        assertNotNull(this.ocd);
        assertEquals("NetworkConfigurationService", this.ocd.getName());
        assertEquals("org.eclipse.kura.net.admin.NetworkConfigurationService", this.ocd.getId());
        assertEquals("Network Configuration Service", this.ocd.getDescription());
    }

    private void thenComponentDefinitionHasInterfaceTypes() {
        assertEquals("ETHERNET", this.retrievedProperties.get("net.interface.eno1.type"));
        assertEquals("ETHERNET", this.retrievedProperties.get("net.interface.enp5s0.type"));
        assertEquals("LOOPBACK", this.retrievedProperties.get("net.interface.lo.type"));
        assertEquals("MODEM", this.retrievedProperties.get("net.interface.1-4.type"));
        assertEquals("WIFI", this.retrievedProperties.get("net.interface.wlp1s0.type"));
    }

    private void thenComponentDefinitionHasCorrectNumberOfResources() {
        assertEquals(105, this.retrievedProperties.size());
        assertNotNull(this.ads);
        assertEquals(157, this.ads.size());
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

    private void thenComponentDefinitionHasCorrectEnterpriseProperties() {
        int adsConfigured = 0;
        for (AD ad : this.ads) {

            if ("net.interfaces".equals(ad.getId())) {
                assertEquals("net.interfaces", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp2s0.config.802-1x.eap".equals(ad.getId())) {
                assertEquals("net.interface.wlp2s0.config.802-1x.eap", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp2s0.config.802-1x.keystore.pid".equals(ad.getId())) {
                assertEquals("net.interface.wlp2s0.config.802-1x.keystore.pid", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp2s0.config.802-1x.ca-cert-name".equals(ad.getId())) {
                assertEquals("net.interface.wlp2s0.config.802-1x.ca-cert-name", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp2s0.config.802-1x.client-cert-name".equals(ad.getId())) {
                assertEquals("net.interface.wlp2s0.config.802-1x.client-cert-name", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlp2s0.config.802-1x.private-key-name".equals(ad.getId())) {
                assertEquals("net.interface.wlp2s0.config.802-1x.private-key-name", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }
        }
        assertEquals(5, adsConfigured);
    }

    private void thenPropertiesNumberIsCorrect() {
        assertEquals(33, this.event.getPropertyNames().length);
    }

    private void thenPppInterfaceNamesAreReplaced() {
        for (String propertyName : this.event.getPropertyNames()) {
            assertTrue(propertyName.startsWith("net.interface.1-4") || propertyName.startsWith("net.interface.eno1")
                    || propertyName.startsWith("net.interface.wlp1s0") || propertyName.startsWith("net.interface.lo")
                    || propertyName.startsWith("net.interface.enp5s0") || propertyName.equals("net.interfaces")
                    || propertyName.equals("event.topics"));
        }
    }

    private void thenPropertiesNotContainPppNames() {
        for (String propertyName : this.event.getPropertyNames()) {
            assertFalse(propertyName.startsWith("net.interface.ppp"));
        }
    }

    private void thenNetInterfacesPropertyIsCorrect() {
        assertEquals("enp5s0,lo,eno1,wlp1s0,1-4", this.event.getProperty("net.interfaces"));
    }

    private static NetInterfaceType guessNetworkType(final String interfaceName) {

        if (interfaceName.startsWith("eth") || interfaceName.startsWith("en")) {
            return NetInterfaceType.ETHERNET;
        } else if (interfaceName.equals("lo")) {
            return NetInterfaceType.LOOPBACK;
        } else if (interfaceName.startsWith("ppp") || Character.isDigit(interfaceName.charAt(0))) {
            return NetInterfaceType.MODEM;
        } else if (interfaceName.startsWith("wlan") || interfaceName.startsWith("wlp")) {
            return NetInterfaceType.WIFI;
        } else {
            return NetInterfaceType.UNKNOWN;
        }

    }

}
