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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.LinkTool;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.route.RouteConfigIP4;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiClientMonitorListener;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class WifiMonitorServiceImplTest {

    @Test
    public void testActivateNetConfigException() throws KuraException {
        // activation, but test the exception log message

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        NetworkConfigurationService netCfgSvcMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(netCfgSvcMock);

        when(netCfgSvcMock.getNetworkConfiguration())
                .thenThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test"));

        svc.activate(ccMock);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());
    }

    @Test
    public void testActivateNullConfigMessage() throws KuraException {
        // activation with info message about null network configuration

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        NetworkConfigurationService netCfgSvcMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(netCfgSvcMock);

        when(netCfgSvcMock.getNetworkConfiguration()).thenReturn(null);

        svc.activate(ccMock);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());
    }

    @Test
    public void testActivateDeactivate() throws KuraException, NoSuchFieldException {
        // test activate and deactivate sequence; a new monitor task is expected after activation that is finished later

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        TestUtil.setFieldValue(svc, "monitorTask", null);

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        doAnswer(invocation -> {
            Hashtable props = invocation.getArgumentAt(2, Hashtable.class);
            String[] topics = (String[]) props.get(EventConstants.EVENT_TOPIC);

            assertEquals(1, topics.length);
            assertEquals("org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC", topics[0]);

            return null;
        }).when(bcMock).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        NetworkConfigurationService netCfgSvcMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(netCfgSvcMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        ModemInterfaceConfigImpl nic = new ModemInterfaceConfigImpl("ppp1");
        nc.addNetInterfaceConfig(nic);

        WifiInterfaceConfigImpl monInterfaceConfig = new WifiInterfaceConfigImpl("mon0");
        nc.addNetInterfaceConfig(monInterfaceConfig);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiBuilder("wlan0")
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).build();
        nc.addNetInterfaceConfig(netInterfaceConfig);

        WifiInterfaceConfigImpl netInterfaceConfig2 = new WifiBuilder("wlan1")
                .addWifiInterfaceAddressConfig(WifiMode.UNKNOWN)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true).build();
        nc.addNetInterfaceConfig(netInterfaceConfig2);

        when(netCfgSvcMock.getNetworkConfiguration()).thenReturn(nc);

        svc.activate(ccMock);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        assertNotNull(TestUtil.getFieldValue(svc, "monitorTask"));
        assertFalse(((FutureTask) TestUtil.getFieldValue(svc, "monitorTask")).isDone());

        svc.deactivate(ccMock);

        assertTrue(((FutureTask) TestUtil.getFieldValue(svc, "monitorTask")).isDone());
        assertNull(TestUtil.getFieldValue(svc, "executor"));

    }

    @Test
    public void testGetWifiConfigNull() throws Throwable {
        // test getWifiConfig - nothing to do

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        WifiInterfaceConfigImpl wifiInterfaceConfig = null;

        WifiConfig result = (WifiConfig) TestUtil.invokePrivate(svc, "getWifiConfig", wifiInterfaceConfig);

        assertNull(result);
    }

    @Test
    public void testGetWifiConfigNoMatch() throws Throwable {
        // config not found

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder("wlan1")
                .addWifiInterfaceAddressConfig(WifiMode.UNKNOWN)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true)
                .addWifiConfig("falseDriver", WifiMode.INFRA).build();

        WifiConfig result = (WifiConfig) TestUtil.invokePrivate(svc, "getWifiConfig", wifiInterfaceConfig);

        assertNull(result);
    }

    @Test
    public void testGetWifiConfig() throws Throwable {
        // config found and returned

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder("wlan1")
                .addWifiInterfaceAddressConfig(WifiMode.UNKNOWN).addWifiConfig("testDriver", WifiMode.UNKNOWN)
                .addWifiConfig("falseDriver", WifiMode.INFRA).build();

        WifiConfig result = (WifiConfig) TestUtil.invokePrivate(svc, "getWifiConfig", wifiInterfaceConfig);

        assertNotNull(result);
        assertEquals("testDriver", result.getDriver());
    }

    @Test
    public void testRegisterUnregisterListener() throws NoSuchFieldException {
        // register listener and then unregister it

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        WifiClientMonitorListener listener = mock(WifiClientMonitorListener.class);

        svc.registerListener(listener);
        svc.registerListener(listener); // try to add a duplicate listener - shouldn't work

        List<WifiClientMonitorListener> listeners = (List<WifiClientMonitorListener>) TestUtil.getFieldValue(svc,
                "listeners");

        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertEquals(listener, listeners.get(0));

        svc.unregisterListener(listener);

        assertNotNull(listeners);
        assertTrue(listeners.isEmpty());
    }

    @Test
    public void testHandleEventWrongTopic() {
        // handdle a wrong event

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        String topic = "topic"; // wrong event topic
        Map<String, ?> properties = null;
        Event event = new Event(topic, properties);

        svc.handleEvent(event);

        // nothing should have happened
    }

    @Test
    public void testHandleEvent() throws NoSuchFieldException, InterruptedException {
        // handle the proper event

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        Event event = new NetworkConfigurationChangeEvent(properties);

        Future<?> taskMock = mock(Future.class);
        TestUtil.setFieldValue(svc, "monitorTask", taskMock);

        AtomicBoolean stopThread = new AtomicBoolean(true);
        TestUtil.setFieldValue(svc, "stopThread", stopThread);

        NetworkConfiguration newNetConfiguration = (NetworkConfiguration) TestUtil.getFieldValue(svc,
                "newNetConfiguration");

        assertNull(newNetConfiguration);

        svc.handleEvent(event);

        newNetConfiguration = (NetworkConfiguration) TestUtil.getFieldValue(svc, "newNetConfiguration");

        assertNotNull(newNetConfiguration);
    }

    @Test
    public void testHandleEventExceptionWarning() throws NoSuchFieldException, InterruptedException {
        // test event handling - warning due to an exception

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("modified.interface.names", 1); // this will cause ClassCastException
        Event event = new NetworkConfigurationChangeEvent(properties);

        Future<?> taskMock = mock(Future.class);
        TestUtil.setFieldValue(svc, "monitorTask", taskMock);

        AtomicBoolean stopThread = new AtomicBoolean(true);
        TestUtil.setFieldValue(svc, "stopThread", stopThread);

        NetworkConfiguration newNetConfiguration = (NetworkConfiguration) TestUtil.getFieldValue(svc,
                "newNetConfiguration");

        assertNull(newNetConfiguration);

        svc.handleEvent(event);

        newNetConfiguration = (NetworkConfiguration) TestUtil.getFieldValue(svc, "newNetConfiguration");

        assertNull(newNetConfiguration);
    }

    @Test
    public void testEnableInterfaceNotWifi() throws Throwable {
        // does nothing and returns

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        ModemInterfaceConfigImpl nic = new ModemInterfaceConfigImpl("ppp1");

        TestUtil.invokePrivate(svc, "enableInterface", nic);
    }

    @Test
    public void testEnableInterfaceWrongMode() throws Throwable {
        // nothing will be reconfigured if WiFi mode is wrong

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        NetworkAdminService naMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(naMock);

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder("wlan1")
                .addWifiInterfaceAddressConfig(WifiMode.ADHOC) // wrong mode
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).addDhcpConfig(true).build();

        TestUtil.invokePrivate(svc, "enableInterface", wifiInterfaceConfig);

        verify(naMock, times(0)).enableInterface(anyObject(), anyBoolean());
        verify(naMock, times(0)).manageDhcpServer(anyObject(), anyBoolean());
    }

    @Test
    public void testEnableInterfaceNoDHCPServer() throws Throwable {
        // interface will be enabled, but DHCP server will not be started

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        NetworkAdminService naMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(naMock);

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder("wlan1")
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).addDhcpConfig(false).build();

        TestUtil.invokePrivate(svc, "enableInterface", wifiInterfaceConfig);

        verify(naMock, times(1)).enableInterface("wlan1", false);
        verify(naMock, times(0)).manageDhcpServer(anyObject(), anyBoolean());
    }

    @Test
    public void testEnableInterface() throws Throwable {
        // interface will be enabled and DHCP server will be started

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        NetworkAdminService naMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(naMock);

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder("wlan1")
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true).addDhcpConfig(true).build();

        TestUtil.invokePrivate(svc, "enableInterface", wifiInterfaceConfig);

        verify(naMock, times(1)).enableInterface("wlan1", false);
        verify(naMock, times(1)).manageDhcpServer("wlan1", true);
    }

    @Test
    public void testDisableInterface() throws Throwable {
        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        NetworkAdminService naMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(naMock);

        String interfaceName = "wlan1";
        TestUtil.invokePrivate(svc, "disableInterface", interfaceName);

        verify(naMock, times(1)).disableInterface(interfaceName);
        verify(naMock, times(1)).manageDhcpServer(interfaceName, false);
    }

    @Test
    public void testUpdateInterfacesListsEmpty() throws Throwable {
        // input list is empty, so enabled and disabled interfaces remain unset

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        Set<String> interfaces = new HashSet<>();

        TestUtil.invokePrivate(svc, "updateInterfacesLists", interfaces);

        assertNull(TestUtil.getFieldValue(svc, "enabledInterfaces"));
        assertNull(TestUtil.getFieldValue(svc, "disabledInterfaces"));
        assertNull(TestUtil.getFieldValue(svc, "unmanagedInterfaces"));
    }

    @Test
    public void testUpdateInterfacesListsOnlyDisable() throws Throwable {
        // no new configuration => only disable interfaces

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();
        
        String interfaceName = "wlan1";

        Set<String> interfaces = new HashSet<>();
        interfaces.add(interfaceName);
        
        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder(interfaceName)
                .addWifiInterfaceAddressConfig(WifiMode.UNKNOWN)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, false).build();

        NetworkConfiguration nc = new NetworkConfiguration();
        nc.addNetInterfaceConfig(wifiInterfaceConfig);
        TestUtil.setFieldValue(svc, "newNetConfiguration", nc); // add to new configurations - enabled for enabling
        
        TestUtil.invokePrivate(svc, "updateInterfacesLists", interfaces);
        
        Set<String> enabled = (Set<String>) TestUtil.getFieldValue(svc, "enabledInterfaces");
        Set<String> disabled = (Set<String>) TestUtil.getFieldValue(svc, "disabledInterfaces");
        Set<String> unmanaged = (Set<String>) TestUtil.getFieldValue(svc, "unmanagedInterfaces");
        
        assertNotNull(enabled);
        assertTrue(enabled.isEmpty());
        assertNotNull(disabled);
        assertEquals(1, disabled.size());
        assertNotNull(unmanaged);
        assertTrue(unmanaged.isEmpty());
    }

    @Test
    public void testUpdateInterfacesListsEnabled() throws Throwable {
        // test finding new interfaces to enable

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        String interfaceName = "wlan1";

        Set<String> interfaces = new HashSet<>();
        interfaces.add(interfaceName);

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder(interfaceName)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).build();

        NetworkConfiguration nc = new NetworkConfiguration();
        nc.addNetInterfaceConfig(wifiInterfaceConfig);
        TestUtil.setFieldValue(svc, "newNetConfiguration", nc); // add to new configurations - enabled for enabling

        TestUtil.invokePrivate(svc, "updateInterfacesLists", interfaces);

        Set<String> enabled = (Set<String>) TestUtil.getFieldValue(svc, "enabledInterfaces");
        Set<String> disabled = (Set<String>) TestUtil.getFieldValue(svc, "disabledInterfaces");
        Set<String> unmanaged = (Set<String>) TestUtil.getFieldValue(svc, "unmanagedInterfaces");

        assertNotNull(enabled);
        assertEquals(1, enabled.size());
        assertNotNull(disabled);
        assertTrue(disabled.isEmpty());
        assertNotNull(unmanaged);
        assertTrue(unmanaged.isEmpty());
    }

    @Test
    public void testUpdateInterfacesLists() throws Throwable {
        // test combination of enabled and disabled interfaces

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        String interfaceName1 = "wlan0";
        String interfaceName2 = "wlan1";
        String interfaceName3 = "wlan2";
        
        Set<String> interfaces = new HashSet<>();
        interfaces.add(interfaceName1);
        interfaces.add(interfaceName2);
        interfaces.add(interfaceName3);

        WifiInterfaceConfigImpl wifiInterfaceConfig1 = new WifiBuilder(interfaceName1)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).build(); // to be enabled
        WifiInterfaceConfigImpl wifiInterfaceConfig2 = new WifiBuilder(interfaceName2)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true).build(); // to be disabled
        WifiInterfaceConfigImpl wifiInterfaceConfig3 = new WifiBuilder(interfaceName3)
                .addWifiInterfaceAddressConfig(WifiMode.UNKNOWN)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusUnmanaged, true).build(); // to be unmanaged

        NetworkConfiguration nc = new NetworkConfiguration();
        nc.addNetInterfaceConfig(wifiInterfaceConfig1);
        nc.addNetInterfaceConfig(wifiInterfaceConfig2);
        nc.addNetInterfaceConfig(wifiInterfaceConfig3);
        TestUtil.setFieldValue(svc, "newNetConfiguration", nc);

        TestUtil.invokePrivate(svc, "updateInterfacesLists", interfaces);

        Set<String> enabled = (Set<String>) TestUtil.getFieldValue(svc, "enabledInterfaces");
        Set<String> disabled = (Set<String>) TestUtil.getFieldValue(svc, "disabledInterfaces");
        Set<String> unmanaged = (Set<String>) TestUtil.getFieldValue(svc, "unmanagedInterfaces");

        assertNotNull(enabled);
        assertEquals(1, enabled.size());
        assertEquals(interfaceName1, enabled.iterator().next());
        assertNotNull(disabled);
        assertEquals(1, disabled.size());
        assertEquals(interfaceName2, disabled.iterator().next());
        assertNotNull(unmanaged);
        assertEquals(1, unmanaged.size());
        assertEquals(interfaceName3, unmanaged.iterator().next());
    }

    @Test
    public void testGetReconfiguredWifiInterfaces() throws Throwable {
        // detect changes and prepare the lists - 1 enabled and 1 disabled interface

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                NetInterfaceType type = NetInterfaceType.UNKNOWN;
                if ("eth0".equals(interfaceName)) {
                    type = NetInterfaceType.ETHERNET;
                } else if (interfaceName.startsWith("wlan")) {
                    type = NetInterfaceType.WIFI;
                } else if ("mon0".equals(interfaceName)) {
                    type = NetInterfaceType.WIFI;
                }

                return type;
            }
        };

        String interfaceName = "wlan0";
        String interfaceName2 = "mon0";
        String interfaceName3 = "wlan1";
        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder(interfaceName)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                // enabled -> to be disabled
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).build();
        WifiInterfaceConfigImpl wifiInterfaceConfig2 = new WifiBuilder(interfaceName2) // to be skipped
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true).build();

        NetworkConfiguration nc = new NetworkConfiguration();
        nc.addNetInterfaceConfig(wifiInterfaceConfig);
        nc.addNetInterfaceConfig(wifiInterfaceConfig2);
        TestUtil.setFieldValue(svc, "currentNetworkConfiguration", nc);

        WifiInterfaceConfigImpl wifiInterfaceConfig3 = new WifiBuilder(interfaceName)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true).build(); // to be disabled
        WifiInterfaceConfigImpl wifiInterfaceConfig4 = new WifiBuilder(interfaceName3)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true).build(); // new -> to be enabled

        NetworkConfiguration nc2 = new NetworkConfiguration();
        nc2.addNetInterfaceConfig(wifiInterfaceConfig3);
        nc2.addNetInterfaceConfig(wifiInterfaceConfig4);
        TestUtil.setFieldValue(svc, "newNetConfiguration", nc2);

        NetworkService nsMock = mock(NetworkService.class);
        svc.setNetworkService(nsMock);

        List<String> list = new ArrayList<>();
        list.add("eth0");
        list.add(interfaceName);
        list.add(interfaceName2);
        list.add(interfaceName3);
        when(nsMock.getAllNetworkInterfaceNames()).thenReturn(list);

        Collection<String> result = (Collection<String>) TestUtil.invokePrivate(svc, "getReconfiguredWifiInterfaces");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(interfaceName));
        assertTrue(result.contains(interfaceName3));

        Set<String> enabled = (Set<String>) TestUtil.getFieldValue(svc, "enabledInterfaces");
        Set<String> disabled = (Set<String>) TestUtil.getFieldValue(svc, "disabledInterfaces");

        assertNotNull(enabled);
        assertEquals(1, enabled.size());
        assertEquals(interfaceName3, enabled.iterator().next());
        assertNotNull(disabled);
        assertEquals(1, disabled.size());
        assertEquals(interfaceName, disabled.iterator().next());
    }

    @Test
    public void testCheckStatusChangeNulls() throws Throwable {
        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        Map<String, InterfaceState> oldStatuses = null;
        Map<String, InterfaceState> newStatuses = null;

        TestUtil.invokePrivate(svc, "checkStatusChange", oldStatuses, newStatuses);

        // doesn't throw exception
    }

    @Test
    public void testCheckStatusChangeNulls2() throws Throwable {
        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        Map<String, InterfaceState> oldStatuses = null;
        Map<String, InterfaceState> newStatuses = new HashMap<>();

        TestUtil.invokePrivate(svc, "checkStatusChange", oldStatuses, newStatuses);

        // doesn't throw exception
    }

    @Test
    public void testCheckStatusChange() throws Throwable {
        // test that proper events are sent with appropriate contents

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        String wlan0 = "wlan0";
        String wlan1 = "wlan1";
        String wlan2 = "wlan2";
        String wlan3 = "wlan3";

        InterfaceState wlan3OldState = new InterfaceState(wlan3, true, true, IPAddress.parseHostAddress("10.10.0.3"));
        InterfaceState wlan3NewState = new InterfaceState(wlan3, true, true, IPAddress.parseHostAddress("10.10.0.4"));

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        AtomicInteger visited = new AtomicInteger(0);
        doAnswer(invocation -> {
            NetworkStatusChangeEvent event = invocation.getArgumentAt(0, NetworkStatusChangeEvent.class);

            final String interfaceName = event.getInterfaceState().getName();
            assertTrue(wlan0.equals(interfaceName) || wlan2.equals(interfaceName) || wlan3.equals(interfaceName));

            // check that the correct wlan3 event was posted
            if (wlan3.equals(interfaceName)) {
                assertEquals(wlan3NewState, event.getInterfaceState());
            }

            // managed to get this far => increment number of successful visits
            visited.getAndIncrement();

            return null;
        }).when(eaMock).postEvent(anyObject());

        Map<String, InterfaceState> oldStatuses = new HashMap<>();
        oldStatuses.put(wlan1, new InterfaceState(wlan1, true, true, IPAddress.parseHostAddress("10.10.0.1")));
        oldStatuses.put(wlan2, new InterfaceState(wlan2, true, true, IPAddress.parseHostAddress("10.10.0.2"))); // disabled
        oldStatuses.put(wlan3, wlan3OldState);

        Map<String, InterfaceState> newStatuses = new HashMap<>();
        newStatuses.put(wlan0, new InterfaceState(wlan0, true, true, IPAddress.parseHostAddress("10.10.0.0"))); // enabled
        newStatuses.put(wlan1, new InterfaceState(wlan1, true, true, IPAddress.parseHostAddress("10.10.0.1")));
        newStatuses.put(wlan3, wlan3NewState); // modified

        TestUtil.invokePrivate(svc, "checkStatusChange", oldStatuses, newStatuses);

        verify(eaMock, times(3)).postEvent(anyObject());

        assertEquals(3, visited.get());
    }

    @Test
    public void testIsAccessPointAvailableNoScanTool() throws Throwable {
        // what happens if scan tool is not available? AP is not available

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected IScanTool getScanTool(String interfaceName) throws KuraException {
                return null;
            }
        };

        boolean available = (boolean) TestUtil.invokePrivate(svc, "isAccessPointAvailable", "wlan0", "ssid");

        assertFalse(available);
    }

    @Test
    public void testIsAccessPointAvailableNoStrength() throws Throwable {
        // what happens if strength is 0? AP is not available

        String ssid = "mySSID";

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, 0);

        boolean available = (boolean) TestUtil.invokePrivate(svc, "isAccessPointAvailable", "wlan0", ssid);

        assertFalse(available);
    }

    protected WifiMonitorServiceImpl getServiceWithScanTool(String ssid, int strength) {
        return getServiceWithScanTool(ssid, strength, null);
    }

    protected WifiMonitorServiceImpl getServiceWithScanTool(String ssid, int strength, LinkTool linkTool) {
        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected LinkTool getLinkTool(String interfaceName) throws KuraException {
                if (linkTool != null) {
                    return linkTool;
                }

                return super.getLinkTool(interfaceName);
            }

            @Override
            protected IScanTool getScanTool(String interfaceName) throws KuraException {
                return new IScanTool() {

                    @Override
                    public List<WifiAccessPoint> scan() throws KuraException {
                        List<WifiAccessPoint> list = new ArrayList<>();

                        WifiAccessPointImpl ap = new WifiAccessPointImpl(ssid);
                        ap.setStrength(strength);
                        list.add(ap);

                        return list;
                    }
                };
            }
        };
        return svc;
    }

    @Test
    public void testIsAccessPointAvailableNegativeStrength() throws Throwable {
        // negative strength also makes AP available

        String ssid = "mySSID";

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, -5);

        boolean available = (boolean) TestUtil.invokePrivate(svc, "isAccessPointAvailable", "wlan0", ssid);

        assertTrue(available);
    }

    @Test
    public void testIsAccessPointAvailableWrongSSID() throws Throwable {
        // if SSID is not correct, AP is not available

        String ssid = "mySSID";

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, 5);

        boolean available = (boolean) TestUtil.invokePrivate(svc, "isAccessPointAvailable", "wlan0", "ssid");

        assertFalse(available);
    }

    @Test
    public void testIsAccessPointAvailable() throws Throwable {
        // positive strength => AP is available

        String ssid = "mySSID";

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, 5);

        boolean available = (boolean) TestUtil.invokePrivate(svc, "isAccessPointAvailable", "wlan0", ssid);

        assertTrue(available);
    }

    @Test
    public void testGetSignalLevelNoInterface() throws NoSuchFieldException, UnknownHostException, KuraException {
        // test with interface status not filled for the selected interface

        String wlan1 = "wlan1";
        String ssid = "mySSID";

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        Map<String, InterfaceState> stats = new HashMap<>();
        TestUtil.setFieldValue(svc, "interfaceStatuses", stats);

        int level = svc.getSignalLevel(wlan1, ssid);

        assertEquals(0, level);
    }

    @Test
    public void testGetSignalLevelLink() throws NoSuchFieldException, UnknownHostException, KuraException {
        // signal level != 0 => use link tool strength

        String wlan1 = "wlan1";
        String ssid = "mySSID";

        LinkTool ltMock = mock(LinkTool.class);

        when(ltMock.get()).thenReturn(true);
        when(ltMock.isLinkDetected()).thenReturn(true);
        when(ltMock.getSignal()).thenReturn(5);

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, 0, ltMock);

        Map<String, InterfaceState> stats = new HashMap<>();
        stats.put(wlan1, new InterfaceState(wlan1, true, true, IPAddress.parseHostAddress("10.10.0.1")));

        TestUtil.setFieldValue(svc, "interfaceStatuses", stats);

        int level = svc.getSignalLevel(wlan1, ssid);

        verify(ltMock, times(1)).getSignal();

        assertEquals(5, level);
    }

    @Test
    public void testGetSignalLevelNegativeStrength() throws NoSuchFieldException, UnknownHostException, KuraException {
        // strength of the selected interface is negative => signal level remains 0

        String wlan1 = "wlan1";
        String ssid = "mySSID";

        LinkTool ltMock = mock(LinkTool.class);

        when(ltMock.get()).thenReturn(true);
        when(ltMock.isLinkDetected()).thenReturn(true);
        when(ltMock.getSignal()).thenReturn(0);

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, -5, ltMock);

        Map<String, InterfaceState> stats = new HashMap<>();
        stats.put(wlan1, new InterfaceState(wlan1, true, true, IPAddress.parseHostAddress("10.10.0.1")));

        TestUtil.setFieldValue(svc, "interfaceStatuses", stats);

        int level = svc.getSignalLevel(wlan1, ssid);

        verify(ltMock, times(1)).getSignal();

        assertEquals(0, level);
    }

    @Test
    public void testGetSignalLevel() throws NoSuchFieldException, UnknownHostException, KuraException {
        // XXX: LinkTool and ScanTool return differently signed strengths - intentionally?
        // positive signal level => if link tool returns 0, strength is inverted

        String wlan1 = "wlan1";
        String ssid = "mySSID";

        LinkTool ltMock = mock(LinkTool.class);

        when(ltMock.get()).thenReturn(true);
        when(ltMock.isLinkDetected()).thenReturn(true);
        when(ltMock.getSignal()).thenReturn(0);

        WifiMonitorServiceImpl svc = getServiceWithScanTool(ssid, 5, ltMock);

        Map<String, InterfaceState> stats = new HashMap<>();
        stats.put(wlan1, new InterfaceState(wlan1, true, true, IPAddress.parseHostAddress("10.10.0.1")));

        TestUtil.setFieldValue(svc, "interfaceStatuses", stats);

        int level = svc.getSignalLevel(wlan1, ssid);

        verify(ltMock, times(1)).getSignal();

        assertEquals(-5, level);
    }

    @Test
    public void testIsAccessPointReachable() throws Throwable {
        // test if a host is reachable, but the final call is not mocked

        String interfaceName = "wlan1";

        RouteService rsMock = mock(RouteService.class);

        IP4Address destination = (IP4Address) IP4Address.parseHostAddress("10.10.2.0");
        IP4Address gateway = (IP4Address) IP4Address.parseHostAddress("10.10.0.200");
        IP4Address netmask = (IP4Address) IP4Address.parseHostAddress("255.255.255.0");
        int metric = 1;
        RouteConfig route = new RouteConfigIP4(destination, gateway, netmask, interfaceName, metric);
        when(rsMock.getDefaultRoute(interfaceName)).thenReturn(route);

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected RouteService getRouteService() {
                return rsMock;
            }
        };

        int timeout = 100;

        boolean result = (boolean) TestUtil.invokePrivate(svc, "isAccessPointReachable", interfaceName, timeout);

        // will likely be false, but let's not presume it - concrete implementation is called
        // we should be satisfied that no exception was thrown
    }

    @Test
    public void testMonitor() throws Throwable {
        // only makes part of the method traversal easier - can hardly avoid native calls... check nothing, in the end

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                NetInterfaceType type = NetInterfaceType.UNKNOWN;
                if ("eth0".equals(interfaceName)) {
                    type = NetInterfaceType.ETHERNET;
                } else if (interfaceName.startsWith("wlan")) {
                    type = NetInterfaceType.WIFI;
                } else if ("mon0".equals(interfaceName)) {
                    type = NetInterfaceType.WIFI;
                }

                return type;
            }
        };

        String interfaceName = "wlan3";

        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiBuilder(interfaceName)
                .addWifiInterfaceAddressConfig(WifiMode.INFRA)
                .addNetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true)
                .addWifiConfig("testDdriver", WifiMode.INFRA) // don't add this - calls a native binary
                .build();

        NetworkConfiguration nc = new NetworkConfiguration();
        nc.addNetInterfaceConfig(wifiInterfaceConfig);
        TestUtil.setFieldValue(svc, "newNetConfiguration", nc);

        NetworkService nsMock = mock(NetworkService.class);
        svc.setNetworkService(nsMock);

        List<String> list = new ArrayList<>();
        list.add("eth4");
        list.add(interfaceName);
        when(nsMock.getAllNetworkInterfaceNames()).thenReturn(list);

        NetworkAdminService naMock = mock(NetworkAdminService.class);
        svc.setNetworkAdminService(naMock);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        TestUtil.invokePrivate(svc, "monitor");

        verify(nsMock, times(1)).getAllNetworkInterfaceNames();
        verify(naMock, times(1)).disableInterface(interfaceName);
        verify(eaMock, times(1)).postEvent(anyObject());
    }

    @Test
    public void testIsWifiReady() throws Throwable {
        // we expect the device to be on

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected boolean isWifiDeviceOn(String interfaceName) {
                return true;
            }
        };

        String interfaceName = "wlan0";
        boolean expected = true;
        int timeout = 100;

        boolean ready = (boolean) TestUtil.invokePrivate(svc, "isWifiDeviceReady", interfaceName, expected, timeout);

        assertTrue(ready);
    }

    @Test
    public void testIsWifiReadyDiff() throws Throwable {
        // we expect the device to be off

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected boolean isWifiDeviceOn(String interfaceName) {
                return true;
            }
        };

        String interfaceName = "wlan0";
        boolean expected = false;
        int timeout = 1;

        boolean ready = (boolean) TestUtil.invokePrivate(svc, "isWifiDeviceReady", interfaceName, expected, timeout);

        assertFalse(ready);
    }

    @Test
    public void testIsWifiReadyDiffMulti() throws Throwable {
        // we expect the device to be off

        AtomicInteger cnt = new AtomicInteger(0);

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl() {

            @Override
            protected boolean isWifiDeviceOn(String interfaceName) {
                if (cnt.getAndIncrement() < 1) {
                    return true;
                }
                return false;
            }
        };

        String interfaceName = "wlan0";
        boolean expected = false;
        int timeout = 2;

        boolean ready = (boolean) TestUtil.invokePrivate(svc, "isWifiDeviceReady", interfaceName, expected, timeout);

        assertTrue(ready);
        assertEquals(2, cnt.get());
    }

}

