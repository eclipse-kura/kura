package org.eclipse.kura.nm;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.nm.enums.NMSettingIP6ConfigAddrGenMode;
import org.eclipse.kura.nm.enums.NMSettingIP6ConfigPrivacy;

public enum KuraIP6ConfigAddGenMode {

    UNKNOWN,
    EUI64,
    STABLE_PRIVACY;

    private static final List<KuraIP6ConfigAddGenMode> ADDRESS_GEN_MODE = Arrays.asList(KuraIP6ConfigAddGenMode.UNKNOWN,
            KuraIP6ConfigAddGenMode.EUI64, KuraIP6ConfigAddGenMode.STABLE_PRIVACY);

    public static Boolean isEnabled(KuraIP6ConfigAddGenMode status) {
        return ADDRESS_GEN_MODE.contains(status);
    }

    // UNKNOWN value is not present in NM, it will be used to track unwanted configurations
    public static KuraIP6ConfigAddGenMode fromString(String status) {
        switch (status) {
        case "netIPv6AddressGenModeEUI64":
            return KuraIP6ConfigAddGenMode.EUI64;
        case "netIPv6AddressGenModeStablePrivacy":
            return KuraIP6ConfigAddGenMode.STABLE_PRIVACY;
        default:
            return KuraIP6ConfigAddGenMode.UNKNOWN;
        }
    }

    // Default will throw an IllegalArgumentException
    public static NMSettingIP6ConfigAddrGenMode toNMSettingIP6ConfigAddrGenMode(KuraIP6ConfigAddGenMode privacyValue) {
        switch (privacyValue) {
        case EUI64:
            return NMSettingIP6ConfigAddrGenMode.NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_EUI64;
        case STABLE_PRIVACY:
            return NMSettingIP6ConfigAddrGenMode.NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_STABLE_PRIVACY;
        default:
            return NMSettingIP6ConfigAddrGenMode.NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_DEFAULT;
        }
    }
}
