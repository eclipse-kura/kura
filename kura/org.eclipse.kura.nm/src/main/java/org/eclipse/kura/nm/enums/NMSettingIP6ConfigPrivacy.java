package org.eclipse.kura.nm.enums;

public enum NMSettingIP6ConfigPrivacy {

    NM_SETTING_IP6_CONFIG_PRIVACY_UNKNOWN(-1),
    NM_SETTING_IP6_CONFIG_PRIVACY_DISABLED(0),
    NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_PUBLIC_ADDR(1),
    NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_TEMP_ADDR(2);

    private int value;

    private NMSettingIP6ConfigPrivacy(int value) {
        this.value = value;
    }

    public Integer toInt32() {
        return this.value;
    }

    public static NMSettingIP6ConfigPrivacy fromInt32(Integer intValue) {
        switch (intValue) {
        case -1:
            return NM_SETTING_IP6_CONFIG_PRIVACY_UNKNOWN;
        case 0:
            return NM_SETTING_IP6_CONFIG_PRIVACY_DISABLED;
        case 1:
            return NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_PUBLIC_ADDR;
        case 2:
            return NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_TEMP_ADDR;
        default:
            return NM_SETTING_IP6_CONFIG_PRIVACY_UNKNOWN;
        }
    }

}
