/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.route.RouteConfigIP4;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public class EthernetMonitorServiceImplTest {

    @Test
    public void testActivateExceptionsNoConfig() throws NoSuchFieldException, KuraException {
        // test activation without configuration and the exception message

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        when(ncsMock.getNetworkConfiguration()).thenThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test"));

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        svc.activate(ccMock);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        assertNotNull(TestUtil.getFieldValue(svc, "executor"));

        TestUtil.setFieldValue(svc, "tasks", null);
    }

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException, KuraException {
        // test activation and deactivation

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        EthernetInterfaceConfigImpl nic = new EthernetInterfaceConfigImpl("eth3");
        nc.addNetInterfaceConfig(nic);
        when(ncsMock.getNetworkConfiguration()).thenReturn(nc);

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        svc.activate(ccMock);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        assertNotNull(TestUtil.getFieldValue(svc, "executor"));
        Map<String, Future<?>> tasks = (Map<String, Future<?>>) TestUtil.getFieldValue(svc, "tasks");
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertTrue(tasks.containsKey("eth3"));
        Future<?> future = tasks.get("eth3");
        assertFalse(future.isCancelled());

        Map<String, EthernetInterfaceConfigImpl> cfgs = (Map<String, EthernetInterfaceConfigImpl>) TestUtil
                .getFieldValue(svc, "networkConfiguration");
        Map<String, EthernetInterfaceConfigImpl> newCfgs = (Map<String, EthernetInterfaceConfigImpl>) TestUtil
                .getFieldValue(svc, "newNetworkConfiguration");

        assertNotNull(cfgs);
        assertEquals(1, cfgs.size());
        assertTrue(cfgs.containsValue(nic));

        assertNotNull(newCfgs);
        assertEquals(1, newCfgs.size());
        assertTrue(newCfgs.containsValue(nic));

        svc.deactivate(ccMock);

        assertNull(TestUtil.getFieldValue(svc, "executor"));

        tasks = (Map<String, Future<?>>) TestUtil.getFieldValue(svc, "tasks");
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertTrue(tasks.containsKey("eth3"));
        assertNull(tasks.get("eth3"));
        assertTrue(future.isCancelled() || future.isDone());
    }

    @Test
    public void testHandleEventException() {
        // produce and log exception

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");
        props.put("net.interfaces", 1); // cause exception
        NetworkConfigurationChangeEvent event = new NetworkConfigurationChangeEvent(props);

        svc.handleEvent(event);
    }

    @Test
    public void testHandleEvent() throws NoSuchFieldException {
        // handle the event but don't start monitor

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        // reset the state
        TestUtil.setFieldValue(svc, "tasks", null);

        Map<String, Object> props = new HashMap<>();
        props.put("net.interfaces", "eth3,eth4");
        props.put("net.interface.eth3.type", "ETHERNET");
        NetworkConfigurationChangeEvent event = new NetworkConfigurationChangeEvent(props);

        svc.handleEvent(event);

        Map<String, EthernetInterfaceConfigImpl> newCfgs = (Map<String, EthernetInterfaceConfigImpl>) TestUtil
                .getFieldValue(svc, "newNetworkConfiguration");

        assertNotNull(newCfgs);
        assertEquals(1, newCfgs.size());

        Map<String, Future> tasks = (Map<String, Future>) TestUtil.getFieldValue(svc, "tasks");
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
    }

    @Test
    public void testDisableInterface() throws Throwable {
        // make external calls to disable the interface

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        NetworkAdminService nasMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(nasMock);

        TestUtil.invokePrivate(svc, "disableInterface", "eth3");

        verify(nasMock, times(1)).disableInterface("eth3");
        verify(nasMock, times(1)).manageDhcpServer("eth3", false);
    }

    @Test
    public void testIsConfigChangedNulls() throws Throwable {
        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        // both null => not changed
        List<NetInterfaceAddressConfig> newConfig = null;
        List<NetInterfaceAddressConfig> currentConfig = null;

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertFalse(result);
    }

    @Test
    public void testIsConfigChangedOneNull() throws Throwable {
        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        // only one null => changed
        List<NetInterfaceAddressConfig> newConfig = null;
        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertTrue(result);
    }

    @Test
    public void testIsConfigChangedSizes() throws Throwable {
        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        // sizes differ => changed
        List<NetInterfaceAddressConfig> newConfig = new ArrayList<>();
        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();

        newConfig.add(null);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertTrue(result);
    }

    @Test
    public void testIsConfigChangedAllNetConfigsNull() throws Throwable {
        // both NetConfig lists are null => not changed

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        List<NetInterfaceAddressConfig> newConfig = new ArrayList<>();
        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();

        NetInterfaceAddressConfigImpl nic = new NetInterfaceAddressConfigImpl();
        newConfig.add(nic);

        nic = new NetInterfaceAddressConfigImpl();
        currentConfig.add(nic);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertFalse(result);
    }

    @Test
    public void testIsConfigChangedDifferentNetConfigLists() throws Throwable {
        // NetConfig lists differ => changed

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        List<NetInterfaceAddressConfig> newConfig = new ArrayList<>();
        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();

        NetInterfaceAddressConfigImpl nic = new NetInterfaceAddressConfigImpl();
        nic.setNetConfigs(new ArrayList<>());
        newConfig.add(nic);

        nic = new NetInterfaceAddressConfigImpl();
        currentConfig.add(nic);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertTrue(result);
    }

    @Test
    public void testIsConfigChangedNetConfigsDontDiffer() throws Throwable {
        // NetConfig lists the same => not changed

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        List<NetInterfaceAddressConfig> newConfig = new ArrayList<>();
        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();

        NetInterfaceAddressConfigImpl nic = new NetInterfaceAddressConfigImpl();
        ArrayList<NetConfig> newNets = new ArrayList<>();
        NetConfig nc = new FirewallAutoNatConfig(); // skipped
        newNets.add(nc);
        nc = mock(DhcpServerConfigIP4.class); // skip the disabled config
        newNets.add(nc);
        nic.setNetConfigs(newNets);
        newConfig.add(nic);

        nic = new NetInterfaceAddressConfigImpl();
        ArrayList<NetConfig> currentNets = new ArrayList<>();
        nc = new FirewallAutoNatConfig();
        currentNets.add(nc);
        nc = mock(DhcpServerConfigIP4.class);
        currentNets.add(nc);
        nic.setNetConfigs(currentNets);
        currentConfig.add(nic);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertFalse(result);
    }

    @Test
    public void testIsConfigChangedNetConfigsDiffer() throws Throwable {
        // there's a change in a NetConfig list element

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl();

        // sizes differ = > changed
        List<NetInterfaceAddressConfig> newConfig = new ArrayList<>();
        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();

        NetInterfaceAddressConfigImpl nic = new NetInterfaceAddressConfigImpl();
        ArrayList<NetConfig> newNets = new ArrayList<>();
        NetConfig nc = new FirewallAutoNatConfig(); // skipped
        newNets.add(nc);
        nc = mock(DhcpServerConfigIP4.class); // skip the disabled config
        newNets.add(nc);
        nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true); // diff LAN->WAN
        newNets.add(nc);
        nic.setNetConfigs(newNets);
        newConfig.add(nic);

        nic = new NetInterfaceAddressConfigImpl();
        ArrayList<NetConfig> currentNets = new ArrayList<>();
        nc = new FirewallAutoNatConfig();
        currentNets.add(nc);
        nc = mock(DhcpServerConfigIP4.class);
        currentNets.add(nc);
        nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        currentNets.add(nc);
        nic.setNetConfigs(currentNets);
        currentConfig.add(nic);

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isConfigChanged", newConfig, currentConfig);

        assertTrue(result);
    }

    @Test
    public void testMonitor() throws Throwable {
        // test a part of monitor() implementation

        String interfaceName = "eth3";

        EthernetMonitorServiceImpl svc = new EthernetMonitorServiceImpl() {

            int callNo = 1;

            @Override
            protected InterfaceState getEthernetInterfaceState(String interfaceName, boolean isL2Only) throws KuraException {
                InterfaceState isMock = mock(InterfaceState.class);

                if (callNo == 1) {
                    when(isMock.isUp()).thenReturn(false);
                    when(isMock.isLinkUp()).thenReturn(true);
                } else {
                    when(isMock.isUp()).thenReturn(true);
                    when(isMock.isLinkUp()).thenReturn(true);
                }

                callNo++;

                return isMock;
            }

            @Override
            protected void startInterfaceIfDown(String interfaceName) throws KuraException {
                // do nothing, in this case
            }
        };

        NetworkAdminService nasMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(nasMock);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        RouteService rsMock = mock(RouteService.class);
        TestUtil.setFieldValue(svc, "routeService", rsMock);

        IP4Address destination = (IP4Address) IP4Address.parseHostAddress("10.10.0.0");
        IP4Address gateway = (IP4Address) IP4Address.parseHostAddress("10.10.0.200");
        IP4Address netmask = (IP4Address) IP4Address.parseHostAddress("255.255.255.0");
        RouteConfig route = new RouteConfigIP4(destination, gateway, netmask, interfaceName, 1);
        when(rsMock.getDefaultRoute(interfaceName)).thenReturn(route);

        List<NetInterfaceAddressConfig> newConfig = new ArrayList<>();
        NetInterfaceAddressConfigImpl niac = new NetInterfaceAddressConfigImpl();
        ArrayList<NetConfig> newNets = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true); // diff LAN->WAN
        ((NetConfigIP4) nc).setDhcp(true); // make it a DHCP client
        newNets.add(nc);
        nc = mock(DhcpServerConfigIP4.class); // this way server is disabled by default
        newNets.add(nc);
        niac.setNetConfigs(newNets);
        newConfig.add(niac);
        Map<String, EthernetInterfaceConfigImpl> newMap = new HashMap<>();
        EthernetInterfaceConfigImpl newIC = new EthernetInterfaceConfigImpl(interfaceName);
        List<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(niac);
        newIC.setNetInterfaceAddresses(interfaceAddresses);
        newMap.put(interfaceName, newIC);
        TestUtil.setFieldValue(svc, "newNetworkConfiguration", newMap);

        List<NetInterfaceAddressConfig> currentConfig = new ArrayList<>();
        niac = new NetInterfaceAddressConfigImpl();
        ArrayList<NetConfig> currentNets = new ArrayList<>();
        nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        currentNets.add(nc);
        niac.setNetConfigs(currentNets);
        currentConfig.add(niac);
        Map<String, EthernetInterfaceConfigImpl> currentMap = new HashMap<>();
        EthernetInterfaceConfigImpl currentIC = new EthernetInterfaceConfigImpl(interfaceName);
        interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(niac);
        currentIC.setNetInterfaceAddresses(interfaceAddresses);
        currentMap.put(interfaceName, currentIC);
        TestUtil.setFieldValue(svc, "networkConfiguration", currentMap);

        TestUtil.invokePrivate(svc, "monitor", interfaceName);

        verify(nasMock, times(1)).enableInterface(interfaceName, true);
        verify(nasMock, times(1)).manageDhcpServer(interfaceName, false);
        verify(nasMock, times(0)).manageDhcpServer(interfaceName, true);

        verify(eaMock, times(1)).postEvent(isA(NetworkStatusChangeEvent.class));

        verify(rsMock, times(1)).removeStaticRoute(destination, gateway, netmask, interfaceName);

        Map<String, InterfaceState> states = (Map<String, InterfaceState>) TestUtil.getFieldValue(svc,
                "interfaceState");
        assertNotNull(states.get(interfaceName));
    }

}
