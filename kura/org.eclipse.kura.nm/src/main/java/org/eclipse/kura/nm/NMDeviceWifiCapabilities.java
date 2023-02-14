package org.eclipse.kura.nm;

import java.util.ArrayList;
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
        List<NMDeviceWifiCapabilities> capabilities = new ArrayList<>();

        // TODO

        return capabilities;
    }
}
