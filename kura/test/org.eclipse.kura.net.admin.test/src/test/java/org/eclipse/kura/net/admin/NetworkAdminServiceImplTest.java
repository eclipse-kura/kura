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

    // TODO: anything more depends on heavier refactoring of the implementation

}
