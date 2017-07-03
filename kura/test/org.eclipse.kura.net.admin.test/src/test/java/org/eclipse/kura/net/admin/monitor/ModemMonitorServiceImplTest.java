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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.CellularModem.SerialPortType;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public class ModemMonitorServiceImplTest {

    @Test
    public void testActivateNoModems() throws NoSuchFieldException {
        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl();

        ComponentContext cctxMock = mock(ComponentContext.class);
        BundleContext bctxMock = mock(BundleContext.class);
        when(cctxMock.getBundleContext()).thenReturn(bctxMock);

        svc.activate(cctxMock);

        verify(bctxMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        assertTrue((boolean) TestUtil.getFieldValue(svc, "serviceActivated"));
    }

    @Test
    public void testActivateWithModemNoFramework() throws NoSuchFieldException, KuraException {
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

        ComponentContext cctxMock = mock(ComponentContext.class);
        BundleContext bctxMock = mock(BundleContext.class);
        when(cctxMock.getBundleContext()).thenReturn(bctxMock);

        svc.activate(cctxMock);

        verify(bctxMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());

        assertTrue((boolean) TestUtil.getFieldValue(svc, "serviceActivated"));
        assertTrue(((Map) TestUtil.getFieldValue(svc, "modems")).isEmpty());
    }

    @Test
    public void testActivateWithUsbModem() throws NoSuchFieldException, KuraException, URISyntaxException {
        // enters creation of a modem, and ends up activating the GPS

        ModemMonitorServiceImpl svc = new ModemMonitorServiceImpl() {

            @Override
            protected Class<? extends CellularModemFactory> getModemFactoryClass(ModemDevice modemDevice) {
                return TestModemFactory.class;
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
        }).when(eaMock).postEvent(anyObject());

        // modem mock
        setModemMock();

        TestUtil.setFieldValue(svc, "task", mock(Future.class));

        // args, event handler
        ComponentContext cctxMock = mock(ComponentContext.class);
        BundleContext bctxMock = mock(BundleContext.class);
        when(cctxMock.getBundleContext()).thenReturn(bctxMock);

        svc.activate(cctxMock);

        verify(bctxMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(svc), anyObject());
        verify(eaMock, times(3)).postEvent(anyObject());

        assertTrue((boolean) TestUtil.getFieldValue(svc, "serviceActivated"));

        Map modems = (Map) TestUtil.getFieldValue(svc, "modems");
        assertEquals(1, modems.size());
        CellularModem modem = (CellularModem) modems.get("usb0");
        assertNotNull(modem);
        assertEquals("imei", modem.getSerialNumber());

        verify(modem, times(1)).enableGps();
    }

    private void setModemMock() throws KuraException, URISyntaxException {
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

        CommURI commuri = CommURI.parseString(
                "comm:gps0;baudrate=56400;databits=8;stopbits=1;parity=0;flowcontrol=1;timeout=10;receivetimeout=5");
        when(modem.getSerialConnectionProperties(SerialPortType.GPSPORT)).thenReturn(commuri);
    }

}

class TestModemFactory implements CellularModemFactory {

    private static TestModemFactory instance;

    private CellularModem modem;

    public static TestModemFactory getInstance() {
        if (instance == null) {
            instance = new TestModemFactory();
        }
        return instance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {
        assertEquals("testPlatform", platform);

        return this.modem;
    }

    @Override
    public Hashtable<String, ? extends CellularModem> getModemServices() {
        return null;
    }

    @Override
    public void releaseModemService(String usbPortAddress) {
    }

    @Override
    public ModemTechnologyType getType() {
        return null;
    }

    public CellularModem getModem() {
        return modem;
    }

    public void setModem(CellularModem modem) {
        this.modem = modem;
    }

}
