package org.eclipse.kura.nm;

import org.freedesktop.dbus.types.UInt32;

public enum MMModemStateChangeReason {

    MM_MODEM_STATE_CHANGE_REASON_UNKNOWN(0),
    MM_MODEM_STATE_CHANGE_REASON_USER_REQUESTED(1),
    MM_MODEM_STATE_CHANGE_REASON_SUSPEND(2),
    MM_MODEM_STATE_CHANGE_REASON_FAILURE(3);

    private int value;

    private MMModemStateChangeReason(int value) {
        this.value = value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMModemStateChangeReason fromUInt32(UInt32 uValue) {
        switch (uValue.intValue()) {
        case 1:
            return MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_USER_REQUESTED;
        case 2:
            return MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_SUSPEND;
        case 3:
            return MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_FAILURE;
        case 0:
        default:
            return MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_UNKNOWN;
        }
    }
}