class WifiBuilder {

    private WifiInterfaceConfigImpl wifiInterfaceConfig;
    private List<WifiInterfaceAddressConfig> interfaceAddressConfigs;
    private WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig;
    private List<NetConfig> netConfigs;

    public WifiBuilder(String interfaceName) {
        wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
    }

    public WifiInterfaceConfigImpl build() {
        return wifiInterfaceConfig;
    }

    public WifiBuilder addWifiInterfaceAddressConfig() {
        return addWifiInterfaceAddressConfig(null);
    }

    public WifiBuilder addWifiInterfaceAddressConfig(WifiMode mode) {
        if (interfaceAddressConfigs == null) {
            interfaceAddressConfigs = new ArrayList<>();
            wifiInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        }

        wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        if (mode != null) {
            wifiInterfaceAddressConfig.setMode(mode);
        }

        netConfigs = new ArrayList<>();
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);

        return this;
    }

    public WifiBuilder addDhcpConfig(boolean enabled) throws KuraException {
        if (wifiInterfaceAddressConfig == null) {
            addWifiInterfaceAddressConfig();
        }

        DhcpServerCfg svrCfg = new DhcpServerCfg(wifiInterfaceConfig.getName(), enabled, 900, 1000, true);
        try {
            IP4Address subnet = (IP4Address) IPAddress.parseHostAddress("10.10.0.0");
            IP4Address subnetMask = (IP4Address) IPAddress.parseHostAddress("255.255.255.0");
            IP4Address routerAddress = (IP4Address) IPAddress.parseHostAddress("10.10.0.250");
            IP4Address rangeStart = (IP4Address) IPAddress.parseHostAddress("10.10.0.10");
            IP4Address rangeEnd = (IP4Address) IPAddress.parseHostAddress("10.10.0.15");
            List<IP4Address> dnsServers = new ArrayList<>();
            IP4Address dnsAddress = (IP4Address) IPAddress.parseHostAddress("10.10.0.254");
            dnsServers.add(dnsAddress);
            DhcpServerCfgIP4 svrCfg4 = new DhcpServerCfgIP4(subnet, subnetMask, (short) 24, routerAddress, rangeStart,
                    rangeEnd, dnsServers);

            DhcpServerConfig4 netConfig = new DhcpServerConfigIP4(svrCfg, svrCfg4);
            netConfigs.add(netConfig);
        } catch (UnknownHostException e) {
        }

        return this;
    }

    public WifiBuilder addNetConfigIP4(NetInterfaceStatus status, boolean autoConnect) {
        if (wifiInterfaceAddressConfig == null) {
            addWifiInterfaceAddressConfig();
        }

        NetConfigIP4 netConfig = new NetConfigIP4(status, autoConnect);
        netConfigs.add(netConfig);

        return this;
    }

    public WifiBuilder addWifiConfig() {
        return addWifiConfig(null, null);
    }

    public WifiBuilder addWifiConfig(String driver, WifiMode mode) {
        if (wifiInterfaceAddressConfig == null) {
            addWifiInterfaceAddressConfig();
        }

        WifiConfig netConfig = new WifiConfig();
        netConfigs.add(netConfig);

        if (driver != null) {
            netConfig.setDriver(driver);
        }

        if (mode != null) {
            netConfig.setMode(mode);
        }

        return this;
    }

    public NetConfig getNetConfig() {
        if (wifiInterfaceAddressConfig == null) {
            return null;
        }

        NetConfig netConfig = null;
        if (!netConfigs.isEmpty()) {
            netConfig = netConfigs.get(netConfigs.size() - 1);
        }

        return netConfig;
    }

}
