package org.eclipse.kura.nm;

import org.freedesktop.dbus.types.UInt32;

public enum NM80211Mode {

    NM_802_11_MODE_UNKNOWN,
    NM_802_11_MODE_ADHOC,
    NM_802_11_MODE_INFRA,
    NM_802_11_MODE_AP,
    NM_802_11_MODE_MESH;

    public static NM80211Mode fromUInt32(UInt32 value) {
        switch (value.intValue()) {
        case 1:
            return NM80211Mode.NM_802_11_MODE_ADHOC;
        case 2:
            return NM80211Mode.NM_802_11_MODE_INFRA;
        case 3:
            return NM80211Mode.NM_802_11_MODE_AP;
        case 4:
            return NM80211Mode.NM_802_11_MODE_MESH;
        case 0:
        default:
            return NM80211Mode.NM_802_11_MODE_UNKNOWN;
        }
    }
}