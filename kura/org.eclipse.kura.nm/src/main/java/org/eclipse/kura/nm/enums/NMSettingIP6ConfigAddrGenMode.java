package org.eclipse.kura.nm.enums;

public enum NMSettingIP6ConfigAddrGenMode {

    NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_EUI64(0),
    NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_STABLE_PRIVACY(1),
    NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_DEFAULT_OR_EUI64(2),
    NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_DEFAULT(3);

    private int value;

    private NMSettingIP6ConfigAddrGenMode(int value) {
        this.value = value;
    }

    public Integer toInt32() {
        return this.value;
    }

    public static NMSettingIP6ConfigAddrGenMode fromInt32(Integer intValue) {
        switch (intValue) {
        case 0:
            return NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_EUI64;
        case 1:
            return NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_STABLE_PRIVACY;
        case 2:
            return NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_DEFAULT_OR_EUI64;
        case 3:
            return NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_DEFAULT;
        default:
            return NM_SETTING_IP6_CONFIG_ADDR_GEN_MODE_DEFAULT;
        }
    }

}
