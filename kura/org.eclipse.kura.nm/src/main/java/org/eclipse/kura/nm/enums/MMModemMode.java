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

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.kura.net.status.modem.ModemMode;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemMode {

    MM_MODEM_MODE_NONE(0x00000000),
    MM_MODEM_MODE_CS(0x00000001),
    MM_MODEM_MODE_2G(0x00000002),
    MM_MODEM_MODE_3G(0x00000004),
    MM_MODEM_MODE_4G(0x00000008),
    MM_MODEM_MODE_5G(0x00000010),
    MM_MODEM_MODE_ANY(0xFFFFFFFF);

    private int value;

    private MMModemMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(Integer.toUnsignedString(this.value));
    }

    public static MMModemMode toMMModemMode(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return MMModemMode.MM_MODEM_MODE_NONE;
        case 0x00000001:
            return MMModemMode.MM_MODEM_MODE_CS;
        case 0x00000002:
            return MMModemMode.MM_MODEM_MODE_2G;
        case 0x00000004:
            return MMModemMode.MM_MODEM_MODE_3G;
        case 0x00000008:
            return MMModemMode.MM_MODEM_MODE_4G;
        case 0x00000010:
            return MMModemMode.MM_MODEM_MODE_5G;
        case 0xFFFFFFFF:
            return MMModemMode.MM_MODEM_MODE_ANY;
        default:
            return MMModemMode.MM_MODEM_MODE_NONE;
        }
    }

    public static ModemMode toModemMode(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return ModemMode.NONE;
        case 0x00000001:
            return ModemMode.CS;
        case 0x00000002:
            return ModemMode.MODE_2G;
        case 0x00000004:
            return ModemMode.MODE_3G;
        case 0x00000008:
            return ModemMode.MODE_4G;
        case 0x00000010:
            return ModemMode.MODE_5G;
        case 0xFFFFFFFF:
            return ModemMode.ANY;
        default:
            return ModemMode.NONE;
        }
    }

    public static Set<ModemMode> toModemModeFromBitMask(UInt32 bitMask) {
        long bitMaskValue = bitMask.longValue();
        if (bitMaskValue == 0x00000000L) {
            return EnumSet.of(ModemMode.NONE);
        }
        if (bitMaskValue == 0xFFFFFFFFL) {
            return EnumSet.of(ModemMode.ANY);
        }

        EnumSet<ModemMode> modemModes = EnumSet.noneOf(ModemMode.class);
        for (MMModemMode mode : MMModemMode.values()) {
            if (mode == MM_MODEM_MODE_NONE || mode == MM_MODEM_MODE_ANY) {
                continue;
            }
            if ((bitMaskValue & mode.getValue()) == mode.getValue()) {
                modemModes.add(toModemMode(mode.toUInt32()));
            }
        }
        return modemModes;
    }
}
