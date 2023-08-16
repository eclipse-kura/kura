/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.Test;

public class NMStatusConverterTest {

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";

    private Properties mockDeviceProperties = mock(Properties.class);
    private Properties mockIp4ConfigProperties = mock(Properties.class);

    private DevicePropertiesWrapper mockDevicePropertiesWrapper;

    private NetworkInterfaceStatus resultingStatus;
    private EthernetInterfaceStatus resultingEthernetStatus;

    private Exception occurredException;

    @Test
    public void buildLoopbackStatusThrowsWithEmptyProperties() {
        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper, Optional.empty(), Optional.empty());

        thenExceptionOccurred(NullPointerException.class);
    }

    @Test
    public void buildLoopbackStatusThrowsWithPartialProperties() {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_UNMANAGED));
        givenDevicePropertiesWith("Autoconnect", true);
        givenDevicePropertiesWith("FirmwareVersion", "awesomeFirmwareVersion");

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper, Optional.empty(), Optional.empty());

        thenExceptionOccurred(NullPointerException.class);
    }

    private void givenDevicePropertiesWrapperBuiltWith(Properties deviceProperties,
            Optional<Properties> deviceSpecificProperties, NMDeviceType nmDeviceType) {
        this.mockDevicePropertiesWrapper = new DevicePropertiesWrapper(deviceProperties, deviceSpecificProperties,
                nmDeviceType);
    }

    @Test
    public void buildLoopbackStatusWorksWithoutIPV4Info() {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_UNMANAGED));
        givenDevicePropertiesWith("Autoconnect", true);
        givenDevicePropertiesWith("FirmwareVersion", "awesomeFirmwareVersion");
        givenDevicePropertiesWith("Driver", "awesomeDriver");
        givenDevicePropertiesWith("DriverVersion", "awesomeDriverVersion");
        givenDevicePropertiesWith("Mtu", new UInt32(42));
        givenDevicePropertiesWith("HwAddress", "00:00:00:00:00:00");

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper, Optional.empty(), Optional.empty());

        thenNoExceptionIsThrown();

        thenResultingNetworkInterfaceIsVirtual(true);
        thenResultingNetworkInterfaceAutoConnectIs(true);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.UNMANAGED);
        thenResultingNetworkInterfaceFirmwareVersionIs("awesomeFirmwareVersion");
        thenResultingNetworkInterfaceDriverIs("awesomeDriver");
        thenResultingNetworkInterfaceDriverVersionIs("awesomeDriverVersion");
        thenResultingNetworkInterfaceMtuIs(42);
        thenResultingNetworkInterfaceHardwareAddressIs(new byte[] { 0, 0, 0, 0, 0, 0 });

        thenResultingIp4InterfaceAddressIsMissing();
    }

    @Test
    public void buildLoopbackStatusWorksWithIPV4Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv4ConfigPropertiesWith("Gateway", "");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList());
        givenIpv4ConfigPropertiesWithAddress("127.0.0.1", new UInt32(8));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionIsThrown();

        thenResultingNetworkInterfaceIsVirtual(true);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("isThisRealLife");
        thenResultingNetworkInterfaceDriverIs("isThisJustFantasy");
        thenResultingNetworkInterfaceDriverVersionIs("caughtInALandslide");
        thenResultingNetworkInterfaceMtuIs(69);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x32, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });

        thenResultingIp4InterfaceGatewayIsMissing();
        thenResultingIp4InterfaceDNSIsMissing();
        thenResultingIp4InterfaceAddressIs(IPAddress.parseHostAddress("127.0.0.1"), (short) 8);
    }

    @Test
    public void buildEthernetStatusThrowsWithEmptyProperties() {
        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper, Optional.empty(),
                Optional.empty());

        thenExceptionOccurred(NullPointerException.class);
    }

    @Test
    public void buildEthernetStatusThrowsWithPartialProperties() {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_UNMANAGED));
        givenDevicePropertiesWith("Autoconnect", true);
        givenDevicePropertiesWith("FirmwareVersion", "awesomeFirmwareVersion");

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper, Optional.empty(),
                Optional.empty());

        thenExceptionOccurred(NullPointerException.class);
    }

    @Test
    public void buildEthernetStatusWorksWithoutIPV4Info() {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_UNMANAGED));
        givenDevicePropertiesWith("Autoconnect", true);
        givenDevicePropertiesWith("FirmwareVersion", "awesomeFirmwareVersion");
        givenDevicePropertiesWith("Driver", "awesomeDriver");
        givenDevicePropertiesWith("DriverVersion", "awesomeDriverVersion");
        givenDevicePropertiesWith("Mtu", new UInt32(42));
        givenDevicePropertiesWith("HwAddress", "00:00:00:00:00:00");

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper, Optional.empty(),
                Optional.empty());

        thenNoExceptionIsThrown();

        thenResultingNetworkInterfaceIsVirtual(false);
        thenResultingNetworkInterfaceAutoConnectIs(true);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.UNMANAGED);
        thenResultingNetworkInterfaceFirmwareVersionIs("awesomeFirmwareVersion");
        thenResultingNetworkInterfaceDriverIs("awesomeDriver");
        thenResultingNetworkInterfaceDriverVersionIs("awesomeDriverVersion");
        thenResultingNetworkInterfaceMtuIs(42);
        thenResultingNetworkInterfaceHardwareAddressIs(new byte[] { 0, 0, 0, 0, 0, 0 });

        thenResultingIp4InterfaceAddressIsMissing();
    }

    @Test
    public void buildEthernetStatusWorksWithIPV4Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv4ConfigPropertiesWith("Gateway", "192.168.1.1");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList("192.168.1.10"));
        givenIpv4ConfigPropertiesWithAddress("192.168.1.82", new UInt32(24));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionIsThrown();

        thenResultingNetworkInterfaceIsVirtual(false);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("isThisRealLife");
        thenResultingNetworkInterfaceDriverIs("isThisJustFantasy");
        thenResultingNetworkInterfaceDriverVersionIs("caughtInALandslide");
        thenResultingNetworkInterfaceMtuIs(69);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x32, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });

        thenResultingEthernetInterfaceLinkUpIs(true);

        thenResultingIp4InterfaceGatewayIs(IPAddress.parseHostAddress("192.168.1.1"));
        thenResultingIp4InterfaceDNSIs(Arrays.asList(IPAddress.parseHostAddress("192.168.1.10")));
        thenResultingIp4InterfaceAddressIs(IPAddress.parseHostAddress("192.168.1.82"), (short) 24);
    }

    /*
     * Given
     */

    private void givenDevicePropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockDeviceProperties.Get(NM_DEVICE_BUS_NAME, propertyName))
                .thenReturn(propertyValue);
    }

    private void givenIpv4ConfigPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockIp4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, propertyName))
                .thenReturn(propertyValue);
    }

    private void givenIpv4ConfigPropertiesWithDNS(List<String> addresses) {
        List<Map<String, Variant<?>>> addressList = new ArrayList<>();

        for (String address : addresses) {
            Map<String, Variant<?>> structure = new HashMap<>();
            structure.put("address", new Variant<>(address));

            addressList.add(structure);
        }

        when(this.mockIp4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, "NameserverData")).thenReturn(addressList);
    }

    private void givenIpv4ConfigPropertiesWithAddress(String address, UInt32 prefix) {
        Map<String, Variant<?>> structure = new HashMap<>();
        structure.put("address", new Variant<>(address));
        structure.put("prefix", new Variant<>(prefix));

        when(this.mockIp4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, "AddressData"))
                .thenReturn(Arrays.asList(structure));
    }

    /*
     * When
     */

    private void whenBuildLoopbackStatusIsCalledWith(String ifaceName, DevicePropertiesWrapper deviceProps,
            Optional<Properties> ip4Properties, Optional<Properties> ip6Properties) {
        try {
            this.resultingStatus = NMStatusConverter.buildLoopbackStatus(ifaceName, deviceProps, ip4Properties,
                    ip6Properties);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenBuildEthernetStatusIsCalledWith(String ifaceName, DevicePropertiesWrapper deviceProps,
            Optional<Properties> ip4Properties, Optional<Properties> ip6Properties) {
        try {
            this.resultingStatus = NMStatusConverter.buildEthernetStatus(ifaceName, deviceProps, ip4Properties,
                    ip6Properties);
            this.resultingEthernetStatus = (EthernetInterfaceStatus) this.resultingStatus;
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionIsThrown() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    private void thenResultingNetworkInterfaceIsVirtual(boolean expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.isVirtual());
    }

    private void thenResultingNetworkInterfaceAutoConnectIs(boolean expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.isAutoConnect());
    }

    private void thenResultingNetworkInterfaceStateIs(NetworkInterfaceState expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.getState());
    }

    private void thenResultingNetworkInterfaceFirmwareVersionIs(String expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.getFirmwareVersion());
    }

    private void thenResultingNetworkInterfaceDriverIs(String expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.getDriver());
    }

    private void thenResultingNetworkInterfaceDriverVersionIs(String expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.getDriverVersion());
    }

    private void thenResultingNetworkInterfaceMtuIs(int expectedResult) {
        assertEquals(expectedResult, this.resultingStatus.getMtu());
    }

    private void thenResultingNetworkInterfaceHardwareAddressIs(byte[] expectedResult) {
        assertArrayEquals(expectedResult, this.resultingStatus.getHardwareAddress());
    }

    private void thenResultingIp4InterfaceAddressIsMissing() {
        assertFalse(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
    }

    private void thenResultingIp4InterfaceGatewayIsMissing() {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        assertFalse(address.getGateway().isPresent());
    }

    private void thenResultingIp4InterfaceGatewayIs(IPAddress expectedResult) {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        assertTrue(address.getGateway().isPresent());
        assertEquals(expectedResult, address.getGateway().get());
    }

    private void thenResultingIp4InterfaceDNSIsMissing() {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        List<IP4Address> dns = address.getDnsServerAddresses();
        assertTrue(dns.isEmpty());
    }

    private void thenResultingIp4InterfaceDNSIs(List<IPAddress> expectedDNSAddresses) {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        List<IP4Address> dns = address.getDnsServerAddresses();
        assertEquals(expectedDNSAddresses.size(), dns.size());

        for (IPAddress expectedDNSAddress : expectedDNSAddresses) {
            assertTrue(dns.contains(expectedDNSAddress));
        }
    }

    private void thenResultingIp4InterfaceAddressIs(IPAddress expectedAddress, short expectedPrefix) {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        List<NetworkInterfaceIpAddress<IP4Address>> addresses = address.getAddresses();
        assertEquals(1, addresses.size());

        assertEquals(expectedAddress, addresses.get(0).getAddress());
        assertEquals(expectedPrefix, addresses.get(0).getPrefix());
    }

    private void thenResultingEthernetInterfaceLinkUpIs(boolean expectedResult) {
        assertEquals(expectedResult, this.resultingEthernetStatus.isLinkUp());
    }

}
