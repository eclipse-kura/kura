package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.nm.NMDeviceState;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMStatusConverterTest {

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";

    Properties mockProperties = mock(Properties.class);
    NetworkInterfaceStatus resultingStatus;

    private boolean nullPointerExceptionWasThrown = false;

    @Test
    public void buildLoopbackStatusThrowsWithEmptyProperties() {
        whenBuildLoopbackStatusIsCalledWith("lo", this.mockProperties, Optional.empty());

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

        whenBuildLoopbackStatusIsCalledWith("lo", this.mockProperties, Optional.empty());

        thenNoExceptionIsThrown();

        assertTrue(this.resultingStatus.isAutoConnect());
        assertEquals(NetworkInterfaceState.UNMANAGED, this.resultingStatus.getState());
        assertEquals("awesomeFirmwareVersion", this.resultingStatus.getFirmwareVersion());
        assertEquals("awesomeDriver", this.resultingStatus.getDriver());
        assertEquals("awesomeDriverVersion", this.resultingStatus.getDriverVersion());
        assertEquals(42, this.resultingStatus.getMtu());
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0 }, this.resultingStatus.getHardwareAddress());

        assertEquals(Optional.empty(), this.resultingStatus.getInterfaceIp4Addresses());
    }

    private void givenDevicePropertiesWith(String propertyName, Object propertyValue) {
        when(this.mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq(propertyName)))
                .thenReturn(propertyValue);
    }

    private void whenBuildLoopbackStatusIsCalledWith(String ifaceName, Properties deviceProps,
            Optional<Properties> ip4Properties) {
        try {
            this.resultingStatus = NMStatusConverter.buildLoopbackStatus(ifaceName, deviceProps, ip4Properties);
        } catch (NullPointerException e) {
            this.nullPointerExceptionWasThrown = true;
        }
    }

    private void thenNoExceptionIsThrown() {
        assertFalse(this.nullPointerExceptionWasThrown);
    }

    private void thenNullPointerExceptionIsThrown() {
        assertTrue(this.nullPointerExceptionWasThrown);
    }

}
