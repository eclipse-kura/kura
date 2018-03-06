/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiMode;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WifiInterfaceAddressConfigImplTest {

    @Test
    public void testEqualsObject() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl a = createConfig();
        assertEquals(a, a);

        WifiInterfaceAddressConfigImpl b = createConfig();
        assertEquals(a, b);

        a.setNetConfigs(null);
        b.setNetConfigs(null);

        assertEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentNetConfigs() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl a = createConfig();
        WifiInterfaceAddressConfigImpl b = createConfig();

        ArrayList<NetConfig> configsA = new ArrayList<>();
        configsA.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true));
        configsA.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));
        a.setNetConfigs(configsA);

        ArrayList<NetConfig> configsB = new ArrayList<>();
        configsB.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        configsB.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));
        b.setNetConfigs(configsB);

        assertNotEquals(a, b);

        a.setNetConfigs(null);
        assertNotEquals(a, b);

        a.setNetConfigs(configsA);
        b.setNetConfigs(null);
        assertNotEquals(a, b);

        ArrayList<NetConfig> configsA2 = new ArrayList<>();
        configsA2.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        configsA2.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));
        a.setNetConfigs(configsA2);

        ArrayList<NetConfig> configsB2 = new ArrayList<>();
        configsB2.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        b.setNetConfigs(configsB2);

        assertNotEquals(a, b);

        configsB2.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        b.setNetConfigs(configsB2);

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentMode() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl a = createConfig();
        WifiInterfaceAddressConfigImpl b = createConfig();

        a.setMode(WifiMode.ADHOC);
        b.setMode(WifiMode.INFRA);

        assertNotEquals(a, b);

        a.setMode(null);
        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentBitrate() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl a = createConfig();
        WifiInterfaceAddressConfigImpl b = createConfig();

        a.setBitrate(42);
        b.setBitrate(100);

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentWifiAccessPoint() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl a = createConfig();
        WifiInterfaceAddressConfigImpl b = createConfig();

        a.setWifiAccessPoint(new WifiAccessPointImpl("ssid1"));
        b.setWifiAccessPoint(new WifiAccessPointImpl("ssid2"));

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectWifiInterfaceAddress() {
        WifiInterfaceAddressImpl a = new WifiInterfaceAddressImpl();
        NetInterfaceAddressImpl b = new NetInterfaceAddressImpl();

        assertNotEquals(a, b);

        b.setNetworkPrefixLength((short) 24);

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectWifiInterfaceAddressConfig() {
        WifiInterfaceAddressConfigImpl a = new WifiInterfaceAddressConfigImpl();
        WifiInterfaceAddressImpl b = new WifiInterfaceAddressImpl();

        assertNotEquals(a, b);
    }

    @Test
    public void testWifiInterfaceAddressConfigImpl() {
        WifiInterfaceAddressConfigImpl value = new WifiInterfaceAddressConfigImpl();

        assertNull(value.getMode());
        assertEquals(0, value.getBitrate());
        assertNull(value.getWifiAccessPoint());
        assertNull(value.getAddress());
        assertEquals(0, value.getNetworkPrefixLength());
        assertNull(value.getNetmask());
        assertNull(value.getGateway());
        assertNull(value.getBroadcast());
        assertNull(value.getDnsServers());
    }

    @Test
    public void testWifiInterfaceAddressConfigImplWifiInterfaceAddress() throws UnknownHostException {
        ArrayList<IPAddress> dnsServers = new ArrayList<>();
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.1"));
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.2"));

        WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl("ssid");

        WifiInterfaceAddressImpl address = new WifiInterfaceAddressImpl();
        address.setAddress(IPAddress.parseHostAddress("10.0.0.100"));
        address.setBitrate(42);
        address.setBroadcast(IPAddress.parseHostAddress("10.0.0.255"));
        address.setDnsServers(dnsServers);
        address.setGateway(IPAddress.parseHostAddress("10.0.0.1"));
        address.setMode(WifiMode.MASTER);
        address.setNetmask(IPAddress.parseHostAddress("255.255.255.0"));
        address.setNetworkPrefixLength((short) 24);
        address.setWifiAccessPoint(wifiAccessPoint);

        WifiInterfaceAddressConfigImpl value = new WifiInterfaceAddressConfigImpl(address);

        assertEquals(WifiMode.MASTER, value.getMode());
        assertEquals(42, value.getBitrate());
        assertEquals(wifiAccessPoint, value.getWifiAccessPoint());
        assertEquals(IPAddress.parseHostAddress("10.0.0.100"), value.getAddress());
        assertEquals(24, value.getNetworkPrefixLength());
        assertEquals(IPAddress.parseHostAddress("255.255.255.0"), value.getNetmask());
        assertEquals(IPAddress.parseHostAddress("10.0.0.1"), value.getGateway());
        assertEquals(IPAddress.parseHostAddress("10.0.0.255"), value.getBroadcast());
        assertEquals(dnsServers, value.getDnsServers());
    }

    @Test
    public void testNetConfigs() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl value = createConfig();

        ArrayList<NetConfig> configs = new ArrayList<>();
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true));
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));
        value.setNetConfigs(configs);
        assertEquals(value.getConfigs(), configs);

        configs = new ArrayList<>();
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false));
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));
        value.setNetConfigs(configs);
        assertEquals(value.getConfigs(), configs);
    }

    @Test
    public void testToStringNoConfigs() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl value = createConfig();
        value.setNetConfigs(null);

        String expected = "NetConfig: no configurations";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testToStringEmptyConfigs() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl value = createConfig();
        value.setNetConfigs(new ArrayList<NetConfig>());

        String expected = "";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testToStringNullConfig() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl value = createConfig();

        ArrayList<NetConfig> configs = new ArrayList<>();
        configs.add(null);
        value.setNetConfigs(configs);

        String expected = "NetConfig: null - ";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testToStringWithConfigs() throws UnknownHostException {
        WifiInterfaceAddressConfigImpl value = createConfig();

        String expected = "NetConfig: NetConfigIP4 [winsServers=[], super.toString()=NetConfigIP"
                + " [status=netIPv4StatusEnabledLAN, autoConnect=true, dhcp=false, address=null,"
                + " networkPrefixLength=-1, subnetMask=null, gateway=null, dnsServers=[], domains=[],"
                + " properties={}]] - NetConfig: NetConfigIP4 [winsServers=[], super.toString()=NetConfigIP"
                + " [status=netIPv4StatusEnabledWAN, autoConnect=false, dhcp=false, address=null,"
                + " networkPrefixLength=-1, subnetMask=null, gateway=null, dnsServers=[], domains=[],"
                + " properties={}]] - ";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testMode() {
        WifiInterfaceAddressConfigImpl value = new WifiInterfaceAddressConfigImpl();

        value.setMode(WifiMode.ADHOC);
        assertEquals(WifiMode.ADHOC, value.getMode());

        value.setMode(WifiMode.INFRA);
        assertEquals(WifiMode.INFRA, value.getMode());
    }

    @Test
    public void testBitrate() {
        WifiInterfaceAddressConfigImpl value = new WifiInterfaceAddressConfigImpl();

        value.setBitrate(42);
        assertEquals(42, value.getBitrate());

        value.setBitrate(100);
        assertEquals(100, value.getBitrate());
    }

    @Test
    public void testWifiAccessPoint() {
        WifiInterfaceAddressConfigImpl value = new WifiInterfaceAddressConfigImpl();

        WifiAccessPointImpl ap1 = new WifiAccessPointImpl("ssid1");
        value.setWifiAccessPoint(ap1);
        assertEquals(ap1, value.getWifiAccessPoint());

        WifiAccessPointImpl ap2 = new WifiAccessPointImpl("ssid2");
        value.setWifiAccessPoint(ap2);
        assertEquals(ap2, value.getWifiAccessPoint());
    }

    private WifiInterfaceAddressConfigImpl createConfig() throws UnknownHostException {
        ArrayList<IPAddress> dnsServers = new ArrayList<IPAddress>();
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.1"));
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.2"));

        ArrayList<NetConfig> configs = new ArrayList<NetConfig>();
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true));
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));

        WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl("ssid");

        WifiInterfaceAddressConfigImpl value = new WifiInterfaceAddressConfigImpl();
        value.setAddress(IPAddress.parseHostAddress("10.0.0.100"));
        value.setBitrate(42);
        value.setBroadcast(IPAddress.parseHostAddress("10.0.0.255"));
        value.setDnsServers(dnsServers);
        value.setGateway(IPAddress.parseHostAddress("10.0.0.1"));
        value.setMode(WifiMode.MASTER);
        value.setNetConfigs(configs);
        value.setNetmask(IPAddress.parseHostAddress("255.255.255.0"));
        value.setNetworkPrefixLength((short) 24);
        value.setWifiAccessPoint(wifiAccessPoint);

        return value;
    }
}
