/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.enums;

import org.eclipse.kura.net.status.modem.ModemConnectionStatus;

public enum MMModemState {

    MM_MODEM_STATE_FAILED(-1),
    MM_MODEM_STATE_UNKNOWN(0),
    MM_MODEM_STATE_INITIALIZING(1),
    MM_MODEM_STATE_LOCKED(2),
    MM_MODEM_STATE_DISABLED(3),
    MM_MODEM_STATE_DISABLING(4),
    MM_MODEM_STATE_ENABLING(5),
    MM_MODEM_STATE_ENABLED(6),
    MM_MODEM_STATE_SEARCHING(7),
    MM_MODEM_STATE_REGISTERED(8),
    MM_MODEM_STATE_DISCONNECTING(9),
    MM_MODEM_STATE_CONNECTING(10),
    MM_MODEM_STATE_CONNECTED(11);

    private int value;

    private MMModemState(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static MMModemState toMMModemState(Integer state) {
        switch (state) {
        case -1:
            return MMModemState.MM_MODEM_STATE_FAILED;
        case 0:
            return MMModemState.MM_MODEM_STATE_UNKNOWN;
        case 1:
            return MMModemState.MM_MODEM_STATE_INITIALIZING;
        case 2:
            return MMModemState.MM_MODEM_STATE_LOCKED;
        case 3:
            return MMModemState.MM_MODEM_STATE_DISABLED;
        case 4:
            return MMModemState.MM_MODEM_STATE_DISABLING;
        case 5:
            return MMModemState.MM_MODEM_STATE_ENABLING;
        case 6:
            return MMModemState.MM_MODEM_STATE_ENABLED;
        case 7:
            return MMModemState.MM_MODEM_STATE_SEARCHING;
        case 8:
            return MMModemState.MM_MODEM_STATE_REGISTERED;
        case 9:
            return MMModemState.MM_MODEM_STATE_DISCONNECTING;
        case 10:
            return MMModemState.MM_MODEM_STATE_CONNECTING;
        case 11:
            return MMModemState.MM_MODEM_STATE_CONNECTED;
        default:
            return MMModemState.MM_MODEM_STATE_UNKNOWN;
        }
    }

    public static ModemConnectionStatus toModemState(Integer state) {
        switch (state) {
        case -1:
            return ModemConnectionStatus.FAILED;
        case 0:
            return ModemConnectionStatus.UNKNOWN;
        case 1:
            return ModemConnectionStatus.INITIALIZING;
        case 2:
            return ModemConnectionStatus.LOCKED;
        case 3:
            return ModemConnectionStatus.DISABLED;
        case 4:
            return ModemConnectionStatus.DISABLING;
        case 5:
            return ModemConnectionStatus.ENABLING;
        case 6:
            return ModemConnectionStatus.ENABLED;
        case 7:
            return ModemConnectionStatus.SEARCHING;
        case 8:
            return ModemConnectionStatus.REGISTERED;
        case 9:
            return ModemConnectionStatus.DISCONNECTING;
        case 10:
            return ModemConnectionStatus.CONNECTING;
        case 11:
            return ModemConnectionStatus.CONNECTED;
        default:
            return ModemConnectionStatus.UNKNOWN;
        }
    }
}
