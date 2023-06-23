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

import org.eclipse.kura.net.status.modem.ModemPowerState;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemPowerState {

    MM_MODEM_POWER_STATE_UNKNOWN(0x00),
    MM_MODEM_POWER_STATE_OFF(0x01),
    MM_MODEM_POWER_STATE_LOW(0x02),
    MM_MODEM_POWER_STATE_ON(0x03);

    private int value;

    private MMModemPowerState(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMModemPowerState toMMModemPowerState(UInt32 type) {
        switch (type.intValue()) {
        case 0x00:
            return MMModemPowerState.MM_MODEM_POWER_STATE_UNKNOWN;
        case 0x01:
            return MMModemPowerState.MM_MODEM_POWER_STATE_OFF;
        case 0x02:
            return MMModemPowerState.MM_MODEM_POWER_STATE_LOW;
        case 0x03:
            return MMModemPowerState.MM_MODEM_POWER_STATE_ON;
        default:
            return MMModemPowerState.MM_MODEM_POWER_STATE_UNKNOWN;
        }
    }

    public static ModemPowerState toModemPowerState(UInt32 type) {
        switch (type.intValue()) {
        case 0x00:
            return ModemPowerState.UNKNOWN;
        case 0x01:
            return ModemPowerState.OFF;
        case 0x02:
            return ModemPowerState.LOW;
        case 0x03:
            return ModemPowerState.ON;
        default:
            return ModemPowerState.UNKNOWN;
        }
    }
}
