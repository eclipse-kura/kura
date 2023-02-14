package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMDeviceWifiCapabilitiesTest {

    UInt32 value;
    List<NMDeviceWifiCapabilities> capabilities;

    @Test
    public void fromUInt32WorksWithNone() {
        givenValue(0x00000000);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_NONE);
    }

    private void givenValue(int intValue) {
        this.value = new UInt32(intValue);
    }

    private void whenFromUInt32IsCalledWith(UInt32 intValue) {
        this.capabilities = NMDeviceWifiCapabilities.fromUInt32(intValue);
    }

    private void thenCapabilitiesSizeIs(int size) {
        assertEquals(size, this.capabilities.size());
    }

    private void thenCapabilitiesContains(NMDeviceWifiCapabilities nmWifiDeviceCap) {
        assertTrue(this.capabilities.contains(nmWifiDeviceCap));
    }
}
