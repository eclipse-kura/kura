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

public enum NM80211ApSecurityFlags {

    NM_802_11_AP_SEC_NONE(0x00000000),
    NM_802_11_AP_SEC_PAIR_WEP40(0x00000001),
    NM_802_11_AP_SEC_PAIR_WEP104(0x00000002),
    NM_802_11_AP_SEC_PAIR_TKIP(0x00000004),
    NM_802_11_AP_SEC_PAIR_CCMP(0x00000008),
    NM_802_11_AP_SEC_GROUP_WEP40(0x00000010),
    NM_802_11_AP_SEC_GROUP_WEP104(0x00000020),
    NM_802_11_AP_SEC_GROUP_TKIP(0x00000040),
    NM_802_11_AP_SEC_GROUP_CCMP(0x00000080),
    NM_802_11_AP_SEC_KEY_MGMT_PSK(0x00000100),
    NM_802_11_AP_SEC_KEY_MGMT_802_1X(0x00000200),
    NM_802_11_AP_SEC_KEY_MGMT_SAE(0x00000400),
    NM_802_11_AP_SEC_KEY_MGMT_OWE(0x00000800),
    NM_802_11_AP_SEC_KEY_MGMT_OWE_TM(0x00001000),
    NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192(0x00002000);

    private final int value;

    private NM80211ApSecurityFlags(int value) {
        this.value = value;
    }

    public static List<NM80211ApSecurityFlags> fromUInt32(UInt32 val) {
        int intVal = val.intValue();

        if (intVal == NM80211ApSecurityFlags.NM_802_11_AP_SEC_NONE.value) {
            return Arrays.asList(NM80211ApSecurityFlags.NM_802_11_AP_SEC_NONE);
        }

        List<NM80211ApSecurityFlags> flags = new ArrayList<>();

        for (NM80211ApSecurityFlags flag : NM80211ApSecurityFlags.values()) {
            if (flag == NM80211ApSecurityFlags.NM_802_11_AP_SEC_NONE) {
                continue;
            }

            if ((intVal & flag.value) == flag.value) {
                flags.add(flag);
            }
        }

        return flags;
    }
}