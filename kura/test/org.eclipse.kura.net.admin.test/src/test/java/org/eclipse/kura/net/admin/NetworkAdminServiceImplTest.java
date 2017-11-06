/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP6;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.junit.Test;
import org.osgi.service.event.Event;

public class NetworkAdminServiceImplTest {

    @Test
    public void testGetNetworkInterfaceConfigs() throws KuraException {
        // test call to another service

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> configs = nasi
                .getNetworkInterfaceConfigs();

        assertEquals(1, configs.size());
        assertEquals(interfaceName, configs.get(0).getName());
    }

    @Test
    public void testGetNetworkInterfaceConfigsName() throws KuraException, UnknownHostException {
        // test fetching by interface name

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        List<NetConfig> configs = nasi.getNetworkInterfaceConfigs(interfaceName);

        assertEquals(1, configs.size());
        DhcpServerConfigIP4 cfgdhcp = (DhcpServerConfigIP4) configs.get(0);
        assertEquals(interfaceName, cfgdhcp.getInterfaceName());
        assertEquals(24, cfgdhcp.getPrefix());
        assertNotNull(cfgdhcp.getDnsServers());
        assertEquals(24, cfgdhcp.getPrefix());
        assertEquals("10.10.0.0", cfgdhcp.getSubnet().getHostAddress());
        assertEquals("255.255.255.0", cfgdhcp.getSubnetMask().getHostAddress());
        assertEquals("10.10.0.10", cfgdhcp.getRangeStart().getHostAddress());
        assertEquals("10.10.0.15", cfgdhcp.getRangeEnd().getHostAddress());
        assertEquals("10.10.0.250", cfgdhcp.getRouterAddress().getHostAddress());
        assertEquals(true, cfgdhcp.isEnabled());
        assertEquals(true, cfgdhcp.isPassDns());
        assertEquals(900, cfgdhcp.getDefaultLeaseTime());
        assertEquals(1, cfgdhcp.getDnsServers().size());
        assertEquals("10.10.0.254", cfgdhcp.getDnsServers().get(0).getHostAddress());
    }

