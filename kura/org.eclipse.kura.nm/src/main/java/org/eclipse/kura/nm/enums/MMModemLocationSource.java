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

import org.freedesktop.dbus.types.UInt32;

public enum MMModemLocationSource {

    MM_MODEM_LOCATION_SOURCE_NONE(0x00000000),
    MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI(0x00000001),
    MM_MODEM_LOCATION_SOURCE_GPS_RAW(0x00000002),
    MM_MODEM_LOCATION_SOURCE_GPS_NMEA(0x00000004),
    MM_MODEM_LOCATION_SOURCE_CDMA_BS(0x00000008),
    MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED(0x00000010),
    MM_MODEM_LOCATION_SOURCE_AGPS_MSA(0x00000020),
    MM_MODEM_LOCATION_SOURCE_AGPS_MSB(0x00000040);

    private long value;

    private MMModemLocationSource(long value) {
        this.value = value;
    }

    public long getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMModemLocationSource toMMModemLocationSource(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE;
        case 0x00000001:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI;
        case 0x00000002:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW;
        case 0x00000004:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA;
        case 0x00000008:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_CDMA_BS;
        case 0x00000010:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED;
        case 0x00000020:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_AGPS_MSA;
        case 0x00000040:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_AGPS_MSB;
        default:
            return MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE;
        }
    }

    public static Set<MMModemLocationSource> toMMModemLocationSourceFromBitMask(UInt32 bitMask) {
        long bitMaskValue = bitMask.longValue();
        if (bitMaskValue == 0x00000000) {
            return EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE);
        }

        EnumSet<MMModemLocationSource> modemLocationSources = EnumSet.noneOf(MMModemLocationSource.class);
        for (MMModemLocationSource locationSource : MMModemLocationSource.values()) {
            if (locationSource == MM_MODEM_LOCATION_SOURCE_NONE) {
                continue;
            }
            if ((bitMaskValue & locationSource.getValue()) == locationSource.getValue()) {
                modemLocationSources.add(locationSource);
            }
        }
        return modemLocationSources;
    }

    public static UInt32 toBitMaskFromMMModemLocationSource(Set<MMModemLocationSource> desiredLocationSources) {
        long bitmask = 0x00000000;
        for (MMModemLocationSource source : desiredLocationSources) {
            bitmask = bitmask | source.value;
        }

        return new UInt32(bitmask);
    }
}
