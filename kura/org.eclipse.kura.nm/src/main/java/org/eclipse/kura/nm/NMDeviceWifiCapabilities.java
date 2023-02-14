package org.eclipse.kura.nm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;

public enum NMDeviceWifiCapabilities {

    NM_WIFI_DEVICE_CAP_NONE,
    NM_WIFI_DEVICE_CAP_CIPHER_WEP40,
    NM_WIFI_DEVICE_CAP_CIPHER_WEP104,
    NM_WIFI_DEVICE_CAP_CIPHER_TKIP,
    NM_WIFI_DEVICE_CAP_CIPHER_CCMP,
    NM_WIFI_DEVICE_CAP_WPA,
    NM_WIFI_DEVICE_CAP_RSN,
    NM_WIFI_DEVICE_CAP_AP,
    NM_WIFI_DEVICE_CAP_ADHOC,
    NM_WIFI_DEVICE_CAP_FREQ_VALID,
    NM_WIFI_DEVICE_CAP_FREQ_2GHZ,
    NM_WIFI_DEVICE_CAP_FREQ_5GHZ,
    NM_WIFI_DEVICE_CAP_MESH,
    NM_WIFI_DEVICE_CAP_IBSS_RSN;

    public static List<NMDeviceWifiCapabilities> fromUInt32(UInt32 val) {
        if (val.intValue() == 0x00000000) {
            return Arrays.asList(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_NONE);
        }

        List<NMDeviceWifiCapabilities> capabilities = new ArrayList<>();

        if ((val.intValue() & 0x00000001) == 0x00000001) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP40);
        }

        if ((val.intValue() & 0x00000002) == 0x00000002) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP104);
        }

        if ((val.intValue() & 0x00000004) == 0x00000004) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_TKIP);
        }

        if ((val.intValue() & 0x00000008) == 0x00000008) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_CCMP);
        }

        if ((val.intValue() & 0x00000010) == 0x00000010) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_WPA);
        }

        if ((val.intValue() & 0x00000020) == 0x00000020) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_RSN);
        }

        if ((val.intValue() & 0x00000040) == 0x00000040) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_AP);
        }

        if ((val.intValue() & 0x00000080) == 0x00000080) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_ADHOC);
        }

        if ((val.intValue() & 0x00000100) == 0x00000100) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_VALID);
        }

        if ((val.intValue() & 0x00000200) == 0x00000200) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_2GHZ);
        }

        if ((val.intValue() & 0x00000400) == 0x00000400) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_5GHZ);
        }

        if ((val.intValue() & 0x00001000) == 0x00001000) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_MESH);
        }

        if ((val.intValue() & 0x00002000) == 0x00002000) {
            capabilities.add(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_IBSS_RSN);
        }

        return capabilities;
    }
}
