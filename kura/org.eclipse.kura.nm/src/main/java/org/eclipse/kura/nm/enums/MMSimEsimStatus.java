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

import org.eclipse.kura.net.status.modem.ESimStatus;
import org.freedesktop.dbus.types.UInt32;

public enum MMSimEsimStatus {

    MM_SIM_ESIM_STATUS_UNKNOWN(0x00),
    MM_SIM_ESIM_STATUS_NO_PROFILES(0x01),
    MM_SIM_ESIM_STATUS_WITH_PROFILES(0x02);

    private int value;

    private MMSimEsimStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMSimEsimStatus toMMSimEsimStatus(UInt32 type) {
        switch (type.intValue()) {
        case 0x00:
            return MMSimEsimStatus.MM_SIM_ESIM_STATUS_UNKNOWN;
        case 0x01:
            return MMSimEsimStatus.MM_SIM_ESIM_STATUS_NO_PROFILES;
        case 0x02:
            return MMSimEsimStatus.MM_SIM_ESIM_STATUS_WITH_PROFILES;
        default:
            return MMSimEsimStatus.MM_SIM_ESIM_STATUS_UNKNOWN;
        }
    }

    public static ESimStatus toESimStatus(UInt32 type) {
        switch (type.intValue()) {
        case 0x00:
            return ESimStatus.UNKNOWN;
        case 0x01:
            return ESimStatus.NO_PROFILES;
        case 0x02:
            return ESimStatus.WITH_PROFILES;
        default:
            return ESimStatus.UNKNOWN;
        }
    }
}
