package org.eclipse.kura.nm;

import java.util.Arrays;
import java.util.List;

public enum KuraIP6ConfigPrivacy {

    UNKNOWN,
    DISABLED,
    ENABLED_PUBLIC_ADD,
    ENABLED_TEMP_ADD;

    private static final List<KuraIP6ConfigPrivacy> ENABLED_STATUS = Arrays.asList(KuraIP6ConfigPrivacy.UNKNOWN,
            KuraIP6ConfigPrivacy.DISABLED, KuraIP6ConfigPrivacy.ENABLED_PUBLIC_ADD,
            KuraIP6ConfigPrivacy.ENABLED_TEMP_ADD);

    public static Boolean isEnabled(KuraIP6ConfigPrivacy status) {
        return ENABLED_STATUS.contains(status);
    }

    public static KuraIP6ConfigPrivacy fromString(String status) {
        switch (status) {
        case "netIPv6PrivacyUnknown":
            return KuraIP6ConfigPrivacy.UNKNOWN;
        case "netIPv6PrivacyDisabled":
            return KuraIP6ConfigPrivacy.DISABLED;
        case "netIPv6PrivacyEnabledPubAdd":
            return KuraIP6ConfigPrivacy.ENABLED_PUBLIC_ADD;
        case "netIPv6PrivacyEnabledTempAdd":
            return KuraIP6ConfigPrivacy.ENABLED_TEMP_ADD;
        default:
            return KuraIP6ConfigPrivacy.UNKNOWN;
        }
    }

    public static Integer ip6PrivacyCode(KuraIP6ConfigPrivacy privacyValue) {
        switch (privacyValue) {
        case UNKNOWN:
            return -1;
        case DISABLED:
            return 0;
        case ENABLED_PUBLIC_ADD:
            return 1;
        case ENABLED_TEMP_ADD:
            return 2;
        default:
            return -1;
        }
    }
}
