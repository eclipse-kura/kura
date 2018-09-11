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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyLong;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.IModemLinkService;
import org.eclipse.kura.net.admin.modem.PppState;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.CellularModem.SerialPortType;
import org.eclipse.kura.net.modem.ModemAddedEvent;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemGpsDisabledEvent;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.modem.ModemMonitorListener;
import org.eclipse.kura.net.modem.ModemRemovedEvent;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class ModemMonitorServiceImplTest {

    @Test
    public void testActivateNoModems() throws NoSuchFieldException, InterruptedException, ExecutionException {
        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        svc.activate();
        svc.sync();

        assertTrue((boolean) TestUtil.getFieldValue(svc, "serviceActivated"));
    }

    @Test
    public void testActivateWithModemNoFramework()
            throws NoSuchFieldException, KuraException, InterruptedException, ExecutionException {
        // enters creation of a modem, but fails early

        // needed for ModemDriver:
        System.setProperty("target.device", "noKnownDevice");

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        NetworkConfigurationService netConfigServiceMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(netConfigServiceMock);

        NetworkService networkServiceMock = mock(NetworkService.class);
        svc.setNetworkService(networkServiceMock);

        List<NetInterface<? extends NetInterfaceAddress>> infcs = new ArrayList<>();
        ModemInterfaceImpl modemIface = new ModemInterfaceConfigImpl("ppp1");
        UsbModemDevice modemDevice = mock(UsbModemDevice.class);
        when(modemDevice.getProductId()).thenReturn("0021");
        when(modemDevice.getProductName()).thenReturn("HE910-DG");
        when(modemDevice.getVendorId()).thenReturn("1bc7");
        modemIface.setModemDevice(modemDevice);
        infcs.add(modemIface);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(infcs);

        svc.activate();
        svc.sync();

        assertTrue((boolean) TestUtil.getFieldValue(svc, "serviceActivated"));
        assertTrue(((Map) TestUtil.getFieldValue(svc, "modems")).isEmpty());
    }

    @Test
    public void testActivateWithUsbModem()
            throws Throwable {
        // enters creation of a modem, and ends up activating the GPS

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl() {

            @Override
            protected Class<? extends CellularModemFactory> getModemFactoryClass(ModemDevice modemDevice) {
                return TestModemFactory.class;
            }
            
            @Override
            IModemLinkService getPppService(String interfaceName, String port) {
                final IModemLinkService modemLinkServiceMock = mock(IModemLinkService.class);
                try {
                    when(modemLinkServiceMock.getPppState()).thenReturn(PppState.CONNECTED);
                } catch (KuraException e) {
                    //no need
                }
                return modemLinkServiceMock;
            }
        };

        // for obtaining network configuration
        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        svc.setNetworkConfigurationService(ncsMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl("ppp1");
        nc.addNetInterfaceConfig(netInterfaceConfig);

        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        ModemConfig modemConfig = new ModemConfig();
        modemConfig.setGpsEnabled(true);
        netConfigs.add(modemConfig);
        // for EvdoCellularModem configuration/provisioning:
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        netConfigs.add(netConfig);
        modemInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(modemInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        when(ncsMock.getNetworkConfiguration()).thenReturn(nc);
        svc.setNetworkConfigurationService(ncsMock);

        // for obtaining the available network interfaces, ppp port
        NetworkService networkServiceMock = mock(NetworkService.class);
        svc.setNetworkService(networkServiceMock);

        List<NetInterface<? extends NetInterfaceAddress>> infcs = new ArrayList<>();
        ModemInterfaceImpl modemIface = new ModemInterfaceConfigImpl("ppp1");
        UsbModemDevice modemDevice = mock(UsbModemDevice.class);
        when(modemDevice.getUsbPort()).thenReturn("usb0");
        modemIface.setModemDevice(modemDevice);
        infcs.add(modemIface);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(infcs);

        when(networkServiceMock.getModemPppPort(modemDevice)).thenReturn("ppp1");

        // for obtaining the platform
        SystemService ssMock = mock(SystemService.class);
        svc.setSystemService(ssMock);

        when(ssMock.getPlatform()).thenReturn("testPlatform");

        // for ModemReadyEvent
        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/net/modem/READY", event.getTopic());
            assertEquals("imei", event.getProperty("IMEI"));
            assertEquals("phoneno", event.getProperty("IMSI"));
            assertEquals("cardid", event.getProperty("ICCID"));
            assertEquals("2", event.getProperty("RSSI"));

            return null;
        }).doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/net/modem/gps/DISABLED", event.getTopic());

            return null;
        }).doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/net/modem/gps/ENABLED", event.getTopic());
            assertEquals("gps0", event.getProperty("port"));
            assertEquals(56400, event.getProperty("baudRate"));
            assertEquals(8, event.getProperty("bitsPerWord"));
            assertEquals(1, event.getProperty("stopBits"));
            assertEquals(0, event.getProperty("parity"));

            return null;
        }).doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/net/modem/gps/ENABLED", event.getTopic());
            assertEquals("gps0", event.getProperty("port"));
            assertEquals(56400, event.getProperty("baudRate"));
            assertEquals(8, event.getProperty("bitsPerWord"));
            assertEquals(1, event.getProperty("stopBits"));
            assertEquals(0, event.getProperty("parity"));

            return null;
        }).doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/net/admin/event/NETWORK_EVENT_STATUS_CHANGE_TOPIC", event.getTopic());

            return null;
        }).when(eaMock).postEvent(anyObject());

        // modem mock
        setModemMock(modemDevice);

        
        TestUtil.setFieldValue(svc, "task", mock(Future.class));

        svc.activate();
        svc.sync();

        TestUtil.invokePrivate(svc, "monitor");
        
        verify(eaMock, times(5)).postEvent(anyObject());

        assertTrue((boolean) TestUtil.getFieldValue(svc, "serviceActivated"));

        Collection<CellularModem> modems = svc.getAllModemServices();
        assertEquals(1, modems.size());
        CellularModem modem = (CellularModem) modems.iterator().next();
        assertNotNull(modem);
        assertEquals("imei", modem.getSerialNumber());

        verify(modem, times(1)).enableGps();
    }

    @SuppressWarnings("unchecked")
    private void setModemMock(final ModemDevice modemDevice) throws KuraException, URISyntaxException {
        TestModemFactory modemFactory = TestModemFactory.getInstance();
        EvdoCellularModem modem = mock(EvdoCellularModem.class);
        modemFactory.setModem(modem);

        when(modem.getSerialNumber()).thenReturn("imei");
        when(modem.getMobileSubscriberIdentity()).thenReturn("phoneno");
        when(modem.getIntegratedCirquitCardId()).thenReturn("cardid");
        when(modem.getSignalStrength()).thenReturn(2);
        when(modem.isGpsSupported()).thenReturn(true);
        // 1. for start of disabling, 2. check after being disabled, 3. log it, 4. another check before enabling it
        when(modem.isGpsEnabled()).thenReturn(true).thenReturn(false).thenReturn(false).thenReturn(false);
        when(modem.getModel()).thenReturn("testModem");
        when(modem.getAtPort()).thenReturn("usb0");
        when(modem.isPortReachable("usb0")).thenReturn(false).thenReturn(true); // first not, then OK
        when(modem.getGpsPort()).thenReturn("gps0");
        when(modem.getModemDevice()).thenReturn(modemDevice);

        final AtomicReference<List<NetConfig>> config = new AtomicReference<>();
        doAnswer(invocation -> {
            config.set(invocation.getArgumentAt(0, List.class));
            return ((Void) null);
        }).when(modem).setConfiguration(anyObject());
        when(modem.getConfiguration()).thenAnswer(invocation -> config.get());

        CommURI commuri = CommURI.parseString(
                "comm:gps0;baudrate=56400;databits=8;stopbits=1;parity=0;flowcontrol=1;timeout=10;receivetimeout=5");
        when(modem.getSerialConnectionProperties(SerialPortType.GPSPORT)).thenReturn(commuri);
    }

    @Test
    public void testDeactivate() throws Exception {
        // test deactivation - stop the task and shutdown executor

        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl() {

            ScheduledExecutorService createExecutor() {
                return executor;
            }
        };

        Future task = mock(Future.class);
        TestUtil.setFieldValue(svc, "task", task);

        when(task.isDone()).thenReturn(false);

        // produce a warning log...
        when(executor.awaitTermination(anyLong(), eq(TimeUnit.SECONDS))).thenThrow(new InterruptedException("test"));

        svc.deactivate();

        verify(task, times(1)).cancel(false);
        verify(executor, times(1)).shutdownNow();
    }

    @Test
    public void testHandleEventWrongTopic() {
        // handle a wrong type of event

        WifiMonitorServiceImpl svc = new WifiMonitorServiceImpl();

        String topic = "topic"; // wrong event topic
        Map<String, ?> properties = null;
        Event event = new Event(topic, properties);

        svc.handleEvent(event);

        // nothing should have happened
    }

    @Test
    public void testHandleEventNetConfig() throws NoSuchFieldException, InterruptedException {
        // handle the NetworkConfigurationChangeEvent event

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        Event event = new NetworkConfigurationChangeEvent(properties);

        svc.handleEvent(event);
    }

    @Test
    public void testHandleEventModemAdded() throws NoSuchFieldException, InterruptedException {
        // handle the ModemAddedEvent event

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        TestUtil.setFieldValue(svc, "serviceActivated", true);

        Event event = new ModemAddedEvent(null);

        svc.handleEvent(event);
    }

    @Test
    public void testHandleEventModemRemoved() throws NoSuchFieldException, InterruptedException, ExecutionException {
        // handle the ModemAddedEvent event

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        Map<String, CellularModem> modems = new HashMap<>();
        String port = "usb0";
        modems.put(port, null);

        TestUtil.setFieldValue(svc, "modems", modems);

        Map<String, Object> properties = new HashMap<>();
        properties.put("usb.port", port);
        Event event = new ModemRemovedEvent(properties);

        svc.handleEvent(event);
        svc.sync();

        assertTrue(modems.isEmpty());
    }

    @Test
    public void testRegisterUnregisterListener() throws NoSuchFieldException {
        // register listener and then unregister it

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        ModemMonitorListener listener = mock(ModemMonitorListener.class);

        svc.registerListener(listener);
        svc.registerListener(listener); // try to add a duplicate listener - shouldn't work

        Collection<ModemMonitorListener> listeners = (Collection<ModemMonitorListener>) TestUtil.getFieldValue(svc,
                "listeners");

        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertEquals(listener, listeners.iterator().next());

        svc.unregisterListener(listener);

        assertNotNull(listeners);
        assertTrue(listeners.isEmpty());
    }

    @Test
    public void testGetModemConfigNotFound() throws Throwable {
        // test modem configuration retrieval

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(nc);

        NetConfig result = (NetConfig) TestUtil.invokePrivate(svc, "getModemConfig", netConfigs);

        assertNotNull(result);
    }

    @Test
    public void testGetModemConfig() throws Throwable {
        // test modem configuration retrieval

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig nc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(nc);
        NetConfig config = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        netConfigs.add(config);

        NetConfig result = (NetConfig) TestUtil.invokePrivate(svc, "getModemConfig", netConfigs);

        assertEquals(config, result);
    }

    @Test
    public void testGetNetConfigIp4NotFound() throws Throwable {
        // test net configuration retrieval

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        netConfigs.add(nc);

        NetConfigIP4 result = (NetConfigIP4) TestUtil.invokePrivate(svc, "getNetConfigIp4", netConfigs);
        assertNotNull(result);
        assertEquals(result.getStatus(), NetInterfaceStatus.netIPv4StatusUnknown);
    }

    @Test
    public void testGetNetConfigIp4() throws Throwable {
        // test net configuration retrieval

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        NetConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        netConfigs.add(nc);

        NetConfig result = (NetConfig) TestUtil.invokePrivate(svc, "getNetConfigIp4", netConfigs);

        assertEquals(config, result);
    }

    @Test
    public void testGetInterfaceNumberEmpty() throws Throwable {
        // test interface number retrieval from configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();

        int result = (int) TestUtil.invokePrivate(svc, "getInterfaceNumber", new Class[] { List.class }, netConfigs);

        assertEquals(-1, result);
    }

    @Test
    public void testGetInterfaceNumber() throws Throwable {
        // test interface number retrieval from configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setPppNumber(2);
        netConfigs.add(nc);

        int result = (int) TestUtil.invokePrivate(svc, "getInterfaceNumber", new Class[] { List.class }, netConfigs);

        assertEquals(2, result);
    }

    @Test
    public void testSetInterfaceNumber() throws Throwable {
        // test interface number setting in configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setPppNumber(0);
        netConfigs.add(nc);

        assertEquals(0, nc.getPppNumber());

        TestUtil.invokePrivate(svc, "setInterfaceNumber", "ppp2", netConfigs);

        assertEquals(2, nc.getPppNumber());
    }

    @Test
    public void testGetNetInterfaceStatusNotFound() throws Throwable {
        // test interface status retrieval from configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setPppNumber(0);
        netConfigs.add(nc);

        NetInterfaceStatus result = (NetInterfaceStatus) TestUtil.invokePrivate(svc, "getNetInterfaceStatus",
                netConfigs);

        assertEquals(NetInterfaceStatus.netIPv4StatusUnknown, result);
    }

    @Test
    public void testGetNetInterfaceStatus() throws Throwable {
        // test interface status retrieval from configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setPppNumber(0);
        netConfigs.add(nc);

        NetInterfaceStatus result = (NetInterfaceStatus) TestUtil.invokePrivate(svc, "getNetInterfaceStatus",
                netConfigs);

        assertEquals(NetInterfaceStatus.netIPv4StatusEnabledLAN, result);
    }

    @Test
    public void testSetNetInterfaceStatus() throws Throwable {
        // test interface status setting in configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setPppNumber(0);
        netConfigs.add(nc);

        assertEquals(NetInterfaceStatus.netIPv4StatusEnabledLAN, config.getStatus());

        TestUtil.invokePrivate(svc, "setNetInterfaceStatus", NetInterfaceStatus.netIPv4StatusEnabledWAN, netConfigs);

        assertEquals(NetInterfaceStatus.netIPv4StatusEnabledWAN, config.getStatus());
    }

    @Test
    public void testGetModemResetTimeoutMsecNull() throws Throwable {
        // test retrieval of reset timeout from configuration where interface name is null

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setResetTimeout(2);
        netConfigs.add(nc);

        long result = (long) TestUtil.invokePrivate(svc, "getModemResetTimeoutMsec", (String) null, netConfigs);

        assertEquals(0L, result);
    }

    @Test
    public void testGetModemResetTimeoutMsec() throws Throwable {
        // test retrieval of reset timeout from configuration

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(config);
        ModemConfig nc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        nc.setResetTimeout(2);
        netConfigs.add(nc);

        long result = (long) TestUtil.invokePrivate(svc, "getModemResetTimeoutMsec", "ppp0", netConfigs);

        assertEquals(120000, result);
    }

    @Test
    public void testCheckStatusChangeNulls() throws Throwable {
        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        Map<String, InterfaceState> oldStatuses = null;
        Map<String, InterfaceState> newStatuses = null;

        TestUtil.invokePrivate(svc, "checkStatusChange", oldStatuses, newStatuses);

        // doesn't throw exception
    }

    @Test
    public void testCheckStatusChangeNulls2() throws Throwable {
        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        Map<String, InterfaceState> oldStatuses = null;
        Map<String, InterfaceState> newStatuses = new HashMap<>();

        TestUtil.invokePrivate(svc, "checkStatusChange", oldStatuses, newStatuses);

        // doesn't throw exception
    }

    @Test
    public void testCheckStatusChange() throws Throwable {
        // check that the appropriate events are sent when status changes are made

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        String ppp0 = "wlan0";
        String ppp1 = "wlan1";
        String ppp2 = "wlan2";
        String ppp3 = "wlan3";

        InterfaceState ppp3OldState = new InterfaceState(ppp3, true, true, IPAddress.parseHostAddress("10.10.0.3"));
        InterfaceState ppp3NewState = new InterfaceState(ppp3, true, true, IPAddress.parseHostAddress("10.10.0.4"));

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        AtomicInteger visited = new AtomicInteger(0);
        doAnswer(invocation -> {
            NetworkStatusChangeEvent event = invocation.getArgumentAt(0, NetworkStatusChangeEvent.class);

            final String interfaceName = event.getInterfaceState().getName();
            assertTrue(ppp0.equals(interfaceName) || ppp2.equals(interfaceName) || ppp3.equals(interfaceName));

            // check that the correct ppp3 event was posted
            if (ppp3.equals(interfaceName)) {
                assertEquals(ppp3NewState, event.getInterfaceState());
            }

            // managed to get this far => increment number of successful visits
            visited.getAndIncrement();

            return null;
        }).when(eaMock).postEvent(anyObject());

        Map<String, InterfaceState> oldStatuses = new HashMap<>();
        oldStatuses.put(ppp1, new InterfaceState(ppp1, true, true, IPAddress.parseHostAddress("10.10.0.1")));
        oldStatuses.put(ppp2, new InterfaceState(ppp2, true, true, IPAddress.parseHostAddress("10.10.0.2"))); // disabled
        oldStatuses.put(ppp3, ppp3OldState);

        Map<String, InterfaceState> newStatuses = new HashMap<>();
        newStatuses.put(ppp0, new InterfaceState(ppp0, true, true, IPAddress.parseHostAddress("10.10.0.0"))); // enabled
        newStatuses.put(ppp1, new InterfaceState(ppp1, true, true, IPAddress.parseHostAddress("10.10.0.1")));
        newStatuses.put(ppp3, ppp3NewState); // modified

        TestUtil.invokePrivate(svc, "checkStatusChange", oldStatuses, newStatuses);

        verify(eaMock, times(3)).postEvent(anyObject());

        assertEquals(3, visited.get());
    }

    @Test
    public void testProcessNetworkConfigurationChangeEvent() throws Throwable {
        // PPP service preparation
        IModemLinkService pppMock = mock(IModemLinkService.class);
        when(pppMock.getPppState()).thenReturn(PppState.CONNECTED); // trigger disconnect

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl() {

            IModemLinkService getPppService(final String interfaceName, final String port) {
                if ("ppp2".equals(interfaceName)) {
                    return pppMock;
                }
                fail("expected ppp2");
                return null;
            }
        };

        // add so that anything happens at all
        // need at least one modem with a device set
        CellularModem modemMock = mock(CellularModem.class);
        svc.addModem("ttyUsb3", modemMock);

        ModemDevice modemDevice = mock(ModemDevice.class);
        when(modemMock.getModemDevice()).thenReturn(modemDevice);

        // test disabling modem
        when(modemMock.isGpsEnabled()).thenReturn(true);
        when(modemMock.isPortReachable(anyObject())).thenReturn(true);

        // add old configuration with modem and IP4 config
        List<NetConfig> modemOldConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        modemOldConfigs.add(config);
        ModemConfig mc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        mc.setPppNumber(2); // so that
        modemOldConfigs.add(mc);
        when(modemMock.getConfiguration()).thenReturn(modemOldConfigs);

        // needed to return modem's port
        NetworkService nsMock = mock(NetworkService.class);
        svc.setNetworkService(nsMock);

        when(nsMock.getModemPppPort(modemDevice)).thenReturn("ppp2");

        // for posting the modem disabled event
        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        // new configuration
        NetworkConfiguration nc = new NetworkConfiguration();
        ModemInterfaceConfigImpl nic = new ModemInterfaceConfigImpl("ppp2");
        List<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        nic.setNetInterfaceAddresses(interfaceAddresses);
        nc.addNetInterfaceConfig(nic);

        ModemInterfaceAddressConfigImpl interfaceAddress = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        interfaceAddress.setNetConfigs(netConfigs);
        interfaceAddresses.add(interfaceAddress);

        TestUtil.invokePrivate(svc, "processNetworkConfigurationChangeEvent", nc);

        verify(pppMock, times(1)).disconnect();
        verify(eaMock, times(1)).postEvent(org.mockito.Mockito.isA(ModemGpsDisabledEvent.class));
    }

    @Test
    public void testProcessNetworkConfigurationChangeEventEvdo() throws Throwable {
        // PPP service preparation
        IModemLinkService pppMock = mock(IModemLinkService.class);
        when(pppMock.getPppState()).thenReturn(PppState.CONNECTED); // trigger disconnect

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl() {

            IModemLinkService getPppService(final String interfaceName, final String port) {
                if ("ppp2".equals(interfaceName)) {
                    return pppMock;
                }
                fail("expected ppp2");
                return null;
            }
        };

        Future task = mock(Future.class);
        TestUtil.setFieldValue(svc, "task", task);

        ExecutorService executor = mock(ExecutorService.class);
        TestUtil.setFieldValue(svc, "executor", executor);

        CellularModem modemMock = mock(EvdoCellularModem.class);
        svc.addModem("ttyUsb3", modemMock);

        ModemDevice modemDevice = mock(ModemDevice.class);
        when(modemMock.getModemDevice()).thenReturn(modemDevice);

        // Evdo-specific part
        when(((EvdoCellularModem) modemMock).isProvisioned()).thenReturn(false);
        when(((EvdoCellularModem) modemMock).isGpsSupported()).thenReturn(true);
        CommURI commuri = CommURI.parseString(
                "comm:gps0;baudrate=56400;databits=8;stopbits=1;parity=0;flowcontrol=1;timeout=10;receivetimeout=5");
        when(modemMock.getSerialConnectionProperties(SerialPortType.GPSPORT)).thenReturn(commuri);

        // add old configuration with modem and IP4 config
        List<NetConfig> modemOldConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        modemOldConfigs.add(config);
        ModemConfig mc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        mc.setPppNumber(2); // so that
        modemOldConfigs.add(mc);
        when(modemMock.getConfiguration()).thenReturn(modemOldConfigs);

        // needed to return modem's port
        NetworkService nsMock = mock(NetworkService.class);
        svc.setNetworkService(nsMock);

        when(nsMock.getModemPppPort(modemDevice)).thenReturn("ppp2");

        // for posting the modem disabled event
        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        // new configuration
        NetworkConfiguration nc = new NetworkConfiguration();
        ModemInterfaceConfigImpl nic = new ModemInterfaceConfigImpl("ppp2");
        List<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        nic.setNetInterfaceAddresses(interfaceAddresses);
        nc.addNetInterfaceConfig(nic);

        ModemInterfaceAddressConfigImpl interfaceAddress = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfig ipnc = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true); // for Evdo modem
        netConfigs.add(ipnc);
        ModemConfig mc2 = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        mc2.setGpsEnabled(true);
        netConfigs.add(mc2);
        interfaceAddress.setNetConfigs(netConfigs);
        interfaceAddresses.add(interfaceAddress);

        TestUtil.invokePrivate(svc, "processNetworkConfigurationChangeEvent", nc);

        verify(modemMock, times(1)).enableGps();
        verify(eaMock, times(1)).postEvent(org.mockito.Mockito.isA(ModemGpsEnabledEvent.class));
    }

    private void testModemReset(final PppState pppState, final boolean resetExpected, final boolean connectExpected,
            final boolean isSimCardReady) throws Throwable {
        IModemLinkService pppMock = mock(IModemLinkService.class);
        when(pppMock.getPppState()).thenReturn(pppState);

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl() {

            @Override
            IModemLinkService getPppService(final String interfaceName, final String port) {
                if ("ppp0".equals(interfaceName)) {
                    return pppMock;
                }
                fail("expected ppp0");
                return null;
            }

            @Override
            long getModemResetTimeoutMsec(String ifaceName, java.util.List<NetConfig> netConfigs) {
                return 1000;
            }
        };

        final HspaCellularModem mockModem = mock(HspaCellularModem.class);

        ModemDevice modemDevice = mock(ModemDevice.class);
        when(mockModem.getModemDevice()).thenReturn(modemDevice);
        when(mockModem.getTechnologyTypes()).thenReturn(Collections.singletonList(ModemTechnologyType.HSDPA));
        when(mockModem.isSimCardReady()).thenReturn(isSimCardReady);

        List<NetConfig> modemConfigs = new ArrayList<>();
        NetConfig config = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        modemConfigs.add(config);
        ModemConfig mc = new ModemConfig(1, PdpType.PPP, "apn", IPAddress.parseHostAddress("10.10.10.10"), 1, 2);
        mc.setPppNumber(2); // so that
        modemConfigs.add(mc);
        when(mockModem.getConfiguration()).thenReturn(modemConfigs);

        NetworkService nsMock = mock(NetworkService.class);
        when(nsMock.getModemPppPort(modemDevice)).thenReturn("ppp0");

        NetworkConfigurationService nsCfgMock = mock(NetworkConfigurationService.class);
        NetworkConfiguration networkConfigMock = mock(NetworkConfiguration.class);

        when(nsCfgMock.getNetworkConfiguration()).thenReturn(networkConfigMock);
        svc.setNetworkConfigurationService(nsCfgMock);

        svc.setNetworkService(nsMock);
        svc.setEventAdmin(mock(EventAdmin.class));

        svc.activate();
        svc.addModem("ppp0", mockModem);
        svc.sync();

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < 15; i++) {
            TestUtil.invokePrivate(svc, "monitor");
            Thread.sleep(100);
            if (resetExpected && System.currentTimeMillis() - startTime < 1000) {
                verify(mockModem, times(0)).reset();
            }
            verify(pppMock, connectExpected ? times(i + 2) : times(0)).connect();
        }

        verify(mockModem, resetExpected ? times(1) : times(0)).reset();
    }

    @Test
    public void testModemResetNotConnected() throws Throwable {
        testModemReset(PppState.NOT_CONNECTED, true, true, true);
    }

    @Test
    public void testModemResetNotConnectedNoSim() throws Throwable {
        testModemReset(PppState.NOT_CONNECTED, false, false, false);
    }

    @Test
    public void testModemResetInProgress() throws Throwable {
        testModemReset(PppState.IN_PROGRESS, true, false, true);
    }

    @Test
    public void testModemResetConnected() throws Throwable {
        testModemReset(PppState.CONNECTED, false, false, true);
    }
}

class TestModemFactory extends AbstractCellularModemFactory<CellularModem> {

    private static TestModemFactory instance;

    private CellularModem modem;

    public static TestModemFactory getInstance() {
        if (instance == null) {
            instance = new TestModemFactory();
        }
        return instance;
    }

    @Override
    protected CellularModem createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        assertEquals("testPlatform", platform);

        return this.modem;
    }

    public CellularModem getModem() {
        return modem;
    }

    public void setModem(CellularModem modem) {
        this.modem = modem;
    }

    @Override
    public ModemTechnologyType getType() {
        return null;
    }
}
