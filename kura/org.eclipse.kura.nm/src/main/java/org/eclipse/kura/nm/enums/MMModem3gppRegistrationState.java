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

import org.eclipse.kura.net.status.modem.RegistrationStatus;
import org.freedesktop.dbus.types.UInt32;

public enum MMModem3gppRegistrationState {

    MM_MODEM_3GPP_REGISTRATION_STATE_IDLE(0),
    MM_MODEM_3GPP_REGISTRATION_STATE_HOME(1),
    MM_MODEM_3GPP_REGISTRATION_STATE_SEARCHING(2),
    MM_MODEM_3GPP_REGISTRATION_STATE_DENIED(3),
    MM_MODEM_3GPP_REGISTRATION_STATE_UNKNOWN(4),
    MM_MODEM_3GPP_REGISTRATION_STATE_ROAMING(5),
    MM_MODEM_3GPP_REGISTRATION_STATE_HOME_SMS_ONLY(6),
    MM_MODEM_3GPP_REGISTRATION_STATE_ROAMING_SMS_ONLY(7),
    MM_MODEM_3GPP_REGISTRATION_STATE_EMERGENCY_ONLY(8),
    MM_MODEM_3GPP_REGISTRATION_STATE_HOME_CSFB_NOT_PREFERRED(9),
    MM_MODEM_3GPP_REGISTRATION_STATE_ROAMING_CSFB_NOT_PREFERRED(10),
    MM_MODEM_3GPP_REGISTRATION_STATE_ATTACHED_RLOS(11);

    private int value;

    private MMModem3gppRegistrationState(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMModem3gppRegistrationState toMMModem3gppRegistrationState(UInt32 type) {
        switch (type.intValue()) {
        case 0:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_IDLE;
        case 1:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_HOME;
        case 2:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_SEARCHING;
        case 3:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_DENIED;
        case 4:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_UNKNOWN;
        case 5:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_ROAMING;
        case 6:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_HOME_SMS_ONLY;
        case 7:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_ROAMING_SMS_ONLY;
        case 8:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_EMERGENCY_ONLY;
        case 9:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_HOME_CSFB_NOT_PREFERRED;
        case 10:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_ROAMING_CSFB_NOT_PREFERRED;
        case 11:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_ATTACHED_RLOS;
        default:
            return MMModem3gppRegistrationState.MM_MODEM_3GPP_REGISTRATION_STATE_UNKNOWN;
        }
    }

    public static RegistrationStatus toRegistrationStatus(UInt32 type) {
        switch (type.intValue()) {
        case 0:
            return RegistrationStatus.IDLE;
        case 1:
            return RegistrationStatus.HOME;
        case 2:
            return RegistrationStatus.SEARCHING;
        case 3:
            return RegistrationStatus.DENIED;
        case 4:
            return RegistrationStatus.UNKNOWN;
        case 5:
            return RegistrationStatus.ROAMING;
        case 6:
            return RegistrationStatus.HOME_SMS_ONLY;
        case 7:
            return RegistrationStatus.ROAMING_SMS_ONLY;
        case 8:
            return RegistrationStatus.EMERGENCY_ONLY;
        case 9:
            return RegistrationStatus.HOME_CSFB_NOT_PREFERRED;
        case 10:
            return RegistrationStatus.ROAMING_CSFB_NOT_PREFERRED;
        case 11:
            return RegistrationStatus.ATTACHED_RLOS;
        default:
            return RegistrationStatus.UNKNOWN;
        }
    }
}
