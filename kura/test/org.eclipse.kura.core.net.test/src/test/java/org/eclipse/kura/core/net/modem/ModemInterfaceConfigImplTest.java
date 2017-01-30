/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net.modem;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.modem.ModemPowerMode;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModemInterfaceConfigImplTest {

	@Test
	public void testModemInterfaceConfigImplString() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name1");
		assertEquals("name1", config.getName());
		assertTrue(config.getNetInterfaceAddresses().isEmpty());

		config = new ModemInterfaceConfigImpl("name2");
		assertEquals("name2", config.getName());
		assertTrue(config.getNetInterfaceAddresses().isEmpty());
	}

	@Test
	public void testModemInterfaceConfigImplModemInterfaceOfQextendsModemInterfaceAddress()
			throws UnknownHostException {
		ModemInterfaceConfigImpl config = createConfig(0);

		assertEquals("ModemInterface", config.getName());
		assertEquals(1, config.getNetInterfaceAddresses().size());
	}

	@Test
	public void testToString1() throws UnknownHostException {
		ModemInterfaceConfigImpl config = createConfig(0);

		String expected = "name=ModemInterface :: loopback=false :: pointToPoint=false :: virtual=false"
				+ " :: supportsMulticast=false :: up=false :: mtu=0 :: driver=null :: driverVersion=null :: firmwareVersion=null"
				+ " :: state=null :: autoConnect=false :: InterfaceAddress=NetConfig: no configurations ";

		assertEquals(expected, config.toString());
	}

	@Test
	public void testToString2() throws UnknownHostException {
		ModemInterfaceConfigImpl config = createConfig(2);

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

		String expected = "name=ModemInterface :: hardwareAddress=12:34:56:78:90:AB :: loopback=true :: pointToPoint=true"
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
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");
		assertEquals(NetInterfaceType.MODEM, config.getType());
	}

	@Test
	public void testPppNum() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setPppNum(10);
		assertEquals(10, config.getPppNum());

		config.setPppNum(20);
		assertEquals(20, config.getPppNum());
	}

	@Test
	public void testModemIdentifier() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setModemIdentifier("modemId1");
		assertEquals("modemId1", config.getModemIdentifier());

		config.setModemIdentifier("modemId2");
		assertEquals("modemId2", config.getModemIdentifier());
	}

	@Test
	public void testModel() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setModel("model1");
		assertEquals("model1", config.getModel());

		config.setModel("model2");
		assertEquals("model2", config.getModel());
	}

	@Test
	public void testManufacturer() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setManufacturer("manufacturer1");
		assertEquals("manufacturer1", config.getManufacturer());

		config.setManufacturer("manufacturer2");
		assertEquals("manufacturer2", config.getManufacturer());
	}

	@Test
	public void testSerialNumber() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setSerialNumber("serialNumber1");
		assertEquals("serialNumber1", config.getSerialNumber());

		config.setSerialNumber("serialNumber2");
		assertEquals("serialNumber2", config.getSerialNumber());
	}

	@Test
	public void testRevisionId() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		String[] rev1 = new String[] { "rev1", "rev2" };
		config.setRevisionId(rev1);
		assertArrayEquals(rev1, config.getRevisionId());

		String[] rev2 = new String[] { "rev3", "rev4" };
		config.setRevisionId(rev2);
		assertArrayEquals(rev2, config.getRevisionId());
	}

	@Test
	public void testTechnologyTypes() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		List<ModemTechnologyType> technologyTypes1 = new ArrayList<>();
		technologyTypes1.add(ModemTechnologyType.CDMA);
		technologyTypes1.add(ModemTechnologyType.EVDO);
		config.setTechnologyTypes(technologyTypes1);
		assertEquals(technologyTypes1, config.getTechnologyTypes());

		List<ModemTechnologyType> technologyTypes2 = new ArrayList<>();
		technologyTypes2.add(ModemTechnologyType.GPS);
		technologyTypes2.add(ModemTechnologyType.GSM_GPRS);
		config.setTechnologyTypes(technologyTypes2);
		assertEquals(technologyTypes2, config.getTechnologyTypes());
	}

	@Test
	public void testPoweredOn() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setPoweredOn(false);
		assertEquals(false, config.isPoweredOn());

		config.setPoweredOn(true);
		assertEquals(true, config.isPoweredOn());
	}

	@Test
	public void testPowerMode() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setPowerMode(ModemPowerMode.LOW_POWER);
		assertEquals(ModemPowerMode.LOW_POWER, config.getPowerMode());

		config.setPowerMode(ModemPowerMode.ONLINE);
		assertEquals(ModemPowerMode.ONLINE, config.getPowerMode());
	}

	@Test
	public void testModemDevice() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		UsbModemDevice modemDevice1 = new UsbModemDevice("vendorId1", "productId1", "manufacturerName1", "productName1",
				"usbBusNumber1", "usbDevicePath1");
		config.setModemDevice(modemDevice1);
		assertEquals(modemDevice1, config.getModemDevice());

		UsbModemDevice modemDevice2 = new UsbModemDevice("vendorId2", "productId2", "manufacturerName2", "productName2",
				"usbBusNumber2", "usbDevicePath2");
		config.setModemDevice(modemDevice2);
		assertEquals(modemDevice2, config.getModemDevice());
	}

	@Test
	public void testGpsSupported() {
		ModemInterfaceConfigImpl config = new ModemInterfaceConfigImpl("name");

		config.setGpsSupported(false);
		assertEquals(false, config.isGpsSupported());

		config.setGpsSupported(true);
		assertEquals(true, config.isGpsSupported());
	}

	ModemInterfaceConfigImpl createConfig(int noOfAddresses) throws UnknownHostException {
		ModemInterfaceImpl<ModemInterfaceAddressConfigImpl> interfaceImpl = new ModemInterfaceImpl<>(
				"ModemInterface");

		if (noOfAddresses > 0) {
			List<ModemInterfaceAddressConfigImpl> interfaceAddresses = new ArrayList<>();

			for (int i = 0; i < noOfAddresses; i++) {
				String ipAddress = "10.0.0." + Integer.toString(i + 1);
				ModemInterfaceAddressConfigImpl interfaceAddress = createAddress(ipAddress);
				interfaceAddresses.add(interfaceAddress);
			}

			interfaceImpl.setNetInterfaceAddresses(interfaceAddresses);
		} else {
			interfaceImpl.setNetInterfaceAddresses(null);
		}

		return new ModemInterfaceConfigImpl(interfaceImpl);
	}

	ModemInterfaceAddressConfigImpl createAddress(String ipAddress) throws UnknownHostException {
		ModemInterfaceAddressConfigImpl interfaceAddress = new ModemInterfaceAddressConfigImpl();

		interfaceAddress.setAddress(IPAddress.parseHostAddress(ipAddress));

		return interfaceAddress;
	}
}
