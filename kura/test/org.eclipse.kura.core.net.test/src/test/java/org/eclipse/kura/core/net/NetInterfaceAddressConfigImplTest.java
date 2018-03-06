/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetInterfaceAddressConfigImplTest {

    @Test
    public void testEqualsObject() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        assertEquals(a, a);

        NetInterfaceAddressConfigImpl b = createConfig();
        assertEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentIPAddress() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

        a.setAddress(IPAddress.parseHostAddress("10.0.0.101"));
        b.setAddress(IPAddress.parseHostAddress("10.0.0.102"));

        assertNotEquals(a, b);

        a.setAddress(null);
        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentBroadcast() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

        a.setBroadcast(IPAddress.parseHostAddress("10.0.1.255"));
        b.setBroadcast(IPAddress.parseHostAddress("10.0.2.255"));

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentDnsServers() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

        ArrayList<IPAddress> dnsServersA = new ArrayList<>();
        dnsServersA.add(IPAddress.parseHostAddress("10.0.2.1"));
        dnsServersA.add(IPAddress.parseHostAddress("10.0.2.2"));
        a.setDnsServers(dnsServersA);

        ArrayList<IPAddress> dnsServersB = new ArrayList<>();
        dnsServersB.add(IPAddress.parseHostAddress("10.0.3.1"));
        dnsServersB.add(IPAddress.parseHostAddress("10.0.3.2"));
        b.setDnsServers(dnsServersB);

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentGateway() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

        a.setGateway(IPAddress.parseHostAddress("10.0.1.1"));
        b.setGateway(IPAddress.parseHostAddress("10.0.2.1"));

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentNetConfigs() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

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
    public void testEqualsObjectDifferentNetmask() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

        a.setNetmask(IPAddress.parseHostAddress("255.255.0.0"));
        b.setNetmask(IPAddress.parseHostAddress("255.0.0.0"));

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectDifferentNetworkPrefixLength() throws UnknownHostException {
        NetInterfaceAddressConfigImpl a = createConfig();
        NetInterfaceAddressConfigImpl b = createConfig();

        a.setNetworkPrefixLength((short) 20);
        b.setNetworkPrefixLength((short) 30);

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectNetInterfaceAddress() {
        NetInterfaceAddressImpl a = new NetInterfaceAddressImpl();
        String b = "";

        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsObjectNetInterfaceAddressConfig() {
        NetInterfaceAddressConfigImpl a = new NetInterfaceAddressConfigImpl();
        NetInterfaceAddressImpl b = new NetInterfaceAddressImpl();

        assertNotEquals(a, b);
    }

    @Test
    public void testNetInterfaceAddressConfigImpl() {
        NetInterfaceAddressConfigImpl value = new NetInterfaceAddressConfigImpl();

        assertNull(value.getAddress());
        assertEquals(0, value.getNetworkPrefixLength());
        assertNull(value.getNetmask());
        assertNull(value.getGateway());
        assertNull(value.getBroadcast());
        assertNull(value.getDnsServers());
    }

    @Test
    public void testNetInterfaceAddressConfigImplNetInterfaceAddress() throws UnknownHostException {
        ArrayList<IPAddress> dnsServers = new ArrayList<>();
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.1"));
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.2"));

        NetInterfaceAddressImpl address = new NetInterfaceAddressImpl();
        address.setAddress(IPAddress.parseHostAddress("10.0.0.100"));
        address.setBroadcast(IPAddress.parseHostAddress("10.0.0.255"));
        address.setDnsServers(dnsServers);
        address.setGateway(IPAddress.parseHostAddress("10.0.0.1"));
        address.setNetmask(IPAddress.parseHostAddress("255.255.255.0"));
        address.setNetworkPrefixLength((short) 24);

        NetInterfaceAddressConfigImpl value = new NetInterfaceAddressConfigImpl(address);

        assertEquals(IPAddress.parseHostAddress("10.0.0.100"), value.getAddress());
        assertEquals(24, value.getNetworkPrefixLength());
        assertEquals(IPAddress.parseHostAddress("255.255.255.0"), value.getNetmask());
        assertEquals(IPAddress.parseHostAddress("10.0.0.1"), value.getGateway());
        assertEquals(IPAddress.parseHostAddress("10.0.0.255"), value.getBroadcast());
        assertEquals(dnsServers, value.getDnsServers());
    }

    @Test
    public void testNetConfigs() throws UnknownHostException {
        NetInterfaceAddressConfigImpl value = createConfig();

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
        NetInterfaceAddressConfigImpl value = createConfig();
        value.setNetConfigs(null);

        String expected = "NetConfig: no configurations";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testToStringEmptyConfigs() throws UnknownHostException {
        NetInterfaceAddressConfigImpl value = createConfig();
        value.setNetConfigs(new ArrayList<NetConfig>());

        String expected = "";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testToStringNullConfig() throws UnknownHostException {
        NetInterfaceAddressConfigImpl value = createConfig();

        ArrayList<NetConfig> configs = new ArrayList<>();
        configs.add(null);
        value.setNetConfigs(configs);

        String expected = "NetConfig: null - ";

        assertEquals(expected, value.toString());
    }

    @Test
    public void testToStringWithConfigs() throws UnknownHostException {
        NetInterfaceAddressConfigImpl value = createConfig();
        String expected = "NetConfig: NetConfigIP4 [winsServers=[], super.toString()=NetConfigIP"
                + " [status=netIPv4StatusEnabledLAN, autoConnect=true, dhcp=false, address=null,"
                + " networkPrefixLength=-1, subnetMask=null, gateway=null, dnsServers=[],"
                + " domains=[], properties={}]] - "
                + "NetConfig: NetConfigIP4 [winsServers=[], super.toString()=NetConfigIP"
                + " [status=netIPv4StatusEnabledWAN, autoConnect=false, dhcp=false, address=null,"
                + " networkPrefixLength=-1, subnetMask=null, gateway=null, dnsServers=[],"
                + " domains=[], properties={}]] - ";

        assertEquals(expected, value.toString());
    }

    private NetInterfaceAddressConfigImpl createConfig() throws UnknownHostException {
        ArrayList<IPAddress> dnsServers = new ArrayList<>();
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.1"));
        dnsServers.add(IPAddress.parseHostAddress("10.0.1.2"));

        ArrayList<NetConfig> configs = new ArrayList<>();
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true));
        configs.add(new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false));

        NetInterfaceAddressConfigImpl value = new NetInterfaceAddressConfigImpl();
        value.setAddress(IPAddress.parseHostAddress("10.0.0.100"));
        value.setBroadcast(IPAddress.parseHostAddress("10.0.0.255"));
        value.setDnsServers(dnsServers);
        value.setGateway(IPAddress.parseHostAddress("10.0.0.1"));
        value.setNetConfigs(configs);
        value.setNetmask(IPAddress.parseHostAddress("255.255.255.0"));
        value.setNetworkPrefixLength((short) 24);

        return value;
    }
}
