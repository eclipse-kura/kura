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

import org.eclipse.kura.net.status.modem.SimType;
import org.freedesktop.dbus.types.UInt32;

public enum MMSimType {

    MM_SIM_TYPE_UNKNOWN(0x00),
    MM_SIM_TYPE_PHYSICAL(0x01),
    MM_SIM_TYPE_ESIM(0x02);

    private int value;

    private MMSimType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMSimType toMMSimType(UInt32 type) {
        switch (type.intValue()) {
        case 0x00:
            return MMSimType.MM_SIM_TYPE_UNKNOWN;
        case 0x01:
            return MMSimType.MM_SIM_TYPE_PHYSICAL;
        case 0x02:
            return MMSimType.MM_SIM_TYPE_ESIM;
        default:
            return MMSimType.MM_SIM_TYPE_UNKNOWN;
        }
    }

    public static SimType toSimType(UInt32 type) {
        switch (type.intValue()) {
        case 0x00:
            return SimType.UNKNOWN;
        case 0x01:
            return SimType.PHYSICAL;
        case 0x02:
            return SimType.ESIM;
        default:
            return SimType.UNKNOWN;
        }
    }
}
