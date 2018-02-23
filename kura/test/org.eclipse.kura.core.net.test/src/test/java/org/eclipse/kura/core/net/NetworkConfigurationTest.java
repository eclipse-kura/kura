/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.modem.ModemPowerMode;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetworkConfigurationTest {

    @Test
    public void testNetworkConfiguration() {
        NetworkConfiguration config = new NetworkConfiguration();

        assertTrue(config.getNetInterfaceConfigs().isEmpty());
    }

    @Test
    public void testNetworkConfigurationEmpty() throws UnknownHostException, KuraException, NoSuchFieldException {
        Map<String, Object> properties = new HashMap<>();

        NetworkConfiguration config = new NetworkConfiguration(properties);

        assertTrue(config.getNetInterfaceConfigs().isEmpty());
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testNetworkConfigurationWithAvailableInterfaces()
            throws UnknownHostException, KuraException, NoSuchFieldException {
        String[] interfaces = new String[] { "if1", "if2" };

        Map<String, Object> properties = new HashMap<>();
        properties.put("net.interfaces", interfaces);
        properties.put("net.interface.if1.type", "ETHERNET");

        NetworkConfiguration config = new NetworkConfiguration(properties);

        assertEquals(1, config.getNetInterfaceConfigs().size());

        List<NetConfig> configs = new ArrayList<>();
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        configs.add(new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, false));

        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();
        addressConfig.setNetConfigs(configs);

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(addressConfig);

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setState(NetInterfaceState.DISCONNECTED);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testNetworkConfigurationWithAvailableInterfacesGWT()
            throws UnknownHostException, KuraException, NoSuchFieldException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("net.interfaces", "if1,if2");
        properties.put("net.interface.if1.type", "ETHERNET");

        NetworkConfiguration config = new NetworkConfiguration(properties);

        assertEquals(1, config.getNetInterfaceConfigs().size());

        List<NetConfig> configs = new ArrayList<>();
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        configs.add(new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, false));

        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();
        addressConfig.setNetConfigs(configs);

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(addressConfig);

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setState(NetInterfaceState.DISCONNECTED);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testNetworkConfigurationWithModifiedInterfaceNames()
            throws UnknownHostException, KuraException, NoSuchFieldException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("modified.interface.names", "if1,if2");

        NetworkConfiguration config = new NetworkConfiguration(properties);

        assertEquals(2, config.getModifiedInterfaceNames().size());
        assertEquals("if1", config.getModifiedInterfaceNames().get(0));
        assertEquals("if2", config.getModifiedInterfaceNames().get(1));
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testModifiedInterfaceNames() throws NoSuchFieldException {
        NetworkConfiguration config = new NetworkConfiguration();

        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));

        List<String> modifiedInterfaceNames = new ArrayList<>();
        modifiedInterfaceNames.add("if1");
        modifiedInterfaceNames.add("if2");
        config.setModifiedInterfaceNames(modifiedInterfaceNames);
        assertEquals(modifiedInterfaceNames, config.getModifiedInterfaceNames());
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));

        modifiedInterfaceNames.clear();
        modifiedInterfaceNames.add("if3");
        config.setModifiedInterfaceNames(modifiedInterfaceNames);
        assertEquals(modifiedInterfaceNames, config.getModifiedInterfaceNames());
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testModifiedInterfaceNamesNull() throws NoSuchFieldException {
        NetworkConfiguration config = new NetworkConfiguration();

        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));

        List<String> modifiedInterfaceNames = null;
        config.setModifiedInterfaceNames(modifiedInterfaceNames);
        assertNull(config.getModifiedInterfaceNames());
        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testModifiedInterfaceNamesEmpty() throws NoSuchFieldException {
        NetworkConfiguration config = new NetworkConfiguration();

        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));

        List<String> modifiedInterfaceNames = new ArrayList<>();
        config.setModifiedInterfaceNames(modifiedInterfaceNames);
        assertNull(config.getModifiedInterfaceNames());
        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testAccept() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();
        NetworkConfigurationVisitor visitor = mock(NetworkConfigurationVisitor.class);

        Mockito.doNothing().when(visitor).visit(any(NetworkConfiguration.class));

        config.accept(visitor);

        verify(visitor, times(1)).visit(config);
    }

    @Test(expected = KuraException.class)
    public void testAcceptWithException() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();
        NetworkConfigurationVisitor visitor = mock(NetworkConfigurationVisitor.class);

        Mockito.doThrow(new KuraException(KuraErrorCode.INTERNAL_ERROR)).when(visitor)
                .visit(any(NetworkConfiguration.class));

        config.accept(visitor);
        fail("exception was expected");
    }

    @Test
    public void testAddNetInterfaceConfig() throws NoSuchFieldException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig);

        assertEquals(1, config.getNetInterfaceConfigs().size());
        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
    }

    @Test
    public void testAddNetConfigLoopbackEmpty() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.LOOPBACK, netConfig);

        assertTrue(config.getNetInterfaceConfigs().isEmpty());
    }

    @Test
    public void testAddNetConfigLoopback() throws KuraException {
        // Prepare network configuration
        NetInterfaceAddressConfigImpl interfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(new ArrayList<NetConfig>());

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);

        LoopbackInterfaceConfigImpl interfaceConfig = new LoopbackInterfaceConfigImpl("if1");
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        NetworkConfiguration config = new NetworkConfiguration();
        config.addNetInterfaceConfig(interfaceConfig);

        // Add net config
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.ETHERNET, netConfig);

        // Check for success
        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(netConfig);

        interfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(netConfigs);

        interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        assertEquals(1, config.getNetInterfaceConfigs().size());
        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
    }

    @Test
    public void testAddNetConfigEthernetEmpty() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.ETHERNET, netConfig);

        assertTrue(config.getNetInterfaceConfigs().isEmpty());
    }

    @Test
    public void testAddNetConfigEthernet() throws KuraException {
        // Prepare network configuration
        NetInterfaceAddressConfigImpl interfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(new ArrayList<NetConfig>());

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        NetworkConfiguration config = new NetworkConfiguration();
        config.addNetInterfaceConfig(interfaceConfig);

        // Add net config
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.ETHERNET, netConfig);

        // Check for success
        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(netConfig);

        interfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(netConfigs);

        interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        assertEquals(1, config.getNetInterfaceConfigs().size());
        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
    }

    @Test
    public void testAddNetConfigWifiEmpty() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.WIFI, netConfig);

        assertTrue(config.getNetInterfaceConfigs().isEmpty());
    }

    @Test
    public void testAddNetConfigWifi() throws KuraException {
        // Prepare network configuration
        WifiInterfaceAddressConfigImpl interfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(new ArrayList<NetConfig>());

        List<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);

        WifiInterfaceConfigImpl interfaceConfig = new WifiInterfaceConfigImpl("if1");
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        NetworkConfiguration config = new NetworkConfiguration();
        config.addNetInterfaceConfig(interfaceConfig);

        // Add net config
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.WIFI, netConfig);

        // Check for success
        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(netConfig);

        interfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(netConfigs);

        interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        assertEquals(1, config.getNetInterfaceConfigs().size());
        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
    }

    @Test
    public void testAddNetConfigModemEmpty() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.MODEM, netConfig);

        assertTrue(config.getNetInterfaceConfigs().isEmpty());
    }

    @Test
    public void testAddNetConfigModem() throws KuraException {
        // Prepare network configuration
        ModemInterfaceAddressConfigImpl interfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(new ArrayList<NetConfig>());

        List<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);

        ModemInterfaceConfigImpl interfaceConfig = new ModemInterfaceConfigImpl("if1");
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        NetworkConfiguration config = new NetworkConfiguration();
        config.addNetInterfaceConfig(interfaceConfig);

        // Add net config
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.MODEM, netConfig);

        // Check for success
        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(netConfig);

        interfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        interfaceAddressConfig.setNetConfigs(netConfigs);

        interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(interfaceAddressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        assertEquals(1, config.getNetInterfaceConfigs().size());
        assertEquals(interfaceConfig, config.getNetInterfaceConfigs().get(0));
    }

    @Test(expected = KuraException.class)
    public void testAddNetConfigInvalidType() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);

        config.addNetConfig("if1", NetInterfaceType.ADSL, netConfig);
        fail("NullPointerException was expected");
    }

    @Test
    public void testToStringEmpty() {
        NetworkConfiguration config = new NetworkConfiguration();

        String expected = "";
        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringNull() {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();
        addressConfig.setNetConfigs(null);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);
        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null"
                + " :: Type: ETHERNET :: Usb Device: null :: Prefix: 0";
        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringEthernet1() {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setDriver("driver");
        interfaceConfig.setDriverVersion("driverVersion");
        interfaceConfig.setFirmwareVersion("firmwareVersion");
        interfaceConfig.setState(NetInterfaceState.ACTIVATED);
        interfaceConfig.setUsbDevice(new UsbBlockDevice("vendorId", "productId", "manufacturerName", "productName",
                "usbBusNumber", "usbDevicePath", "deviceNode"));

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: driver :: Driver Version: driverVersion :: Firmware Version: firmwareVersion :: MTU: 0"
                + " :: State: ACTIVATED :: Type: ETHERNET :: Usb Device: UsbBlockDevice [getDeviceNode()=deviceNode,"
                + " getVendorId()=vendorId, getProductId()=productId, getManufacturerName()=manufacturerName,"
                + " getProductName()=productName, getUsbBusNumber()=usbBusNumber, getUsbDevicePath()=usbDevicePath,"
                + " getUsbPort()=usbBusNumber-usbDevicePath] :: Prefix: 0\n	IPv4  :: is not configured for STATIC or DHCP";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringEthernet2() throws KuraException, UnknownHostException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");

        interfaceConfig.setHardwareAddress(NetworkUtil.macToBytes("11:22:33:44:55:66"));

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();
        addressConfig.setAddress(IP4Address.parseHostAddress("10.0.0.1"));
        addressConfig.setNetmask(IP4Address.parseHostAddress("255.255.255.0"));
        addressConfig.setBroadcast(IP4Address.parseHostAddress("10.0.0.255"));

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig1 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false);
        netConfig1.setDhcp(true);
        netConfigs.add(netConfig1);

        NetConfigIP4 netConfig2 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false);
        netConfig1.setDhcp(true);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        netConfig1.setProperties(properties);
        netConfigs.add(netConfig2);

        NetConfigIP4 netConfig3 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false);
        netConfig3.setAddress(null);
        netConfigs.add(netConfig3);

        NetConfigIP4 netConfig4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false);
        netConfig4.setAddress((IP4Address) IP4Address.parseHostAddress("10.0.0.2"));
        netConfig4.setNetworkPrefixLength((short) 24);
        netConfig4.setDnsServers(null);
        netConfig4.setWinsServers(null);
        netConfig4.setDomains(null);
        netConfigs.add(netConfig4);

        NetConfigIP4 netConfig5 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false);
        netConfig5.setAddress((IP4Address) IP4Address.parseHostAddress("10.0.0.2"));
        netConfig5.setNetworkPrefixLength((short) 24);
        netConfig5.setGateway((IP4Address) IP4Address.parseHostAddress("10.0.0.1"));

        List<IP4Address> dnsServers = new ArrayList<>();
        dnsServers.add((IP4Address) IP4Address.parseHostAddress("10.0.1.1"));
        netConfig5.setDnsServers(dnsServers);

        List<IP4Address> winsServers = new ArrayList<>();
        winsServers.add((IP4Address) IP4Address.parseHostAddress("10.0.1.1"));
        netConfig5.setWinsServers(winsServers);

        List<String> domains = new ArrayList<>();
        domains.add("example.com");
        netConfig5.setDomains(domains);

        netConfigs.add(netConfig5);

        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: "
                + "Hardware Address: 11:22:33:44:55:66 :: State: null :: Type: ETHERNET :: Usb Device: null"
                + " :: Address: 10.0.0.1 :: Prefix: 0 :: Netmask: 255.255.255.0 :: Broadcast: 10.0.0.255\n	IPv4 "
                + " :: is DHCP client :: key: value\n	IPv4  :: is not configured for STATIC or DHCP\n	IPv4 "
                + " :: is not configured for STATIC or DHCP\n	IPv4  :: is STATIC client :: Address: 10.0.0.2 :: Prefix: 24"
                + "\n	IPv4  :: is STATIC client :: Address: 10.0.0.2 :: Prefix: 24 :: Gateway: 10.0.0.1 :: DNS : 10.0.1.1"
                + " :: WINS Server : 10.0.1.1 :: Domains : example.com";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringEthernet3() throws KuraException, UnknownHostException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP6 netConfig1 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, false);
        netConfig1.setDhcp(true);
        netConfigs.add(netConfig1);

        NetConfigIP6 netConfig2 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, false);
        netConfig1.setDhcp(true);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        netConfig1.setProperties(properties);
        netConfigs.add(netConfig2);

        NetConfigIP6 netConfig3 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, false);
        netConfig3.setAddress(null);
        netConfigs.add(netConfig3);

        NetConfigIP6 netConfig4 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, false);
        netConfig4.setAddress((IP6Address) IP6Address.parseHostAddress("0:0:0:0:0:0:0:1"));

        List<IP6Address> dnsServers = new ArrayList<>();
        dnsServers.add((IP6Address) IP6Address.parseHostAddress("0:0:0:0:0:0:0:1"));
        netConfig4.setDnsServers(dnsServers);

        List<String> domains = new ArrayList<>();
        domains.add("example.com");
        netConfig4.setDomains(domains);

        netConfigs.add(netConfig4);

        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null"
                + " :: Type: ETHERNET :: Usb Device: null :: Prefix: 0\n	IPv6  :: is DHCP client :: key: value\n	IPv6 "
                + " :: is STATIC client\n	IPv6  :: is STATIC client\n	IPv6  :: is STATIC client :: Address: 0:0:0:0:0:0:0:1"
                + " :: DNS : 0:0:0:0:0:0:0:1 :: Domains : example.com";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringWifi1() {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl interfaceConfig = new WifiInterfaceConfigImpl("if1");

        List<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        WifiInterfaceAddressConfigImpl addressConfig = new WifiInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setChannels(null);
        netConfigs.add(wifiConfig);
        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null"
                + " :: Type: WIFI :: Usb Device: null :: Prefix: 0\n	WifiConfig  :: SSID: null :: BgScan: null"
                + " :: Broadcast: false :: Group Ciphers: null :: Hardware Mode: null :: Mode: null"
                + " :: Pairwise Ciphers: null :: Passkey: null :: Security: null";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringWifi2() {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl interfaceConfig = new WifiInterfaceConfigImpl("if1");

        List<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        WifiInterfaceAddressConfigImpl addressConfig = new WifiInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setChannels(new int[0]);
        netConfigs.add(wifiConfig);
        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null :: Type: WIFI"
                + " :: Usb Device: null :: Prefix: 0\n	WifiConfig  :: SSID: null :: BgScan: null :: Broadcast: false"
                + " :: Group Ciphers: null :: Hardware Mode: null :: Mode: null :: Pairwise Ciphers: null :: Passkey: null"
                + " :: Security: null";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringWifi3() {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl interfaceConfig = new WifiInterfaceConfigImpl("if1");

        List<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        WifiInterfaceAddressConfigImpl addressConfig = new WifiInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setChannels(new int[] { 1, 2 });
        netConfigs.add(wifiConfig);
        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null :: Type: WIFI"
                + " :: Usb Device: null :: Prefix: 0\n	WifiConfig  :: SSID: null :: BgScan: null :: Broadcast: false"
                + " :: Channels: 1,2 :: Group Ciphers: null :: Hardware Mode: null :: Mode: null :: Pairwise Ciphers: null"
                + " :: Passkey: null :: Security: null";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringModem() throws UnknownHostException {
        NetworkConfiguration config = new NetworkConfiguration();

        ModemInterfaceConfigImpl interfaceConfig = new ModemInterfaceConfigImpl("if1");

        List<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        ModemInterfaceAddressConfigImpl addressConfig = new ModemInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        ModemConfig modemConfig = new ModemConfig();
        modemConfig.setApn("apn");
        modemConfig.setDataCompression(0);
        modemConfig.setDialString("dialString");
        modemConfig.setHeaderCompression(0);
        modemConfig.setPassword("password");
        modemConfig.setPppNumber(0);
        modemConfig.setProfileID(0);
        modemConfig.setUsername("username");
        modemConfig.setAuthType(AuthType.AUTO);
        modemConfig.setIpAddress((IP4Address) IP4Address.parseHostAddress("10.0.0.2"));
        modemConfig.setPdpType(PdpType.PPP);
        netConfigs.add(modemConfig);
        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null"
                + " :: Type: MODEM :: Usb Device: null :: Prefix: 0\n	ModemConfig  :: APN: apn :: Data Compression: 0"
                + " :: Dial String: dialString :: Header Compression: 0 :: Password: password :: PPP number: 0"
                + " :: Profile ID: 0 :: Username: username :: Auth Type: AUTO :: IP Address: 10.0.0.2 :: PDP Type: PPP";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToStringOther() {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = new ArrayList<>();

        try {
            DhcpServerCfg dhcpServerCfg = new DhcpServerCfg("eth0", true, 7200, 7200, false);
            DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(
                    (IP4Address) IPAddress.parseHostAddress("192.168.1.0"),
                    (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24,
                    (IP4Address) IPAddress.parseHostAddress("192.168.1.1"),
                    (IP4Address) IPAddress.parseHostAddress("192.168.1.100"),
                    (IP4Address) IPAddress.parseHostAddress("192.168.1.254"), null);

            netConfigs.add(new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4));
        } catch (Exception e) {
            fail("failed: " + e);
        }
        netConfigs.add(new FirewallAutoNatConfig());
        netConfigs.add(null);
        netConfigs.add(new MockConfig());

        addressConfig.setNetConfigs(netConfigs);

        interfaceAddresses.add(addressConfig);
        interfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        config.addNetInterfaceConfig(interfaceConfig);

        String expected = "\nname: if1 :: Loopback? false :: Point to Point? false :: Up? false :: Virtual? false"
                + " :: Driver: null :: Driver Version: null :: Firmware Version: null :: MTU: 0 :: State: null"
                + " :: Type: ETHERNET :: Usb Device: null :: Prefix: 0\n	DhcpServerConfig \n	FirewallAutoNatConfig "
                + "\n	NULL NETCONFIG PRESENT?!?\n	UNKNOWN CONFIG TYPE???: org.eclipse.kura.core.net.MockConfig";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testGetModifiedNetInterfaceConfigsNull() {
        // Prepare configuration
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig);

        // Get modified net interface configs (all net interface configs are expected)
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> modifiedNetInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        assertEquals(1, modifiedNetInterfaceConfigs.size());
        assertEquals(interfaceConfig, modifiedNetInterfaceConfigs.get(0));
    }

    @Test
    public void testGetModifiedNetInterfaceConfigsEmpty() throws UnknownHostException, KuraException {
        // Prepare configuration
        NetworkConfiguration config = new NetworkConfiguration(new HashMap<String, Object>());

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig);

        // Get modified net interface configs (all net interface configs are expected)
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> modifiedNetInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        assertEquals(1, modifiedNetInterfaceConfigs.size());
        assertEquals(interfaceConfig, modifiedNetInterfaceConfigs.get(0));
    }

    @Test
    public void testGetModifiedNetInterfaceConfigsNonEmpty1() throws UnknownHostException, KuraException {
        // Prepare configuration
        NetworkConfiguration config = new NetworkConfiguration(new HashMap<String, Object>());

        EthernetInterfaceConfigImpl interfaceConfig1 = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig1);

        List<String> modifiedInterfaceNames = new ArrayList<>();
        modifiedInterfaceNames.add("if2");
        config.setModifiedInterfaceNames(modifiedInterfaceNames);

        // Get modified net interface configs (all net interface configs are expected)
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> modifiedNetInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        assertEquals(0, modifiedNetInterfaceConfigs.size());
    }

    @Test
    public void testGetModifiedNetInterfaceConfigsNonEmpty2() throws UnknownHostException, KuraException {
        // Prepare configuration
        NetworkConfiguration config = new NetworkConfiguration(new HashMap<String, Object>());

        EthernetInterfaceConfigImpl interfaceConfig1 = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig1);

        EthernetInterfaceConfigImpl interfaceConfig2 = new EthernetInterfaceConfigImpl("if2");
        config.addNetInterfaceConfig(interfaceConfig2);

        List<String> modifiedInterfaceNames = new ArrayList<>();
        modifiedInterfaceNames.add("if2");
        config.setModifiedInterfaceNames(modifiedInterfaceNames);

        // Get modified net interface configs (all net interface configs are expected)
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> modifiedNetInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        assertEquals(1, modifiedNetInterfaceConfigs.size());
        assertEquals(interfaceConfig2, modifiedNetInterfaceConfigs.get(0));
    }

    @Test
    public void testGetNetInterfaceConfig() {
        NetworkConfiguration config = new NetworkConfiguration();

        assertNull(config.getNetInterfaceConfig("if1"));

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig);

        assertNull(config.getNetInterfaceConfig("if2"));
        assertEquals(interfaceConfig, config.getNetInterfaceConfig("if1"));
    }

    @Test
    public void testGetConfigurationPropertiesNull() throws NoSuchFieldException {
        NetworkConfiguration config = new NetworkConfiguration();

        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));
        assertNull(config.getConfigurationProperties());
    }

    @Test
    public void testGetConfigurationProperties() throws NoSuchFieldException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig);

        assertEquals(true, TestUtil.getFieldValue(config, "recomputeProperties"));
        Map<String, Object> properties = config.getConfigurationProperties();
        assertEquals(false, TestUtil.getFieldValue(config, "recomputeProperties"));

        Map<String, Object> expected = new HashMap<>();
        expected.put("net.interfaces", "if1");
        expected.put("net.interface.if1.mac", "N/A");
        expected.put("net.interface.if1.loopback", false);
        expected.put("net.interface.if1.ptp", false);
        expected.put("net.interface.if1.up", false);
        expected.put("net.interface.if1.virtual", false);
        expected.put("net.interface.if1.type", "ETHERNET");
        expected.put("net.interface.if1.driver", null);
        expected.put("net.interface.if1.driver.version", null);
        expected.put("net.interface.if1.firmware.version", null);
        expected.put("net.interface.if1.config.name", "if1");
        expected.put("net.interface.if1.config.autoconnect", false);
        expected.put("net.interface.if1.config.mtu", 0);
        expected.put("net.interface.if1.eth.link.up", false);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testIsValidEmpty() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidNonEmpty() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(interfaceConfig);

        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidValidMTU() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setMTU(0);
        config.addNetInterfaceConfig(interfaceConfig);

        interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setMTU(1);
        config.addNetInterfaceConfig(interfaceConfig);

        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidInvalidMTU() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl interfaceConfig = new EthernetInterfaceConfigImpl("if1");
        interfaceConfig.setMTU(-1);
        config.addNetInterfaceConfig(interfaceConfig);

        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidInvalidType() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        MockInterfaceConfigImpl icMock = new MockInterfaceConfigImpl("ifMock");
        icMock.setType(NetInterfaceType.ADSL);
        config.addNetInterfaceConfig(icMock);

        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidValidType() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl icEthernet = new EthernetInterfaceConfigImpl("if1");
        config.addNetInterfaceConfig(icEthernet);

        WifiInterfaceConfigImpl icWifi = new WifiInterfaceConfigImpl("if2");
        config.addNetInterfaceConfig(icWifi);

        ModemInterfaceConfigImpl icModem = new ModemInterfaceConfigImpl("if3");
        config.addNetInterfaceConfig(icModem);

        LoopbackInterfaceConfigImpl icLoopback = new LoopbackInterfaceConfigImpl("if4");
        config.addNetInterfaceConfig(icLoopback);

        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidValidConfig() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        NetInterfaceAddressConfigImpl addressConfig1 = new NetInterfaceAddressConfigImpl();
        addressConfig1.setNetConfigs(null);

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false);
        netConfig.setDhcp(true);

        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(netConfig);

        NetInterfaceAddressConfigImpl addressConfig2 = new NetInterfaceAddressConfigImpl();
        addressConfig2.setNetConfigs(netConfigs);

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(addressConfig1);
        interfaceAddresses.add(addressConfig2);

        EthernetInterfaceConfigImpl icEthernet = new EthernetInterfaceConfigImpl("if1");
        icEthernet.setNetInterfaceAddresses(interfaceAddresses);
        config.addNetInterfaceConfig(icEthernet);

        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidInvalidConfig() throws KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));

        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();
        addressConfig.setNetConfigs(netConfigs);

        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(addressConfig);

        EthernetInterfaceConfigImpl icEthernet = new EthernetInterfaceConfigImpl("if1");
        icEthernet.setNetInterfaceAddresses(interfaceAddresses);
        config.addNetInterfaceConfig(icEthernet);

        assertFalse(config.isValid());
    }

    // ---------------------------------------------------------------
    //
    // Private Methods
    //
    // ---------------------------------------------------------------

    @Test
    public void testRecomputeNetworkPropertiesEmpty() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("net.interfaces", "");

        TestUtil.invokePrivate(config, "recomputeNetworkProperties");

        Map<String, Object> properties = (Map<String, Object>) TestUtil.getFieldValue(config, "properties");
        assertMapEquals(expected, properties);
    }

    @Test
    public void testRecomputeNetworkPropertiesWithModifiedInterfaces() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        ArrayList<String> modifiedInterfaceNames = new ArrayList<>();
        modifiedInterfaceNames.add("if1");
        modifiedInterfaceNames.add("if2");
        config.setModifiedInterfaceNames(modifiedInterfaceNames);

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("modified.interface.names", "if1,if2");
        expected.put("net.interfaces", "");

        TestUtil.invokePrivate(config, "recomputeNetworkProperties");

        Map<String, Object> properties = (Map<String, Object>) TestUtil.getFieldValue(config, "properties");
        assertMapEquals(expected, properties);
    }

    @Test
    public void testRecomputeNetworkPropertiesWithEthernetInterfaces() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl netInterfaceConfig1 = new EthernetInterfaceConfigImpl("if1");
        netInterfaceConfig1.setState(NetInterfaceState.ACTIVATED);
        netInterfaceConfig1.setUsbDevice(new UsbBlockDevice("vendorId", "productId", "vendorName", "productName",
                "usbBusNumber", "usbDevicePath", "deviceNode"));
        config.addNetInterfaceConfig(netInterfaceConfig1);

        EthernetInterfaceConfigImpl netInterfaceConfig2 = new EthernetInterfaceConfigImpl("if2");
        ArrayList<NetInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        interfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
        NetInterfaceAddressConfigImpl interfaceAddressConfig = new NetInterfaceAddressConfigImpl();
        interfaceAddressConfig.setAddress(IPAddress.parseHostAddress("10.0.0.10"));
        interfaceAddressConfig.setBroadcast(IPAddress.parseHostAddress("10.0.0.255"));
        interfaceAddressConfig.setGateway(IPAddress.parseHostAddress("10.0.0.1"));
        interfaceAddressConfig.setNetmask(IPAddress.parseHostAddress("255.255.255.0"));
        interfaceAddressConfig.setNetworkPrefixLength((short) 24);
        ArrayList<IPAddress> dnsServers = new ArrayList<>();
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.1"));
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.2"));
        interfaceAddressConfig.setDnsServers(dnsServers);

        ArrayList<NetConfig> netConfigs = new ArrayList<>();

        NetConfigIP4 netConfig1 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfig1.setDhcp(true);
        netConfig1.setDnsServers(new ArrayList<IP4Address>());
        netConfigs.add(netConfig1);

        NetConfigIP6 netConfig2 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true);
        netConfig2.setDhcp(true);
        netConfigs.add(netConfig2);

        DhcpServerConfigIP4 netConfig3 = new DhcpServerConfigIP4("if1", false,
                (IP4Address) IP4Address.parseHostAddress("10.0.0.0"),
                (IP4Address) IP4Address.parseHostAddress("10.0.0.1"),
                (IP4Address) IP4Address.parseHostAddress("255.255.255.0"), 1, 2, (short) 24,
                (IP4Address) IP4Address.parseHostAddress("10.0.0.10"),
                (IP4Address) IP4Address.parseHostAddress("10.0.0.15"), true, null);
        netConfigs.add(netConfig3);

        netConfigs.add(new FirewallAutoNatConfig());

        interfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(interfaceAddressConfig);
        netInterfaceConfig2.setNetInterfaceAddresses(interfaceAddressConfigs);
        config.addNetInterfaceConfig(netInterfaceConfig2);

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("net.interface.if1.type", "ETHERNET");
        expected.put("net.interface.if1.config.name", "if1");
        expected.put("net.interface.if1.config.state", "ACTIVATED");
        expected.put("net.interface.if1.config.autoconnect", netInterfaceConfig1.isAutoConnect());
        expected.put("net.interface.if1.config.mtu", netInterfaceConfig1.getMTU());
        expected.put("net.interface.if1.driver", netInterfaceConfig1.getDriver());
        expected.put("net.interface.if1.driver.version", netInterfaceConfig1.getDriverVersion());
        expected.put("net.interface.if1.firmware.version", netInterfaceConfig1.getFirmwareVersion());
        expected.put("net.interface.if1.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig1.getHardwareAddress()));
        expected.put("net.interface.if1.loopback", netInterfaceConfig1.isLoopback());
        expected.put("net.interface.if1.ptp", netInterfaceConfig1.isPointToPoint());
        expected.put("net.interface.if1.up", netInterfaceConfig1.isUp());
        expected.put("net.interface.if1.virtual", netInterfaceConfig1.isVirtual());
        expected.put("net.interface.if1.usb.vendor.id", "vendorId");
        expected.put("net.interface.if1.usb.vendor.name", "vendorName");
        expected.put("net.interface.if1.usb.product.id", "productId");
        expected.put("net.interface.if1.usb.product.name", "productName");
        expected.put("net.interface.if1.usb.busNumber", "usbBusNumber");
        expected.put("net.interface.if1.usb.devicePath", "usbDevicePath");
        expected.put("net.interface.if1.eth.link.up", netInterfaceConfig1.isLinkUp());

        expected.put("net.interface.if2.type", "ETHERNET");
        expected.put("net.interface.if2.config.name", "if2");
        expected.put("net.interface.if2.config.autoconnect", netInterfaceConfig2.isAutoConnect());
        expected.put("net.interface.if2.config.mtu", netInterfaceConfig2.getMTU());
        expected.put("net.interface.if2.driver", netInterfaceConfig2.getDriver());
        expected.put("net.interface.if2.driver.version", netInterfaceConfig2.getDriverVersion());
        expected.put("net.interface.if2.firmware.version", netInterfaceConfig2.getFirmwareVersion());
        expected.put("net.interface.if2.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig2.getHardwareAddress()));
        expected.put("net.interface.if2.loopback", netInterfaceConfig2.isLoopback());
        expected.put("net.interface.if2.ptp", netInterfaceConfig2.isPointToPoint());
        expected.put("net.interface.if2.up", netInterfaceConfig2.isUp());
        expected.put("net.interface.if2.virtual", netInterfaceConfig2.isVirtual());
        expected.put("net.interface.if2.eth.link.up", netInterfaceConfig2.isLinkUp());
        expected.put("net.interface.if2.ip4.address", "10.0.0.10");
        expected.put("net.interface.if2.ip4.broadcast", "10.0.0.255");
        expected.put("net.interface.if2.ip4.gateway", "10.0.0.1");
        expected.put("net.interface.if2.ip4.netmask", "255.255.255.0");
        expected.put("net.interface.if2.ip4.prefix", (short) 24);
        expected.put("net.interface.if2.ip4.dnsServers", "10.0.1.1,10.0.1.2");
        expected.put("net.interface.if2.config.autoconnect", true);
        expected.put("net.interface.if2.config.ip4.status", "netIPv4StatusEnabledLAN");
        expected.put("net.interface.if2.config.ip4.dnsServers", "");
        expected.put("net.interface.if2.config.dhcpClient4.enabled", true);
        expected.put("net.interface.if2.config.ip6.status", "netIPv6StatusEnabledLAN");
        expected.put("net.interface.if2.config.dhcpClient6.enabled", true);
        expected.put("net.interface.if2.config.dhcpServer4.enabled", false);
        expected.put("net.interface.if2.config.dhcpServer4.defaultLeaseTime", 1);
        expected.put("net.interface.if2.config.dhcpServer4.maxLeaseTime", 2);
        expected.put("net.interface.if2.config.dhcpServer4.prefix", (short) 24);
        expected.put("net.interface.if2.config.dhcpServer4.rangeStart", "10.0.0.10");
        expected.put("net.interface.if2.config.dhcpServer4.rangeEnd", "10.0.0.15");
        expected.put("net.interface.if2.config.dhcpServer4.passDns", true);
        expected.put("net.interface.if2.config.nat.enabled", true);

        expected.put("net.interfaces", "if2,if1");

        TestUtil.invokePrivate(config, "recomputeNetworkProperties");

        Map<String, Object> properties = (Map<String, Object>) TestUtil.getFieldValue(config, "properties");
        assertMapEquals(expected, properties);
    }

    @Test
    public void testRecomputeNetworkPropertiesWithWifiInterfaces() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig1 = new WifiInterfaceConfigImpl("if1");
        netInterfaceConfig1.setCapabilities(null);
        config.addNetInterfaceConfig(netInterfaceConfig1);

        WifiInterfaceConfigImpl netInterfaceConfig2 = new WifiInterfaceConfigImpl("if2");
        netInterfaceConfig2.setCapabilities(EnumSet.noneOf(Capability.class));
        config.addNetInterfaceConfig(netInterfaceConfig2);

        WifiInterfaceConfigImpl netInterfaceConfig3 = new WifiInterfaceConfigImpl("if3");
        netInterfaceConfig3.setCapabilities(EnumSet.of(Capability.CIPHER_CCMP, Capability.CIPHER_TKIP));
        config.addNetInterfaceConfig(netInterfaceConfig3);

        ArrayList<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        interfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
        WifiInterfaceAddressConfigImpl interfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        interfaceAddressConfig.setBitrate(42);
        interfaceAddressConfig.setMode(WifiMode.ADHOC);

        ArrayList<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig netConfig = new WifiConfig();
        netConfig.setMode(WifiMode.ADHOC);
        netConfig.setSSID("ssid");
        netConfig.setDriver("driver");
        netConfig.setBroadcast(false);
        netConfig.setPingAccessPoint(false);
        netConfig.setIgnoreSSID(false);
        netConfigs.add(netConfig);
        interfaceAddressConfig.setNetConfigs(netConfigs);

        interfaceAddressConfigs.add(interfaceAddressConfig);
        netInterfaceConfig3.setNetInterfaceAddresses(interfaceAddressConfigs);

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("net.interface.if1.type", "WIFI");
        expected.put("net.interface.if1.config.name", "if1");
        expected.put("net.interface.if1.config.autoconnect", netInterfaceConfig1.isAutoConnect());
        expected.put("net.interface.if1.config.mtu", netInterfaceConfig1.getMTU());
        expected.put("net.interface.if1.driver", netInterfaceConfig1.getDriver());
        expected.put("net.interface.if1.driver.version", netInterfaceConfig1.getDriverVersion());
        expected.put("net.interface.if1.firmware.version", netInterfaceConfig1.getFirmwareVersion());
        expected.put("net.interface.if1.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig1.getHardwareAddress()));
        expected.put("net.interface.if1.loopback", netInterfaceConfig1.isLoopback());
        expected.put("net.interface.if1.ptp", netInterfaceConfig1.isPointToPoint());
        expected.put("net.interface.if1.up", netInterfaceConfig1.isUp());
        expected.put("net.interface.if1.virtual", netInterfaceConfig1.isVirtual());

        expected.put("net.interface.if2.type", "WIFI");
        expected.put("net.interface.if2.config.name", "if2");
        expected.put("net.interface.if2.config.autoconnect", netInterfaceConfig2.isAutoConnect());
        expected.put("net.interface.if2.config.mtu", netInterfaceConfig2.getMTU());
        expected.put("net.interface.if2.driver", netInterfaceConfig2.getDriver());
        expected.put("net.interface.if2.driver.version", netInterfaceConfig2.getDriverVersion());
        expected.put("net.interface.if2.firmware.version", netInterfaceConfig2.getFirmwareVersion());
        expected.put("net.interface.if2.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig2.getHardwareAddress()));
        expected.put("net.interface.if2.loopback", netInterfaceConfig2.isLoopback());
        expected.put("net.interface.if2.ptp", netInterfaceConfig2.isPointToPoint());
        expected.put("net.interface.if2.up", netInterfaceConfig2.isUp());
        expected.put("net.interface.if2.virtual", netInterfaceConfig2.isVirtual());
        expected.put("net.interface.if2.wifi.capabilities", "");

        expected.put("net.interface.if3.type", "WIFI");
        expected.put("net.interface.if3.config.name", "if3");
        expected.put("net.interface.if3.config.autoconnect", netInterfaceConfig3.isAutoConnect());
        expected.put("net.interface.if3.config.mtu", netInterfaceConfig3.getMTU());
        expected.put("net.interface.if3.driver", netInterfaceConfig3.getDriver());
        expected.put("net.interface.if3.driver.version", netInterfaceConfig3.getDriverVersion());
        expected.put("net.interface.if3.firmware.version", netInterfaceConfig3.getFirmwareVersion());
        expected.put("net.interface.if3.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig3.getHardwareAddress()));
        expected.put("net.interface.if3.loopback", netInterfaceConfig3.isLoopback());
        expected.put("net.interface.if3.ptp", netInterfaceConfig3.isPointToPoint());
        expected.put("net.interface.if3.up", netInterfaceConfig3.isUp());
        expected.put("net.interface.if3.virtual", netInterfaceConfig3.isVirtual());
        expected.put("net.interface.if3.wifi.capabilities", "CIPHER_TKIP CIPHER_CCMP ");
        expected.put("net.interface.if3.wifi.bitrate", (long) 42);
        expected.put("net.interface.if3.wifi.mode", null);
        expected.put("net.interface.if3.config.wifi.adhoc.ssid", "ssid");
        expected.put("net.interface.if3.config.wifi.adhoc.driver", "driver");
        expected.put("net.interface.if3.config.wifi.adhoc.mode", "ADHOC");
        expected.put("net.interface.if3.config.wifi.adhoc.securityType", "NONE");
        expected.put("net.interface.if3.config.wifi.adhoc.channel", "");
        expected.put("net.interface.if3.config.wifi.adhoc.passphrase", new Password(""));
        expected.put("net.interface.if3.config.wifi.adhoc.hardwareMode", "");
        expected.put("net.interface.if3.config.wifi.adhoc.broadcast", false);
        expected.put("net.interface.if3.config.wifi.adhoc.bgscan", "");
        expected.put("net.interface.if3.config.wifi.adhoc.pingAccessPoint", false);
        expected.put("net.interface.if3.config.wifi.adhoc.ignoreSSID", false);

        expected.put("net.interfaces", "if2,if1,if3");

        TestUtil.invokePrivate(config, "recomputeNetworkProperties");

        Map<String, Object> properties = (Map<String, Object>) TestUtil.getFieldValue(config, "properties");
        assertMapEquals(expected, properties);
    }

    @Test
    public void testRecomputeNetworkPropertiesWithModemInterfaces() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        ModemInterfaceConfigImpl netInterfaceConfig1 = new ModemInterfaceConfigImpl("if1");
        netInterfaceConfig1.setRevisionId(null);
        netInterfaceConfig1.setTechnologyTypes(null);
        netInterfaceConfig1.setPowerMode(null);
        config.addNetInterfaceConfig(netInterfaceConfig1);

        ModemInterfaceConfigImpl netInterfaceConfig2 = new ModemInterfaceConfigImpl("if2");
        netInterfaceConfig2.setRevisionId(new String[] { "rev1", "rev2" });

        ArrayList<ModemTechnologyType> technologyTypes = new ArrayList<>();
        technologyTypes.add(ModemTechnologyType.CDMA);
        technologyTypes.add(ModemTechnologyType.EVDO);
        netInterfaceConfig2.setTechnologyTypes(technologyTypes);

        ArrayList<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        ModemInterfaceAddressConfigImpl interfaceAddressConfig1 = new ModemInterfaceAddressConfigImpl();
        interfaceAddressConfig1.setConnectionType(null);
        interfaceAddressConfig1.setConnectionStatus(null);
        interfaceAddresses.add(interfaceAddressConfig1);
        ModemInterfaceAddressConfigImpl interfaceAddressConfig2 = new ModemInterfaceAddressConfigImpl();
        interfaceAddressConfig2.setConnectionType(ModemConnectionType.PPP);
        interfaceAddressConfig2.setConnectionStatus(ModemConnectionStatus.CONNECTED);
        ArrayList<NetConfig> netConfigs = new ArrayList<>();
        ModemConfig netConfig = new ModemConfig();
        netConfig.setApn("apn");
        netConfig.setAuthType(null);
        netConfig.setDataCompression(42);
        netConfig.setDialString("dialString");
        netConfig.setHeaderCompression(100);
        netConfig.setIpAddress(null);
        netConfig.setPassword("password");
        netConfig.setPdpType(null);
        netConfig.setPppNumber(123);
        netConfig.setPersist(true);
        netConfig.setMaxFail(10);
        netConfig.setIdle(20);
        netConfig.setActiveFilter("activeFilter");
        netConfig.setResetTimeout(30);
        netConfig.setLcpEchoInterval(40);
        netConfig.setLcpEchoFailure(50);
        netConfig.setProfileID(60);
        netConfig.setUsername("username");
        netConfig.setEnabled(true);
        netConfig.setGpsEnabled(true);
        netConfigs.add(netConfig);
        netConfigs.add(new MockConfig());
        interfaceAddressConfig2.setNetConfigs(netConfigs);
        interfaceAddresses.add(interfaceAddressConfig2);
        netInterfaceConfig2.setNetInterfaceAddresses(interfaceAddresses);

        netInterfaceConfig2.setPowerMode(ModemPowerMode.LOW_POWER);
        config.addNetInterfaceConfig(netInterfaceConfig2);

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("net.interface.if1.type", "MODEM");
        expected.put("net.interface.if1.config.name", "if1");
        expected.put("net.interface.if1.config.autoconnect", netInterfaceConfig1.isAutoConnect());
        expected.put("net.interface.if1.config.mtu", netInterfaceConfig1.getMTU());
        expected.put("net.interface.if1.driver", netInterfaceConfig1.getDriver());
        expected.put("net.interface.if1.driver.version", netInterfaceConfig1.getDriverVersion());
        expected.put("net.interface.if1.firmware.version", netInterfaceConfig1.getFirmwareVersion());
        expected.put("net.interface.if1.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig1.getHardwareAddress()));
        expected.put("net.interface.if1.loopback", netInterfaceConfig1.isLoopback());
        expected.put("net.interface.if1.ptp", netInterfaceConfig1.isPointToPoint());
        expected.put("net.interface.if1.up", netInterfaceConfig1.isUp());
        expected.put("net.interface.if1.virtual", netInterfaceConfig1.isVirtual());
        expected.put("net.interface.if1.manufacturer", netInterfaceConfig1.getManufacturer());
        expected.put("net.interface.if1.model", netInterfaceConfig1.getModel());
        expected.put("net.interface.if1.revisionId", "");
        expected.put("net.interface.if1.serialNum", netInterfaceConfig1.getSerialNumber());
        expected.put("net.interface.if1.technologyTypes", "");
        expected.put("net.interface.if1.config.identifier", netInterfaceConfig1.getModemIdentifier());
        expected.put("net.interface.if1.config.powerMode", "UNKNOWN");
        expected.put("net.interface.if1.config.pppNum", netInterfaceConfig1.getPppNum());
        expected.put("net.interface.if1.config.poweredOn", netInterfaceConfig1.isPoweredOn());

        expected.put("net.interface.if2.type", "MODEM");
        expected.put("net.interface.if2.config.name", "if2");
        expected.put("net.interface.if2.config.autoconnect", netInterfaceConfig2.isAutoConnect());
        expected.put("net.interface.if2.config.mtu", netInterfaceConfig2.getMTU());
        expected.put("net.interface.if2.driver", netInterfaceConfig2.getDriver());
        expected.put("net.interface.if2.driver.version", netInterfaceConfig2.getDriverVersion());
        expected.put("net.interface.if2.firmware.version", netInterfaceConfig2.getFirmwareVersion());
        expected.put("net.interface.if2.mac",
                NetUtil.hardwareAddressToString(netInterfaceConfig2.getHardwareAddress()));
        expected.put("net.interface.if2.loopback", netInterfaceConfig2.isLoopback());
        expected.put("net.interface.if2.ptp", netInterfaceConfig2.isPointToPoint());
        expected.put("net.interface.if2.up", netInterfaceConfig2.isUp());
        expected.put("net.interface.if2.virtual", netInterfaceConfig2.isVirtual());
        expected.put("net.interface.if2.manufacturer", netInterfaceConfig2.getManufacturer());
        expected.put("net.interface.if2.model", netInterfaceConfig2.getModel());
        expected.put("net.interface.if2.revisionId", "rev1,rev2");
        expected.put("net.interface.if2.serialNum", netInterfaceConfig2.getSerialNumber());
        expected.put("net.interface.if2.technologyTypes", "CDMA,EVDO");
        expected.put("net.interface.if2.config.identifier", netInterfaceConfig2.getModemIdentifier());
        expected.put("net.interface.if2.config.powerMode", "LOW_POWER");
        expected.put("net.interface.if2.config.pppNum", netInterfaceConfig2.getPppNum());
        expected.put("net.interface.if2.config.poweredOn", netInterfaceConfig2.isPoweredOn());
        expected.put("net.interface.if2.config.connection.type", "PPP");
        expected.put("net.interface.if2.config.connection.status", "CONNECTED");
        expected.put("net.interface.if2.config.apn", "apn");
        expected.put("net.interface.if2.config.authType", "");
        expected.put("net.interface.if2.config.dataCompression", 42);
        expected.put("net.interface.if2.config.dialString", "dialString");
        expected.put("net.interface.if2.config.headerCompression", 100);
        expected.put("net.interface.if2.config.ipAddress", "");
        expected.put("net.interface.if2.config.password", new Password("password"));
        expected.put("net.interface.if2.config.pdpType", "");
        expected.put("net.interface.if2.config.pppNum", 123);
        expected.put("net.interface.if2.config.persist", true);
        expected.put("net.interface.if2.config.maxFail", 10);
        expected.put("net.interface.if2.config.idle", 20);
        expected.put("net.interface.if2.config.activeFilter", "activeFilter");
        expected.put("net.interface.if2.config.resetTimeout", 30);
        expected.put("net.interface.if2.config.lcpEchoInterval", 40);
        expected.put("net.interface.if2.config.lcpEchoFailure", 50);
        expected.put("net.interface.if2.config.profileId", 60);
        expected.put("net.interface.if2.config.username", "username");
        expected.put("net.interface.if2.config.enabled", true);
        expected.put("net.interface.if2.config.gpsEnabled", true);

        expected.put("net.interfaces", "if2,if1");

        TestUtil.invokePrivate(config, "recomputeNetworkProperties");

        Map<String, Object> properties = (Map<String, Object>) TestUtil.getFieldValue(config, "properties");
        assertMapEquals(expected, properties);
    }

    @Test
    public void testAddWifiConfigIP4PropertiesWifiModeNull() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(null);

        String netIfConfigPrefix = "";
        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addWifiConfigIP4Properties", wifiConfig, netIfConfigPrefix, properties);

        assertTrue(properties.isEmpty());
    }

    @Test
    public void testAddWifiConfigIP4PropertiesBasic() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setSSID("ssid");
        wifiConfig.setDriver("driver");
        wifiConfig.setBroadcast(false);
        wifiConfig.setPingAccessPoint(false);
        wifiConfig.setIgnoreSSID(false);

        String netIfConfigPrefix = "prefix.";
        HashMap<String, Object> properties = new HashMap<>();
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.wifi.adhoc.ssid", "ssid");
        expected.put("prefix.wifi.adhoc.driver", "driver");
        expected.put("prefix.wifi.adhoc.mode", "ADHOC");
        expected.put("prefix.wifi.adhoc.securityType", "NONE");
        expected.put("prefix.wifi.adhoc.channel", "");
        expected.put("prefix.wifi.adhoc.passphrase", new Password(""));
        expected.put("prefix.wifi.adhoc.hardwareMode", "");
        expected.put("prefix.wifi.adhoc.broadcast", (Boolean) false);
        expected.put("prefix.wifi.adhoc.bgscan", "");
        expected.put("prefix.wifi.adhoc.pingAccessPoint", (Boolean) false);
        expected.put("prefix.wifi.adhoc.ignoreSSID", (Boolean) false);

        TestUtil.invokePrivate(config, "addWifiConfigIP4Properties", wifiConfig, netIfConfigPrefix, properties);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testAddWifiConfigIP4PropertiesFull() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setChannels(new int[] { 1, 2, 3 });
        wifiConfig.setSSID("ssid");
        wifiConfig.setDriver("driver");
        wifiConfig.setSecurity(WifiSecurity.GROUP_CCMP);
        wifiConfig.setPasskey("password");
        wifiConfig.setHardwareMode("HW mode");
        wifiConfig.setBroadcast(true);
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        wifiConfig.setBgscan(new WifiBgscan(WifiBgscanModule.LEARN, 1, 2, 3));
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
        wifiConfig.setGroupCiphers(WifiCiphers.TKIP);
        wifiConfig.setPingAccessPoint(true);
        wifiConfig.setIgnoreSSID(true);

        String netIfConfigPrefix = "prefix.";
        HashMap<String, Object> properties = new HashMap<>();
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.wifi.adhoc.ssid", "ssid");
        expected.put("prefix.wifi.adhoc.driver", "driver");
        expected.put("prefix.wifi.adhoc.mode", "ADHOC");
        expected.put("prefix.wifi.adhoc.securityType", "GROUP_CCMP");
        expected.put("prefix.wifi.adhoc.channel", "1 2 3");
        expected.put("prefix.wifi.adhoc.passphrase", new Password("password"));
        expected.put("prefix.wifi.adhoc.hardwareMode", "HW mode");
        expected.put("prefix.wifi.adhoc.broadcast", (Boolean) true);
        expected.put("prefix.wifi.adhoc.radioMode", "RADIO_MODE_80211a");
        expected.put("prefix.wifi.adhoc.bgscan", "learn:1:2:3");
        expected.put("prefix.wifi.adhoc.pairwiseCiphers", "CCMP");
        expected.put("prefix.wifi.adhoc.groupCiphers", "TKIP");
        expected.put("prefix.wifi.adhoc.pingAccessPoint", (Boolean) true);
        expected.put("prefix.wifi.adhoc.ignoreSSID", (Boolean) true);

        TestUtil.invokePrivate(config, "addWifiConfigIP4Properties", wifiConfig, netIfConfigPrefix, properties);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testGetWifiConfigMinimal1() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.adhoc.passphrase", new Password("password"));
        properties.put("prefix.wifi.adhoc.broadcast", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.ADHOC);
        expected.setSSID("");
        expected.setDriver("");
        expected.setSecurity(WifiSecurity.NONE);
        expected.setPasskey("password");
        expected.setHardwareMode("");
        expected.setIgnoreSSID(false);
        expected.setBroadcast(true);

        WifiConfig wifiConfig = (WifiConfig) TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix,
                WifiMode.ADHOC, properties);

        assertEquals(expected, wifiConfig);
    }

    @Test
    public void testGetWifiConfigMinimal2() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.infra.ssid", "");
        properties.put("prefix.wifi.infra.driver", "");
        properties.put("prefix.wifi.infra.securityType", "");
        properties.put("prefix.wifi.infra.channel", "");
        properties.put("prefix.wifi.infra.passphrase", "password");
        properties.put("prefix.wifi.infra.radioMode", "");

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.INFRA);
        expected.setSSID("");
        expected.setDriver("");
        expected.setSecurity(WifiSecurity.NONE);
        expected.setPasskey("password");
        expected.setHardwareMode("");
        expected.setIgnoreSSID(false);
        expected.setBgscan(new WifiBgscan(""));

        WifiConfig wifiConfig = (WifiConfig) TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix,
                WifiMode.INFRA, properties);

        assertEquals(expected, wifiConfig);
    }

    @Test(expected = KuraException.class)
    public void testGetWifiConfigInvalidSecurityType() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.infra.securityType", "xyz");

        TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix, WifiMode.INFRA, properties);
    }

    @Test
    public void testGetWifiConfigInvalidChannel() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.infra.channel", "1 a 3");
        properties.put("prefix.wifi.infra.ssid", "ssid");
        properties.put("prefix.wifi.infra.driver", "driver");
        properties.put("prefix.wifi.infra.securityType", "GROUP_CCMP");
        properties.put("prefix.wifi.infra.passphrase", new Password("password"));
        properties.put("prefix.wifi.infra.hardwareMode", "HW mode");
        properties.put("prefix.wifi.infra.broadcast", (Boolean) true);
        properties.put("prefix.wifi.infra.radioMode", "RADIO_MODE_80211a");
        properties.put("prefix.wifi.infra.bgscan", "learn:1:2:3");
        properties.put("prefix.wifi.infra.pairwiseCiphers", "CCMP");
        properties.put("prefix.wifi.infra.groupCiphers", "TKIP");
        properties.put("prefix.wifi.infra.pingAccessPoint", (Boolean) true);
        properties.put("prefix.wifi.infra.ignoreSSID", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.INFRA);
        expected.setChannels(new int[] { 1, 0, 3 });
        expected.setSSID("ssid");
        expected.setDriver("driver");
        expected.setSecurity(WifiSecurity.GROUP_CCMP);
        expected.setPasskey("password");
        expected.setHardwareMode("HW mode");
        expected.setBroadcast(true);
        expected.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        expected.setBgscan(new WifiBgscan(WifiBgscanModule.LEARN, 1, 2, 3));
        expected.setPairwiseCiphers(WifiCiphers.CCMP);
        expected.setGroupCiphers(WifiCiphers.TKIP);
        expected.setPingAccessPoint(true);
        expected.setIgnoreSSID(true);

        WifiConfig wifiConfig = (WifiConfig) TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix,
                WifiMode.INFRA, properties);

        assertEquals(expected, wifiConfig);
    }

    @Test(expected = KuraException.class)
    public void testGetWifiConfigInvalidRadioMode() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.infra.radioMode", "xyz");

        TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix, WifiMode.INFRA, properties);
    }

    @Test
    public void testGetWifiConfigFullInfra() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.infra.ssid", "ssid");
        properties.put("prefix.wifi.infra.driver", "driver");
        properties.put("prefix.wifi.infra.securityType", "GROUP_CCMP");
        properties.put("prefix.wifi.infra.channel", "1 2 3");
        properties.put("prefix.wifi.infra.passphrase", new Password("password"));
        properties.put("prefix.wifi.infra.hardwareMode", "HW mode");
        properties.put("prefix.wifi.infra.broadcast", (Boolean) true);
        properties.put("prefix.wifi.infra.radioMode", "RADIO_MODE_80211a");
        properties.put("prefix.wifi.infra.bgscan", "learn:1:2:3");
        properties.put("prefix.wifi.infra.pairwiseCiphers", "CCMP");
        properties.put("prefix.wifi.infra.groupCiphers", "TKIP");
        properties.put("prefix.wifi.infra.pingAccessPoint", (Boolean) true);
        properties.put("prefix.wifi.infra.ignoreSSID", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.INFRA);
        expected.setChannels(new int[] { 1, 2, 3 });
        expected.setSSID("ssid");
        expected.setDriver("driver");
        expected.setSecurity(WifiSecurity.GROUP_CCMP);
        expected.setPasskey("password");
        expected.setHardwareMode("HW mode");
        expected.setBroadcast(true);
        expected.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        expected.setBgscan(new WifiBgscan(WifiBgscanModule.LEARN, 1, 2, 3));
        expected.setPairwiseCiphers(WifiCiphers.CCMP);
        expected.setGroupCiphers(WifiCiphers.TKIP);
        expected.setPingAccessPoint(true);
        expected.setIgnoreSSID(true);

        WifiConfig wifiConfig = (WifiConfig) TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix,
                WifiMode.INFRA, properties);

        assertEquals(expected, wifiConfig);
    }

    @Test
    public void testGetWifiConfigFullNonInfra() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String netIfConfigPrefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.wifi.adhoc.ssid", "ssid");
        properties.put("prefix.wifi.adhoc.driver", "driver");
        properties.put("prefix.wifi.adhoc.securityType", "GROUP_CCMP");
        properties.put("prefix.wifi.adhoc.channel", "1 2 3");
        properties.put("prefix.wifi.adhoc.passphrase", new Password("password"));
        properties.put("prefix.wifi.adhoc.hardwareMode", "HW mode");
        properties.put("prefix.wifi.adhoc.broadcast", (Boolean) true);
        properties.put("prefix.wifi.adhoc.radioMode", "RADIO_MODE_80211a");
        properties.put("prefix.wifi.adhoc.pairwiseCiphers", "CCMP");
        properties.put("prefix.wifi.adhoc.ignoreSSID", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.ADHOC);
        expected.setChannels(new int[] { 1, 2, 3 });
        expected.setSSID("ssid");
        expected.setDriver("driver");
        expected.setSecurity(WifiSecurity.GROUP_CCMP);
        expected.setPasskey("password");
        expected.setHardwareMode("HW mode");
        expected.setBroadcast(true);
        expected.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        expected.setPairwiseCiphers(WifiCiphers.CCMP);
        expected.setIgnoreSSID(true);

        WifiConfig wifiConfig = (WifiConfig) TestUtil.invokePrivate(config, "getWifiConfig", netIfConfigPrefix,
                WifiMode.ADHOC, properties);

        assertEquals(expected, wifiConfig);
    }

    @Test
    public void testAddModemConfigProperties1() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.apn", "apn");
        expected.put("prefix.authType", "");
        expected.put("prefix.dataCompression", 42);
        expected.put("prefix.dialString", "dialString");
        expected.put("prefix.headerCompression", 100);
        expected.put("prefix.ipAddress", "");
        expected.put("prefix.password", new Password("password"));
        expected.put("prefix.pdpType", "");
        expected.put("prefix.pppNum", 123);
        expected.put("prefix.persist", true);
        expected.put("prefix.maxFail", 10);
        expected.put("prefix.idle", 20);
        expected.put("prefix.activeFilter", "activeFilter");
        expected.put("prefix.resetTimeout", 30);
        expected.put("prefix.lcpEchoInterval", 40);
        expected.put("prefix.lcpEchoFailure", 50);
        expected.put("prefix.profileId", 60);
        expected.put("prefix.username", "username");
        expected.put("prefix.enabled", true);
        expected.put("prefix.gpsEnabled", true);

        ModemConfig modemConfig = new ModemConfig();
        modemConfig.setApn("apn");
        modemConfig.setAuthType(null);
        modemConfig.setDataCompression(42);
        modemConfig.setDialString("dialString");
        modemConfig.setHeaderCompression(100);
        modemConfig.setIpAddress(null);
        modemConfig.setPassword("password");
        modemConfig.setPdpType(null);
        modemConfig.setPppNumber(123);
        modemConfig.setPersist(true);
        modemConfig.setMaxFail(10);
        modemConfig.setIdle(20);
        modemConfig.setActiveFilter("activeFilter");
        modemConfig.setResetTimeout(30);
        modemConfig.setLcpEchoInterval(40);
        modemConfig.setLcpEchoFailure(50);
        modemConfig.setProfileID(60);
        modemConfig.setUsername("username");
        modemConfig.setEnabled(true);
        modemConfig.setGpsEnabled(true);

        String prefix = "prefix.";
        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addModemConfigProperties", modemConfig, prefix, properties);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testAddModemConfigProperties2() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.apn", "apn");
        expected.put("prefix.authType", "AUTO");
        expected.put("prefix.dataCompression", 42);
        expected.put("prefix.dialString", "dialString");
        expected.put("prefix.headerCompression", 100);
        expected.put("prefix.ipAddress", "10.0.0.1");
        expected.put("prefix.password", new Password("password"));
        expected.put("prefix.pdpType", "IP");
        expected.put("prefix.pppNum", 123);
        expected.put("prefix.persist", true);
        expected.put("prefix.maxFail", 10);
        expected.put("prefix.idle", 20);
        expected.put("prefix.activeFilter", "activeFilter");
        expected.put("prefix.resetTimeout", 30);
        expected.put("prefix.lcpEchoInterval", 40);
        expected.put("prefix.lcpEchoFailure", 50);
        expected.put("prefix.profileId", 60);
        expected.put("prefix.username", "username");
        expected.put("prefix.enabled", true);
        expected.put("prefix.gpsEnabled", true);

        ModemConfig modemConfig = new ModemConfig();
        modemConfig.setApn("apn");
        modemConfig.setAuthType(AuthType.AUTO);
        modemConfig.setDataCompression(42);
        modemConfig.setDialString("dialString");
        modemConfig.setHeaderCompression(100);
        modemConfig.setIpAddress(IP4Address.parseHostAddress("10.0.0.1"));
        modemConfig.setPassword("password");
        modemConfig.setPdpType(PdpType.IP);
        modemConfig.setPppNumber(123);
        modemConfig.setPersist(true);
        modemConfig.setMaxFail(10);
        modemConfig.setIdle(20);
        modemConfig.setActiveFilter("activeFilter");
        modemConfig.setResetTimeout(30);
        modemConfig.setLcpEchoInterval(40);
        modemConfig.setLcpEchoFailure(50);
        modemConfig.setProfileID(60);
        modemConfig.setUsername("username");
        modemConfig.setEnabled(true);
        modemConfig.setGpsEnabled(true);

        String prefix = "prefix.";
        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addModemConfigProperties", modemConfig, prefix, properties);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testGetModemConfigNull() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String prefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.apn", "apn");
        properties.put("prefix.authType", null);
        properties.put("prefix.dataCompression", null);
        properties.put("prefix.dialString", "dialString");
        properties.put("prefix.headerCompression", null);
        properties.put("prefix.ipAddress", null);
        properties.put("prefix.password", "password");
        properties.put("prefix.pdpType", null);
        properties.put("prefix.pppNum", null);
        properties.put("prefix.persist", null);
        properties.put("prefix.maxFail", null);
        properties.put("prefix.idle", null);
        properties.put("prefix.activeFilter", null);
        properties.put("prefix.resetTimeout", null);
        properties.put("prefix.lcpEchoInterval", null);
        properties.put("prefix.lcpEchoFailure", null);
        properties.put("prefix.profileId", null);
        properties.put("prefix.username", "username");
        properties.put("prefix.enabled", null);
        properties.put("prefix.gpsEnabled", null);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.NONE);
        expected.setDialString("dialString");
        expected.setIpAddress(null);
        expected.setPassword("password");
        expected.setPdpType(PdpType.UNKNOWN);
        expected.setUsername("username");
        expected.setEnabled(false);
        expected.setGpsEnabled(false);

        ModemConfig modemConfig = (ModemConfig) TestUtil.invokePrivate(config, "getModemConfig", prefix, properties);

        assertEquals(expected, modemConfig);
    }

    @Test
    public void testGetModemConfigEmpty() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String prefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.apn", "apn");
        properties.put("prefix.authType", "");
        properties.put("prefix.dataCompression", 1);
        properties.put("prefix.dialString", "dialString");
        properties.put("prefix.headerCompression", 2);
        properties.put("prefix.ipAddress", "");
        properties.put("prefix.password", "password");
        properties.put("prefix.pdpType", "");
        properties.put("prefix.pppNum", 4);
        properties.put("prefix.persist", true);
        properties.put("prefix.maxFail", 5);
        properties.put("prefix.idle", 6);
        properties.put("prefix.activeFilter", "activeFilter");
        properties.put("prefix.resetTimeout", 7);
        properties.put("prefix.lcpEchoInterval", 8);
        properties.put("prefix.lcpEchoFailure", 9);
        properties.put("prefix.profileId", 10);
        properties.put("prefix.username", "username");
        properties.put("prefix.enabled", true);
        properties.put("prefix.gpsEnabled", true);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.NONE);
        expected.setDataCompression(1);
        expected.setDialString("dialString");
        expected.setHeaderCompression(2);
        expected.setIpAddress(null);
        expected.setPassword("password");
        expected.setPdpType(PdpType.UNKNOWN);
        expected.setPppNumber(4);
        expected.setPersist(true);
        expected.setMaxFail(5);
        expected.setIdle(6);
        expected.setActiveFilter("activeFilter");
        expected.setResetTimeout(7);
        expected.setLcpEchoInterval(8);
        expected.setLcpEchoFailure(9);
        expected.setProfileID(10);
        expected.setUsername("username");
        expected.setEnabled(true);
        expected.setGpsEnabled(true);

        ModemConfig modemConfig = (ModemConfig) TestUtil.invokePrivate(config, "getModemConfig", prefix, properties);

        assertEquals(expected, modemConfig);
    }

    @Test
    public void testGetModemConfigAll() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String prefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.apn", "apn");
        properties.put("prefix.authType", "AUTO");
        properties.put("prefix.dataCompression", 1);
        properties.put("prefix.dialString", "dialString");
        properties.put("prefix.headerCompression", 2);
        properties.put("prefix.ipAddress", "10.0.0.1");
        properties.put("prefix.password", "password");
        properties.put("prefix.pdpType", "IP");
        properties.put("prefix.pppNum", 4);
        properties.put("prefix.persist", true);
        properties.put("prefix.maxFail", 5);
        properties.put("prefix.idle", 6);
        properties.put("prefix.activeFilter", "activeFilter");
        properties.put("prefix.resetTimeout", 7);
        properties.put("prefix.lcpEchoInterval", 8);
        properties.put("prefix.lcpEchoFailure", 9);
        properties.put("prefix.profileId", 10);
        properties.put("prefix.username", "username");
        properties.put("prefix.enabled", true);
        properties.put("prefix.gpsEnabled", true);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.AUTO);
        expected.setDataCompression(1);
        expected.setDialString("dialString");
        expected.setHeaderCompression(2);
        expected.setIpAddress(IPAddress.parseHostAddress("10.0.0.1"));
        expected.setPassword("password");
        expected.setPdpType(PdpType.IP);
        expected.setPppNumber(4);
        expected.setPersist(true);
        expected.setMaxFail(5);
        expected.setIdle(6);
        expected.setActiveFilter("activeFilter");
        expected.setResetTimeout(7);
        expected.setLcpEchoInterval(8);
        expected.setLcpEchoFailure(9);
        expected.setProfileID(10);
        expected.setUsername("username");
        expected.setEnabled(true);
        expected.setGpsEnabled(true);

        ModemConfig modemConfig = (ModemConfig) TestUtil.invokePrivate(config, "getModemConfig", prefix, properties);

        assertEquals(expected, modemConfig);
    }

    @Test(expected = KuraException.class)
    public void testGetModemConfigInvalidAuthType() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String prefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.authType", "xyz");

        TestUtil.invokePrivate(config, "getModemConfig", prefix, properties);
    }

    @Test(expected = KuraException.class)
    public void testGetModemConfigInvalidIPAddress() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String prefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.ipAddress", "1.2.3.4.5");

        TestUtil.invokePrivate(config, "getModemConfig", prefix, properties);
    }

    @Test(expected = KuraException.class)
    public void testGetModemConfigInvalidPdpType() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String prefix = "prefix.";

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("prefix.pdpType", "xyz");

        TestUtil.invokePrivate(config, "getModemConfig", prefix, properties);
    }

    @Test
    public void testAddNetConfigIP4PropertiesWithDhcp() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfig.setDhcp(true);
        ArrayList<IP4Address> dnsServers = new ArrayList<>();
        dnsServers.add((IP4Address) IPAddress.parseHostAddress("10.0.0.1"));
        dnsServers.add((IP4Address) IPAddress.parseHostAddress("10.0.0.2"));
        netConfig.setDnsServers(dnsServers);

        String prefix = "prefix.";
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.autoconnect", true);
        expected.put("prefix.ip4.status", "netIPv4StatusEnabledLAN");
        expected.put("prefix.ip4.dnsServers", "10.0.0.1,10.0.0.2");
        expected.put("prefix.dhcpClient4.enabled", true);

        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addNetConfigIP4Properties", netConfig, prefix, properties);
        assertEquals(expected, properties);
    }

    @Test
    public void testAddNetConfigIP4PropertiesWithoutDhcpFull() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfig.setDhcp(false);
        netConfig.setAddress((IP4Address) IPAddress.parseHostAddress("10.0.0.1"));
        netConfig.setNetworkPrefixLength((short) 24);
        netConfig.setGateway((IP4Address) IPAddress.parseHostAddress("10.0.0.2"));
        ArrayList<IP4Address> winsServers = new ArrayList<>();
        winsServers.add((IP4Address) IPAddress.parseHostAddress("10.0.1.1"));
        winsServers.add((IP4Address) IPAddress.parseHostAddress("10.0.1.2"));
        netConfig.setWinsServers(winsServers);
        ArrayList<String> domains = new ArrayList<>();
        domains.add("domain1");
        domains.add("domain2");
        netConfig.setDomains(domains);

        String prefix = "prefix.";
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.autoconnect", true);
        expected.put("prefix.ip4.status", "netIPv4StatusEnabledLAN");
        expected.put("prefix.ip4.dnsServers", "");
        expected.put("prefix.dhcpClient4.enabled", false);
        expected.put("prefix.ip4.address", "10.0.0.1");
        expected.put("prefix.ip4.prefix", (short) 24);
        expected.put("prefix.ip4.gateway", "10.0.0.2");
        expected.put("prefix.winsServers", "10.0.1.1,10.0.1.2");
        expected.put("prefix.domains", "domain1,domain2");

        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addNetConfigIP4Properties", netConfig, prefix, properties);
        assertEquals(expected, properties);
    }

    @Test
    public void testAddNetConfigIP6PropertiesWithDhcp() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP6 netConfig = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true);
        netConfig.setDhcp(true);

        String prefix = "prefix.";
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.ip6.status", "netIPv6StatusEnabledLAN");
        expected.put("prefix.dhcpClient6.enabled", true);

        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addNetConfigIP6Properties", netConfig, prefix, properties);
        assertEquals(expected, properties);
    }

    @Test
    public void testAddNetConfigIP6PropertiesWithoutDhcpFull() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        NetConfigIP6 netConfig = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true);
        netConfig.setDhcp(false);
        netConfig.setAddress((IP6Address) IPAddress.parseHostAddress("2001:db8:0:0:0:ff00:42:8329"));
        ArrayList<IP6Address> dnsServers = new ArrayList<>();
        dnsServers.add((IP6Address) IPAddress.parseHostAddress("2001:db8:0:0:0:ff00:42:1000"));
        dnsServers.add((IP6Address) IPAddress.parseHostAddress("2001:db8:0:0:0:ff00:42:1001"));
        netConfig.setDnsServers(dnsServers);
        ArrayList<String> domains = new ArrayList<>();
        domains.add("domain1");
        domains.add("domain2");
        netConfig.setDomains(domains);

        String prefix = "prefix.";
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("prefix.ip6.status", "netIPv6StatusEnabledLAN");
        expected.put("prefix.dhcpClient6.enabled", false);
        expected.put("prefix.address", "2001:db8:0:0:0:ff00:42:8329");
        expected.put("prefix.ip6.dnsServers", "2001:db8:0:0:0:ff00:42:1000,2001:db8:0:0:0:ff00:42:1001");
        expected.put("prefix.domains", "domain1,domain2");

        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addNetConfigIP6Properties", netConfig, prefix, properties);
        assertEquals(expected, properties);
    }

    @Test
    public void testAddDhcpServerConfig4() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        ArrayList<IP4Address> dnsServers = new ArrayList<>();
        dnsServers.add((IP4Address) IP4Address.parseHostAddress("10.0.1.1"));

        DhcpServerConfigIP4 dhcpConfig = new DhcpServerConfigIP4("if1", false,
                (IP4Address) IP4Address.parseHostAddress("10.0.0.0"),
                (IP4Address) IP4Address.parseHostAddress("10.0.0.1"),
                (IP4Address) IP4Address.parseHostAddress("255.255.255.0"), 1, 2, (short) 24,
                (IP4Address) IP4Address.parseHostAddress("10.0.0.10"),
                (IP4Address) IP4Address.parseHostAddress("10.0.0.15"), true, dnsServers);

        String netIfConfigPrefix = "prefix.";
        HashMap<String, Object> properties = new HashMap<>();
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.dhcpServer4.enabled", false);
        expected.put("prefix.dhcpServer4.defaultLeaseTime", 1);
        expected.put("prefix.dhcpServer4.maxLeaseTime", 2);
        expected.put("prefix.dhcpServer4.prefix", (short) 24);
        expected.put("prefix.dhcpServer4.rangeStart", "10.0.0.10");
        expected.put("prefix.dhcpServer4.rangeEnd", "10.0.0.15");
        expected.put("prefix.dhcpServer4.passDns", true);

        TestUtil.invokePrivate(config, "addDhcpServerConfig4", dhcpConfig, netIfConfigPrefix, properties);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testAddFirewallNatConfig() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        FirewallAutoNatConfig autoNatConfig = new FirewallAutoNatConfig();

        String netIfConfigPrefix = "prefix.";
        HashMap<String, Object> properties = new HashMap<>();
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("prefix.nat.enabled", true);

        TestUtil.invokePrivate(config, "addFirewallNatConfig", netIfConfigPrefix, properties);

        assertMapEquals(expected, properties);
    }

    @Test
    public void testAddInterfaceConfigurationNull() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String interfaceName = "if1";
        NetInterfaceType type = null;
        HashMap<String, Object> properties = new HashMap<String, Object>();

        TestUtil.invokePrivate(config, "addInterfaceConfiguration", interfaceName, type, properties);

        Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = (Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .getFieldValue(config, "netInterfaceConfigs");
        assertTrue(netInterfaceConfigs.isEmpty());
    }

    @Test
    public void testAddInterfaceConfigurationLoopback() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String interfaceName = "if1";
        NetInterfaceType type = NetInterfaceType.LOOPBACK;
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.if1.type", "LOOPBACK");
        properties.put("net.interface.if1.config.autoconnect", true);

        LoopbackInterfaceConfigImpl loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> loopbackInterfaceAddressConfigs = new ArrayList<>();
        NetInterfaceAddressConfigImpl addressConfig = new NetInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true));
        netConfigs.add(new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, true));
        addressConfig.setNetConfigs(netConfigs);
        loopbackInterfaceAddressConfigs.add(addressConfig);
        loopbackInterfaceConfig.setNetInterfaceAddresses(loopbackInterfaceAddressConfigs);
        loopbackInterfaceConfig.setAutoConnect(true);
        loopbackInterfaceConfig.setState(NetInterfaceState.DISCONNECTED);

        HashMap<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> expected = new HashMap<>();
        expected.put(interfaceName, loopbackInterfaceConfig);

        TestUtil.invokePrivate(config, "addInterfaceConfiguration", interfaceName, type, properties);

        assertMapEquals(expected, (Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .getFieldValue(config, "netInterfaceConfigs"));
    }

    @Test
    public void testAddInterfaceConfigurationWifi() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String interfaceName = "if1";
        NetInterfaceType type = NetInterfaceType.WIFI;
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.if1.type", "WIFI");
        properties.put("net.interface.if1.config.autoconnect", true);

        properties.put("net.interface.if1.config.wifi.master.ssid", "ssid");
        properties.put("net.interface.if1.config.wifi.master.passphrase", "passphrase");

        properties.put("net.interface.if1.config.wifi.infra.ssid", "ssid");
        properties.put("net.interface.if1.config.wifi.infra.passphrase", "passphrase");

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl addressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        netConfigs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true));
        netConfigs.add(new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, true));

        WifiConfig wifiConfig1 = new WifiConfig(WifiMode.MASTER, "ssid", null, WifiSecurity.NONE, "passphrase", "",
                false, null);
        wifiConfig1.setDriver("");
        netConfigs.add(wifiConfig1);

        WifiConfig wifiConfig2 = new WifiConfig(WifiMode.INFRA, "ssid", null, WifiSecurity.NONE, "passphrase", "",
                false, new WifiBgscan(""));
        wifiConfig2.setDriver("");
        netConfigs.add(wifiConfig2);

        addressConfig.setNetConfigs(netConfigs);
        wifiInterfaceAddressConfigs.add(addressConfig);
        wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
        wifiInterfaceConfig.setAutoConnect(true);
        wifiInterfaceConfig.setState(NetInterfaceState.DISCONNECTED);

        HashMap<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> expected = new HashMap<>();
        expected.put(interfaceName, wifiInterfaceConfig);

        TestUtil.invokePrivate(config, "addInterfaceConfiguration", interfaceName, type, properties);

        assertMapEquals(expected, (Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .getFieldValue(config, "netInterfaceConfigs"));
    }

    @Test
    public void testAddInterfaceConfigurationUnknown() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String interfaceName = "if1";
        NetInterfaceType type = NetInterfaceType.UNKNOWN;
        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addInterfaceConfiguration", interfaceName, type, properties);

        Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = (Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .getFieldValue(config, "netInterfaceConfigs");
        assertTrue(netInterfaceConfigs.isEmpty());
    }

    @Test
    public void testAddInterfaceConfigurationUnsupported() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        String interfaceName = "if1";
        NetInterfaceType type = NetInterfaceType.ADSL;
        HashMap<String, Object> properties = new HashMap<>();

        TestUtil.invokePrivate(config, "addInterfaceConfiguration", interfaceName, type, properties);

        Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = (Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>) TestUtil
                .getFieldValue(config, "netInterfaceConfigs");
        assertTrue(netInterfaceConfigs.isEmpty());
    }

    @Test
    public void testPopulateNetInterfaceConfigurationEthernetMinimal() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        EthernetInterfaceConfigImpl netInterfaceConfig = new EthernetInterfaceConfigImpl("if1");

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.if1.type", "ETHERNET");

        TestUtil.invokePrivate(config, "populateNetInterfaceConfiguration", netInterfaceConfig, properties);

        EthernetInterfaceConfigImpl expected = new EthernetInterfaceConfigImpl("if1");
        expected.setState(NetInterfaceState.DISCONNECTED);

        assertEquals(expected, netInterfaceConfig);
    }

    @Test
    public void testPopulateNetInterfaceConfigurationWifi() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("if1");
        ArrayList<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(new WifiInterfaceAddressConfigImpl());
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.if1.type", "WIFI");
        properties.put("net.interface.if1.up", false);
        properties.put("net.interface.if1.config.autoconnect", true);
        properties.put("net.interface.if1.wifi.capabilities", "CIPHER_CCMP CIPHER_TKIP");
        properties.put("net.interface.if1.config.wifi.mode", "ADHOC");
        properties.put("net.interface.if1.config.wifi.master.passphrase", "password");
        properties.put("net.interface.if1.config.wifi.infra.passphrase", "password");

        TestUtil.invokePrivate(config, "populateNetInterfaceConfiguration", netInterfaceConfig, properties);

        WifiInterfaceConfigImpl expected = new WifiInterfaceConfigImpl("if1");
        expected.setState(NetInterfaceState.DISCONNECTED);
        expected.setAutoConnect(true);
        expected.setUp(false);
        expected.setCapabilities(EnumSet.of(Capability.CIPHER_CCMP, Capability.CIPHER_TKIP));

        ArrayList<WifiInterfaceAddressConfig> expectedInterfaceAddresses = new ArrayList<>();
        WifiInterfaceAddressConfigImpl expectedInterfaceAddress = new WifiInterfaceAddressConfigImpl();
        expectedInterfaceAddress.setMode(WifiMode.ADHOC);

        List<NetConfig> netConfigs = new ArrayList<>();

        netConfigs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true));
        netConfigs.add(new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, true));

        WifiConfig netConfig1 = new WifiConfig();
        netConfig1.setMode(WifiMode.MASTER);
        netConfig1.setSSID("");
        netConfig1.setDriver("");
        netConfig1.setSecurity(WifiSecurity.NONE);
        netConfig1.setHardwareMode("");
        netConfig1.setPasskey("password");
        netConfigs.add(netConfig1);

        WifiConfig netConfig2 = new WifiConfig();
        netConfig2.setMode(WifiMode.INFRA);
        netConfig2.setSSID("");
        netConfig2.setDriver("");
        netConfig2.setSecurity(WifiSecurity.NONE);
        netConfig2.setHardwareMode("");
        netConfig2.setPasskey("password");
        netConfig2.setBgscan(new WifiBgscan(""));
        netConfigs.add(netConfig2);

        expectedInterfaceAddress.setNetConfigs(netConfigs);

        expectedInterfaceAddresses.add(expectedInterfaceAddress);
        expected.setNetInterfaceAddresses(expectedInterfaceAddresses);

        assertEquals(expected, netInterfaceConfig);
    }

    // FIXME: failures since rebase
    public void testPopulateNetInterfaceConfigurationModem() throws Throwable {
        NetworkConfiguration config = new NetworkConfiguration();

        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl("if1");
        ArrayList<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(new ModemInterfaceAddressConfigImpl());
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.if1.type", "MODEM");
        properties.put("net.interface.if1.up", false);
        properties.put("net.interface.if1.config.autoconnect", true);
        properties.put("net.interface.if1.manufacturer", "manufacturer");
        properties.put("net.interface.if1.model", "model");
        properties.put("net.interface.if1.revisionId", "rev1,rev2");
        properties.put("net.interface.if1.serialNum", "serialNumber");
        properties.put("net.interface.if1.technologyTypes", "CDMA,EVDO");
        properties.put("net.interface.if1.config.password", new Password((String) null));
        properties.put("net.interface.if1.config.identifier", "modemId");
        properties.put("net.interface.if1.config.powerMode", "LOW_POWER");
        properties.put("net.interface.if1.config.pppNum", 100);
        properties.put("net.interface.if1.config.poweredOn", true);
        properties.put("net.interface.if1.config.connection.type", "PPP");
        properties.put("net.interface.if1.config.connection.status", "CONNECTED");

        TestUtil.invokePrivate(config, "populateNetInterfaceConfiguration", netInterfaceConfig, properties);

        ModemInterfaceConfigImpl expected = new ModemInterfaceConfigImpl("if1");
        expected.setState(NetInterfaceState.DISCONNECTED);
        expected.setAutoConnect(true);
        expected.setUp(false);
        expected.setManufacturer("manufacturer");
        expected.setModel("model");
        expected.setRevisionId(new String[] { "rev1", "rev2" });
        expected.setSerialNumber("serialNumber");
        List<ModemTechnologyType> technologyTypes = new ArrayList<>();
        technologyTypes.add(ModemTechnologyType.CDMA);
        technologyTypes.add(ModemTechnologyType.EVDO);
        expected.setTechnologyTypes(technologyTypes);
        expected.setModemIdentifier("modemId");
        expected.setPowerMode(ModemPowerMode.LOW_POWER);
        expected.setPppNum(100);
        expected.setPoweredOn(true);

        ArrayList<ModemInterfaceAddressConfig> expectedInterfaceAddresses = new ArrayList<>();
        ModemInterfaceAddressConfigImpl expectedInterfaceAddress = new ModemInterfaceAddressConfigImpl();
        expectedInterfaceAddress.setConnectionType(ModemConnectionType.PPP);
        expectedInterfaceAddress.setConnectionStatus(ModemConnectionStatus.CONNECTED);

        List<NetConfig> netConfigs = new ArrayList<>();

        netConfigs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true));
        netConfigs.add(new NetConfigIP6(NetInterfaceStatus.netIPv6StatusDisabled, true));

        ModemConfig netConfig1 = new ModemConfig();
        netConfig1.setDialString(null);
        netConfig1.setPdpType(PdpType.UNKNOWN);
        netConfig1.setApn(null);
        netConfig1.setUsername(null);
        netConfig1.setPassword((String) null);
        netConfig1.setPppNumber(100);
        netConfigs.add(netConfig1);

        expectedInterfaceAddress.setNetConfigs(netConfigs);

        expectedInterfaceAddresses.add(expectedInterfaceAddress);
        expected.setNetInterfaceAddresses(expectedInterfaceAddresses);

        assertEquals(expected, netInterfaceConfig);
    }

    private <T> void assertMapEquals(Map<String, T> expected, Map<String, T> actual) {
        assertEquals("Size", expected.size(), actual.size());

        for (Map.Entry<String, T> entry : expected.entrySet()) {
            String key = entry.getKey();
            Object expectedValue = entry.getValue();
            Object actualValue = actual.get(key);

            if ((expectedValue instanceof Password) && (actualValue instanceof Password)) {
                Password expectedPassword = (Password) expectedValue;
                Password actualPassword = (Password) actualValue;

                assertEquals("Key: " + key, expectedPassword.toString(), actualPassword.toString());
            } else {
                assertEquals("Key: " + key, expectedValue, actualValue);
            }
        }
    }
}
