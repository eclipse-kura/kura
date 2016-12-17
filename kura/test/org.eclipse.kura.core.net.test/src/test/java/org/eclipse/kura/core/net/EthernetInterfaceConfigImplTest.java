/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EthernetInterfaceConfigImplTest {

	@Test
	public void testEqualsObject() throws UnknownHostException {
		EthernetInterfaceConfigImpl a = createConfig(2);
		assertEquals(a, a);

		EthernetInterfaceConfigImpl b = createConfig(2);
		assertEquals(a, b);
	}

	@Test
	public void testEqualsObjectDifferentLinkUp() throws UnknownHostException {
		EthernetInterfaceConfigImpl a = createConfig(2);
		EthernetInterfaceConfigImpl b = createConfig(2);

		a.setLinkUp(true);
		b.setLinkUp(false);

		assertNotEquals(a, b);
	}

    @Test
    public void testEthernetInterfaceConfigImplString() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name1");
        assertEquals("name1", config.getName());
        assertTrue(config.getNetInterfaceAddresses().isEmpty());

        config = new EthernetInterfaceConfigImpl("name2");
        assertEquals("name2", config.getName());
        assertTrue(config.getNetInterfaceAddresses().isEmpty());
    }

    @Test
    public void testEthernetInterfaceConfigImplEthernetInterfaceOfQextendsNetInterfaceAddressEmptyAddress()
            throws UnknownHostException {
        EthernetInterfaceConfigImpl config = createConfig(0);

        assertEquals("ethInterface", config.getName());
        assertEquals(1, config.getNetInterfaceAddresses().size());
    }

    @Test
    public void testEthernetInterfaceConfigImplEthernetInterfaceOfQextendsNetInterfaceAddressNonEmptyAddress()
            throws UnknownHostException {
        EthernetInterfaceConfigImpl config = createConfig(2);

        assertEquals("ethInterface", config.getName());
        assertEquals(2, config.getNetInterfaceAddresses().size());

        assertEquals(createAddress("10.0.0.1"), config.getNetInterfaceAddresses().get(0));
        assertEquals(createAddress("10.0.0.2"), config.getNetInterfaceAddresses().get(1));
    }

    @Test
    public void testToString1() throws UnknownHostException {
        EthernetInterfaceConfigImpl config = createConfig(0);

        String expected = "name=ethInterface :: loopback=false :: pointToPoint=false :: virtual=false"
                + " :: supportsMulticast=false :: up=false :: mtu=0 :: driver=null :: driverVersion=null"
                + " :: firmwareVersion=null :: state=null :: autoConnect=false"
                + " :: InterfaceAddress=NetConfig: no configurations  :: linkUp=false";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testToString2() throws UnknownHostException {
        EthernetInterfaceConfigImpl config = createConfig(2);

        config.setHardwareAddress(NetworkUtil.macToBytes("12:34:56:78:90:AB"));
        config.setLoopback(true);
        config.setPointToPoint(true);
        config.setVirtual(true);
        config.setSupportsMulticast(true);
        config.setUp(true);
        config.setMTU(42);
        config.setUsbDevice(new UsbNetDevice("vendorId", "productId", "manufacturerName", "productName", "usbBusNumber",
                "usbDevicePath", "interfaceName"));
        config.setDriver("driverName");
        config.setDriverVersion("driverVersion");
        config.setFirmwareVersion("firmwareVersion");
        config.setState(NetInterfaceState.ACTIVATED);
        config.setAutoConnect(true);
        config.setLinkUp(true);

        String expected = "name=ethInterface :: hardwareAddress=12:34:56:78:90:AB :: loopback=true"
                + " :: pointToPoint=true :: virtual=true :: supportsMulticast=true :: up=true :: mtu=42"
                + " :: usbDevice=UsbNetDevice [getInterfaceName()=interfaceName, getVendorId()=vendorId,"
                + " getProductId()=productId, getManufacturerName()=manufacturerName, getProductName()=productName,"
                + " getUsbBusNumber()=usbBusNumber, getUsbDevicePath()=usbDevicePath,"
                + " getUsbPort()=usbBusNumber-usbDevicePath] :: driver=driverName :: driverVersion=driverVersion"
                + " :: firmwareVersion=firmwareVersion :: state=ACTIVATED :: autoConnect=true"
                + " :: InterfaceAddress=NetConfig: no configurations NetConfig: no configurations  :: linkUp=true";

        assertEquals(expected, config.toString());
    }

    @Test
    public void testGetType() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");
        assertEquals(NetInterfaceType.ETHERNET, config.getType());
    }

    @Test
    public void testLinkUp() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setLinkUp(true);
        assertEquals(true, config.isLinkUp());

        config.setLinkUp(false);
        assertEquals(false, config.isLinkUp());
    }

    @Test
    public void testName() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name1");
        assertEquals("name1", config.getName());

        config.setName("name2");
        assertEquals("name2", config.getName());
    }

    @Test
    public void testGetHardwareAddress() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setHardwareAddress(NetworkUtil.macToBytes("12:34:56:78:90:AB"));
        assertArrayEquals(NetworkUtil.macToBytes("12:34:56:78:90:AB"), config.getHardwareAddress());

        config.setHardwareAddress(NetworkUtil.macToBytes("11:22:33:44:55:66"));
        assertArrayEquals(NetworkUtil.macToBytes("11:22:33:44:55:66"), config.getHardwareAddress());
    }

    @Test
    public void testIsLoopback() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setLoopback(true);
        assertEquals(true, config.isLoopback());

        config.setLoopback(false);
        assertEquals(false, config.isLoopback());
    }

    @Test
    public void testIsPointToPoint() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setPointToPoint(true);
        assertEquals(true, config.isPointToPoint());

        config.setPointToPoint(false);
        assertEquals(false, config.isPointToPoint());
    }

    @Test
    public void testIsVirtual() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setVirtual(true);
        assertEquals(true, config.isVirtual());

        config.setVirtual(false);
        assertEquals(false, config.isVirtual());
    }

    @Test
    public void testSupportsMulticast() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setSupportsMulticast(true);
        assertEquals(true, config.supportsMulticast());

        config.setSupportsMulticast(false);
        assertEquals(false, config.supportsMulticast());
    }

    @Test
    public void testIsUp() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setUp(true);
        assertEquals(true, config.isUp());

        config.setUp(false);
        assertEquals(false, config.isUp());
    }

    @Test
    public void testMTU() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setMTU(42);
        assertEquals(42, config.getMTU());

        config.setMTU(1000);
        assertEquals(1000, config.getMTU());
    }

    @Test
    public void testDriver() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setDriver("driverName1");
        assertEquals("driverName1", config.getDriver());

        config.setDriver("driverName2");
        assertEquals("driverName2", config.getDriver());
    }

    @Test
    public void testDriverVersion() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setDriverVersion("driverVersion1");
        assertEquals("driverVersion1", config.getDriverVersion());

        config.setDriverVersion("driverVersion2");
        assertEquals("driverVersion2", config.getDriverVersion());
    }

    @Test
    public void testFirmwareVersion() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setFirmwareVersion("firmwareVersion1");
        assertEquals("firmwareVersion1", config.getFirmwareVersion());

        config.setFirmwareVersion("firmwareVersion2");
        assertEquals("firmwareVersion2", config.getFirmwareVersion());
    }

    @Test
    public void testState() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setState(NetInterfaceState.DISCONNECTED);
        assertEquals(NetInterfaceState.DISCONNECTED, config.getState());

        config.setState(NetInterfaceState.PREPARE);
        assertEquals(NetInterfaceState.PREPARE, config.getState());
    }

    @Test
    public void testUsbDevice() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        UsbDevice usbDevice1 = new UsbNetDevice("vendorId1", "productId1", "manufacturerName1", "productName1",
                "usbBusNumber1", "usbDevicePath1", "interfaceName1");
        config.setUsbDevice(usbDevice1);
        assertEquals(usbDevice1, config.getUsbDevice());

        UsbDevice usbDevice2 = new UsbNetDevice("vendorId2", "productId2", "manufacturerName2", "productName2",
                "usbBusNumber2", "usbDevicePath2", "interfaceName2");
        config.setUsbDevice(usbDevice2);
        assertEquals(usbDevice2, config.getUsbDevice());
    }

    @Test
    public void testAutoConnect() {
        EthernetInterfaceConfigImpl config = new EthernetInterfaceConfigImpl("name");

        config.setAutoConnect(true);
        assertEquals(true, config.isAutoConnect());

        config.setAutoConnect(false);
        assertEquals(false, config.isAutoConnect());
    }

    @Test
    public void testNetInterfaceAddresses() throws UnknownHostException {
        EthernetInterfaceConfigImpl config = createConfig(2);

        assertEquals(2, config.getNetInterfaceAddresses().size());

        assertEquals(createAddress("10.0.0.1"), config.getNetInterfaceAddresses().get(0));
        assertEquals(createAddress("10.0.0.2"), config.getNetInterfaceAddresses().get(1));
    }

    EthernetInterfaceConfigImpl createConfig(int noOfAddresses) throws UnknownHostException {
        EthernetInterfaceImpl<NetInterfaceAddressConfigImpl> interfaceImpl = new EthernetInterfaceImpl<>(
                "ethInterface");

        if (noOfAddresses > 0) {
            List<NetInterfaceAddressConfigImpl> interfaceAddresses = new ArrayList<>();

            for (int i = 0; i < noOfAddresses; i++) {
                String ipAddress = "10.0.0." + Integer.toString(i + 1);
                NetInterfaceAddressConfigImpl interfaceAddress = createAddress(ipAddress);
                interfaceAddresses.add(interfaceAddress);
            }

            interfaceImpl.setNetInterfaceAddresses(interfaceAddresses);
        } else {
            interfaceImpl.setNetInterfaceAddresses(null);
        }

        return new EthernetInterfaceConfigImpl(interfaceImpl);
    }

    NetInterfaceAddressConfigImpl createAddress(String ipAddress) throws UnknownHostException {
        NetInterfaceAddressConfigImpl interfaceAddress = new NetInterfaceAddressConfigImpl();

        interfaceAddress.setAddress(IPAddress.parseHostAddress(ipAddress));

        return interfaceAddress;
    }
}
