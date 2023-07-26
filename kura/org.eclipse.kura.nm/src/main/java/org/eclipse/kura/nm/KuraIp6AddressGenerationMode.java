package org.eclipse.kura.nm;

import org.eclipse.kura.nm.enums.NMSettingIP6ConfigAddrGenMode;

public enum KuraIp6AddressGenerationMode {

    EUI64,
    STABLE_PRIVACY;

    public static KuraIp6AddressGenerationMode fromString(String status) {
        switch (status) {
        case "netIPv6AddressGenModeEUI64":
            return KuraIp6AddressGenerationMode.EUI64;
        case "netIPv6AddressGenModeStablePrivacy":
            return KuraIp6AddressGenerationMode.STABLE_PRIVACY;
        default:
            throw new IllegalArgumentException(
                    String.format("Unsupported IPv6 address generation mode: \"%s\"", status));
        }
    }

    public static NMSettingIP6ConfigAddrGenMode toNMSettingIP6ConfigAddrGenMode(
            KuraIp6AddressGenerationMode privacyValue) {
        switch (privacyValue) {
        case EUI64:
            return NMSettingIP6ConfigAddrGenMode.NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_EUI64;
        case STABLE_PRIVACY:
            return NMSettingIP6ConfigAddrGenMode.NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_STABLE_PRIVACY;
        default:
            throw new IllegalArgumentException(
                    String.format("Unsupported IPv6 address generation mode: \"%s\"", privacyValue));
        }
    }
}