    @Test
    public void testUpdateWifiInterfaceConfigInvalidNetConfig() throws KuraException, UnknownHostException {
        // test error with invalid net configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true) {

            @Override
            public boolean isValid() {
                return false;
            }
        };
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        WifiAccessPoint accessPoint = new WifiAccessPointImpl("testSSID");
        try {
            nasi.updateWifiInterfaceConfig(interfaceName, true, accessPoint, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("invalid"));
        }

    }

    @Test
    public void testUpdateWifiInterfaceConfigMissingConfigIP() throws KuraException, UnknownHostException {
        // test error with missing IP configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        WifiAccessPoint accessPoint = new WifiAccessPointImpl("testSSID");
        try {
            nasi.updateWifiInterfaceConfig(interfaceName, true, accessPoint, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
            assertTrue(e.getMessage().contains("IPv4 or IPv6"));
        }

    }

    @Test
    public void testUpdateWifiInterfaceConfigMissingConfigWifi() throws KuraException, UnknownHostException {
        // test error with missing WiFi configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        NetConfigIP4 netConfig4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig4);

        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        WifiAccessPoint accessPoint = new WifiAccessPointImpl("testSSID");
        try {
            nasi.updateWifiInterfaceConfig(interfaceName, true, accessPoint, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
            assertTrue(e.getMessage().contains("WiFi configuration"));
        }

    }

    @Test
    public void testUpdateWifiInterfaceConfig() throws KuraException, UnknownHostException {
        // update wifi with new dhcp and nat configs

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class,
                withSettings().extraInterfaces(SelfConfiguringComponent.class));
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        String interfaceName = "intf";
        String passkey = "pass";

        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", "intf");
        props.put("net.interface.intf.type", "WIFI");
        props.put("net.interface.intf.state", NetInterfaceState.ACTIVATED);
        props.put("net.interface.intf.driver", "driver");
        props.put("net.interface.intf.mtu", "1492");
        props.put("net.interface.intf.mac", "12:34:56:78:90:12");
        props.put("net.interface.intf.up", true);

        props.put("net.interface.intf.config.ip4.address", "10.10.10.5");
        // props.put("net.interface.intf.config.ip6.address", "1080:0:0:0:8:800:200C:417A");
        // props.put("net.interface.intf.config.ip6.dnsServers", "1080:0:0:0:8:800:200C:417B");

        props.put("net.interface.intf.config.wifi.mode", WifiMode.INFRA.name());
        props.put("net.interface.intf.config.wifi.master.passphrase", passkey);
        props.put("net.interface.intf.config.wifi.infra.passphrase", passkey);

        props.put("net.interface.intf.config.dhcpServer4.enabled", true);
        props.put("net.interface.intf.config.dhcpServer4.rangeStart", "10.10.10.10");
        props.put("net.interface.intf.config.dhcpServer4.rangeEnd", "10.10.10.15");
        props.put("net.interface.intf.config.dhcpServer4.defaultLeaseTime", 900);
        props.put("net.interface.intf.config.dhcpServer4.maxLeaseTime", 900);
        props.put("net.interface.intf.config.dhcpServer4.prefix", "24");

        props.put("net.interface.intf.config.nat.enabled", true);

        ComponentConfiguration cc = new ComponentConfigurationImpl("pid", null, props);
        when(((SelfConfiguringComponent) networkConfigurationServiceMock).getConfiguration()).thenReturn(cc);

        doAnswer(invocation -> {
            NetworkConfiguration networkConfiguration = invocation.getArgumentAt(0, NetworkConfiguration.class);

            assertEquals(1, networkConfiguration.getModifiedInterfaceNames().size());
            assertEquals(interfaceName, networkConfiguration.getModifiedInterfaceNames().get(0));

            NetInterfaceConfig<? extends NetInterfaceAddressConfig> config = networkConfiguration
                    .getNetInterfaceConfig(interfaceName);

            assertEquals("driver", config.getDriver());
            assertEquals(NetInterfaceState.ACTIVATED, config.getState());
            assertFalse(config.isAutoConnect());

            List<? extends NetInterfaceAddressConfig> addresses = config.getNetInterfaceAddresses();
            assertEquals(1, addresses.size());

            WifiInterfaceAddressConfigImpl address = (WifiInterfaceAddressConfigImpl) addresses.get(0);

            assertEquals(WifiMode.INFRA, address.getMode());
            assertEquals(6, address.getConfigs().size());

            for (NetConfig cfg : address.getConfigs()) {
                assertFalse(cfg instanceof DhcpServerConfigIP6);

                if (cfg instanceof DhcpServerConfigIP4 || cfg instanceof FirewallAutoNatConfig) {
                    if (cfg instanceof DhcpServerConfigIP4) {
                        DhcpServerConfigIP4 dhcpcfg = (DhcpServerConfigIP4) cfg;

                        assertEquals(1000, dhcpcfg.getMaximumLeaseTime());
                    } else if (cfg instanceof FirewallAutoNatConfig) {
                        FirewallAutoNatConfig nat = (FirewallAutoNatConfig) cfg;

                        assertEquals("src", nat.getSourceInterface());
                        assertEquals("dest", nat.getDestinationInterface());
                    }
                }
            }

            return null;
        }).when(networkConfigurationServiceMock).setNetworkConfiguration(anyObject());

        ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
        nasi.setConfigurationService(configurationServiceMock);

        when(configurationServiceMock.snapshot()).thenAnswer(invocation -> {
            // snapshot is created just before waiting for the event is started
            Thread.sleep(500);

            Event event = new Event(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
                    (Map<String, ?>) null);

            nasi.handleEvent(event);

            return 1234;
        });

        NetworkConfiguration nc = new NetworkConfiguration();
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        NetConfigIP4 netConfig4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig4);

        NetConfigIP6 netConfig6 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true, true);
        netConfigs.add(netConfig6);

        FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig("src", "dest", true);
        netConfigs.add(natConfig);

        WifiMode mode = WifiMode.INFRA;
        String ssid = "testSSID";
        int[] channels = { 1, 3 };
        WifiSecurity security = WifiSecurity.NONE;
        String hwMode = "";
        boolean broadcast = false;
        WifiBgscan bgscan = new WifiBgscan("");
        WifiConfig wifiConfig = new WifiConfig(mode, ssid, channels, security, passkey, hwMode, broadcast, bgscan);
        netConfigs.add(wifiConfig);

        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        WifiAccessPoint accessPoint = new WifiAccessPointImpl(ssid);
        nasi.updateWifiInterfaceConfig(interfaceName, true, accessPoint, netConfigs);

        verify(networkConfigurationServiceMock, times(1)).setNetworkConfiguration(anyObject());
    }

    @Test
    public void testUpdateEthernetInterfaceConfigInvalidNetConfig() throws KuraException, UnknownHostException {
        // test error with invalid network configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        EthernetInterfaceConfigImpl netInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        NetInterfaceAddressConfigImpl netInterfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        NetConfig netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true) {

            @Override
            public boolean isValid() {
                return false;
            }
        };
        netConfigs.add(netConfig);

        netInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(netInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        try {
            nasi.updateEthernetInterfaceConfig(interfaceName, true, 1492, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("invalid"));
        }

    }

    @Test
    public void testUpdateEthernetInterfaceConfigMissingConfigIP() throws KuraException, UnknownHostException {
        // test error with missing IP configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        EthernetInterfaceConfigImpl netInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        NetInterfaceAddressConfigImpl netInterfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        netInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(netInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new EthernetInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        try {
            nasi.updateEthernetInterfaceConfig(interfaceName, true, 1492, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
            assertTrue(e.getMessage().contains("IPv4 or IPv6"));
        }

    }

    private void addDhcpServerConfig4(String interfaceName, List<NetConfig> netConfigs)
            throws UnknownHostException, KuraException {
        boolean enabled = true;
        int defaultLeaseTime = 900;
        int maximumLeaseTime = 1000;
        boolean passDns = true;
        DhcpServerCfg svrCfg = new DhcpServerCfg(interfaceName, enabled, defaultLeaseTime, maximumLeaseTime, passDns);
        IP4Address subnet = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x00 });
        IP4Address subnetMask = (IP4Address) IPAddress
                .getByAddress(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00 });
        short prefix = 24;
        IP4Address routerAddress = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFA });
        IP4Address rangeStart = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x0A });
        IP4Address rangeEnd = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x0F });
        List<IP4Address> dnsServers = new ArrayList<>();
        IP4Address dnsAddress = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFE });
        dnsServers.add(dnsAddress);
        DhcpServerCfgIP4 svrCfg4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress, rangeStart, rangeEnd,
                dnsServers);
        DhcpServerConfig4 netConfig = new DhcpServerConfigIP4(svrCfg, svrCfg4);
        netConfigs.add(netConfig);
    }

    @Test
    public void testUpdateEthernetInterfaceConfig() throws KuraException, UnknownHostException {
        // update ethernet interface with new dhcp and nat configs

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class,
                withSettings().extraInterfaces(SelfConfiguringComponent.class));
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        String interfaceName = "intf";
        String passkey = "pass";

        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", "intf");
        props.put("net.interface.intf.type", "ETHERNET");
        props.put("net.interface.intf.state", NetInterfaceState.ACTIVATED);
        props.put("net.interface.intf.driver", "driver");
        props.put("net.interface.intf.mtu", "1500");
        props.put("net.interface.intf.mac", "12:34:56:78:90:12");
        props.put("net.interface.intf.up", true);

        props.put("net.interface.intf.config.ip4.address", "10.10.10.5");

        props.put("net.interface.intf.config.eth.link.up", true);

        props.put("net.interface.intf.config.dhcpServer4.enabled", true);
        props.put("net.interface.intf.config.dhcpServer4.rangeStart", "10.10.10.10");
        props.put("net.interface.intf.config.dhcpServer4.rangeEnd", "10.10.10.15");
        props.put("net.interface.intf.config.dhcpServer4.defaultLeaseTime", 900);
        props.put("net.interface.intf.config.dhcpServer4.maxLeaseTime", 900);
        props.put("net.interface.intf.config.dhcpServer4.prefix", "24");

        props.put("net.interface.intf.config.nat.enabled", true);

        ComponentConfiguration cc = new ComponentConfigurationImpl("pid", null, props);
        when(((SelfConfiguringComponent) networkConfigurationServiceMock).getConfiguration()).thenReturn(cc);

        doAnswer(invocation -> {
            NetworkConfiguration networkConfiguration = invocation.getArgumentAt(0, NetworkConfiguration.class);

            assertEquals(1, networkConfiguration.getModifiedInterfaceNames().size());
            assertEquals(interfaceName, networkConfiguration.getModifiedInterfaceNames().get(0));

            NetInterfaceConfig<? extends NetInterfaceAddressConfig> config = networkConfiguration
                    .getNetInterfaceConfig(interfaceName);

            assertEquals("driver", config.getDriver());
            assertEquals(NetInterfaceState.ACTIVATED, config.getState());
            assertTrue(config.isAutoConnect());

            List<? extends NetInterfaceAddressConfig> addresses = config.getNetInterfaceAddresses();
            assertEquals(1, addresses.size());

            NetInterfaceAddressConfigImpl address = (NetInterfaceAddressConfigImpl) addresses.get(0);

            assertEquals(4, address.getConfigs().size());

            for (NetConfig cfg : address.getConfigs()) {
                assertFalse(cfg instanceof DhcpServerConfigIP6);

                if (cfg instanceof DhcpServerConfigIP4 || cfg instanceof FirewallAutoNatConfig) {
                    if (cfg instanceof DhcpServerConfigIP4) {
                        DhcpServerConfigIP4 dhcpcfg = (DhcpServerConfigIP4) cfg;

                        assertEquals(1000, dhcpcfg.getMaximumLeaseTime());
                    } else if (cfg instanceof FirewallAutoNatConfig) {
                        FirewallAutoNatConfig nat = (FirewallAutoNatConfig) cfg;

                        assertEquals("src", nat.getSourceInterface());
                        assertEquals("dest", nat.getDestinationInterface());
                    }
                }
            }

            return null;
        }).when(networkConfigurationServiceMock).setNetworkConfiguration(anyObject());

        ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
        nasi.setConfigurationService(configurationServiceMock);

        when(configurationServiceMock.snapshot()).thenAnswer(invocation -> {
            // snapshot is created just before waiting for the event is started
            Thread.sleep(500);

            Event event = new Event(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
                    (Map<String, ?>) null);

            nasi.handleEvent(event);

            return 1234;
        });

        NetworkConfiguration nc = new NetworkConfiguration();
        EthernetInterfaceConfigImpl netInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        NetInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        NetConfigIP4 netConfig4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig4);

        NetConfigIP6 netConfig6 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true, true);
        netConfigs.add(netConfig6);

        FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig("src", "dest", true);
        netConfigs.add(natConfig);

        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new EthernetInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        nasi.updateEthernetInterfaceConfig(interfaceName, true, 1500, netConfigs);

        verify(networkConfigurationServiceMock, times(1)).setNetworkConfiguration(anyObject());
    }

    @Test
    public void testUpdateModemInterfaceConfigInvalidNetConfig() throws KuraException, UnknownHostException {
        // test error with invalid network configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl netInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        NetConfig netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true) {

            @Override
            public boolean isValid() {
                return false;
            }
        };
        netConfigs.add(netConfig);

        netInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(netInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        String serial = "modemserial";
        String modemId = "modemid";
        int ppp = 0;

        try {
            nasi.updateModemInterfaceConfig(interfaceName, serial, modemId, ppp, true, 1492, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("invalid"));
        }

    }

    @Test
    public void testUpdateModemInterfaceConfigMissingConfigIP() throws KuraException, UnknownHostException {
        // test error with missing IP configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl netInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        netInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(netInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new ModemInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        String serial = "modemserial";
        String modemId = "modemid";
        int ppp = 0;

        try {
            nasi.updateModemInterfaceConfig(interfaceName, serial, modemId, ppp, true, 1492, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
            assertTrue(e.getMessage().contains("IPv4 or IPv6"));
        }

    }

    @Test
    public void testUpdateModemInterfaceConfigMissingModemConfig() throws KuraException, UnknownHostException {
        // test error with missing modem configuration

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class);
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "intf";
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl netInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        NetConfigIP4 ipConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        ipConfig.setAddress((IP4Address) IP4Address.parseHostAddress("10.10.10.5"));
        netConfigs.add(ipConfig);

        netInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(netInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new ModemInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        String serial = "modemserial";
        String modemId = "modemid";
        int ppp = 0;

        try {
            nasi.updateModemInterfaceConfig(interfaceName, serial, modemId, ppp, true, 1492, netConfigs);
            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
            assertTrue(e.getMessage().contains("Modem configuration"));
        }

    }

    @Test
    public void testUpdateModemInterfaceConfig() throws KuraException, UnknownHostException {
        // update modem interface with new dhcp and nat configs

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class,
                withSettings().extraInterfaces(SelfConfiguringComponent.class));
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        String interfaceName = "intf";
        String passkey = "pass";

        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", "intf");
        props.put("net.interface.intf.type", "MODEM");
        props.put("net.interface.intf.state", NetInterfaceState.ACTIVATED);
        props.put("net.interface.intf.driver", "driver");
        props.put("net.interface.intf.mtu", "1500");
        props.put("net.interface.intf.mac", "12:34:56:78:90:12");
        props.put("net.interface.intf.up", true);

        props.put("net.interface.intf.config.password", passkey);

        props.put("net.interface.intf.config.ip4.address", "10.10.10.5");

        props.put("net.interface.intf.config.eth.link.up", true);

        props.put("net.interface.intf.config.dhcpServer4.enabled", true);
        props.put("net.interface.intf.config.dhcpServer4.rangeStart", "10.10.10.10");
        props.put("net.interface.intf.config.dhcpServer4.rangeEnd", "10.10.10.15");
        props.put("net.interface.intf.config.dhcpServer4.defaultLeaseTime", 900);
        props.put("net.interface.intf.config.dhcpServer4.maxLeaseTime", 900);
        props.put("net.interface.intf.config.dhcpServer4.prefix", "24");

        props.put("net.interface.intf.config.nat.enabled", true);

        ComponentConfiguration cc = new ComponentConfigurationImpl("pid", null, props);
        when(((SelfConfiguringComponent) networkConfigurationServiceMock).getConfiguration()).thenReturn(cc);

        doAnswer(invocation -> {
            NetworkConfiguration networkConfiguration = invocation.getArgumentAt(0, NetworkConfiguration.class);

            assertEquals(1, networkConfiguration.getModifiedInterfaceNames().size());
            assertEquals(interfaceName, networkConfiguration.getModifiedInterfaceNames().get(0));

            NetInterfaceConfig<? extends NetInterfaceAddressConfig> config = networkConfiguration
                    .getNetInterfaceConfig(interfaceName);

            assertEquals("driver", config.getDriver());
            assertEquals(NetInterfaceState.ACTIVATED, config.getState());
            assertFalse(config.isAutoConnect());

            List<? extends NetInterfaceAddressConfig> addresses = config.getNetInterfaceAddresses();
            assertEquals(1, addresses.size());

            ModemInterfaceAddressConfigImpl address = (ModemInterfaceAddressConfigImpl) addresses.get(0);

            assertEquals(3, address.getConfigs().size());

            for (NetConfig cfg : address.getConfigs()) {
                assertFalse(cfg instanceof DhcpServerConfigIP6);

                if (cfg instanceof DhcpServerConfigIP4 || cfg instanceof FirewallAutoNatConfig) {
                    if (cfg instanceof DhcpServerConfigIP4) {
                        DhcpServerConfigIP4 dhcpcfg = (DhcpServerConfigIP4) cfg;

                        assertEquals(1000, dhcpcfg.getMaximumLeaseTime());
                    } else if (cfg instanceof FirewallAutoNatConfig) {
                        FirewallAutoNatConfig nat = (FirewallAutoNatConfig) cfg;

                        assertEquals("src", nat.getSourceInterface());
                        assertEquals("dest", nat.getDestinationInterface());
                    }
                }
            }

            return null;
        }).when(networkConfigurationServiceMock).setNetworkConfiguration(anyObject());

        ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
        nasi.setConfigurationService(configurationServiceMock);

        when(configurationServiceMock.snapshot()).thenAnswer(invocation -> {
            // snapshot is created just before waiting for the event is started
            Thread.sleep(500);

            Event event = new Event(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
                    (Map<String, ?>) null);

            nasi.handleEvent(event);

            return 1234;
        });

        NetworkConfiguration nc = new NetworkConfiguration();
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        addDhcpServerConfig4(interfaceName, netConfigs);

        NetConfigIP4 netConfig4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig4);

        NetConfigIP6 netConfig6 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true, true);
        netConfigs.add(netConfig6);

        FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig("src", "dest", true);
        netConfigs.add(natConfig);

        ModemConfig modemConfig = new ModemConfig();
        netConfigs.add(modemConfig);

        modemInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(modemInterfaceAddressConfig);

        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new ModemInterfaceConfigImpl("intf2");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        String serial = "serial";
        String modemId = "modemid";
        int ppp = 0;
        nasi.updateModemInterfaceConfig(interfaceName, serial, modemId, ppp, true, 1500, netConfigs);

        verify(networkConfigurationServiceMock, times(1)).setNetworkConfiguration(anyObject());
    }

    @Test
    public void testGetWifiAddressConfigEmpty() throws Throwable {
        // no configs available

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        List<? extends NetInterfaceAddressConfig> configs = new ArrayList<>();

        Object wifiConfig = TestUtil.invokePrivate(nasi, "getWifiAddressConfig", configs);

        assertNull(wifiConfig);
    }

    @Test
    public void testGetWifiAddressConfigNoWifi() throws Throwable {
        // no wifi configs available

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetInterfaceAddressConfig[] arr = { new ModemInterfaceAddressConfigImpl() };
        List<? extends NetInterfaceAddressConfig> configs = Arrays.asList(arr);

        Object wifiConfig = TestUtil.invokePrivate(nasi, "getWifiAddressConfig", configs);

        assertNull(wifiConfig);
    }

    @Test
    public void testGetWifiAddressConfig() throws Throwable {
        // first wifi config returned

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        WifiInterfaceAddressConfigImpl wifi1 = new WifiInterfaceAddressConfigImpl();
        wifi1.setMode(WifiMode.ADHOC);

        WifiInterfaceAddressConfigImpl wifi2 = new WifiInterfaceAddressConfigImpl();
        wifi2.setMode(WifiMode.INFRA);

        NetInterfaceAddressConfig[] arr = { new ModemInterfaceAddressConfigImpl(), wifi1, wifi2 };
        List<? extends NetInterfaceAddressConfig> configs = Arrays.asList(arr);

        Object wifiConfig = TestUtil.invokePrivate(nasi, "getWifiAddressConfig", configs);

        assertNotNull(wifiConfig);
        assertEquals(wifi1, wifiConfig);
    }

    @Test
    public void testGetWifiInterfaceConfigsNoWifi() throws Throwable {
        // test it with no wifi available

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class,
                withSettings().extraInterfaces(SelfConfiguringComponent.class));
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "modem";
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> result = (List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .invokePrivate(nasi, "getWifiInterfaceConfigs");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetWifiInterfaceConfigs() throws Throwable {
        // test with one wifi available

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class,
                withSettings().extraInterfaces(SelfConfiguringComponent.class));
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "modem";
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        interfaceName = "wifi";
        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> wifiAddressConfigs = new ArrayList<>();
        wifiInterfaceConfig.setNetInterfaceAddresses(wifiAddressConfigs);
        nc.addNetInterfaceConfig(wifiInterfaceConfig);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> result = (List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .invokePrivate(nasi, "getWifiInterfaceConfigs");

        assertEquals(1, result.size());
        assertEquals(wifiInterfaceConfig, result.get(0));
    }

    @Test
    public void testGetWifiInterfaceConfigsMultiWifi() throws Throwable {
        // test with multiple wifis available

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        NetworkConfigurationService networkConfigurationServiceMock = mock(NetworkConfigurationService.class,
                withSettings().extraInterfaces(SelfConfiguringComponent.class));
        nasi.setNetworkConfigurationService(networkConfigurationServiceMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        String interfaceName = "modem";
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        nc.addNetInterfaceConfig(netInterfaceConfig);

        interfaceName = "wifi1";
        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> wifiAddressConfigs = new ArrayList<>();
        wifiInterfaceConfig.setNetInterfaceAddresses(wifiAddressConfigs);
        nc.addNetInterfaceConfig(wifiInterfaceConfig);

        interfaceName = "wif2";
        WifiInterfaceConfigImpl wifiInterfaceConfig2 = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> wifiAddressConfigs2 = new ArrayList<>();
        wifiInterfaceConfig2.setNetInterfaceAddresses(wifiAddressConfigs2);
        nc.addNetInterfaceConfig(wifiInterfaceConfig2);

        when(networkConfigurationServiceMock.getNetworkConfiguration()).thenReturn(nc);

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> result = (List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .invokePrivate(nasi, "getWifiInterfaceConfigs");

        assertEquals(2, result.size());
        assertEquals(wifiInterfaceConfig, result.get(0));
        assertEquals(wifiInterfaceConfig2, result.get(1));
    }

    @Test
    public void testGetWifiNetInterfaceAddressConfigsEmptyList() throws Throwable {
        // empty original list
        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> configs = new ArrayList<>();

        String interfaceName = "wlan0";
        List<? extends NetInterfaceAddressConfig> result = (List<? extends NetInterfaceAddressConfig>) TestUtil
                .invokePrivate(nasi, "getWifiNetInterfaceAddressConfigs", interfaceName, configs);

        assertNull(result);
    }

    @Test
    public void testGetWifiNetInterfaceAddressConfigsNoWifi() throws Throwable {
        // sought service not in the original list

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> configs = new ArrayList<>();

        NetInterfaceConfig<? extends NetInterfaceAddressConfig> intf1 = new EthernetInterfaceConfigImpl("eth0");
        configs.add(intf1);
        NetInterfaceConfig<? extends NetInterfaceAddressConfig> intf2 = new ModemInterfaceConfigImpl("ppp0");
        configs.add(intf2);

        String interfaceName = "wlan0";
        List<? extends NetInterfaceAddressConfig> result = (List<? extends NetInterfaceAddressConfig>) TestUtil
                .invokePrivate(nasi, "getWifiNetInterfaceAddressConfigs", interfaceName, configs);

        assertNull(result);
    }

    @Test
    public void testGetWifiNetInterfaceAddressConfigs() throws Throwable {
        // sought service is in the original list, but not necessarily WiFi

        NetworkAdminServiceImpl nasi = new NetworkAdminServiceImpl();

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> configs = new ArrayList<>();

        EthernetInterfaceConfigImpl intf1 = new EthernetInterfaceConfigImpl("eth0");
        List<NetInterfaceAddressConfig> ia = new ArrayList<>();
        intf1.setNetInterfaceAddresses(ia);
        configs.add(intf1);

        ModemInterfaceConfigImpl intf2 = new ModemInterfaceConfigImpl("wlan0");
        List<ModemInterfaceAddressConfig> ia2 = new ArrayList<>();
        ModemInterfaceAddressConfig miac = new ModemInterfaceAddressConfigImpl();
        ia2.add(miac);
        intf2.setNetInterfaceAddresses(ia2);
        configs.add(intf2);

        String interfaceName = "wlan0";
        List<? extends NetInterfaceAddressConfig> result = (List<? extends NetInterfaceAddressConfig>) TestUtil
                .invokePrivate(nasi, "getWifiNetInterfaceAddressConfigs", interfaceName, configs);

        assertNotNull(result);
        assertEquals(intf2.getNetInterfaceAddresses(), result);
    }

    @Test
    public void testGetCiphersEmpty() throws Throwable {
        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        EnumSet<WifiSecurity> esSecurity = EnumSet.noneOf(WifiSecurity.class);
        EnumSet<WifiSecurity> pairCiphers = EnumSet.noneOf(WifiSecurity.class);
        EnumSet<WifiSecurity> groupCiphers = EnumSet.noneOf(WifiSecurity.class);

        TestUtil.invokePrivate(svc, "getCiphers", esSecurity, pairCiphers, groupCiphers);

        assertEquals(0, pairCiphers.size());
        assertEquals(0, groupCiphers.size());
    }

    @Test
    public void testGetCiphersSomeIn() throws Throwable {
        // some ciphers are already populated

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        EnumSet<WifiSecurity> esSecurity = EnumSet.allOf(WifiSecurity.class);
        EnumSet<WifiSecurity> pairCiphers = EnumSet.of(WifiSecurity.PAIR_TKIP);
        EnumSet<WifiSecurity> groupCiphers = EnumSet.of(WifiSecurity.GROUP_TKIP);

        TestUtil.invokePrivate(svc, "getCiphers", esSecurity, pairCiphers, groupCiphers);

        assertEquals(2, pairCiphers.size());
        assertTrue(pairCiphers.contains(WifiSecurity.PAIR_CCMP));
        assertTrue(pairCiphers.contains(WifiSecurity.PAIR_TKIP));
        assertEquals(2, groupCiphers.size());
        assertTrue(groupCiphers.contains(WifiSecurity.GROUP_CCMP));
        assertTrue(groupCiphers.contains(WifiSecurity.GROUP_TKIP));
    }

    @Test
    public void testGetCiphersSomeOut() throws Throwable {
        // some ciphers are already populated, but not available

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        EnumSet<WifiSecurity> esSecurity = EnumSet.of(WifiSecurity.PAIR_TKIP);
        EnumSet<WifiSecurity> pairCiphers = EnumSet.of(WifiSecurity.PAIR_TKIP);
        EnumSet<WifiSecurity> groupCiphers = EnumSet.of(WifiSecurity.GROUP_TKIP);

        TestUtil.invokePrivate(svc, "getCiphers", esSecurity, pairCiphers, groupCiphers);

        assertEquals(1, pairCiphers.size());
        assertTrue(pairCiphers.contains(WifiSecurity.PAIR_TKIP));
        assertEquals(1, groupCiphers.size());
        assertTrue(groupCiphers.contains(WifiSecurity.GROUP_TKIP));
    }

    @Test
    public void testGetCiphersSomeNotAvailable() throws Throwable {
        // some ciphers are not available

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        EnumSet<WifiSecurity> esSecurityTemp = EnumSet.of(WifiSecurity.PAIR_CCMP, WifiSecurity.GROUP_TKIP);
        EnumSet<WifiSecurity> esSecurity = EnumSet.complementOf(esSecurityTemp);

        EnumSet<WifiSecurity> pairCiphers = EnumSet.noneOf(WifiSecurity.class);
        EnumSet<WifiSecurity> groupCiphers = EnumSet.noneOf(WifiSecurity.class);

        TestUtil.invokePrivate(svc, "getCiphers", esSecurity, pairCiphers, groupCiphers);

        assertEquals(1, pairCiphers.size());
        assertTrue(pairCiphers.contains(WifiSecurity.PAIR_TKIP));
        assertEquals(1, groupCiphers.size());
        assertTrue(groupCiphers.contains(WifiSecurity.GROUP_CCMP));
    }

    @Test
    public void testGetCiphersAll() throws Throwable {
        // all are available

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        EnumSet<WifiSecurity> esSecurity = EnumSet.allOf(WifiSecurity.class);
        EnumSet<WifiSecurity> pairCiphers = EnumSet.noneOf(WifiSecurity.class);
        EnumSet<WifiSecurity> groupCiphers = EnumSet.noneOf(WifiSecurity.class);

        TestUtil.invokePrivate(svc, "getCiphers", esSecurity, pairCiphers, groupCiphers);

        assertEquals(2, pairCiphers.size());
        assertTrue(pairCiphers.contains(WifiSecurity.PAIR_CCMP));
        assertTrue(pairCiphers.contains(WifiSecurity.PAIR_TKIP));
        assertEquals(2, groupCiphers.size());
        assertTrue(groupCiphers.contains(WifiSecurity.GROUP_CCMP));
        assertTrue(groupCiphers.contains(WifiSecurity.GROUP_TKIP));
    }

    @Test
    public void testSetCiphersNoSecurity() throws Throwable {
        // tests with input mostly empty producing security_none result

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiHotspotInfo wifiHotspotInfo = prepareWifiHotspotInfo(ssid);

        WifiAccessPointImpl wap = prepareEmptyWap(ssid);

        WifiSecurity wifiSecurity = WifiSecurity.SECURITY_NONE;

        TestUtil.invokePrivate(svc, "setCiphers", wifiHotspotInfo, wap, wifiSecurity);

        final EnumSet<WifiSecurity> groups = wifiHotspotInfo.getGroupCiphers();
        assertNotNull(groups);
        assertTrue(groups.isEmpty());

        final EnumSet<WifiSecurity> pairs = wifiHotspotInfo.getPairCiphers();
        assertNotNull(pairs);
        assertTrue(pairs.isEmpty());
    }

    private WifiHotspotInfo prepareWifiHotspotInfo(String ssid) {
        String macAddress = "12345678";
        int signalLevel = 1;
        int channel = 1;
        int frequency = 1415;
        WifiSecurity security = WifiSecurity.NONE;

        WifiHotspotInfo wifiHotspotInfo = new WifiHotspotInfo(ssid, macAddress, signalLevel, channel, frequency,
                security);

        return wifiHotspotInfo;
    }

    private WifiAccessPointImpl prepareEmptyWap(String ssid) {
        WifiAccessPointImpl wap = new WifiAccessPointImpl(ssid);

        EnumSet<WifiSecurity> wpaSecurity = EnumSet.noneOf(WifiSecurity.class);
        wap.setWpaSecurity(wpaSecurity);
        EnumSet<WifiSecurity> rsnSecurity = EnumSet.noneOf(WifiSecurity.class);
        wap.setRsnSecurity(rsnSecurity);

        return wap;
    }

    private WifiAccessPointImpl prepareFilledWap(String ssid) {
        WifiAccessPointImpl wap = new WifiAccessPointImpl(ssid);

        EnumSet<WifiSecurity> wpaSecurity = EnumSet.of(WifiSecurity.PAIR_TKIP, WifiSecurity.GROUP_WEP40);
        wap.setWpaSecurity(wpaSecurity);
        EnumSet<WifiSecurity> rsnSecurity = EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.KEY_MGMT_PSK);
        wap.setRsnSecurity(rsnSecurity);

        return wap;
    }

    @Test
    public void testSetCiphersWpaSecurity() throws Throwable {
        // WPA with both pair and group ciphers configured => both used

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiHotspotInfo wifiHotspotInfo = prepareWifiHotspotInfo(ssid);

        WifiAccessPointImpl wap = prepareFilledWap(ssid);

        WifiSecurity wifiSecurity = WifiSecurity.SECURITY_WPA;

        TestUtil.invokePrivate(svc, "setCiphers", wifiHotspotInfo, wap, wifiSecurity);

        final EnumSet<WifiSecurity> groups = wifiHotspotInfo.getGroupCiphers();
        assertNotNull(groups);
        assertTrue(groups.isEmpty());

        final EnumSet<WifiSecurity> pairs = wifiHotspotInfo.getPairCiphers();
        assertNotNull(pairs);
        assertEquals(1, pairs.size());
        assertEquals(WifiSecurity.PAIR_TKIP, pairs.iterator().next());
    }

    @Test
    public void testSetCiphersWpa2Security() throws Throwable {
        // WPA2 with both pair and group ciphers configured => only group ciphers are used

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiHotspotInfo wifiHotspotInfo = prepareWifiHotspotInfo(ssid);

        WifiAccessPointImpl wap = prepareFilledWap(ssid);

        WifiSecurity wifiSecurity = WifiSecurity.SECURITY_WPA2;

        TestUtil.invokePrivate(svc, "setCiphers", wifiHotspotInfo, wap, wifiSecurity);

        final EnumSet<WifiSecurity> groups = wifiHotspotInfo.getGroupCiphers();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(WifiSecurity.GROUP_CCMP, groups.iterator().next());

        final EnumSet<WifiSecurity> pairs = wifiHotspotInfo.getPairCiphers();
        assertNotNull(pairs);
        assertTrue(pairs.isEmpty());
    }

    @Test
    public void testSetCiphersWpaWpa2Security() throws Throwable {
        // WPA_WPA2 with both pair and group ciphers configured => only pair ciphers are used

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiHotspotInfo wifiHotspotInfo = prepareWifiHotspotInfo(ssid);

        WifiAccessPointImpl wap = prepareFilledWap(ssid);

        WifiSecurity wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;

        TestUtil.invokePrivate(svc, "setCiphers", wifiHotspotInfo, wap, wifiSecurity);

        final EnumSet<WifiSecurity> groups = wifiHotspotInfo.getGroupCiphers();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(WifiSecurity.GROUP_CCMP, groups.iterator().next());

        final EnumSet<WifiSecurity> pairs = wifiHotspotInfo.getPairCiphers();
        assertNotNull(pairs);
        assertEquals(1, pairs.size());
        assertEquals(WifiSecurity.PAIR_TKIP, pairs.iterator().next());

        System.out.println(pairs.toString());
    }

    @Test
    public void testGetWifiSecurityNoSecurity() throws Throwable {
        // no ciphers allowed => no security

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiAccessPointImpl wap = prepareEmptyWap(ssid);

        WifiSecurity result = (WifiSecurity) TestUtil.invokePrivate(svc, "getWifiSecurity", wap);

        assertEquals(WifiSecurity.NONE, result);
    }

    @Test
    public void testGetWifiSecurityWrongWep() throws Throwable {
        // no ciphers and no bad capability => no WEP

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiAccessPointImpl wap = prepareEmptyWap(ssid);
        List<String> capabilities = new ArrayList<>();
        capabilities.add("WEP Privacy");
        wap.setCapabilities(capabilities);

        WifiSecurity result = (WifiSecurity) TestUtil.invokePrivate(svc, "getWifiSecurity", wap);

        assertEquals(WifiSecurity.NONE, result);
    }

    @Test
    public void testGetWifiSecurityWep() throws Throwable {
        // no ciphers but Privacy capability => WEP

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiAccessPointImpl wap = prepareEmptyWap(ssid);
        List<String> capabilities = new ArrayList<>();
        capabilities.add("Privacy");
        wap.setCapabilities(capabilities);

        WifiSecurity result = (WifiSecurity) TestUtil.invokePrivate(svc, "getWifiSecurity", wap);

        assertEquals(WifiSecurity.SECURITY_WEP, result);
    }

    @Test
    public void testGetWifiSecurityWpaWpa2() throws Throwable {
        // filled values correspond to both WPA and WPA2

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiAccessPointImpl wap = prepareFilledWap(ssid);

        WifiSecurity result = (WifiSecurity) TestUtil.invokePrivate(svc, "getWifiSecurity", wap);

        assertEquals(WifiSecurity.SECURITY_WPA_WPA2, result);
    }

    @Test
    public void testGetWifiSecurityWpa2() throws Throwable {
        // only rsn is filled => WPA2, but not WPA

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiAccessPointImpl wap = prepareFilledWap(ssid);
        wap.setWpaSecurity(null);

        WifiSecurity result = (WifiSecurity) TestUtil.invokePrivate(svc, "getWifiSecurity", wap);

        assertEquals(WifiSecurity.SECURITY_WPA2, result);
    }

    @Test
    public void testGetWifiSecurityWpa() throws Throwable {
        // only wpa is filled
        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String ssid = "testSSID";
        WifiAccessPointImpl wap = prepareFilledWap(ssid);
        wap.setRsnSecurity(null);

        WifiSecurity result = (WifiSecurity) TestUtil.invokePrivate(svc, "getWifiSecurity", wap);

        assertEquals(WifiSecurity.SECURITY_WPA, result);
    }

    @Test
    public void testGetWifiModeEmpty() throws Throwable {
        // wifi mode with no wifi => unknown
        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        List<NetInterfaceAddressConfig> addresses = new ArrayList<>();

        WifiMode result = (WifiMode) TestUtil.invokePrivate(svc, "getWifiMode", new Class[] { List.class }, addresses);

        assertEquals(WifiMode.UNKNOWN, result);
    }

    @Test
    public void testGetWifiModeWithWifi() throws Throwable {
        // return the first configured wifi mode

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        List<WifiInterfaceAddressConfigImpl> addresses = new ArrayList<>();
        WifiInterfaceAddressConfigImpl iac = new WifiInterfaceAddressConfigImpl();
        WifiMode mode = WifiMode.INFRA; // this one will be used
        iac.setMode(mode);
        addresses.add(iac);
        iac = new WifiInterfaceAddressConfigImpl();
        iac.setMode(WifiMode.MASTER);
        addresses.add(iac);

        WifiMode result = (WifiMode) TestUtil.invokePrivate(svc, "getWifiMode", new Class[] { List.class }, addresses);

        assertEquals(mode, result);
    }

    @Test
    public void testGetWifiModeIntf() throws Throwable {
        // return the first configured wifi mode

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        String interfaceName = "wlan3";

        List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs;
        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        when(ncsMock.getNetworkConfiguration()).thenReturn(nc);

        List<WifiInterfaceAddressConfig> addresses = new ArrayList<>();
        WifiInterfaceAddressConfigImpl iac = new WifiInterfaceAddressConfigImpl();
        WifiMode mode = WifiMode.INFRA; // this one is to be returned
        iac.setMode(mode);
        addresses.add(iac);
        iac = new WifiInterfaceAddressConfigImpl();
        iac.setMode(WifiMode.MASTER);
        addresses.add(iac);

        WifiInterfaceConfigImpl nic = new WifiInterfaceConfigImpl("testwlan3");
        nc.addNetInterfaceConfig(nic);

        nic = new WifiInterfaceConfigImpl(interfaceName);
        nic.setNetInterfaceAddresses(addresses);
        nc.addNetInterfaceConfig(nic);

        WifiMode result = (WifiMode) TestUtil.invokePrivate(svc, "getWifiMode", new Class[] { String.class },
                interfaceName);

        assertEquals(mode, result);
    }

    @Test
    public void testIsHotspotInListEmptyList() throws Throwable {
        // no hotspots available

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        List<WifiHotspotInfo> list = new ArrayList<>();

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isHotspotInList", 1, "testSSID", list);

        assertFalse(result);
    }

    @Test
    public void testIsHotspotInListNotThere() throws Throwable {
        // the sought hostpot is unavailable

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        List<WifiHotspotInfo> list = new ArrayList<>();
        WifiHotspotInfo whi1 = prepareWifiHotspotInfo("someSsid");
        list.add(whi1);
        WifiHotspotInfo whi2 = prepareWifiHotspotInfo("testSsid");
        list.add(whi2);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isHotspotInList", 1, "testSSID", list);

        assertFalse(result);
    }

    @Test
    public void testIsHotspotInList() throws Throwable {
        // it's there...

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        List<WifiHotspotInfo> list = new ArrayList<>();
        WifiHotspotInfo whi1 = prepareWifiHotspotInfo("someSsid");
        list.add(whi1);
        final String ssid = "testSSID";
        WifiHotspotInfo whi2 = prepareWifiHotspotInfo(ssid);
        list.add(whi2);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isHotspotInList", 1, "testSSID", list);

        assertTrue(result);
    }

    @Test
    public void testGetMacAddress() throws Throwable {
        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        byte[] mac = { 10, 11, 12, 13, 14, (byte) 255 };

        String result = (String) TestUtil.invokePrivate(svc, "getMacAddress", mac);

        assertEquals("0A:0B:0C:0D:0E:FF", result);
    }

    @Test
    public void testFrequencyToChannel() throws Throwable {
        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl();

        int[] freqs = { 0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462 };

        for (int i = 1; i < freqs.length; i++) {
            int result = (int) TestUtil.invokePrivate(svc, "frequencyMhz2Channel", freqs[i]);

            assertEquals(i, result);
        }
    }

    @Test
    public void testGetWifiHotspotListException() throws KuraException {
        // tests retrieval of available hotspots - exception case

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl() {

            @Override
            protected List<WifiAccessPoint> getWifiAccessPoints(String ifaceName) throws KuraException {
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "test");
            }
        };

        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        String ifaceName = "wlan3";

        NetworkConfiguration nc = new NetworkConfiguration();
        WifiInterfaceConfigImpl nic = new WifiInterfaceConfigImpl(ifaceName);
        List<WifiInterfaceAddressConfig> ias = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wiac = new WifiInterfaceAddressConfigImpl();
        wiac.setMode(WifiMode.ADHOC); // don't set to MASTER so as not to test the OS-specific functionality
        ias.add(wiac);
        nic.setNetInterfaceAddresses(ias);
        nc.addNetInterfaceConfig(nic);
        when(ncsMock.getNetworkConfiguration()).thenReturn(nc);

        try {
            svc.getWifiHotspotList(ifaceName);

            fail("Exception was expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
            assertEquals(KuraErrorCode.UNAVAILABLE_DEVICE, ((KuraException) e.getCause()).getCode());
            assertTrue(e.getCause().getMessage().contains("test"));
        }
    }

    @Test
    public void testGetWifiHotspotListEmpty() throws KuraException {
        // tests retrieval of all available hotspots - none

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl() {

            @Override
            protected List<WifiAccessPoint> getWifiAccessPoints(String ifaceName) throws KuraException {
                List<WifiAccessPoint> list = new ArrayList<>();
                return list;
            }
        };

        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        String ifaceName = "wlan3";

        NetworkConfiguration nc = new NetworkConfiguration();
        WifiInterfaceConfigImpl nic = new WifiInterfaceConfigImpl(ifaceName);
        List<WifiInterfaceAddressConfig> ias = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wiac = new WifiInterfaceAddressConfigImpl();
        wiac.setMode(WifiMode.ADHOC); // don't set to MASTER so as not to test the OS-specific functionality
        ias.add(wiac);
        nic.setNetInterfaceAddresses(ias);
        nc.addNetInterfaceConfig(nic);
        when(ncsMock.getNetworkConfiguration()).thenReturn(nc);

        List<WifiHotspotInfo> hotspotList = svc.getWifiHotspotList(ifaceName);

        assertNotNull(hotspotList);
        assertTrue(hotspotList.isEmpty());
    }

    @Test
    public void testGetWifiHotspotList() throws KuraException {
        // tests retrieval of all available hotspots with shown SSIDs

        String ssid = "testSSID";
        byte[] mac = { 10, 11, 12, 13, 14, 15 };

        NetworkAdminServiceImpl svc = new NetworkAdminServiceImpl() {

            @Override
            protected List<WifiAccessPoint> getWifiAccessPoints(String ifaceName) throws KuraException {
                List<WifiAccessPoint> list = new ArrayList<>();
                WifiAccessPointImpl wap = new WifiAccessPointImpl("");
                list.add(wap);
                wap = new WifiAccessPointImpl(ssid);
                wap.setHardwareAddress(mac);
                wap.setWpaSecurity(EnumSet.of(WifiSecurity.PAIR_TKIP));
                list.add(wap);
                return list;
            }
        };

        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        String ifaceName = "wlan3";

        NetworkConfiguration nc = new NetworkConfiguration();
        WifiInterfaceConfigImpl nic = new WifiInterfaceConfigImpl(ifaceName);
        List<WifiInterfaceAddressConfig> ias = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wiac = new WifiInterfaceAddressConfigImpl();
        wiac.setMode(WifiMode.ADHOC); // don't set to MASTER so as not to test the OS-specific functionality
        ias.add(wiac);
        nic.setNetInterfaceAddresses(ias);
        nc.addNetInterfaceConfig(nic);
        when(ncsMock.getNetworkConfiguration()).thenReturn(nc);

        List<WifiHotspotInfo> hotspotList = svc.getWifiHotspotList(ifaceName);

        assertNotNull(hotspotList);
        assertEquals(1, hotspotList.size());

        WifiHotspotInfo whi = hotspotList.get(0);
        assertEquals(WifiSecurity.SECURITY_WPA, whi.getSecurity());
        assertEquals(1, whi.getPairCiphers().size());
        assertEquals(WifiSecurity.PAIR_TKIP, whi.getPairCiphers().iterator().next());
        assertEquals(0, whi.getGroupCiphers().size());
    }

    // TODO: some heavier refactoring of the implementation

}
