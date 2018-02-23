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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;


public class NetworkConfigurationServiceimplTest {

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException {
        // test activate and deactivate sequence

        AtomicBoolean inited = new AtomicBoolean(false);

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected void initVisitors() {
                inited.set(true);
            }
        };

        ComponentContext componentContextMock = mock(ComponentContext.class);
        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleCtxMock);

        when(bundleCtxMock.registerService(eq(EventHandler.class.getName()), eq(svc), anyObject()))
                .thenAnswer(invocation -> {
                    Dictionary<String, String[]> dict = invocation.getArgumentAt(2, Dictionary.class);

                    assertEquals(1, dict.size());

                    String[] topics = dict.get("event.topics");
                    assertNotNull(topics);
                    assertEquals(1, topics.length);
                    assertEquals("org/eclipse/kura/configuration/ConfigEvent/READY", topics[0]);

                    return null;
                });

        Map<String, Object> properties = new HashMap<>();

        svc.activate(componentContextMock, properties);

        assertTrue(inited.get());

        verify(bundleCtxMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        ScheduledExecutorService executor = (ScheduledExecutorService) TestUtil.getFieldValue(svc, "executorUtil");

        assertNotNull(executor);
        assertFalse(executor.isShutdown());

        svc.deactivate(componentContextMock);

        assertTrue(executor.isShutdown());
    }

    @Test
    public void testHandleEvent() throws InterruptedException, NoSuchFieldException {
        // test event handling and sending of a new event using the executor service

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected void initVisitors() {
            }
        };

        ComponentContext componentContextMock = mock(ComponentContext.class);
        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleCtxMock);

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        Object lock = new Object();
        AtomicBoolean posted = new AtomicBoolean(false);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);
            assertEquals("org/eclipse/kura/configuration/NetConfigEvent/READY", event.getTopic());

            posted.set(true);

            synchronized (lock) {
                lock.notifyAll();
            }

            return null;
        }).when(eventAdminMock).postEvent(anyObject());

        Map<String, Object> properties = null;

        svc.activate(componentContextMock, properties);

        Event event = new Event("org/eclipse/kura/configuration/ConfigEvent/READY", properties);

        svc.handleEvent(event);

        synchronized (lock) {
            lock.wait(6000); // > 5s wait is necessary
        }

        assertTrue(posted.get());

        assertFalse((boolean) TestUtil.getFieldValue(svc, "firstConfig"));
    }

    @Test
    public void testUpdatedFirstRun() throws NoSuchFieldException {
        // test first run 'update'

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl();

        assertTrue((boolean) TestUtil.getFieldValue(svc, "firstConfig"));

        svc.updated(null);

        assertFalse((boolean) TestUtil.getFieldValue(svc, "firstConfig"));
    }

    @Test
    public void testUpdatedNullProperties() throws NoSuchFieldException {
        // test null properties 'update'

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl();

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        TestUtil.setFieldValue(svc, "firstConfig", false);

        svc.updated(null);

        verify(eventAdminMock, never()).postEvent(anyObject());
    }

    @Test
    public void testUpdatedException() throws NoSuchFieldException {
        // test update with exception - no event is posted

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test");
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        TestUtil.setFieldValue(svc, "firstConfig", false);

        Map<String, Object> properties = new HashMap<>();
        properties.put("modified.interface.names", "testIntf");
        properties.put("net.interfaces", "eth1,ppp0");

        svc.updated(properties);

        verify(eventAdminMock, never()).postEvent(anyObject());
    }

    @Test
    public void testUpdated() throws NoSuchFieldException {
        // test complete updated handler

        boolean[] invocations = { false, false };

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                if ("eth1".equals(interfaceName)) {
                    return NetInterfaceType.ETHERNET;
                }

                return NetInterfaceType.UNKNOWN;
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC", event.getTopic());
            assertEquals(7, event.getPropertyNames().length); // 6+topics!

            assertEquals("UNKNOWN", event.getProperty("net.interface.ppp0.type"));
            assertEquals("MODEM", event.getProperty("net.interface.1-2.3.type"));

            invocations[1] = true;

            return null;
        }).when(eventAdminMock).postEvent(anyObject());

        TestUtil.setFieldValue(svc, "firstConfig", false);

        List<NetworkConfigurationVisitor> visitors = new ArrayList<>();
        NetworkConfigurationVisitor visitor = new NetworkConfigurationVisitor() {

            @Override
            public void visit(NetworkConfiguration config) throws KuraException {
                assertTrue(config.getModifiedInterfaceNames().contains("testIntf"));

                assertEquals(1, config.getNetInterfaceConfigs().size());
                assertNotNull(config.getNetInterfaceConfig("1-2.3"));

                invocations[0] = true;
            }
        };
        visitors.add(visitor);
        TestUtil.setFieldValue(svc, "writeVisitors", visitors);

        Map<String, Object> properties = new HashMap<>();
        properties.put("modified.interface.names", "testIntf");
        properties.put("net.interfaces", "ppp0,1-2.3");
        properties.put("net.interface.ppp0.config.password", "pass");
        properties.put("net.interface.1-2.3.config.password", "pass123");

        svc.updated(properties);

        verify(eventAdminMock, times(1)).postEvent(anyObject());

        assertTrue(invocations[0]);
        assertTrue(invocations[1]);
    }

    @Test
    public void testGetNetworkConfiguration() throws KuraException, NoSuchFieldException {
        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl();

        NetworkService networkServiceMock = mock(NetworkService.class);
        svc.setNetworkService(networkServiceMock);

        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<>();
        NetInterface<? extends NetInterfaceAddress> netInterface = new WifiInterfaceImpl("wlan1");
        ((WifiInterfaceImpl) netInterface).setUp(true);
        interfaces.add(netInterface);
        netInterface = new WifiInterfaceImpl("mon.test"); // will be skipped
        interfaces.add(netInterface);
        netInterface = new WifiInterfaceImpl("rpine.test"); // will be skipped
        interfaces.add(netInterface);
        netInterface = new EthernetInterfaceImpl("eth2");
        interfaces.add(netInterface);
        netInterface = new LoopbackInterfaceImpl("lo");
        interfaces.add(netInterface);
        netInterface = new ModemInterfaceImpl("ppp1");
        interfaces.add(netInterface);

        // type will remain unknown => will be skipped
        netInterface = new AbstractNetInterface<NetInterfaceAddress>("ppp2") {

            @Override
            public NetInterfaceType getType() {
                return NetInterfaceType.UNKNOWN;
            }
        };
        interfaces.add(netInterface);

        // will result in type MODEM, but will cause exception later on
        netInterface = new AbstractNetInterface<NetInterfaceAddress>("1-2.3") {

            @Override
            public NetInterfaceType getType() {
                return NetInterfaceType.UNKNOWN;
            }
        };
        interfaces.add(netInterface);

        // will result in unsupported type, so it will be skipped
        netInterface = new AbstractNetInterface<NetInterfaceAddress>("dsl") {

            @Override
            public NetInterfaceType getType() {
                return NetInterfaceType.ADSL;
            }
        };
        interfaces.add(netInterface);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(interfaces);

        AtomicBoolean visited = new AtomicBoolean(false);
        List<NetworkConfigurationVisitor> visitors = new ArrayList<>();
        NetworkConfigurationVisitor visitor = new NetworkConfigurationVisitor() {

            @Override
            public void visit(NetworkConfiguration config) throws KuraException {
                visited.set(true);
            }
        };
        visitors.add(visitor);
        TestUtil.setFieldValue(svc, "readVisitors", visitors);

        NetworkConfiguration networkConfiguration = svc.getNetworkConfiguration();

        assertTrue(visited.get());

        assertEquals(4, networkConfiguration.getNetInterfaceConfigs().size());

        NetInterfaceConfig<? extends NetInterfaceAddressConfig> config = networkConfiguration
                .getNetInterfaceConfig("eth2");
        assertNotNull(config);
        config = networkConfiguration.getNetInterfaceConfig("lo");
        assertNotNull(config);

        // don't add ModemManagerService as it will complicate matters
        config = networkConfiguration.getNetInterfaceConfig("ppp1");
        assertNotNull(config);

        config = networkConfiguration.getNetInterfaceConfig("wlan1");
        assertNotNull(config);
    }

    @Test
    public void testGetConfigurationException() throws KuraException, NoSuchFieldException {
        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected List<String> getAllInterfaceNames() throws KuraException {
                throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, "test");
            }
        };

        NetworkService networkServiceMock = mock(NetworkService.class);
        svc.setNetworkService(networkServiceMock);

        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<>();
        NetInterface<? extends NetInterfaceAddress> netInterface = new WifiInterfaceImpl("wlan1");
        interfaces.add(netInterface);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(interfaces);

        List<NetworkConfigurationVisitor> visitors = new ArrayList<>();
        NetworkConfigurationVisitor visitor = new NetworkConfigurationVisitor() {

            @Override
            public void visit(NetworkConfiguration config) throws KuraException {
            }
        };
        visitors.add(visitor);
        TestUtil.setFieldValue(svc, "readVisitors", visitors);

        UsbService usbServiceMock = mock(UsbService.class);
        svc.setUsbService(usbServiceMock);

        try {
            svc.getConfiguration();
            fail("Exception was expected.");
        } catch (KuraException e) {
            e = (KuraException) e.getCause();
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
            e = (KuraException) e.getCause();
            assertEquals(KuraErrorCode.CONFIGURATION_UPDATE, e.getCode());
            assertTrue(e.getMessage().endsWith("test"));
        }

    }

    @Test
    public void testGetConfiguration() throws KuraException, NoSuchFieldException {
        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected List<String> getAllInterfaceNames() throws KuraException {
                List<String> list = new ArrayList<>();

                list.add("eth2");
                list.add("lo");
                list.add("ppp1");
                list.add("wlan1");

                return list;
            }

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                NetInterfaceType type;

                switch (interfaceName) {
                case "eth2":
                    type = NetInterfaceType.ETHERNET;
                    break;
                case "lo":
                    type = NetInterfaceType.LOOPBACK;
                    break;
                case "ppp1":
                    type = NetInterfaceType.MODEM;
                    break;
                case "wlan1":
                    type = NetInterfaceType.WIFI;
                    break;
                default:
                    type = NetInterfaceType.UNKNOWN;
                }

                return type;
            }
        };

        NetworkService networkServiceMock = mock(NetworkService.class);
        svc.setNetworkService(networkServiceMock);

        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<>();
        NetInterface<? extends NetInterfaceAddress> netInterface = new WifiInterfaceImpl("wlan1");
        ((WifiInterfaceImpl) netInterface).setUp(true);
        interfaces.add(netInterface);
        netInterface = new EthernetInterfaceImpl("eth2");
        interfaces.add(netInterface);
        netInterface = new LoopbackInterfaceImpl("lo");
        interfaces.add(netInterface);
        netInterface = new ModemInterfaceImpl("ppp1");
        interfaces.add(netInterface);
        when(networkServiceMock.getNetworkInterfaces()).thenReturn(interfaces);

        AtomicBoolean visited = new AtomicBoolean(false);
        List<NetworkConfigurationVisitor> visitors = new ArrayList<>();
        NetworkConfigurationVisitor visitor = new NetworkConfigurationVisitor() {

            @Override
            public void visit(NetworkConfiguration config) throws KuraException {
                visited.set(true);
            }
        };
        visitors.add(visitor);
        TestUtil.setFieldValue(svc, "readVisitors", visitors);

        UsbService usbServiceMock = mock(UsbService.class);
        svc.setUsbService(usbServiceMock);

        List<UsbNetDevice> usbNetDevices = new ArrayList<>();
        usbNetDevices.add(new UsbNetDevice("vendor", "product", "manufacturer", "productName", "usbBusNumber",
                "usbDevicePath", "wlan1"));
        when(usbServiceMock.getUsbNetDevices()).thenReturn(usbNetDevices);

        ComponentConfiguration configuration = svc.getConfiguration();

        assertEquals(NetworkConfigurationService.PID, configuration.getPid());

        Map<String, Object> properties = configuration.getConfigurationProperties();

        assertNotNull(properties);
        assertEquals(62, properties.size());
        assertEquals("eth2", properties.get("net.interface.eth2.config.name"));
        assertEquals("ETHERNET", properties.get("net.interface.eth2.type"));
        assertFalse((boolean) properties.get("net.interface.eth2.up"));
        assertEquals("lo", properties.get("net.interface.lo.config.name"));
        assertEquals("LOOPBACK", properties.get("net.interface.lo.type"));
        assertFalse((boolean) properties.get("net.interface.lo.up"));
        assertEquals("ppp1", properties.get("net.interface.ppp1.config.name"));
        assertEquals("MODEM", properties.get("net.interface.ppp1.type"));
        assertFalse((boolean) properties.get("net.interface.ppp1.up"));
        assertEquals("wlan1", properties.get("net.interface.wlan1.config.name"));
        assertEquals("WIFI", properties.get("net.interface.wlan1.type"));
        assertTrue((boolean) properties.get("net.interface.wlan1.up"));

        OCD ocd = configuration.getDefinition();

        assertNotNull(ocd);

        assertEquals("NetworkConfigurationService", ocd.getName());
        assertEquals("org.eclipse.kura.net.admin.NetworkConfigurationService", ocd.getId());
        assertEquals("Network Configuration Service", ocd.getDescription());

        List<AD> ads = ocd.getAD();
        assertNotNull(ads);
        assertEquals(60, ads.size());

        int adsConfigured = 0;
        for (AD ad : ads) {
            if ("net.interface.eth2.config.autoconnect".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.autoconnect", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }
            
            if ("net.interface.eth2.config.dhcpClient4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpClient4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.defaultLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.defaultLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.maxLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.maxLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.passDns".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.passDns", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.rangeEnd".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.rangeEnd", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dhcpServer4.rangeStart".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dhcpServer4.rangeStart", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.dnsServers".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.dnsServers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip4.gateway".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip4.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.mtu".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.mtu", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.nat.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.nat.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.winsServers".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.winsServers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.autoconnect".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.autoconnect", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.driver".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.driver", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.ip4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.ip4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.mtu".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.mtu", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.autoconnect".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.autoconnect", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }
            
            if ("net.interface.wlan1.config.dhcpClient4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpClient4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.defaultLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.defaultLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.enabled".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.maxLeaseTime".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.maxLeaseTime", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.passDns".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.passDns", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.rangeEnd".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.rangeEnd", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dhcpServer4.rangeStart".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dhcpServer4.rangeStart", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.dnsServers".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.dnsServers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip4.address".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip4.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip4.gateway".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip4.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip4.prefix".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip4.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.mtu".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.mtu", ad.getName());
                assertEquals("INTEGER", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.nat.enabled".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.nat.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.channel".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.channel", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.groupCiphers".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.groupCiphers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.hardwareMode".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.hardwareMode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.pairwiseCiphers".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.pairwiseCiphers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.passphrase".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.passphrase", ad.getName());
                assertEquals("PASSWORD", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.radioMode".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.radioMode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.securityType".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.securityType", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.infra.ssid".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.infra.ssid", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.broadcast".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.broadcast", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.channel".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.channel", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.hardwareMode".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.hardwareMode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.passphrase".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.passphrase", ad.getName());
                assertEquals("PASSWORD", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.radioMode".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.radioMode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.securityType".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.securityType", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.master.ssid".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.ssid", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.wifi.mode".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.mode", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.winsServers".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.winsServers", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.usb.manufacturer".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.usb.manfacturer", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.usb.manufacturer.id".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.usb.manfacturer.id", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.usb.port".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.usb.port", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.usb.product".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.usb.product", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.usb.product.id".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.usb.product.id", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.wifi.capabilities".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.wifi.capabilities", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interfaces".equals(ad.getId())) {
                assertEquals("net.interfaces", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }
        }
        assertEquals(60, adsConfigured);
    }

}
