/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.junit.Test;

public class IpConfigurationInterpreterTest {

    private Map<String, Object> properties;
    private IP4Address currentAddress;

    private List<NetConfig> netConfigs;
    private UsbDevice usbDevice;

    @Test
    public void shouldReturnEmptyListWhenPropertiesAreNull() throws Exception {
        givenNullProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsIsEmpty();
    }

    @Test
    public void shouldPopulateStaticIp4AndIp6Configuration() throws Exception {
        givenStaticIp4AndIp6Properties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(2);
        thenNetConfigAtIndexEquals(0, expectedStaticIp4Config());
        thenNetConfigAtIndexEquals(1, expectedStaticIp6Config());
    }

    @Test
    public void shouldUseDefaultStatusWhenIp4StatusMissingAndNotVirtual() throws Exception {
        givenIp4StatusMissingProperties();

        whenConfigurationIsPopulated();

        thenIp4ConfigStatusIs(NetInterfaceStatus.netIPv4StatusDisabled);
        thenIp4ConfigIsNotAutoConnect();
    }

    @Test
    public void shouldSetDhcpAndIgnoreStaticFieldsWhenDhcpClient4Enabled() throws Exception {
        givenDhcpClient4EnabledWithStaticFieldsProperties();

        whenConfigurationIsPopulated();

        thenIp4ConfigIsDhcp();
        thenIp4ConfigAddressAndGatewayAreNull();
    }

    @Test
    public void shouldAddFirewallAutoNatConfigWhenNatEnabled() throws Exception {
        givenNatEnabledProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(2);
        thenNetConfigAtIndexEquals(1, new FirewallAutoNatConfig("eth0", "unknown", true));
    }

    @Test
    public void shouldNotAddFirewallAutoNatConfigWhenNatDisabled() throws Exception {
        givenNatDisabledProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(1);
    }

    @Test
    public void shouldAddDhcpServerConfigWhenValidRangeProvided() throws Exception {
        givenValidDhcpServer4RangeProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(2);
        thenNetConfigAtIndexEquals(1, expectedDhcpServerConfig());
    }

    @Test
    public void shouldNotAddDhcpServerConfigWhenRangeIsMissing() throws Exception {
        givenDhcpServer4EnabledWithMissingRangeProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(1);
    }

    @Test
    public void shouldSilentlyIgnoreInvalidDhcpServerConfigWhenDisabled() throws Exception {
        givenDhcpServer4DisabledWithInvalidRangeProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(1);
    }

    @Test
    public void shouldNotAddIp6ConfigWhenDhcp6Enabled() throws Exception {
        givenDhcp6EnabledProperties();

        whenConfigurationIsPopulated();

        thenNetConfigsHasSize(1);
    }

    @Test
    public void shouldReturnUsbNetDeviceWhenVendorAndProductIdPresent() {
        givenUsbVendorAndProductProperties();

        whenUsbDeviceInfoIsRetrieved();

        thenUsbDeviceEquals(new UsbNetDevice("1234", "5678", "VendorName", "ProductName", "1", "1.2", "eth0"));
    }

    @Test
    public void shouldReturnNullUsbDeviceWhenVendorOrProductIdMissing() {
        givenEmptyProperties();

        whenUsbDeviceInfoIsRetrieved();

        thenUsbDeviceIsNull();
    }

    @Test
    public void shouldReturnNullUsbDeviceWhenPropertiesAreNull() {
        givenNullProperties();

        whenUsbDeviceInfoIsRetrieved();

        thenUsbDeviceIsNull();
    }

    // Given methods

    private void givenNullProperties() {
        this.properties = null;
    }

    private void givenEmptyProperties() {
        this.properties = new HashMap<>();
    }

    private void givenStaticIp4AndIp6Properties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledLAN");
        this.properties.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.eth0.config.ip4.address", "192.168.1.10");
        this.properties.put("net.interface.eth0.config.ip4.prefix", "24");
        this.properties.put("net.interface.eth0.config.ip4.gateway", "192.168.1.1");
        this.properties.put("net.interface.eth0.config.ip4.dnsServers", "8.8.8.8, 8.8.4.4");
        this.properties.put("net.interface.eth0.config.ip4.winsServers", "10.0.0.1");
        this.properties.put("net.interface.eth0.config.ip4.domains", "example.com,test.com");

        this.properties.put("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledLAN");
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", false);
        this.properties.put("net.interface.eth0.config.ip6.address", "fe80::1");
        this.properties.put("net.interface.eth0.config.ip6.dnsServers", "fe80::2,fe80::3");
        this.properties.put("net.interface.eth0.config.ip6.domains", "example6.com");

