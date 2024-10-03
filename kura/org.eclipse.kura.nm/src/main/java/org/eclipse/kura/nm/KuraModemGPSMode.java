/*******************************************************************************
 * Copyright (c) 2014 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.nm;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.kura.nm.enums.MMModemLocationSource;

public enum KuraModemGPSMode {

    KURA_MODEM_GPS_MODE_UNMANAGED("kuraModemGpsModeUnmanaged"),
    KURA_MODEM_GPS_MODE_MANAGED_GPS("kuraModemGpsModeManagedGps");

    private final String value;

    private KuraModemGPSMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static KuraModemGPSMode fromString(String name) {
        for (KuraModemGPSMode auth : KuraModemGPSMode.values()) {
            if (auth.getValue().equals(name)) {
                return auth;
            }
        }

        throw new IllegalArgumentException("Invalid modem GPS mode in snapshot: " + name);
    }

    public static Set<MMModemLocationSource> toMMModemLocationSources(KuraModemGPSMode mode) {
        switch (mode) {
        case KURA_MODEM_GPS_MODE_UNMANAGED:
            return EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED);
        case KURA_MODEM_GPS_MODE_MANAGED_GPS:
            return EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA);
        default:
            throw new IllegalArgumentException(String.format("Unsupported modem GPS mode value: \"%s\"", mode));
        }
    }

    public static Set<KuraModemGPSMode> fromMMModemLocationSources(Set<MMModemLocationSource> sources) {
        Set<KuraModemGPSMode> modes = EnumSet.noneOf(KuraModemGPSMode.class);

        if (sources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED)) {
            modes.add(KuraModemGPSMode.KURA_MODEM_GPS_MODE_UNMANAGED);
        }

        if (sources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW)
                && sources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA)) {
            modes.add(KuraModemGPSMode.KURA_MODEM_GPS_MODE_MANAGED_GPS);
        }

        return modes;
    }
}
