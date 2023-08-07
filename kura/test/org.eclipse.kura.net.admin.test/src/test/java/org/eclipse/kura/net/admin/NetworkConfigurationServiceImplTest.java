/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Optional;
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
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public class NetworkConfigurationServiceImplTest {

    @Test
    public void testActivate() throws NoSuchFieldException {
        AtomicBoolean inited = new AtomicBoolean(false);

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                return guessNetworkType(interfaceName);
            }

            @Override
            protected void initVisitors() {
                inited.set(true);
            }
        };

        ComponentContext componentContextMock = mock(ComponentContext.class);
        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleCtxMock);
        UsbService usbService = mock(UsbService.class);
        when(usbService.getUsbNetDevices()).thenReturn(new ArrayList<UsbNetDevice>());
        svc.setUsbService(usbService);

        when(bundleCtxMock.registerService(eq(EventHandler.class.getName()), eq(svc), any())).thenAnswer(invocation -> {
            Dictionary<String, String[]> dict = invocation.getArgument(2, Dictionary.class);

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
    }

    @Test
    public void testPostEvent() throws InterruptedException, NoSuchFieldException {
        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                return guessNetworkType(interfaceName);
            }

            @Override
            protected void initVisitors() {
            }

            @Override
            protected List<NetworkConfigurationVisitor> getVisitors() {
                return new ArrayList<>();
            }
        };

        NetworkService networkService = mock(NetworkService.class);
        ComponentContext componentContextMock = mock(ComponentContext.class);
        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleCtxMock);
        UsbService usbService = mock(UsbService.class);
        when(usbService.getUsbNetDevices()).thenReturn(new ArrayList<UsbNetDevice>());
        svc.setUsbService(usbService);

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        Object lock = new Object();
        AtomicBoolean posted = new AtomicBoolean(false);

        doAnswer(invocation -> {
            Event event = invocation.getArgument(0, Event.class);
            assertEquals("org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC", event.getTopic());

            posted.set(true);

            synchronized (lock) {
                lock.notifyAll();
            }

            return null;
        }).when(eventAdminMock).postEvent(any());

        Map<String, Object> properties = new HashMap<>();
        properties.put("net.interfaces", "");

        svc.setNetworkService(networkService);
        svc.activate(componentContextMock, properties);

        synchronized (lock) {
            lock.wait(6000); // > 5s wait is necessary
        }

        assertTrue(posted.get());
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

        UsbService usbService = mock(UsbService.class);
        when(usbService.getUsbNetDevices()).thenReturn(new ArrayList<UsbNetDevice>());
        svc.setUsbService(usbService);

        Map<String, Object> properties = new HashMap<>();
        properties.put("modified.interface.names", "testIntf");
        properties.put("net.interfaces", "eth1,ppp0");

        svc.updated(properties);

        verify(eventAdminMock, never()).postEvent(any());
    }

    @Test
    public void testUpdated() throws NoSuchFieldException, KuraException {
        // test complete updated handler

        boolean[] invocations = { false, false };

        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                if (interfaceName.equals("1-2.3")) {
                    return NetInterfaceType.MODEM;
                }

                return NetInterfaceType.UNKNOWN;
            }
        };

        UsbService usbService = mock(UsbService.class);
        when(usbService.getUsbNetDevices()).thenReturn(new ArrayList<UsbNetDevice>());
        svc.setUsbService(usbService);

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgument(0, Event.class);

            assertEquals("org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC", event.getTopic());
            assertEquals(12, event.getPropertyNames().length);

            assertEquals("MODEM", event.getProperty("net.interface.1-2.3.type"));

            invocations[1] = true;

            return null;
        }).when(eventAdminMock).postEvent(any());

        List<NetworkConfigurationVisitor> visitors = new ArrayList<>();
        NetworkConfigurationVisitor visitor = new NetworkConfigurationVisitor() {

            @Override
            public void visit(NetworkConfiguration config) throws KuraException {
                assertTrue(config.getModifiedInterfaceNames().contains("testIntf"));

                assertEquals(1, config.getNetInterfaceConfigs().size());
                assertNotNull(config.getNetInterfaceConfig("1-2.3"));

                invocations[0] = true;
            }

            @Override
            public void setExecutorService(CommandExecutorService executorService) {
                // Do nothing...
            }
        };
        visitors.add(visitor);
        TestUtil.setFieldValue(svc, "writeVisitors", visitors);

        NetworkService nsMock = mock(NetworkService.class);
        when(nsMock.getModemPppInterfaceName("1-2.3")).thenReturn("ppp3");
        ModemDevice modemDevice = new UsbModemDevice("1111", "2222", "Acme", "CoolModem", "1", "2.3");
        when(nsMock.getModemDevice("1-2.3")).thenReturn(Optional.of(modemDevice));
        svc.setNetworkService(nsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("modified.interface.names", "testIntf");
        properties.put("net.interfaces", "1-2.3");
        properties.put("net.interface.1-2.3.config.password", "pass123");

        svc.updated(properties);

        verify(eventAdminMock, times(1)).postEvent(any());

        assertTrue(invocations[0]);
        assertTrue(invocations[1]);
    }

    @Test
    public void testGetNetworkConfiguration() throws KuraException, NoSuchFieldException {
        NetworkConfigurationServiceImpl svc = new NetworkConfigurationServiceImpl() {

            @Override
            protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
                return guessNetworkType(interfaceName);
            }

            @Override
            protected void initVisitors() {
            }

            @Override
            protected List<NetworkConfigurationVisitor> getVisitors() {
                return new ArrayList<>();
            }
        };

        NetworkService networkServiceMock = mock(NetworkService.class);
        svc.setNetworkService(networkServiceMock);
        UsbService usbService = mock(UsbService.class);
        when(usbService.getUsbNetDevices()).thenReturn(new ArrayList<UsbNetDevice>());
        svc.setUsbService(usbService);

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

        Map<String, Object> properties = new HashMap<>();
        properties.put("net.interfaces", "");

        svc.activate(null, properties);
        NetworkConfiguration networkConfiguration = svc.getNetworkConfiguration();

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
                return guessNetworkType(interfaceName);
            }

            @Override
            protected void initVisitors() {
            }

            @Override
            protected List<NetworkConfigurationVisitor> getVisitors() {
                return new ArrayList<>();
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

        UsbService usbServiceMock = mock(UsbService.class);
        svc.setUsbService(usbServiceMock);

        Map<String, Object> inputProperties = new HashMap<>();
        inputProperties.put("net.interfaces", "");

        List<UsbNetDevice> usbNetDevices = new ArrayList<>();
        usbNetDevices.add(new UsbNetDevice("vendor", "product", "manufacturer", "productName", "usbBusNumber",
                "usbDevicePath", "wlan1"));
        when(usbServiceMock.getUsbNetDevices()).thenReturn(usbNetDevices);

        svc.activate(null, inputProperties);

        ComponentConfiguration configuration = svc.getConfiguration();

        assertEquals(NetworkConfigurationService.PID, configuration.getPid());

        Map<String, Object> properties = configuration.getConfigurationProperties();

        assertNotNull(properties);

        assertEquals(83, properties.size());
        assertEquals("ETHERNET", properties.get("net.interface.eth2.type"));
        assertEquals("LOOPBACK", properties.get("net.interface.lo.type"));
        assertEquals("MODEM", properties.get("net.interface.ppp1.type"));
        assertEquals("WIFI", properties.get("net.interface.wlan1.type"));

        OCD ocd = configuration.getDefinition();

        assertNotNull(ocd);

        assertEquals("NetworkConfigurationService", ocd.getName());
        assertEquals("org.eclipse.kura.net.admin.NetworkConfigurationService", ocd.getId());
        assertEquals("Network Configuration Service", ocd.getDescription());

        List<AD> ads = ocd.getAD();
        assertNotNull(ads);
        assertEquals(124, ads.size());

        int adsConfigured = 0;
        for (AD ad : ads) {
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

            if ("net.interface.eth2.config.nat.enabled".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.nat.enabled", ad.getName());
                assertEquals("BOOLEAN", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip6.address.method".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip6.address.method", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip6.gateway".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip6.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip6.prefix".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip6.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.eth2.config.ip6.address".equals(ad.getId())) {
                assertEquals("net.interface.eth2.config.ip6.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
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

            if ("net.interface.lo.config.ip6.prefix".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.ip6.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.lo.config.ip6.address".equals(ad.getId())) {
                assertEquals("net.interface.lo.config.ip6.address", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
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

            if ("net.interface.wlan1.config.wifi.master.channel".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.wifi.master.channel", ad.getName());
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

            if ("net.interface.wlan1.wifi.capabilities".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.wifi.capabilities", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip6.address.method".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip6.address.method", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertTrue(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip6.gateway".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip6.gateway", ad.getName());
                assertEquals("STRING", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip6.prefix".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip6.prefix", ad.getName());
                assertEquals("SHORT", ad.getType().name());
                assertFalse(ad.isRequired());
                adsConfigured++;
            }

            if ("net.interface.wlan1.config.ip6.address".equals(ad.getId())) {
                assertEquals("net.interface.wlan1.config.ip6.address", ad.getName());
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
        assertEquals(51, adsConfigured);
    }

    private static NetInterfaceType guessNetworkType(final String interfaceName) {

        if (interfaceName.startsWith("eth")) {
            return NetInterfaceType.ETHERNET;
        } else if (interfaceName.equals("lo")) {
            return NetInterfaceType.LOOPBACK;
        } else if (interfaceName.startsWith("ppp")) {
            return NetInterfaceType.MODEM;
        } else if (interfaceName.startsWith("wlan")) {
            return NetInterfaceType.WIFI;
        } else {
            return NetInterfaceType.UNKNOWN;
        }

    }

}