        this.currentAddress = ip4("192.168.1.10");
    }

    private void givenIp4StatusMissingProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.dhcpClient4.enabled", false);

        this.currentAddress = ip4("192.168.1.10");
    }

    private void givenDhcpClient4EnabledWithStaticFieldsProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.dhcpClient4.enabled", true);
        this.properties.put("net.interface.eth0.config.ip4.address", "192.168.1.10");
        this.properties.put("net.interface.eth0.config.ip4.gateway", "192.168.1.1");
        this.properties.put("net.interface.eth0.config.ip4.prefix", "24");

        this.currentAddress = ip4("192.168.1.10");
    }

    private void givenNatEnabledProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.nat.enabled", true);
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        this.currentAddress = ip4("192.168.1.10");
    }

    private void givenNatDisabledProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.nat.enabled", false);
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        this.currentAddress = ip4("192.168.1.10");
    }

    private void givenValidDhcpServer4RangeProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.eth0.config.ip4.address", "192.168.1.1");
        this.properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        this.properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        this.properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.1.100");
        this.properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.1.200");
        this.properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 7200);
        this.properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 14400);
        this.properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        this.currentAddress = ip4("192.168.1.1");
    }

    private void givenDhcpServer4EnabledWithMissingRangeProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        this.currentAddress = ip4("192.168.1.1");
    }

    private void givenDhcpServer4DisabledWithInvalidRangeProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        this.properties.put("net.interface.eth0.config.ip4.address", "192.168.1.1");
        this.properties.put("net.interface.eth0.config.dhcpServer4.enabled", false);
        this.properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.1.100");
        this.properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.1.200");
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        this.currentAddress = ip4("192.168.1.1");
    }

    private void givenDhcp6EnabledProperties() throws Exception {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        this.currentAddress = ip4("192.168.1.1");
    }

    private void givenUsbVendorAndProductProperties() {
        this.properties = new HashMap<>();
        this.properties.put("net.interface.eth0.usb.vendor.id", "1234");
        this.properties.put("net.interface.eth0.usb.vendor.name", "VendorName");
        this.properties.put("net.interface.eth0.usb.product.id", "5678");
        this.properties.put("net.interface.eth0.usb.product.name", "ProductName");
        this.properties.put("net.interface.eth0.usb.busNumber", "1");
        this.properties.put("net.interface.eth0.usb.devicePath", "1.2");
    }

    // When methods

    private void whenConfigurationIsPopulated() throws Exception {
        this.netConfigs = IpConfigurationInterpreter.populateConfiguration(this.properties, "eth0",
                this.currentAddress, false);
    }

    private void whenUsbDeviceInfoIsRetrieved() {
        this.usbDevice = IpConfigurationInterpreter.getUsbDeviceInfo(this.properties, "eth0");
    }

    // Then methods

    private void thenNetConfigsIsEmpty() {
        assertNotNull(this.netConfigs);
        assertTrue(this.netConfigs.isEmpty());
    }

    private void thenNetConfigsHasSize(int expectedSize) {
        assertNotNull(this.netConfigs);
        assertEquals(expectedSize, this.netConfigs.size());
    }

    private void thenNetConfigAtIndexEquals(int index, NetConfig expected) {
        assertEquals(expected, this.netConfigs.get(index));
    }

    private void thenIp4ConfigStatusIs(NetInterfaceStatus expectedStatus) {
        NetConfigIP4 ip4Config = (NetConfigIP4) this.netConfigs.get(0);
        assertEquals(expectedStatus, ip4Config.getStatus());
    }

    private void thenIp4ConfigIsNotAutoConnect() {
        NetConfigIP4 ip4Config = (NetConfigIP4) this.netConfigs.get(0);
        assertFalse(ip4Config.isAutoConnect());
    }

    private void thenIp4ConfigIsDhcp() {
        NetConfigIP4 ip4Config = (NetConfigIP4) this.netConfigs.get(0);
        assertTrue(ip4Config.isDhcp());
    }

    private void thenIp4ConfigAddressAndGatewayAreNull() {
        NetConfigIP4 ip4Config = (NetConfigIP4) this.netConfigs.get(0);
        assertNull(ip4Config.getAddress());
        assertNull(ip4Config.getGateway());
    }

    private void thenUsbDeviceEquals(UsbDevice expected) {
        assertEquals(expected, this.usbDevice);
    }

    private void thenUsbDeviceIsNull() {
        assertNull(this.usbDevice);
    }

    // Expected value builders

    private static NetConfigIP4 expectedStaticIp4Config() throws Exception {
        NetConfigIP4 expectedIp4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        expectedIp4.setAddress(ip4("192.168.1.10"));
        expectedIp4.setNetworkPrefixLength((short) 24);
        expectedIp4.setGateway(ip4("192.168.1.1"));
        expectedIp4.setDnsServers(Arrays.asList(ip4("8.8.8.8"), ip4("8.8.4.4")));
        expectedIp4.setWinsServers(Arrays.asList(ip4("10.0.0.1")));
        expectedIp4.setDomains(Arrays.asList("example.com", "test.com"));
        return expectedIp4;
    }

    private static NetConfigIP6 expectedStaticIp6Config() throws Exception {
        NetConfigIP6 expectedIp6 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true, false);
        expectedIp6.setAddress(ip6("fe80::1"));
        expectedIp6.setDnsServers(Arrays.asList(ip6("fe80::2"), ip6("fe80::3")));
        expectedIp6.setDomains(Arrays.asList("example6.com"));
        return expectedIp6;
    }

    private static DhcpServerConfigIP4 expectedDhcpServerConfig() throws Exception {
        IP4Address routerAddress = ip4("192.168.1.1");
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg("eth0", true, 7200, 14400, true);
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(ip4("192.168.1.0"), ip4("255.255.255.0"), (short) 24,
                routerAddress, ip4("192.168.1.100"), ip4("192.168.1.200"), Arrays.asList(routerAddress));
        return new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
    }

    private static IP4Address ip4(String address) throws Exception {
        return (IP4Address) IPAddress.parseHostAddress(address);
    }

    private static IP6Address ip6(String address) throws Exception {
        return (IP6Address) IPAddress.parseHostAddress(address);
    }
}
