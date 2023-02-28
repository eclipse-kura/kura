package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.nm.NMDeviceState;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMStatusConverterTest {

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";

    Properties mockDeviceProperties = mock(Properties.class);
    Properties mockIp4ConfigProperties = mock(Properties.class);

    NetworkInterfaceStatus resultingStatus;

    private boolean nullPointerExceptionWasThrown = false;

    @Test
    public void buildLoopbackStatusThrowsWithEmptyProperties() {
        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDeviceProperties, Optional.empty());

        thenNullPointerExceptionIsThrown();
    }

    @Test
    public void buildLoopbackStatusThrowsWithPartialProperties() {
        givenDevicePropertiesWith("State", NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_UNMANAGED));
        givenDevicePropertiesWith("Autoconnect", true);
        givenDevicePropertiesWith("FirmwareVersion", "awesomeFirmwareVersion");

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDeviceProperties, Optional.empty());

        thenNullPointerExceptionIsThrown();
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

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDeviceProperties, Optional.empty());

        thenNoExceptionIsThrown();

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

        givenIpv4ConfigPropertiesWith("Gateway", "127.0.0.1");
        givenIpv4ConfigPropertiesWith("NameserverData", Arrays.asList());
        givenIpv4ConfigPropertiesWith("AddressData", Arrays.asList());

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockDeviceProperties, Optional.of(this.mockIp4ConfigProperties));

        thenNoExceptionIsThrown();

        thenResultingNetworkInterfaceAutoConnectIs(false);
        thenResultingNetworkInterfaceStateIs(NetworkInterfaceState.ACTIVATED);
        thenResultingNetworkInterfaceFirmwareVersionIs("isThisRealLife");
        thenResultingNetworkInterfaceDriverIs("isThisJustFantasy");
        thenResultingNetworkInterfaceDriverVersionIs("caughtInALandslide");
        thenResultingNetworkInterfaceMtuIs(69);
        thenResultingNetworkInterfaceHardwareAddressIs(
                new byte[] { (byte) 0xF5, (byte) 0x5B, (byte) 0x32, (byte) 0x7C, (byte) 0x40, (byte) 0xEA });

        thenResultingIp4InterfaceGatewayIs(IPAddress.parseHostAddress("127.0.0.1"));
    }

    /*
     * Given
     */

    private void givenDevicePropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockDeviceProperties.Get(eq(NM_DEVICE_BUS_NAME), eq(propertyName)))
                .thenReturn(propertyValue);
    }

    private void givenIpv4ConfigPropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockIp4ConfigProperties.Get(eq(NM_IP4CONFIG_BUS_NAME), eq(propertyName)))
                .thenReturn(propertyValue);
    }

    /*
     * When
     */

    private void whenBuildLoopbackStatusIsCalledWith(String ifaceName, Properties deviceProps,
            Optional<Properties> ip4Properties) {
        try {
            this.resultingStatus = NMStatusConverter.buildLoopbackStatus(ifaceName, deviceProps, ip4Properties);
        } catch (NullPointerException e) {
            this.nullPointerExceptionWasThrown = true;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionIsThrown() {
        assertFalse(this.nullPointerExceptionWasThrown);
    }

    private void thenNullPointerExceptionIsThrown() {
        assertTrue(this.nullPointerExceptionWasThrown);
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

    private void thenResultingIp4InterfaceGatewayIs(IPAddress expectedResult) {
        assertTrue(this.resultingStatus.getInterfaceIp4Addresses().isPresent());
        NetworkInterfaceIpAddressStatus<IP4Address> address = this.resultingStatus.getInterfaceIp4Addresses().get();

        assertTrue(address.getGateway().isPresent());
        IPAddress gateway = address.getGateway().get();

        assertEquals(expectedResult, gateway);
    }
}
