package org.eclipse.kura.nm;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.nm.enums.MMModemLocationSource;

public enum KuraModemGPSMode {

    UNMANAGED,
    MANAGED_GPS;

    public static KuraModemGPSMode fromString(String mode) {
        switch (mode) {
        case "unmanaged":
            return KuraModemGPSMode.UNMANAGED;
        case "managed-gps":
            return KuraModemGPSMode.MANAGED_GPS;
        default:
            throw new IllegalArgumentException(String.format("Unsupported modem GPS mode value: \"%s\"", mode));
        }
    }

    public static Optional<KuraModemGPSMode> fromString(Optional<String> mode) {
        if (mode.isPresent()) {
            switch (mode.get()) {
            case "unmanaged":
                return Optional.of(KuraModemGPSMode.UNMANAGED);
            case "managed-gps":
                return Optional.of(KuraModemGPSMode.MANAGED_GPS);
            default:
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static Set<MMModemLocationSource> toMMModemLocationSources(KuraModemGPSMode mode) {
        switch (mode) {
        case UNMANAGED:
            return EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED);
        case MANAGED_GPS:
            return EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA);
        default:
            throw new IllegalArgumentException(String.format("Unsupported modem GPS mode value: \"%s\"", mode));
        }
    }
}
