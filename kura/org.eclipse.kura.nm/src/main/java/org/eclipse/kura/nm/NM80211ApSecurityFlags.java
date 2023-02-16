package org.eclipse.kura.nm;

import java.util.Arrays;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;

public enum NM80211ApSecurityFlags {

    NM_802_11_AP_SEC_NONE,
    NM_802_11_AP_SEC_PAIR_WEP40,
    NM_802_11_AP_SEC_PAIR_WEP104,
    NM_802_11_AP_SEC_PAIR_TKIP,
    NM_802_11_AP_SEC_PAIR_CCMP,
    NM_802_11_AP_SEC_GROUP_WEP40,
    NM_802_11_AP_SEC_GROUP_WEP104,
    NM_802_11_AP_SEC_GROUP_TKIP,
    NM_802_11_AP_SEC_GROUP_CCMP,
    NM_802_11_AP_SEC_KEY_MGMT_PSK,
    NM_802_11_AP_SEC_KEY_MGMT_802_1X,
    NM_802_11_AP_SEC_KEY_MGMT_SAE,
    NM_802_11_AP_SEC_KEY_MGMT_OWE,
    NM_802_11_AP_SEC_KEY_MGMT_OWE_TM,
    NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192;

    public static List<NM80211ApSecurityFlags> fromUInt32(UInt32 value) {
        // TODO
        return Arrays.asList(NM80211ApSecurityFlags.NM_802_11_AP_SEC_NONE);
    }
}
