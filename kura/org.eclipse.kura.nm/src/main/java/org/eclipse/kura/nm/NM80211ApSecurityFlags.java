package org.eclipse.kura.nm;

import java.util.ArrayList;
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

    public static List<NM80211ApSecurityFlags> fromUInt32(UInt32 val) {
        if (val.intValue() == 0x00000000) {
            return Arrays.asList(NM80211ApSecurityFlags.NM_802_11_AP_SEC_NONE);
        }

        List<NM80211ApSecurityFlags> flags = new ArrayList<>();

        if ((val.intValue() & 0x00000001) == 0x00000001) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_WEP40);
        }

        if ((val.intValue() & 0x00000002) == 0x00000002) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_WEP104);
        }

        if ((val.intValue() & 0x00000004) == 0x00000004) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_TKIP);
        }

        if ((val.intValue() & 0x00000008) == 0x00000008) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_CCMP);
        }

        if ((val.intValue() & 0x00000010) == 0x00000010) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_WEP40);
        }

        if ((val.intValue() & 0x00000020) == 0x00000020) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_WEP104);
        }

        if ((val.intValue() & 0x00000040) == 0x00000040) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_TKIP);
        }

        if ((val.intValue() & 0x00000080) == 0x00000080) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_CCMP);
        }

        if ((val.intValue() & 0x00000100) == 0x00000100) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_PSK);
        }

        if ((val.intValue() & 0x00000200) == 0x00000200) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_802_1X);
        }

        if ((val.intValue() & 0x00000400) == 0x00000400) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_SAE);
        }

        if ((val.intValue() & 0x00000800) == 0x00000800) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_OWE);
        }

        if ((val.intValue() & 0x00001000) == 0x00001000) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_OWE_TM);
        }

        if ((val.intValue() & 0x00002000) == 0x00002000) {
            flags.add(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192);
        }

        return flags;
    }
}
