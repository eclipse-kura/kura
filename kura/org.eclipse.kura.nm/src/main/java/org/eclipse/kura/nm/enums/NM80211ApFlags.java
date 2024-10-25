/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

public enum NM80211ApFlags {

    NM_802_11_AP_FLAGS_NONE(0x00000000),
    NM_802_11_AP_FLAGS_PRIVACY(0x00000001),
    NM_802_11_AP_FLAGS_WPS(0x00000002),
    NM_802_11_AP_FLAGS_WPS_PBC(0x00000004),
    NM_802_11_AP_FLAGS_WPS_PIN(0x00000008);

    private final int value;

    private NM80211ApFlags(int value) {
        this.value = value;
    }

    public static List<NM80211ApFlags> fromUInt32(UInt32 val) {
        int intVal = val.intValue();

        if (intVal == NM80211ApFlags.NM_802_11_AP_FLAGS_NONE.value) {
            return Arrays.asList(NM80211ApFlags.NM_802_11_AP_FLAGS_NONE);
        }

        List<NM80211ApFlags> flags = new ArrayList<>();

        for (NM80211ApFlags flag : NM80211ApFlags.values()) {
            if (flag == NM80211ApFlags.NM_802_11_AP_FLAGS_NONE) {
                continue;
            }

            if ((intVal & flag.value) == flag.value) {
                flags.add(flag);
            }
        }

        return flags;
    }
}