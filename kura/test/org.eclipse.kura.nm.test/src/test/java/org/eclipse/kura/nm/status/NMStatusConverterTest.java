package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void buildLoopbackStatusWorksWithoutIPV4Info() {
        Properties mockProperties = mock(Properties.class);

        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("State")))
                .thenReturn(NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_UNMANAGED));
        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("Autoconnect"))).thenReturn(true);
        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("FirmwareVersion"))).thenReturn("awesomeFirmwareVersion");
        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("Driver"))).thenReturn("awesomeDriver");
        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("DriverVersion"))).thenReturn("awesomeDriverVersion");
        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("Mtu"))).thenReturn(new UInt32(42));
        when(mockProperties.Get(eq(NM_DEVICE_BUS_NAME), eq("HwAddress"))).thenReturn("00:00:00:00:00:00");

        NetworkInterfaceStatus status = NMStatusConverter.buildLoopbackStatus("lo", mockProperties, Optional.empty());

        assertTrue(status.isAutoConnect());
        assertEquals(NetworkInterfaceState.UNMANAGED, status.getState());
        assertEquals("awesomeFirmwareVersion", status.getFirmwareVersion());
        assertEquals("awesomeDriver", status.getDriver());
        assertEquals("awesomeDriverVersion", status.getDriverVersion());
        assertEquals(42, status.getMtu());
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0 }, status.getHardwareAddress());

        assertEquals(Optional.empty(), status.getInterfaceIp4Addresses());
    }

}
