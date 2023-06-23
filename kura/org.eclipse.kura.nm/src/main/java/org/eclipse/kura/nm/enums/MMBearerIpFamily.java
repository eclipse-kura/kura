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

import org.eclipse.kura.net.status.modem.BearerIpType;
import org.freedesktop.dbus.types.UInt32;

public enum MMBearerIpFamily {

    MM_BEARER_IP_FAMILY_NONE(0x00000000),
    MM_BEARER_IP_FAMILY_IPV4(0x00000001),
    MM_BEARER_IP_FAMILY_IPV6(0x00000002),
    MM_BEARER_IP_FAMILY_IPV4V6(0x00000004),
    MM_BEARER_IP_FAMILY_NON_IP(0x00000008),
    MM_BEARER_IP_FAMILY_ANY(0xFFFFFFF7);

    private int value;

    private MMBearerIpFamily(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(Integer.toUnsignedString(this.value));
    }

    public static MMBearerIpFamily toMMBearerIpFamily(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_NONE;
        case 0x00000001:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4;
        case 0x00000002:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV6;
        case 0x00000004:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4V6;
        case 0x00000008:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_NON_IP;
        case 0xFFFFFFF7:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_ANY;
        default:
            return MMBearerIpFamily.MM_BEARER_IP_FAMILY_NONE;
        }
    }

    public static BearerIpType toBearerIpType(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return BearerIpType.NONE;
        case 0x00000001:
            return BearerIpType.IPV4;
        case 0x00000002:
            return BearerIpType.IPV6;
        case 0x00000004:
            return BearerIpType.IPV4V6;
        case 0x00000008:
            return BearerIpType.NON_IP;
        case 0xFFFFFFF7:
            return BearerIpType.ANY;
        default:
            return BearerIpType.NONE;
        }
    }

    public static Set<BearerIpType> toBearerIpTypeFromBitMask(UInt32 bitMask) {
        int bitMaskValue = bitMask.intValue();
        if (bitMaskValue == 0x00000000) {
            return EnumSet.of(BearerIpType.NONE);
        }
        if (bitMaskValue == 0xFFFFFFF7) {
            return EnumSet.of(BearerIpType.ANY);
        }

        EnumSet<BearerIpType> bearerIpTypes = EnumSet.noneOf(BearerIpType.class);
        for (MMBearerIpFamily family : MMBearerIpFamily.values()) {
            if (family == MM_BEARER_IP_FAMILY_NONE || family == MM_BEARER_IP_FAMILY_ANY) {
                continue;
            }
            if ((bitMaskValue & family.getValue()) == family.getValue()) {
                bearerIpTypes.add(toBearerIpType(family.toUInt32()));
            }
        }
        return bearerIpTypes;
    }
}
