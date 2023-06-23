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

import org.eclipse.kura.net.status.modem.ModemCapability;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemCapability {

    MM_MODEM_CAPABILITY_NONE(0x00000000),
    MM_MODEM_CAPABILITY_POTS(0x00000001),
    MM_MODEM_CAPABILITY_CDMA_EVDO(0x00000002),
    MM_MODEM_CAPABILITY_GSM_UMTS(0x00000004),
    MM_MODEM_CAPABILITY_LTE(0x00000008),
    MM_MODEM_CAPABILITY_IRIDIUM(0x00000020),
    MM_MODEM_CAPABILITY_5GNR(0x00000040),
    MM_MODEM_CAPABILITY_TDS(0x00000080),
    MM_MODEM_CAPABILITY_ANY(0xFFFFFFFF);

    private int value;

    private MMModemCapability(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(Integer.toUnsignedString(this.value));
    }

    public static MMModemCapability toMMModemCapability(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return MMModemCapability.MM_MODEM_CAPABILITY_NONE;
        case 0x00000001:
            return MMModemCapability.MM_MODEM_CAPABILITY_POTS;
        case 0x00000002:
            return MMModemCapability.MM_MODEM_CAPABILITY_CDMA_EVDO;
        case 0x00000004:
            return MMModemCapability.MM_MODEM_CAPABILITY_GSM_UMTS;
        case 0x00000008:
            return MMModemCapability.MM_MODEM_CAPABILITY_LTE;
        case 0x00000020:
            return MMModemCapability.MM_MODEM_CAPABILITY_IRIDIUM;
        case 0x00000040:
            return MMModemCapability.MM_MODEM_CAPABILITY_5GNR;
        case 0x00000080:
            return MMModemCapability.MM_MODEM_CAPABILITY_TDS;
        case 0xFFFFFFFF:
            return MMModemCapability.MM_MODEM_CAPABILITY_ANY;
        default:
            return MMModemCapability.MM_MODEM_CAPABILITY_NONE;
        }
    }

    public static ModemCapability toModemCapability(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return ModemCapability.NONE;
        case 0x00000001:
            return ModemCapability.POTS;
        case 0x00000002:
            return ModemCapability.EVDO;
        case 0x00000004:
            return ModemCapability.GSM_UMTS;
        case 0x00000008:
            return ModemCapability.LTE;
        case 0x00000020:
            return ModemCapability.IRIDIUM;
        case 0x00000040:
            return ModemCapability.FIVE_GNR;
        case 0x00000080:
            return ModemCapability.TDS;
        case 0xFFFFFFFF:
            return ModemCapability.ANY;
        default:
            return ModemCapability.NONE;
        }
    }

    public static Set<ModemCapability> toModemCapabilitiesFromBitMask(UInt32 bitMask) {
        long bitMaskValue = bitMask.longValue();
        if (bitMaskValue == 0x00000000L) {
            return EnumSet.of(ModemCapability.NONE);
        }
        if (bitMaskValue == 0xFFFFFFFFL) {
            return EnumSet.of(ModemCapability.ANY);
        }

        EnumSet<ModemCapability> modemCapabilities = EnumSet.noneOf(ModemCapability.class);
        for (MMModemCapability capability : MMModemCapability.values()) {
            if (capability == MM_MODEM_CAPABILITY_NONE || capability == MM_MODEM_CAPABILITY_ANY) {
                continue;
            }
            if ((bitMaskValue & capability.getValue()) == capability.getValue()) {
                modemCapabilities.add(toModemCapability(capability.toUInt32()));
            }
        }
        return modemCapabilities;
    }
}
