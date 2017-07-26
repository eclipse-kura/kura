/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.admin.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class DnsMonitorServiceImplTest {

    @Test
    public void testActivate() throws NoSuchFieldException {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        doAnswer(invocation -> {
            Dictionary dict = invocation.getArgumentAt(2, Dictionary.class);
            assertEquals(1, dict.size());
            String[] topics = (String[]) dict.get("event.topics");

            assertEquals(2, topics.length);
            for (String topic : topics) {
                assertTrue("org/eclipse/kura/net/admin/event/NETWORK_EVENT_STATUS_CHANGE_TOPIC".equals(topic)
                        || "org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC".equals(topic));
            }

            return null;
        }).when(bcMock).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        svc.activate(ccMock);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        ExecutorService executor = (ExecutorService) TestUtil.getFieldValue(svc, "executor");
        Future task = (Future) TestUtil.getFieldValue(svc, "monitorTask");
        assertNotNull(executor);
        assertNotNull(task);
        assertNotNull(TestUtil.getFieldValue(svc, "stopThread"));
        assertNotNull(TestUtil.getFieldValue(svc, "dnsUtil"));

        svc.deactivate(ccMock);

        assertTrue(executor.isShutdown());
        assertTrue(task.isCancelled() || task.isDone());
        assertNull(TestUtil.getFieldValue(svc, "executor"));
        assertNull(TestUtil.getFieldValue(svc, "monitorTask"));
    }

    @Test
    public void testHandleEventConfigChange() throws NoSuchFieldException {
        AtomicBoolean visited = new AtomicBoolean(false);

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl() {

            @Override
            protected void reconfigureDNSProxy(DnsServerConfigIP4 dnsServerConfigIP4) {
                assertTrue(dnsServerConfigIP4.getForwarders().isEmpty());
                assertTrue(dnsServerConfigIP4.getAllowedNetworks().isEmpty());

                visited.set(true);
            }
        };

        LinuxDns ldMock = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", ldMock);

        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", 1); // cause exception and make sure networkConfiguration remains null

        Event event = new NetworkConfigurationChangeEvent(props);

        svc.handleEvent(event);

        assertTrue(visited.get());

        assertNull(TestUtil.getFieldValue(svc, "networkConfiguration"));
    }

    @Test
    public void testHandleEventStatusChange() throws NoSuchFieldException {
        AtomicBoolean visited = new AtomicBoolean(false);

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl() {

            @Override
            protected void reconfigureDNSProxy(DnsServerConfigIP4 dnsServerConfigIP4) {
                assertTrue(dnsServerConfigIP4.getForwarders().isEmpty());
                assertTrue(dnsServerConfigIP4.getAllowedNetworks().isEmpty());

                visited.set(true);
            }
        };

        LinuxDns ldMock = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", ldMock);

        Event event = new NetworkStatusChangeEvent("eth3", mock(InterfaceState.class), new HashMap<>());

        svc.handleEvent(event);

        assertTrue(visited.get());
        assertNull(TestUtil.getFieldValue(svc, "networkConfiguration"));
    }

    @Test
    public void testGetAllowedNetworks() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
        TestUtil.setFieldValue(svc, "allowedNetworks", allowedNetworks);

        String interfaceName = "eth3";

        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        currentNets.add(nc);
        DhcpServerConfig dhcp = mock(DhcpServerConfig.class);
        when(dhcp.isPassDns()).thenReturn(true);
        when(dhcp.getPrefix()).thenReturn((short) 24);
        IPAddress host = IPAddress.parseHostAddress("10.10.0.200");
        when(dhcp.getRouterAddress()).thenReturn(host);
        currentNets.add(dhcp);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        TestUtil.invokePrivate(svc, "getAllowedNetworks", nic);

        assertTrue((boolean) TestUtil.getFieldValue(svc, "enabled"));

        Set<NetworkPair<IP4Address>> aa = (Set<NetworkPair<IP4Address>>) TestUtil.getFieldValue(svc, "allowedNetworks");
        assertNotNull(aa);
        assertEquals(1, aa.size());
        NetworkPair<IP4Address> pair = aa.iterator().next();
        assertEquals(host, pair.getIpAddress());
        assertEquals(24, pair.getPrefix());

    }

    @Test
    public void testIsEnabledForWanNoIpConfig() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
        TestUtil.setFieldValue(svc, "allowedNetworks", allowedNetworks);

        String interfaceName = "eth3";

        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isEnabledForWan", nic);

        assertFalse(result);
    }

    @Test
    public void testIsEnabledForWanLan() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
        TestUtil.setFieldValue(svc, "allowedNetworks", allowedNetworks);

        String interfaceName = "eth3";

        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isEnabledForWan", nic);

        assertFalse(result);
    }

    @Test
    public void testIsEnabledForWan() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
        TestUtil.setFieldValue(svc, "allowedNetworks", allowedNetworks);

        String interfaceName = "eth3";

        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isEnabledForWan", nic);

        assertTrue(result);
    }

    @Test
    public void testSetDnsServersNull() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        LinuxDns dns = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dns);

        doThrow(new RuntimeException("test")).when(dns).setDnServers(anyObject());

        TestUtil.invokePrivate(svc, "setDnsServers", (Set<IPAddress>) null);

        // no exception expected - set DNS servers should not be called
    }

    @Test
    public void testSetDnsServersEmpty() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        LinuxDns dns = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dns);

        doThrow(new RuntimeException("test")).when(dns).setDnServers(anyObject());

        Set<IPAddress> servers = new HashSet<>();

        TestUtil.invokePrivate(svc, "setDnsServers", servers);

        // no exception expected - set DNS servers should not be called
    }

    @Test
    public void testSetDnsServersNullNew() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        LinuxDns dns = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dns);

        IPAddress address = IPAddress.parseHostAddress("10.10.0.10");
        doAnswer(invocation -> {
            Set<IPAddress> ns = invocation.getArgumentAt(0, Set.class);

            assertEquals(1, ns.size());
            assertEquals(address, ns.iterator().next());

            return null;
        }).when(dns).setDnServers(anyObject());

        Set<IPAddress> servers = new HashSet<>();
        servers.add(address);

        TestUtil.invokePrivate(svc, "setDnsServers", servers);

        verify(dns, times(1)).setDnServers(anyObject());
    }

    @Test
    public void testSetDnsServersFullNewDiff() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        LinuxDns dns = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dns);

        Set<IPAddress> oldServers = new HashSet<>();
        IPAddress oldAddress = IPAddress.parseHostAddress("10.10.0.11");
        oldServers.add(oldAddress);
        when(dns.getDnServers()).thenReturn(oldServers);

        IPAddress address = IPAddress.parseHostAddress("10.10.0.10");
        doAnswer(invocation -> {
            Set<IPAddress> ns = invocation.getArgumentAt(0, Set.class);

            assertEquals(1, ns.size());
            assertEquals(address, ns.iterator().next());

            return null;
        }).when(dns).setDnServers(anyObject());

        Set<IPAddress> servers = new HashSet<>();
        servers.add(address);

        TestUtil.invokePrivate(svc, "setDnsServers", servers);

        verify(dns, times(1)).setDnServers(anyObject());
    }

    @Test
    public void testSetDnsServersFullNewEq() throws Throwable {
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        LinuxDns dns = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dns);

        Set<IPAddress> oldServers = new HashSet<>();
        IPAddress oldAddress = IPAddress.parseHostAddress("10.10.0.10");
        oldServers.add(oldAddress);
        when(dns.getDnServers()).thenReturn(oldServers);

        doThrow(new RuntimeException("test")).when(dns).setDnServers(anyObject());

        Set<IPAddress> servers = new HashSet<>();
        IPAddress address = IPAddress.parseHostAddress("10.10.0.10");
        servers.add(address);

        TestUtil.invokePrivate(svc, "setDnsServers", servers);

        verify(dns, times(0)).setDnServers(anyObject());
    }

    @Test
    public void testUpdateDnsProxyConfig() throws Throwable {
        // test the method descending into getAllowedNetworks, adding forwarders and updating DNS

        String dnsServer = "10.10.0.100";
        IPAddress allowedHost = IPAddress.parseHostAddress("10.10.0.200");

        AtomicBoolean visited = new AtomicBoolean(false);

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl() {

            @Override
            protected void reconfigureDNSProxy(DnsServerConfigIP4 dnsServerConfigIP4) {
                Set<IP4Address> forwarders = dnsServerConfigIP4.getForwarders();
                assertNotNull(forwarders);
                assertEquals(1, forwarders.size());
                assertEquals(dnsServer, forwarders.iterator().next().getHostAddress());

                Set<NetworkPair<IP4Address>> aa = dnsServerConfigIP4.getAllowedNetworks();
                assertNotNull(aa);
                assertEquals(1, aa.size());
                NetworkPair<IP4Address> pair = aa.iterator().next();
                assertEquals(allowedHost, pair.getIpAddress());
                assertEquals(24, pair.getPrefix());

                visited.set(true);
            }
        };

        LinuxDns dnsMock = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dnsMock);

        Set<IPAddress> dnsServers = new HashSet<>();
        dnsServers.add(IPAddress.parseHostAddress(dnsServer));
        when(dnsMock.getDnServers()).thenReturn(dnsServers);

        String interfaceName = "eth3";

        NetworkConfiguration netc = new NetworkConfiguration();
        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        currentNets.add(nc);
        DhcpServerConfig dhcp = mock(DhcpServerConfig.class);
        when(dhcp.isPassDns()).thenReturn(true);
        when(dhcp.getPrefix()).thenReturn((short) 24);
        when(dhcp.getRouterAddress()).thenReturn(allowedHost);
        currentNets.add(dhcp);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);
        netc.addNetInterfaceConfig(nic);

        TestUtil.setFieldValue(svc, "networkConfiguration", netc);

        // the tested invocation
        TestUtil.invokePrivate(svc, "updateDnsProxyConfig");

        assertTrue((boolean) TestUtil.getFieldValue(svc, "enabled"));

        assertTrue(visited.get());
    }

    @Test
    public void testGetConfiguredDnsServersVoidLanOnly() throws Throwable {
        // test with only LAN-enabled interfaces

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        String interfaceName = "eth3";

        NetworkConfiguration netc = new NetworkConfiguration();
        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true); // not WAN
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);
        netc.addNetInterfaceConfig(nic);

        TestUtil.setFieldValue(svc, "networkConfiguration", netc);

        Set<IPAddress> result = (Set<IPAddress>) TestUtil.invokePrivate(svc, "getConfiguredDnsServers", new Class[] {},
                null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetConfiguredDnsServersVoid() throws Throwable {
        // not dhcp client, with user dns servers

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        String interfaceName = "eth3";

        NetworkConfiguration netc = new NetworkConfiguration();
        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfigIP4 nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true); // WAN, not dhcp client
        List<IP4Address> dnsServers = new ArrayList<>();
        IP4Address dnsServer = (IP4Address) IP4Address.parseHostAddress("10.10.0.100");
        dnsServers.add(dnsServer);
        nc.setDnsServers(dnsServers);
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);
        netc.addNetInterfaceConfig(nic);

        TestUtil.setFieldValue(svc, "networkConfiguration", netc);

        Set<IPAddress> result = (Set<IPAddress>) TestUtil.invokePrivate(svc, "getConfiguredDnsServers", new Class[] {},
                null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dnsServer, result.iterator().next());
    }

    @Test
    public void testGetConfiguredDnsServersDhcpClientUserServers() throws Throwable {
        // dhcp client with user-defined dns servers

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl();

        String interfaceName = "eth3";

        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfigIP4 nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        nc.setDhcp(true); // make it a dhcp client
        List<IP4Address> dnsServers = new ArrayList<>();
        IP4Address dnsServer = (IP4Address) IP4Address.parseHostAddress("10.10.0.100");
        dnsServers.add(dnsServer);
        nc.setDnsServers(dnsServers);
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        Set<IPAddress> result = (Set<IPAddress>) TestUtil.invokePrivate(svc, "getConfiguredDnsServers",
                new Class[] { NetInterfaceConfig.class }, nic);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dnsServer, result.iterator().next());
    }

    @Test
    public void testGetConfiguredDnsServersDhcpClientNoUserServersEthernet() throws Throwable {
        // dhcp client without user-defined dns servers, on ethernet

        String interfaceName = "eth3";
        String currentAddress = "10.10.0.10";

        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl() {

            @Override
            protected String getCurrentIpAddress(String intfName) throws KuraException {
                assertEquals(interfaceName, intfName);

                return currentAddress;
            }
        };

        LinuxDns dnsMock = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dnsMock);

        List<IPAddress> servers = new ArrayList<>();
        IPAddress dnsServer = IPAddress.parseHostAddress("10.10.0.100");
        servers.add(dnsServer);
        when(dnsMock.getDhcpDnsServers(interfaceName, currentAddress)).thenReturn(servers);

        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfigIP4 nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        nc.setDhcp(true); // make it a dhcp client
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        Set<IPAddress> result = (Set<IPAddress>) TestUtil.invokePrivate(svc, "getConfiguredDnsServers",
                new Class[] { NetInterfaceConfig.class }, nic);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dnsServer, result.iterator().next());
    }

    @Test
    public void testGetConfiguredDnsServersDhcpClientNoUserServersModem() throws Throwable {
        // dhcp client without user-defined dns servers, modem interface

        String interfaceName = "eth3";
        String currentAddress = "10.10.0.10";

        final int pppNum = 3;
        DnsMonitorServiceImpl svc = new DnsMonitorServiceImpl() {
            @Override
            protected boolean pppHasAddress(int pppNo) throws KuraException {
                assertEquals(pppNum, pppNo);

                return true;
            }
        };

        LinuxDns dnsMock = mock(LinuxDns.class);
        TestUtil.setFieldValue(svc, "dnsUtil", dnsMock);

        List<IPAddress> servers = new ArrayList<>();
        IPAddress dnsServer = IPAddress.parseHostAddress("10.10.0.100");
        servers.add(dnsServer);
        when(dnsMock.getPppDnServers()).thenReturn(servers);

        ModemInterfaceConfigImpl nic = new ModemInterfaceConfigImpl(interfaceName);
        nic.setPppNum(pppNum);
        List<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        ModemInterfaceAddressConfigImpl niac = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> currentNets = new ArrayList<>();
        NetConfigIP4 nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        nc.setDhcp(true); // make it a dhcp client
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        interfaceAddresses.add(niac);
        nic.setNetInterfaceAddresses(interfaceAddresses);

        Set<IPAddress> result = (Set<IPAddress>) TestUtil.invokePrivate(svc, "getConfiguredDnsServers",
                new Class[] { NetInterfaceConfig.class }, nic);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dnsServer, result.iterator().next());
    }

}
