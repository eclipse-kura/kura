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
package org.eclipse.kura.nm.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;

public enum NMDeviceWifiCapabilities {

    NM_WIFI_DEVICE_CAP_NONE(0x00000000),
    NM_WIFI_DEVICE_CAP_CIPHER_WEP40(0x00000001),
    NM_WIFI_DEVICE_CAP_CIPHER_WEP104(0x00000002),
    NM_WIFI_DEVICE_CAP_CIPHER_TKIP(0x00000004),
    NM_WIFI_DEVICE_CAP_CIPHER_CCMP(0x00000008),
    NM_WIFI_DEVICE_CAP_WPA(0x00000010),
    NM_WIFI_DEVICE_CAP_RSN(0x00000020),
    NM_WIFI_DEVICE_CAP_AP(0x00000040),
    NM_WIFI_DEVICE_CAP_ADHOC(0x00000080),
    NM_WIFI_DEVICE_CAP_FREQ_VALID(0x00000100),
    NM_WIFI_DEVICE_CAP_FREQ_2GHZ(0x00000200),
    NM_WIFI_DEVICE_CAP_FREQ_5GHZ(0x00000400),
    NM_WIFI_DEVICE_CAP_MESH(0x00001000),
    NM_WIFI_DEVICE_CAP_IBSS_RSN(0x00002000);

    private final int value;

    NMDeviceWifiCapabilities(int value) {
        this.value = value;
    }

    public static List<NMDeviceWifiCapabilities> fromUInt32(UInt32 val) {
        int intVal = val.intValue();

        if (intVal == NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_NONE.value) {
            return Arrays.asList(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_NONE);
        }

        List<NMDeviceWifiCapabilities> capabilities = new ArrayList<>();

        for (NMDeviceWifiCapabilities capability : NMDeviceWifiCapabilities.values()) {
            if (capability == NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_NONE) {
                continue;
            }

            if ((intVal & capability.value) == capability.value) {
                capabilities.add(capability);
            }
        }

        return capabilities;
    }
}
