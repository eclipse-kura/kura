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

import org.eclipse.kura.net.status.modem.ModemPortType;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemPortType {

    MM_MODEM_PORT_TYPE_UNKNOWN(0x01),
    MM_MODEM_PORT_TYPE_NET(0x02),
    MM_MODEM_PORT_TYPE_AT(0x03),
    MM_MODEM_PORT_TYPE_QCDM(0x04),
    MM_MODEM_PORT_TYPE_GPS(0x05),
    MM_MODEM_PORT_TYPE_QMI(0x06),
    MM_MODEM_PORT_TYPE_MBIM(0x07),
    MM_MODEM_PORT_TYPE_AUDIO(0x08),
    MM_MODEM_PORT_TYPE_IGNORED(0x09);

    private int value;

    private MMModemPortType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMModemPortType toMMModemPortType(UInt32 type) {
        switch (type.intValue()) {
        case 0x01:
            return MMModemPortType.MM_MODEM_PORT_TYPE_UNKNOWN;
        case 0x02:
            return MMModemPortType.MM_MODEM_PORT_TYPE_NET;
        case 0x03:
            return MMModemPortType.MM_MODEM_PORT_TYPE_AT;
        case 0x04:
            return MMModemPortType.MM_MODEM_PORT_TYPE_QCDM;
        case 0x05:
            return MMModemPortType.MM_MODEM_PORT_TYPE_GPS;
        case 0x06:
            return MMModemPortType.MM_MODEM_PORT_TYPE_QMI;
        case 0x07:
            return MMModemPortType.MM_MODEM_PORT_TYPE_MBIM;
        case 0x08:
            return MMModemPortType.MM_MODEM_PORT_TYPE_AUDIO;
        case 0x09:
            return MMModemPortType.MM_MODEM_PORT_TYPE_IGNORED;
        default:
            return MMModemPortType.MM_MODEM_PORT_TYPE_UNKNOWN;
        }
    }

    public static ModemPortType toModemPortType(UInt32 type) {
        switch (type.intValue()) {
        case 0x01:
            return ModemPortType.UNKNOWN;
        case 0x02:
            return ModemPortType.NET;
        case 0x03:
            return ModemPortType.AT;
        case 0x04:
            return ModemPortType.QCDM;
        case 0x05:
            return ModemPortType.GPS;
        case 0x06:
            return ModemPortType.QMI;
        case 0x07:
            return ModemPortType.MBIM;
        case 0x08:
            return ModemPortType.AUDIO;
        case 0x09:
            return ModemPortType.IGNORED;
        default:
            return ModemPortType.UNKNOWN;
        }
    }
}
