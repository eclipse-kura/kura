/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.eclipse.kura.net.status.modem.BearerIpType;
import org.eclipse.kura.net.status.modem.ModemBand;
import org.eclipse.kura.net.status.modem.ModemCapability;
import org.eclipse.kura.net.status.modem.ModemConnectionStatus;
import org.eclipse.kura.net.status.modem.ModemGpsMode;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemMode;
import org.eclipse.kura.net.status.modem.ModemModePair;
import org.eclipse.kura.net.status.modem.ModemPortType;
import org.eclipse.kura.net.status.modem.ModemPowerState;
import org.eclipse.kura.net.status.modem.RegistrationStatus;
import org.eclipse.kura.net.status.modem.SimType;
import org.eclipse.kura.net.status.vlan.VlanInterfaceStatus;
import org.eclipse.kura.nm.enums.MMBearerIpFamily;
import org.eclipse.kura.nm.enums.MMModem3gppRegistrationState;
import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.eclipse.kura.nm.enums.MMModemPowerState;
import org.eclipse.kura.nm.enums.MMSimType;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.junit.Test;

public class NMStatusConverterTest {

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";
    private static final String NM_IP6CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP6Config";
    private static final String NM_VLAN_BUS_NAME = "org.freedesktop.NetworkManager.Device.Vlan";
    private static final String MM_MODEM_BUS_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MM_SIM_BUS_NAME = "org.freedesktop.ModemManager1.Sim";
    private static final String MM_BEARER_BUS_NAME = "org.freedesktop.ModemManager1.Bearer";
    private static final String MM_MODEM_LOCATION_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Location";
    private static final String MM_MODEM_3GPP_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Modem3gpp";

    private Properties mockDeviceProperties = mock(Properties.class);
    private Properties mockParentDeviceProperties = mock(Properties.class);
    private Properties mockIp4ConfigProperties = mock(Properties.class);
    private Properties mockIp6ConfigProperties = mock(Properties.class);
    private Properties mockVlanConfigProperties = mock(Properties.class);
    private Properties mockModemProperties = mock(Properties.class);
    private SimProperties mockSimProperties = mock(SimProperties.class);
    private Properties mockAvailableSimProperties = mock(Properties.class);
    private Properties mockBearerProperties = mock(Properties.class);
    private Map<String, Object> bearerSettings = new HashMap<>();
    private Map<String, Object> bearerStats = new HashMap<>();

    private DevicePropertiesWrapper mockDevicePropertiesWrapper;

    private NetworkInterfaceStatus resultingStatus;
    private EthernetInterfaceStatus resultingEthernetStatus;
    private VlanInterfaceStatus resultingVlanStatus;
    private ModemInterfaceStatus resultingModemStatus;

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

    @Test
    public void buildLoopbackStatusWorksWithoutIPInfo() {
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

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(true);
        thenResultingNetworkInterfaceAutoConnectIs(true);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.UNMANAGED);
        thenResultingNetworkInterfaceFirmwareVersionIs("awesomeFirmwareVersion");
        thenResultingNetworkInterfaceDriverIs("awesomeDriver");
        thenResultingNetworkInterfaceDriverVersionIs("awesomeDriverVersion");
        thenResultingNetworkInterfaceMtuIs(42);
        thenResultingNetworkInterfaceHardwareAddressIs(new byte[] { 0, 0, 0, 0, 0, 0 });

        thenResultingIp4InterfaceAddressIsMissing();
        thenResultingIp6InterfaceAddressIsMissing();
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
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("127.0.0.1/8"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionOccurred();

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
        thenResultingIp4InterfaceAddressIs(Arrays.asList(new NetworkInterfaceIpAddress<IP4Address>(
                (IP4Address) IPAddress.parseHostAddress("127.0.0.1"), (short) 8)));

        thenResultingIp6InterfaceAddressIsMissing();
    }

