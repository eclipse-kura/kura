package org.eclipse.kura.nm;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.nm.enums.NMSettingIP6ConfigPrivacy;

public enum KuraIp6Privacy {

    UNKNOWN,
    DISABLED,
    ENABLED_PUBLIC_ADD,
    ENABLED_TEMP_ADD;

    private static final List<KuraIp6Privacy> ENABLED_STATUS = Arrays.asList(KuraIp6Privacy.UNKNOWN,
            KuraIp6Privacy.DISABLED, KuraIp6Privacy.ENABLED_PUBLIC_ADD, KuraIp6Privacy.ENABLED_TEMP_ADD);

    public static Boolean isEnabled(KuraIp6Privacy status) {
        return ENABLED_STATUS.contains(status);
    }

    public static KuraIp6Privacy fromString(String status) {
        switch (status) {
        case "netIPv6PrivacyUnknown":
            return KuraIp6Privacy.UNKNOWN;
        case "netIPv6PrivacyDisabled":
            return KuraIp6Privacy.DISABLED;
        case "netIPv6PrivacyEnabledPubAdd":
            return KuraIp6Privacy.ENABLED_PUBLIC_ADD;
        case "netIPv6PrivacyEnabledTempAdd":
            return KuraIp6Privacy.ENABLED_TEMP_ADD;
        default:
            return KuraIp6Privacy.UNKNOWN;
        }
    }

    public static NMSettingIP6ConfigPrivacy toNMSettingIP6ConfigPrivacy(KuraIp6Privacy privacyValue) {
        switch (privacyValue) {
        case UNKNOWN:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_UNKNOWN;
        case DISABLED:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_DISABLED;
        case ENABLED_PUBLIC_ADD:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_PUBLIC_ADDR;
        case ENABLED_TEMP_ADD:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_TEMP_ADDR;
        default:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_UNKNOWN;
        }
    }
}
