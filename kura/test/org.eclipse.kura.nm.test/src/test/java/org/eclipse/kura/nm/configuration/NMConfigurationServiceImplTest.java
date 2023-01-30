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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.configuration.AbstractNetworkConfigurationService;
import org.eclipse.kura.nm.configuration.NMConfigurationServiceImpl;
import org.eclipse.kura.usb.UsbService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class NMConfigurationServiceImplTest {

    private NMConfigurationServiceImpl networkConfigurationService;
    private ConfigurationService configurationServiceMock;
    private final Map<String, Object> properties = new HashMap<>();
    private ComponentConfiguration configuration;
    private Map<String, Object> retrievedProperties;
    private OCD ocd;
    private List<AD> ads;
    private final Object lock = new Object();
    private AtomicBoolean posted;
    private Event event;

    @Test
    public void eventPostedAfterActivationTest() throws InterruptedException, KuraException {
        givenPropertiesWithModifiedInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        thenEventIsPosted();
    }

    @Test
    public void eventPostedAfterUpdateTest() throws InterruptedException, KuraException {
        givenPropertiesWithModifiedInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsUpdated();
        thenEventIsPosted();
    }

    @Test
    public void modifiedInterfacesPropertyIsRemoved() throws InterruptedException, KuraException {
        givenPropertiesWithModifiedInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        thenModifiedInterfacesPropertyIsRemoved();
    }

    @Test
    public void getDefinitionHasBasicPropertiesTest() throws KuraException {
        givenPropertiesWithoutInterfaces();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasBasicProperties();
    }

    @Test
    public void getDefinitionHasInterfaceTypesTest() throws KuraException {
        givenFullProperties();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasInterfaceTypes();
    }

    @Test
    public void getDefinitionHasCorrectNumberOfPropertiesTest() throws KuraException {
        givenFullProperties();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasCorrectNumberOfResources();
    }

    @Test
    public void getDefinitionHasCorrectPropertiesTest() throws KuraException {
        givenFullProperties();
        givenNetworkConfigurationService();
        whenServiceIsActivated();
        whenComponentDefinitionIsRetrieved();
        thenComponentDefinitionHasCorrectProperties();
    }

    @Test
    public void checkWanInterfacesTest() throws KuraException {
        givenNetworkConfigurationService();
        whenServiceIsActivatedWithOneWanInterface();
        whenServiceIsUpdatedWithTwoWanInterface();
        thenOldWanIntefaceIsDisabled();
        thenSnapshotIsTaken();
    }

    private void givenPropertiesWithModifiedInterfaces() {
        this.properties.clear();
        this.properties.put("modified.interface.names", "eth0");
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

    private void givenNetworkConfigurationService() throws KuraException {
        ComponentContext componentContextMock = mock(ComponentContext.class);
        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleCtxMock);
        this.networkConfigurationService = new NMConfigurationServiceImpl() {
            @Override
            protected NetInterfaceType getNetworkTypeFromSystem(String interfaceName) throws KuraException {
                return guessNetworkType(interfaceName);
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        this.networkConfigurationService.setEventAdmin(eventAdminMock);

//        UsbService usbServiceMock = mock(UsbService.class);
//        this.networkConfigurationService.setUsbService(usbServiceMock);

        NetworkService networkServiceMock = mock(NetworkService.class);
        when(networkServiceMock.getModemPppInterfaceName("1-4")).thenReturn("ppp3");
        EthernetInterfaceImpl<NetInterfaceAddress> eth0 = new EthernetInterfaceImpl<>("eth0");
//        EthernetInterfaceImpl<NetInterfaceAddress> eth1 = new EthernetInterfaceImpl<>("eth1");
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<>();
        interfaces.add(eth0);
//        interfaces.add(eth1);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(interfaces);
        this.networkConfigurationService.setNetworkService(networkServiceMock);

        this.configurationServiceMock = mock(ConfigurationService.class);
        this.networkConfigurationService.setConfigurationService(configurationServiceMock);

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

    }

    private void whenServiceIsActivated() {
        this.networkConfigurationService.activate(null, this.properties);
    }

    private void whenServiceIsUpdated() {
        this.networkConfigurationService.activate(null, this.properties);
    }

    private void whenServiceIsActivatedWithOneWanInterface() {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", "eth0");
        props.put("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        this.networkConfigurationService.activate(null, props);
    }

    private void whenServiceIsUpdatedWithTwoWanInterface() {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", "eth0,eth1");
        props.put("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        props.put("net.interface.eth1.config.ip4.status", "netIPv4StatusEnabledWAN");
        this.networkConfigurationService.update(props);
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
        assertEquals(AbstractNetworkConfigurationService.PID, this.configuration.getPid());
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
        assertEquals(71, this.ads.size());
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

    private void thenOldWanIntefaceIsDisabled() {
        assertEquals("netIPv4StatusDisabled", this.event.getProperty("net.interface.eth1.config.ip4.status"));
    }

    private void thenSnapshotIsTaken() throws KuraException {
        verify(this.configurationServiceMock).snapshot();
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