    @Test
    public void buildLoopbackStatusWorksWithWrongIPV4Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv4ConfigPropertiesWith("Gateway", "");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList());
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("not-an-ip-address/8"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(true);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("isThisRealLife");
        thenResultingNetworkInterfaceDriverIs("isThisJustFantasy");
        thenResultingNetworkInterfaceDriverVersionIs("caughtInALandslide");
        thenResultingNetworkInterfaceMtuIs(69);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x32, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });

        thenResultingIp4InterfaceAddressIsMissing();
        thenResultingIp6InterfaceAddressIsMissing();
    }

    @Test
    public void buildLoopbackStatusWorksWithIPV6Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv6ConfigPropertiesWith("Gateway", "");
        givenIpv6ConfigPropertiesWithDNS(Arrays.asList());
        givenIpv6ConfigPropertiesWithAddress(Arrays.asList("::1/128"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDevicePropertiesWrapper, Optional.empty(),
                Optional.of(this.mockIp6ConfigProperties));

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(true);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("isThisRealLife");
        thenResultingNetworkInterfaceDriverIs("isThisJustFantasy");
        thenResultingNetworkInterfaceDriverVersionIs("caughtInALandslide");
        thenResultingNetworkInterfaceMtuIs(69);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x32, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });

        thenResultingIp6InterfaceGatewayIsMissing();
        thenResultingIp6InterfaceDNSIsMissing();
        thenResultingIp6InterfaceAddressIs(Arrays.asList(new NetworkInterfaceIpAddress<IP6Address>(
                (IP6Address) IP6Address.parseHostAddress("::1"), (short) 128)));
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
    public void buildEthernetStatusWorksWithoutIPInfo() {
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

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(false);
        thenResultingNetworkInterfaceAutoConnectIs(true);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.UNMANAGED);
        thenResultingNetworkInterfaceFirmwareVersionIs("awesomeFirmwareVersion");
        thenResultingNetworkInterfaceDriverIs("awesomeDriver");
        thenResultingNetworkInterfaceDriverVersionIs("awesomeDriverVersion");
        thenResultingNetworkInterfaceMtuIs(42);
        thenResultingNetworkInterfaceHardwareAddressIs(new byte[] { 0, 0, 0, 0, 0, 0 });

        thenResultingIp4InterfaceAddressIsMissing();
        thenResultingIp6InterfaceAddressIsMissing();
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
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("192.168.1.82/24"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionOccurred();

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
        thenResultingIp4InterfaceAddressIs(Arrays.asList(new NetworkInterfaceIpAddress<IP4Address>(
                (IP4Address) IP4Address.parseHostAddress("192.168.1.82"), (short) 24)));

        thenResultingIp6InterfaceAddressIsMissing();
    }

    @Test
    public void buildEthernetStatusWorksWithMultipleIPV4Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv4ConfigPropertiesWith("Gateway", "192.168.1.1");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList("8.8.8.8", "8.8.4.4"));
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("192.168.1.82/24", "192.168.3.69/24"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionOccurred();

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
        thenResultingIp4InterfaceDNSIs(
                Arrays.asList(IPAddress.parseHostAddress("8.8.4.4"), IPAddress.parseHostAddress("8.8.8.8")));
        thenResultingIp4InterfaceAddressIs(Arrays.asList(
                new NetworkInterfaceIpAddress<IP4Address>((IP4Address) IP4Address.parseHostAddress("192.168.1.82"),
                        (short) 24),
                new NetworkInterfaceIpAddress<IP4Address>((IP4Address) IP4Address.parseHostAddress("192.168.3.69"),
                        (short) 24)));

        thenResultingIp6InterfaceAddressIsMissing();
    }

    @Test
    public void buildEthernetStatusWorksWithIPV6Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv6ConfigPropertiesWith("Gateway", "fe80:0:0:0:dea6:32ff:fee0:0001");
        givenIpv6ConfigPropertiesWithDNS(Arrays.asList("20.01.48.60.48.60.00.00.00.00.00.00.00.00.88.88"));
        givenIpv6ConfigPropertiesWithAddress(Arrays.asList("fe80::dea6:32ff:fee0:54f0/64"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper, Optional.empty(),
                Optional.of(this.mockIp6ConfigProperties));

        thenNoExceptionOccurred();

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

        thenResultingIp4InterfaceAddressIsMissing();

        thenResultingIp6InterfaceGatewayIs(IPAddress.parseHostAddress("fe80::dea6:32ff:fee0:0001"));
        thenResultingIp6InterfaceDNSIs(Arrays.asList(IPAddress.parseHostAddress("2001:4860:4860:0:0:0:0:8888")));
        thenResultingIp6InterfaceAddressIs(Arrays.asList(new NetworkInterfaceIpAddress<IP6Address>(
                (IP6Address) IP6Address.parseHostAddress("fe80::dea6:32ff:fee0:54f0"), (short) 64)));
    }

    @Test
    public void buildEthernetStatusWorksWithMultipleIPV6Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");

        givenIpv6ConfigPropertiesWith("Gateway", "fe80:0:0:0:dea6:32ff:fee0:0001");
        givenIpv6ConfigPropertiesWithDNS(Arrays.asList("20.01.48.60.48.60.00.00.00.00.00.00.00.00.88.88",
                "20.01.48.60.48.60.00.00.00.00.00.00.00.00.88.44"));
        givenIpv6ConfigPropertiesWithAddress(
                Arrays.asList("fe80::dea6:32ff:fee0:54f0/64", "fe80::dea6:32ff:fee0:54f6/64"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper, Optional.empty(),
                Optional.of(this.mockIp6ConfigProperties));

        thenNoExceptionOccurred();

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

        thenResultingIp4InterfaceAddressIsMissing();

        thenResultingIp6InterfaceGatewayIs(IPAddress.parseHostAddress("fe80::dea6:32ff:fee0:0001"));
        thenResultingIp6InterfaceDNSIs(Arrays.asList(IPAddress.parseHostAddress("2001:4860:4860:0:0:0:0:8844"),
                IPAddress.parseHostAddress("2001:4860:4860:0:0:0:0:8888")));
        thenResultingIp6InterfaceAddressIs(Arrays.asList(
                new NetworkInterfaceIpAddress<IP6Address>(
                        (IP6Address) IP6Address.parseHostAddress("fe80::dea6:32ff:fee0:54f0"), (short) 64),
                new NetworkInterfaceIpAddress<IP6Address>(
                        (IP6Address) IP6Address.parseHostAddress("fe80::dea6:32ff:fee0:54f6"), (short) 64)));
    }

    @Test
    public void buildEthernetStatusWorksWithBothIPV4AndIPV6Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "anAwesomeFirmwareVersion");
        givenDevicePropertiesWith("Driver", "anAwesomeDriver");
        givenDevicePropertiesWith("DriverVersion", "anAwesomeDriverVersion");
        givenDevicePropertiesWith("Mtu", new UInt32(42));
        givenDevicePropertiesWith("HwAddress", "DE:AD:BE:EF:66:69");

        givenIpv4ConfigPropertiesWith("Gateway", "192.168.1.1");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList("8.8.8.8", "8.8.4.4"));
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("192.168.1.82/24", "192.168.3.69/24"));

        givenIpv6ConfigPropertiesWith("Gateway", "fe80:0:0:0:dea6:32ff:fee0:0001");
        givenIpv6ConfigPropertiesWithDNS(Arrays.asList("20.01.48.60.48.60.00.00.00.00.00.00.00.00.88.88",
                "20.01.48.60.48.60.00.00.00.00.00.00.00.00.88.44"));
        givenIpv6ConfigPropertiesWithAddress(
                Arrays.asList("fe80::dea6:32ff:fee0:54f0/64", "fe80::dea6:32ff:fee0:54f6/64"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.empty(),
                NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

        whenBuildEthernetStatusIsCalledWith("eth0", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.of(this.mockIp6ConfigProperties));

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(false);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("anAwesomeFirmwareVersion");
        thenResultingNetworkInterfaceDriverIs("anAwesomeDriver");
        thenResultingNetworkInterfaceDriverVersionIs("anAwesomeDriverVersion");
        thenResultingNetworkInterfaceMtuIs(42);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x66, (byte) 0x69 });

        thenResultingEthernetInterfaceLinkUpIs(true);

        thenResultingIp4InterfaceGatewayIs(IPAddress.parseHostAddress("192.168.1.1"));
        thenResultingIp4InterfaceDNSIs(
                Arrays.asList(IPAddress.parseHostAddress("8.8.4.4"), IPAddress.parseHostAddress("8.8.8.8")));
        thenResultingIp4InterfaceAddressIs(Arrays.asList(
                new NetworkInterfaceIpAddress<IP4Address>((IP4Address) IP4Address.parseHostAddress("192.168.1.82"),
                        (short) 24),
                new NetworkInterfaceIpAddress<IP4Address>((IP4Address) IP4Address.parseHostAddress("192.168.3.69"),
                        (short) 24)));

        thenResultingIp6InterfaceGatewayIs(IPAddress.parseHostAddress("fe80::dea6:32ff:fee0:0001"));
        thenResultingIp6InterfaceDNSIs(Arrays.asList(IPAddress.parseHostAddress("2001:4860:4860:0:0:0:0:8844"),
                IPAddress.parseHostAddress("2001:4860:4860:0:0:0:0:8888")));
        thenResultingIp6InterfaceAddressIs(Arrays.asList(
                new NetworkInterfaceIpAddress<IP6Address>(
                        (IP6Address) IP6Address.parseHostAddress("fe80::dea6:32ff:fee0:54f0"), (short) 64),
                new NetworkInterfaceIpAddress<IP6Address>(
                        (IP6Address) IP6Address.parseHostAddress("fe80::dea6:32ff:fee0:54f6"), (short) 64)));
    }

    @Test
    public void buildVlanStatusWorksWithIPV4Info() throws UnknownHostException {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "thunderbolt");
        givenDevicePropertiesWith("Driver", "lightning");
        givenDevicePropertiesWith("DriverVersion", "frightening");
        givenDevicePropertiesWith("Mtu", new UInt32(420));
        givenDevicePropertiesWith("HwAddress", "F5:5B:66:7C:40:EA");

        givenVlanConfigPropertiesWith("VlanId", new UInt32(101));
        givenParentDevicePropertiesWith("Interface", "eth0");

        givenIpv4ConfigPropertiesWith("Gateway", "192.168.3.241");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList("1.1.1.1"));
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("192.168.3.242/28"));

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.of(this.mockVlanConfigProperties),
                NMDeviceType.NM_DEVICE_TYPE_VLAN);

        whenBuildVlanStatusIsCalledWith("eth0.101", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty());

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(true);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("thunderbolt");
        thenResultingNetworkInterfaceDriverIs("lightning");
        thenResultingNetworkInterfaceDriverVersionIs("frightening");
        thenResultingNetworkInterfaceMtuIs(420);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x66, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });

        thenResultingVlanIdIs(101);
        thenResultingVlanParentInterfaceIs("eth0");

        thenResultingIp4InterfaceGatewayIs(IPAddress.parseHostAddress("192.168.3.241"));
        thenResultingIp4InterfaceDNSIs(Arrays.asList(IPAddress.parseHostAddress("1.1.1.1")));
        thenResultingIp4InterfaceAddressIs(Arrays.asList(new NetworkInterfaceIpAddress<IP4Address>(
                (IP4Address) IP4Address.parseHostAddress("192.168.3.242"), (short) 28)));

        thenResultingIp6InterfaceAddressIsMissing();
    }

    @Test
    public void buildModemStatusWorksWithDeviceInfo() {
        givenModemConfiguration();

        whenBuildModemStatusIsCalledWith("1-4", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty(), Arrays.asList(this.mockSimProperties),
                Arrays.asList(this.mockBearerProperties));

        thenNoExceptionOccurred();

        thenResultingNetworkInterfaceIsVirtual(false);
        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("isThisRealLife");
        thenResultingNetworkInterfaceDriverIs("isThisJustFantasy");
        thenResultingNetworkInterfaceDriverVersionIs("caughtInALandslide");
        thenResultingNetworkInterfaceMtuIs(69);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x32, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });
        thenResultingNetworkInterfaceIpInterfaceIs("cdc-acm0");
    }

    @Test
    public void buildModemStatusWorksWithIPInfo() throws UnknownHostException {
        givenModemConfiguration();

        whenBuildModemStatusIsCalledWith("1-4", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty(), Arrays.asList(this.mockSimProperties),
                Arrays.asList(this.mockBearerProperties));

        thenNoExceptionOccurred();

        thenResultingIp4InterfaceGatewayIs(IPAddress.parseHostAddress("192.168.1.1"));
        thenResultingIp4InterfaceDNSIs(Arrays.asList(IPAddress.parseHostAddress("192.168.1.10")));
        thenResultingIp4InterfaceAddressIs(Arrays.asList(new NetworkInterfaceIpAddress<IP4Address>(
                (IP4Address) IP4Address.parseHostAddress("192.168.1.82"), (short) 24)));

        thenResultingIp6InterfaceAddressIsMissing();

    }

    @Test
    public void buildModemStatusWorksWithModemInfo() {
        givenModemConfiguration();

        whenBuildModemStatusIsCalledWith("1-4", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty(), Arrays.asList(this.mockSimProperties),
                Arrays.asList(this.mockBearerProperties));

        thenNoExceptionOccurred();

        thenResultingModemModelIs("EC25");
        thenResultingModemManufacturerIs("Quectel");
        thenResultingModemEquipmentIdentifierIs("CoolQuectel");
        thenResultingModemRevisionIs("Experimental");
        thenResultingModemHWRevisionIs("TopSecret");
        thenResultingModemPrimaryPortIs("NaN");
        thenResultingModemPortsAre(new HashMap<String, ModemPortType>());
        thenResultingModemSupportedCapabilitiesAre(EnumSet.of(ModemCapability.POTS, ModemCapability.GSM_UMTS),
                EnumSet.of(ModemCapability.LTE, ModemCapability.GSM_UMTS));
        thenResultingModemCurrentCapabilitiesAre(EnumSet.of(ModemCapability.POTS, ModemCapability.GSM_UMTS));
        thenResultingModemPowerStateIs(ModemPowerState.ON);
        thenResultingModemSupportedModesAre(new HashSet<>());
        thenResultingModemCurrentModesAre(new ModemModePair(Collections.emptySet(), ModemMode.NONE));
        thenResultingModemSupportedBandsAre(new HashSet<>(Arrays.asList(ModemBand.EGSM, ModemBand.EUTRAN_2)));
        thenResultingModemCurrentBandsAre(new HashSet<>(Arrays.asList(ModemBand.EUTRAN_6, ModemBand.UNKNOWN)));
        thenResultingModemUnlockRequiredIs(false);
        thenResultingModemStateIs(ModemConnectionStatus.CONNECTED);
        thenResultingModemAccessTechnologiesAre(new HashSet<>(Arrays.asList(AccessTechnology.FIVEGNR)));
        thenResultingModemSignalQualityIs(23);
        thenResultingModemSignalStrengthIs(-99);
        thenResultingModemRegistrationStateIs(RegistrationStatus.HOME);
        thenResultingModemOperatorNameIs("VodaTim");
        thenResultingModemLocationCapabilitiesAre(new HashSet<>(Arrays.asList(ModemGpsMode.UNMANAGED)));
        thenResultingModemIsGpsSupported(true);

    }

    @Test
    public void buildModemStatusWorksWithModemSimInfo() {
        givenModemConfiguration();

        whenBuildModemStatusIsCalledWith("1-4", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty(), Arrays.asList(this.mockSimProperties),
                Arrays.asList(this.mockBearerProperties));

        thenNoExceptionOccurred();
        thenResultingModemSimIsActive(true);
        thenResultingModemSimIsPrimary(true);
        thenResultingModemSimIdentifierIs("1234567890123456789");
        thenResultingModemSimImsiIs("098765432109876");
        thenResultingModemSimEidIs("AAA");
        thenResultingModemSimOperatorNameIs("VODA");
        thenResultingModemSimOperatorIdIs("22210");
        thenResultingModemSimTypeIs(SimType.PHYSICAL);
    }

    @Test
    public void buildModemStatusWorksWithModemBearerInfo() {
        givenModemConfiguration();

        whenBuildModemStatusIsCalledWith("1-4", this.mockDevicePropertiesWrapper,
                Optional.of(this.mockIp4ConfigProperties), Optional.empty(), Arrays.asList(this.mockSimProperties),
                Arrays.asList(this.mockBearerProperties));

        thenNoExceptionOccurred();
        thenResultingModemBearerInterfaceIs("TheModemInterface");
        thenResultingModemBearerIsConnected(true);
        thenResultingModemBearerApnIs("web.omnitel.it");
        thenResultingModemBearerIpTypeIs(EnumSet.of(BearerIpType.IPV4V6));
        thenResultingModemBearerTxBytesAre(123456789L);
        thenResultingModemBearerRxBytesAre(987654321L);
    }

    /*
     * Given
     */

    private void givenDevicePropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockDeviceProperties.Get(NM_DEVICE_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenParentDevicePropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockParentDeviceProperties.Get(NM_DEVICE_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenIpv4ConfigPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockIp4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, propertyName)).thenReturn(propertyValue);
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

    private void givenIpv4ConfigPropertiesWithAddress(List<String> addressList) {
        List<Map<String, Variant<?>>> structureList = new ArrayList<>();

        for (String address : addressList) {
            String addressString = address.split("/")[0];
            UInt32 prefix = new UInt32(Integer.parseInt(address.split("/")[1]));

            Map<String, Variant<?>> structure = new HashMap<>();
            structure.put("address", new Variant<>(addressString));
            structure.put("prefix", new Variant<>(prefix));

            structureList.add(structure);
        }

        when(this.mockIp4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, "AddressData")).thenReturn(structureList);
    }

    private void givenIpv6ConfigPropertiesWith(String propertyName, String propertyValue) {
        when(this.mockIp6ConfigProperties.Get(NM_IP6CONFIG_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenIpv6ConfigPropertiesWithDNS(List<String> dnsAddresses) {
        List<List<Byte>> dnsAddressesByteList = new ArrayList<>();

        for (String stringAddress : dnsAddresses) {
            List<Byte> byteAddress = new ArrayList<>();

            for (String sAddressByte : stringAddress.split("\\.")) {
                Integer addressByteInt = Integer.parseInt(sAddressByte, 16);
                byteAddress.add(addressByteInt.byteValue());
            }

            dnsAddressesByteList.add(byteAddress);
        }
        when(this.mockIp6ConfigProperties.Get(NM_IP6CONFIG_BUS_NAME, "Nameservers")).thenReturn(dnsAddressesByteList);
    }

    private void givenIpv6ConfigPropertiesWithAddress(List<String> addressList) {
        List<Map<String, Variant<?>>> structureList = new ArrayList<>();

        for (String address : addressList) {
            String addressString = address.split("/")[0];
            UInt32 prefix = new UInt32(Integer.parseInt(address.split("/")[1]));

            Map<String, Variant<?>> structure = new HashMap<>();
            structure.put("address", new Variant<>(addressString));
            structure.put("prefix", new Variant<>(prefix));

            structureList.add(structure);
        }

        when(this.mockIp6ConfigProperties.Get(NM_IP6CONFIG_BUS_NAME, "AddressData")).thenReturn(structureList);
    }

    private void givenVlanConfigPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockVlanConfigProperties.Get(NM_VLAN_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenDevicePropertiesWrapperBuiltWith(Properties deviceProperties,
            Optional<Properties> deviceSpecificProperties, NMDeviceType nmDeviceType) {
        this.mockDevicePropertiesWrapper = new DevicePropertiesWrapper(deviceProperties, deviceSpecificProperties,
                nmDeviceType);
    }

    private void givenModemPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockModemProperties.Get(MM_MODEM_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenModemLocationPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockModemProperties.Get(MM_MODEM_LOCATION_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenModem3gppPropertiesWith(String propertyName, Object propertyValue) {
        when(mockModemProperties.Get(MM_MODEM_3GPP_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenAvailableSimPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockAvailableSimProperties.Get(MM_SIM_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenSimPropertiesBuiltWith(Properties properties) {
        when(this.mockSimProperties.getProperties()).thenReturn(properties);
    }

    private void givenSimIsActive(boolean isActive) {
        when(this.mockSimProperties.isActive()).thenReturn(isActive);
    }

    private void givenSimIsPrimary(boolean isPrimary) {
        when(this.mockSimProperties.isPrimary()).thenReturn(isPrimary);
    }

    private void givenBearerPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockBearerProperties.Get(MM_BEARER_BUS_NAME, propertyName)).thenReturn(propertyValue);
    }

    private void givenBearerSettingsWith(String propertyName, Object propertyValue) {
        this.bearerSettings.put(propertyName, propertyValue);
    }

    private void givenBearerStatsWith(String propertyName, Object propertyValue) {
        this.bearerStats.put(propertyName, propertyValue);
    }

    private void givenBearerPropertiesBuiltWithSettings(Map<String, Object> bearerSettings) {
        when(this.mockBearerProperties.Get(MM_BEARER_BUS_NAME, "Properties")).thenReturn(bearerSettings);
    }

    private void givenBearerPropertiesBuiltWithStats(Map<String, Object> bearerSettings) {
        when(this.mockBearerProperties.Get(MM_BEARER_BUS_NAME, "Stats")).thenReturn(bearerStats);
    }

    private void givenModemConfiguration() {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED));
        givenDevicePropertiesWith("Autoconnect", false);
        givenDevicePropertiesWith("FirmwareVersion", "isThisRealLife");
        givenDevicePropertiesWith("Driver", "isThisJustFantasy");
        givenDevicePropertiesWith("DriverVersion", "caughtInALandslide");
        givenDevicePropertiesWith("Mtu", new UInt32(69));
        givenDevicePropertiesWith("HwAddress", "F5:5B:32:7C:40:EA");
        givenDevicePropertiesWith("IpInterface", "cdc-acm0");

        givenIpv4ConfigPropertiesWith("Gateway", "192.168.1.1");
        givenIpv4ConfigPropertiesWithDNS(Arrays.asList("192.168.1.10"));
        givenIpv4ConfigPropertiesWithAddress(Arrays.asList("192.168.1.82/24"));

        givenModemPropertiesWith("Model", "EC25");
        givenModemPropertiesWith("Manufacturer", "Quectel");
        givenModemPropertiesWith("EquipmentIdentifier", "CoolQuectel");
        givenModemPropertiesWith("Revision", "Experimental");
        givenModemPropertiesWith("HardwareRevision", "TopSecret");
        givenModemPropertiesWith("PrimaryPort", "NaN");
        givenModemPropertiesWith("Ports", Arrays.asList(new Object[] {}));
        givenModemPropertiesWith("SupportedCapabilities",
                Arrays.asList(new UInt32[] { new UInt32(5), new UInt32(12) }));
        givenModemPropertiesWith("CurrentCapabilities", new UInt32(5));
        givenModemPropertiesWith("PowerState", MMModemPowerState.MM_MODEM_POWER_STATE_ON.toUInt32());
        givenModemPropertiesWith("SupportedModes", Arrays.asList(new Object[] {}));
        givenModemPropertiesWith("CurrentModes", new Object[] {});
        givenModemPropertiesWith("SupportedBands", Arrays.asList(new UInt32[] { new UInt32(1), new UInt32(32) }));
        givenModemPropertiesWith("CurrentBands", Arrays.asList(new UInt32[] { new UInt32(36), new UInt32(102) }));
        givenModemPropertiesWith("UnlockRequired", new UInt32(1));
        givenModemPropertiesWith("State", 11);
        givenModemPropertiesWith("AccessTechnologies", new UInt32(32768));
        givenModemPropertiesWith("SignalQuality", new Object[] { new UInt32(23), new UInt32(0) });

        givenModemLocationPropertiesWith("Capabilities",
                MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED.toUInt32());

        givenModem3gppPropertiesWith("RegistrationState",
                MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_HOME.toUInt32());
        givenModem3gppPropertiesWith("OperatorName", "VodaTim");

        givenSimIsActive(true);
        givenSimIsPrimary(true);
        givenAvailableSimPropertiesWith("SimIdentifier", "1234567890123456789");
        givenAvailableSimPropertiesWith("Imsi", "098765432109876");
        givenAvailableSimPropertiesWith("Eid", "AAA");
        givenAvailableSimPropertiesWith("OperatorName", "VODA");
        givenAvailableSimPropertiesWith("OperatorIdentifier", "22210");
        givenAvailableSimPropertiesWith("SimType", MMSimType.MM_SIM_TYPE_PHYSICAL.toUInt32());

        givenBearerPropertiesWith("Interface", "TheModemInterface");
        givenBearerPropertiesWith("Connected", true);
        givenBearerSettingsWith("apn", "web.omnitel.it");
        givenBearerSettingsWith("ip-type", MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4V6.toUInt32());
        givenBearerStatsWith("tx-bytes", new UInt64(Long.toUnsignedString(123456789L)));
        givenBearerStatsWith("rx-bytes", new UInt64(Long.toUnsignedString(987654321L)));
        givenBearerPropertiesBuiltWithSettings(this.bearerSettings);
        givenBearerPropertiesBuiltWithStats(this.bearerStats);

        givenDevicePropertiesWrapperBuiltWith(this.mockDeviceProperties, Optional.of(this.mockModemProperties),
                NMDeviceType.NM_DEVICE_TYPE_MODEM);

        givenSimPropertiesBuiltWith(this.mockAvailableSimProperties);
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

    private void whenBuildVlanStatusIsCalledWith(String ifaceName, DevicePropertiesWrapper deviceProps,
            Optional<Properties> ip4Properties, Optional<Properties> ip6Properties) {
        try {
            this.resultingStatus = NMStatusConverter.buildVlanStatus(ifaceName, deviceProps, ip4Properties,
                    ip6Properties, this.mockParentDeviceProperties);
            this.resultingVlanStatus = (VlanInterfaceStatus) this.resultingStatus;
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenBuildModemStatusIsCalledWith(String ifaceName, DevicePropertiesWrapper deviceProps,
            Optional<Properties> ip4Properties, Optional<Properties> ip6Properties, List<SimProperties> simProperties,
            List<Properties> bearerProperties) {
        try {
            this.resultingStatus = NMStatusConverter.buildModemStatus(ifaceName, deviceProps, ip4Properties,
                    ip6Properties, simProperties, bearerProperties);
            this.resultingModemStatus = (ModemInterfaceStatus) this.resultingStatus;
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionOccurred() {
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

    private void thenResultingNetworkInterfaceIpInterfaceIs(String ipInterface) {
        assertEquals(ipInterface, this.resultingStatus.getInterfaceName());
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

    private void thenResultingIp4InterfaceAddressIs(List<NetworkInterfaceIpAddress<IP4Address>> expectedAddresses) {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        List<NetworkInterfaceIpAddress<IP4Address>> addresses = address.getAddresses();
        assertEquals(expectedAddresses.size(), addresses.size());

        for (NetworkInterfaceIpAddress<IP4Address> expectedAddress : expectedAddresses) {
            assertTrue(addresses.contains(expectedAddress));
        }
    }

    private void thenResultingEthernetInterfaceLinkUpIs(boolean expectedResult) {
        assertEquals(expectedResult, this.resultingEthernetStatus.isLinkUp());
    }

    private void thenResultingIp6InterfaceAddressIsMissing() {
        assertFalse(this.resultingStatus.getInterfaceIp6Addresses().isPresent());
    }

    private void thenResultingIp6InterfaceGatewayIsMissing() {
        assertTrue(this.resultingStatus.getInterfaceIp6Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP6Address> address = this.resultingStatus.getInterfaceIp6Addresses().get();

        assertFalse(address.getGateway().isPresent());
    }

    private void thenResultingIp6InterfaceDNSIsMissing() {
        assertTrue(this.resultingStatus.getInterfaceIp6Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP6Address> address = this.resultingStatus.getInterfaceIp6Addresses().get();

        List<IP6Address> dns = address.getDnsServerAddresses();
        assertTrue(dns.isEmpty());
    }

    private void thenResultingIp6InterfaceAddressIs(List<NetworkInterfaceIpAddress<IP6Address>> expectedAddresses) {
        assertTrue(this.resultingStatus.getInterfaceIp6Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP6Address> address = this.resultingStatus.getInterfaceIp6Addresses().get();

        List<NetworkInterfaceIpAddress<IP6Address>> addresses = address.getAddresses();
        assertEquals(expectedAddresses.size(), addresses.size());

        for (NetworkInterfaceIpAddress<IP6Address> expectedAddress : expectedAddresses) {
            assertTrue(addresses.contains(expectedAddress));
        }
    }

    private void thenResultingIp6InterfaceDNSIs(List<IPAddress> expectedDNSAddresses) {
        assertTrue(this.resultingStatus.getInterfaceIp6Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP6Address> address = this.resultingStatus.getInterfaceIp6Addresses().get();

        List<IP6Address> dns = address.getDnsServerAddresses();
        assertEquals(expectedDNSAddresses.size(), dns.size());

        for (IPAddress expectedDNSAddress : expectedDNSAddresses) {
            assertTrue(dns.contains(expectedDNSAddress));
        }
    }

    private void thenResultingIp6InterfaceGatewayIs(IPAddress expectedResult) {
        assertTrue(this.resultingStatus.getInterfaceIp6Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP6Address> address = this.resultingStatus.getInterfaceIp6Addresses().get();

        assertTrue(address.getGateway().isPresent());
        assertEquals(expectedResult, address.getGateway().get());
    }

    private void thenResultingVlanIdIs(int expectedVlanId) {
        assertEquals(expectedVlanId, resultingVlanStatus.getVlanId());
    }

    private void thenResultingVlanParentInterfaceIs(String expectedParentInterface) {
        assertEquals(expectedParentInterface, resultingVlanStatus.getParentInterface());
    }

    private void thenResultingModemModelIs(String model) {
        assertEquals(model, this.resultingModemStatus.getModel());
    }

    private void thenResultingModemManufacturerIs(String manufacturer) {
        assertEquals(manufacturer, this.resultingModemStatus.getManufacturer());
    }

    private void thenResultingModemEquipmentIdentifierIs(String EquipmentIdentifier) {
        assertEquals(EquipmentIdentifier, this.resultingModemStatus.getSerialNumber());
    }

    private void thenResultingModemRevisionIs(String Revision) {
        assertEquals(Revision, this.resultingModemStatus.getSoftwareRevision());
    }

    private void thenResultingModemHWRevisionIs(String hwRevision) {
        assertEquals(hwRevision, this.resultingModemStatus.getHardwareRevision());
    }

    private void thenResultingModemPrimaryPortIs(String port) {
        assertEquals(port, this.resultingModemStatus.getPrimaryPort());
    }

    private void thenResultingModemPortsAre(Map<String, ModemPortType> ports) {
        assertEquals(ports, this.resultingModemStatus.getPorts());
    }

    private void thenResultingModemSupportedCapabilitiesAre(Set<ModemCapability> firstModemCapability,
            Set<ModemCapability> secondModemCapability) {
        List<Set<ModemCapability>> supportedModemCapabilities = new ArrayList<Set<ModemCapability>>();
        supportedModemCapabilities.add(firstModemCapability);
        supportedModemCapabilities.add(secondModemCapability);
        assertEquals(supportedModemCapabilities, this.resultingModemStatus.getAllSupportedModemCapabilities());
    }

    private void thenResultingModemCurrentCapabilitiesAre(EnumSet<ModemCapability> modemCapability) {
        assertEquals(modemCapability, this.resultingModemStatus.getCurrentModemCapabilities());
    }

    private void thenResultingModemPowerStateIs(ModemPowerState modemState) {
        assertEquals(modemState, this.resultingModemStatus.getPowerState());
    }

    private void thenResultingModemSupportedModesAre(Set<ModemModePair> modemModePair) {
        assertEquals(modemModePair, this.resultingModemStatus.getSupportedModes());
    }

    private void thenResultingModemCurrentModesAre(ModemModePair modemModePair) {
        assertEquals(modemModePair, this.resultingModemStatus.getCurrentModes());
    }

    private void thenResultingModemSupportedBandsAre(Set<ModemBand> modemBands) {
        assertEquals(modemBands, this.resultingModemStatus.getSupportedBands());
    }

    private void thenResultingModemCurrentBandsAre(Set<ModemBand> modemBands) {
        assertEquals(modemBands, this.resultingModemStatus.getCurrentBands());
    }

    private void thenResultingModemUnlockRequiredIs(boolean unlockRequired) {
        assertEquals(unlockRequired, this.resultingModemStatus.isSimLocked());
    }

    private void thenResultingModemStateIs(ModemConnectionStatus modemStatus) {
        assertEquals(modemStatus, this.resultingModemStatus.getConnectionStatus());
    }

    private void thenResultingModemAccessTechnologiesAre(Set<AccessTechnology> accessTechnologies) {
        assertEquals(accessTechnologies, this.resultingModemStatus.getAccessTechnologies());
    }

    private void thenResultingModemSignalQualityIs(int signalQuality) {
        assertEquals(signalQuality, this.resultingModemStatus.getSignalQuality());
    }

    private void thenResultingModemSignalStrengthIs(int signalStrength) {
        assertEquals(signalStrength, this.resultingModemStatus.getSignalStrength());
    }

    private void thenResultingModemRegistrationStateIs(RegistrationStatus status) {
        assertEquals(status, this.resultingModemStatus.getRegistrationStatus());
    }

    private void thenResultingModemOperatorNameIs(String operatorName) {
        assertEquals(operatorName, this.resultingModemStatus.getOperatorName());
    }

    private void thenResultingModemLocationCapabilitiesAre(Set<ModemGpsMode> gpsModes) {
        assertEquals(gpsModes, this.resultingModemStatus.getSupporteGpsModes());
    }

    private void thenResultingModemIsGpsSupported(boolean isGpsSupported) {
        assertEquals(isGpsSupported, this.resultingModemStatus.isGpsSupported());
    }

    private void thenResultingModemSimIsActive(boolean isActive) {
        assertEquals(isActive, this.resultingModemStatus.getAvailableSims().get(0).isActive());
    }

    private void thenResultingModemSimIsPrimary(boolean isPrimary) {
        assertEquals(isPrimary, this.resultingModemStatus.getAvailableSims().get(0).isPrimary());
    }

    private void thenResultingModemSimIdentifierIs(String iccid) {
        assertEquals(iccid, this.resultingModemStatus.getAvailableSims().get(0).getIccid());
    }

    private void thenResultingModemSimImsiIs(String imsi) {
        assertEquals(imsi, this.resultingModemStatus.getAvailableSims().get(0).getImsi());
    }

    private void thenResultingModemSimEidIs(String eid) {
        assertEquals(eid, this.resultingModemStatus.getAvailableSims().get(0).getEid());
    }

    private void thenResultingModemSimOperatorNameIs(String operatorName) {
        assertEquals(operatorName, this.resultingModemStatus.getAvailableSims().get(0).getOperatorName());
    }

    private void thenResultingModemSimOperatorIdIs(String operatorId) {
        assertEquals(operatorId, this.resultingModemStatus.getAvailableSims().get(0).getOperatorIdentifier());
    }

    private void thenResultingModemSimTypeIs(SimType simType) {
        assertEquals(simType, this.resultingModemStatus.getAvailableSims().get(0).getSimType());
    }

    private void thenResultingModemBearerInterfaceIs(String interfaceName) {
        assertEquals(interfaceName, this.resultingModemStatus.getBearers().get(0).getName());
    }

    private void thenResultingModemBearerIsConnected(boolean isConnected) {
        assertEquals(isConnected, this.resultingModemStatus.getBearers().get(0).isConnected());
    }

    private void thenResultingModemBearerApnIs(String apn) {
        assertEquals(apn, this.resultingModemStatus.getBearers().get(0).getApn());
    }

    private void thenResultingModemBearerIpTypeIs(Set<BearerIpType> ipTypes) {
        assertEquals(ipTypes, this.resultingModemStatus.getBearers().get(0).getIpTypes());
    }

    private void thenResultingModemBearerTxBytesAre(long txBytes) {
        assertEquals(txBytes, this.resultingModemStatus.getBearers().get(0).getBytesTransmitted());
    }

    private void thenResultingModemBearerRxBytesAre(long rxBytes) {
        assertEquals(rxBytes, this.resultingModemStatus.getBearers().get(0).getBytesReceived());
    }
}
