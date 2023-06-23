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

import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemAccessTechnology {

    MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN(0x00000000),
    MM_MODEM_ACCESS_TECHNOLOGY_POTS(0x00000001),
    MM_MODEM_ACCESS_TECHNOLOGY_GSM(0x00000002),
    MM_MODEM_ACCESS_TECHNOLOGY_GSM_COMPACT(0x00000004),
    MM_MODEM_ACCESS_TECHNOLOGY_GPRS(0x00000008),
    MM_MODEM_ACCESS_TECHNOLOGY_EDGE(0x00000010),
    MM_MODEM_ACCESS_TECHNOLOGY_UMTS(0x00000020),
    MM_MODEM_ACCESS_TECHNOLOGY_HSDPA(0x00000040),
    MM_MODEM_ACCESS_TECHNOLOGY_HSUPA(0x00000080),
    MM_MODEM_ACCESS_TECHNOLOGY_HSPA(0x00000100),
    MM_MODEM_ACCESS_TECHNOLOGY_HSPA_PLUS(0x00000200),
    MM_MODEM_ACCESS_TECHNOLOGY_1XRTT(0x00000400),
    MM_MODEM_ACCESS_TECHNOLOGY_EVDO0(0x00000800),
    MM_MODEM_ACCESS_TECHNOLOGY_EVDOA(0x00001000),
    MM_MODEM_ACCESS_TECHNOLOGY_EVDOB(0x00002000),
    MM_MODEM_ACCESS_TECHNOLOGY_LTE(0x00004000),
    MM_MODEM_ACCESS_TECHNOLOGY_5GNR(0x00008000),
    MM_MODEM_ACCESS_TECHNOLOGY_LTE_CAT_M(0x00010000),
    MM_MODEM_ACCESS_TECHNOLOGY_LTE_NB_IOT(0x00020000),
    MM_MODEM_ACCESS_TECHNOLOGY_ANY(0xFFFFFFFF);

    private int value;

    private MMModemAccessTechnology(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(Integer.toUnsignedString(this.value));
    }

    public static MMModemAccessTechnology toMMModemAccessTechnology(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN;
        case 0x00000001:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_POTS;
        case 0x00000002:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GSM;
        case 0x00000004:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GSM_COMPACT;
        case 0x00000008:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GPRS;
        case 0x00000010:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EDGE;
        case 0x00000020:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UMTS;
        case 0x00000040:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSDPA;
        case 0x00000080:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSUPA;
        case 0x00000100:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSPA;
        case 0x00000200:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSPA_PLUS;
        case 0x00000400:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_1XRTT;
        case 0x00000800:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDO0;
        case 0x00001000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDOA;
        case 0x00002000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDOB;
        case 0x00004000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE;
        case 0x00008000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_5GNR;
        case 0x00010000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE_CAT_M;
        case 0x00020000:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE_NB_IOT;
        case 0xFFFFFFFF:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_ANY;
        default:
            return MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN;
        }
    }

    public static AccessTechnology toAccessTechnology(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return AccessTechnology.UNKNOWN;
        case 0x00000001:
            return AccessTechnology.POTS;
        case 0x00000002:
            return AccessTechnology.GSM;
        case 0x00000004:
            return AccessTechnology.GSM_COMPACT;
        case 0x00000008:
            return AccessTechnology.GPRS;
        case 0x00000010:
            return AccessTechnology.EDGE;
        case 0x00000020:
            return AccessTechnology.UMTS;
        case 0x00000040:
            return AccessTechnology.HSDPA;
        case 0x00000080:
            return AccessTechnology.HSUPA;
        case 0x00000100:
            return AccessTechnology.HSPA;
        case 0x00000200:
            return AccessTechnology.HSPA_PLUS;
        case 0x00000400:
            return AccessTechnology.ONEXRTT;
        case 0x00000800:
            return AccessTechnology.EVDO0;
        case 0x00001000:
            return AccessTechnology.EVDOA;
        case 0x00002000:
            return AccessTechnology.EVDOB;
        case 0x00004000:
            return AccessTechnology.LTE;
        case 0x00008000:
            return AccessTechnology.FIVEGNR;
        case 0x00010000:
            return AccessTechnology.LTE_CAT_M;
        case 0x00020000:
            return AccessTechnology.LTE_NB_IOT;
        case 0xFFFFFFFF:
            return AccessTechnology.ANY;
        default:
            return AccessTechnology.UNKNOWN;
        }
    }

    public static Set<AccessTechnology> toAccessTechnologyFromBitMask(UInt32 bitMask) {
        long bitMaskValue = bitMask.longValue();
        if (bitMaskValue == 0x00000000L) {
            return EnumSet.of(AccessTechnology.UNKNOWN);
        }
        if (bitMaskValue == 0xFFFFFFFFL) {
            return EnumSet.of(AccessTechnology.ANY);
        }

        EnumSet<AccessTechnology> accessTechnologies = EnumSet.noneOf(AccessTechnology.class);
        for (MMModemAccessTechnology at : MMModemAccessTechnology.values()) {
            if (at == MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN || at == MM_MODEM_ACCESS_TECHNOLOGY_ANY) {
                continue;
            }
            if ((bitMaskValue & at.getValue()) == at.getValue()) {
                accessTechnologies.add(toAccessTechnology(at.toUInt32()));
            }
        }
        return accessTechnologies;
    }
}
