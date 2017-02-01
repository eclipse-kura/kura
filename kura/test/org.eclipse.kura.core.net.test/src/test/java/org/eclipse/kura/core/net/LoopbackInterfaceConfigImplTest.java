/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
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
import java.util.List;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.usb.UsbNetDevice;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoopbackInterfaceConfigImplTest {

	@Test
	public void testLoopbackInterfaceConfigImplString() {
		LoopbackInterfaceConfigImpl config = new LoopbackInterfaceConfigImpl("name1");
		assertEquals("name1", config.getName());
		assertTrue(config.getNetInterfaceAddresses().isEmpty());

		config = new LoopbackInterfaceConfigImpl("name2");
		assertEquals("name2", config.getName());
		assertTrue(config.getNetInterfaceAddresses().isEmpty());
	}

	@Test
	public void testLoopbackInterfaceConfigImplLoopbackInterfaceOfQextendsNetInterfaceAddress()
			throws UnknownHostException {
		LoopbackInterfaceConfigImpl config = createConfig(0);

		assertEquals("loopbackInterface", config.getName());
		assertEquals(1, config.getNetInterfaceAddresses().size());
	}

	@Test
	public void testToString1() throws UnknownHostException {
		LoopbackInterfaceConfigImpl config = createConfig(0);

		String expected = "name=loopbackInterface :: loopback=false :: pointToPoint=false :: virtual=false"
				+ " :: supportsMulticast=false :: up=false :: mtu=0 :: driver=null :: driverVersion=null"
				+ " :: firmwareVersion=null :: state=null :: autoConnect=false :: InterfaceAddress=NetConfig:"
				+ " no configurations ";

		assertEquals(expected, config.toString());
	}

	@Test
	public void testToString2() throws UnknownHostException {
		LoopbackInterfaceConfigImpl config = createConfig(2);

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

		String expected = "name=loopbackInterface :: hardwareAddress=12:34:56:78:90:AB :: loopback=true :: pointToPoint=true"
				+ " :: virtual=true :: supportsMulticast=true :: up=true :: mtu=42 :: usbDevice=UsbNetDevice"
				+ " [getInterfaceName()=interfaceName, getVendorId()=vendorId, getProductId()=productId,"
				+ " getManufacturerName()=manufacturerName, getProductName()=productName, getUsbBusNumber()=usbBusNumber,"
				+ " getUsbDevicePath()=usbDevicePath, getUsbPort()=usbBusNumber-usbDevicePath] :: driver=driverName"
				+ " :: driverVersion=driverVersion :: firmwareVersion=firmwareVersion :: state=ACTIVATED :: autoConnect=true"
				+ " :: InterfaceAddress=NetConfig: no configurations NetConfig: no configurations ";

		assertEquals(expected, config.toString());
	}

	@Test
	public void testGetType() {
		LoopbackInterfaceConfigImpl config = new LoopbackInterfaceConfigImpl("name");
		assertEquals(NetInterfaceType.LOOPBACK, config.getType());
	}

	LoopbackInterfaceConfigImpl createConfig(int noOfAddresses) throws UnknownHostException {
		LoopbackInterfaceImpl<NetInterfaceAddressConfigImpl> interfaceImpl = new LoopbackInterfaceImpl<NetInterfaceAddressConfigImpl>(
				"loopbackInterface");

		if (noOfAddresses > 0) {
			List<NetInterfaceAddressConfigImpl> interfaceAddresses = new ArrayList<NetInterfaceAddressConfigImpl>();

			for (int i = 0; i < noOfAddresses; i++) {
				String ipAddress = "10.0.0." + Integer.toString(i + 1);
				NetInterfaceAddressConfigImpl interfaceAddress = createAddress(ipAddress);
				interfaceAddresses.add(interfaceAddress);
			}

			interfaceImpl.setNetInterfaceAddresses(interfaceAddresses);
		} else {
			interfaceImpl.setNetInterfaceAddresses(null);
		}

		LoopbackInterfaceConfigImpl config = new LoopbackInterfaceConfigImpl(interfaceImpl);
		return config;
	}

	NetInterfaceAddressConfigImpl createAddress(String ipAddress) throws UnknownHostException {
		NetInterfaceAddressConfigImpl interfaceAddress = new NetInterfaceAddressConfigImpl();

		interfaceAddress.setAddress(IPAddress.parseHostAddress(ipAddress));

		return interfaceAddress;
	}
}
