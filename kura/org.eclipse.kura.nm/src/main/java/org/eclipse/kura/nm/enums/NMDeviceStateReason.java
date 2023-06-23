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

import org.freedesktop.dbus.types.UInt32;

public enum NMDeviceStateReason {

    NM_DEVICE_STATE_REASON_NONE(0),
    NM_DEVICE_STATE_REASON_UNKNOWN(1),
    NM_DEVICE_STATE_REASON_NOW_MANAGED(2),
    NM_DEVICE_STATE_REASON_NOW_UNMANAGED(3),
    NM_DEVICE_STATE_REASON_CONFIG_FAILED(4),
    NM_DEVICE_STATE_REASON_IP_CONFIG_UNAVAILABLE(5),
    NM_DEVICE_STATE_REASON_IP_CONFIG_EXPIRED(6),
    NM_DEVICE_STATE_REASON_NO_SECRETS(7),
    NM_DEVICE_STATE_REASON_SUPPLICANT_DISCONNECT(8),
    NM_DEVICE_STATE_REASON_SUPPLICANT_CONFIG_FAILED(9),
    NM_DEVICE_STATE_REASON_SUPPLICANT_FAILED(10),
    NM_DEVICE_STATE_REASON_SUPPLICANT_TIMEOUT(11),
    NM_DEVICE_STATE_REASON_PPP_START_FAILED(12),
    NM_DEVICE_STATE_REASON_PPP_DISCONNECT(13),
    NM_DEVICE_STATE_REASON_PPP_FAILED(14),
    NM_DEVICE_STATE_REASON_DHCP_START_FAILED(15),
    NM_DEVICE_STATE_REASON_DHCP_ERROR(16),
    NM_DEVICE_STATE_REASON_DHCP_FAILED(17),
    NM_DEVICE_STATE_REASON_SHARED_START_FAILED(18),
    NM_DEVICE_STATE_REASON_SHARED_FAILED(19),
    NM_DEVICE_STATE_REASON_AUTOIP_START_FAILED(20),
    NM_DEVICE_STATE_REASON_AUTOIP_ERROR(21),
    NM_DEVICE_STATE_REASON_AUTOIP_FAILED(22),
    NM_DEVICE_STATE_REASON_MODEM_BUSY(23),
    NM_DEVICE_STATE_REASON_MODEM_NO_DIAL_TONE(24),
    NM_DEVICE_STATE_REASON_MODEM_NO_CARRIER(25),
    NM_DEVICE_STATE_REASON_MODEM_DIAL_TIMEOUT(26),
    NM_DEVICE_STATE_REASON_MODEM_DIAL_FAILED(27),
    NM_DEVICE_STATE_REASON_MODEM_INIT_FAILED(28),
    NM_DEVICE_STATE_REASON_GSM_APN_FAILED(29),
    NM_DEVICE_STATE_REASON_GSM_REGISTRATION_NOT_SEARCHING(30),
    NM_DEVICE_STATE_REASON_GSM_REGISTRATION_DENIED(31),
    NM_DEVICE_STATE_REASON_GSM_REGISTRATION_TIMEOUT(32),
    NM_DEVICE_STATE_REASON_GSM_REGISTRATION_FAILED(33),
    NM_DEVICE_STATE_REASON_GSM_PIN_CHECK_FAILED(34),
    NM_DEVICE_STATE_REASON_FIRMWARE_MISSING(35),
    NM_DEVICE_STATE_REASON_REMOVED(36),
    NM_DEVICE_STATE_REASON_SLEEPING(37),
    NM_DEVICE_STATE_REASON_CONNECTION_REMOVED(38),
    NM_DEVICE_STATE_REASON_USER_REQUESTED(39),
    NM_DEVICE_STATE_REASON_CARRIER(40),
    NM_DEVICE_STATE_REASON_CONNECTION_ASSUMED(41),
    NM_DEVICE_STATE_REASON_SUPPLICANT_AVAILABLE(42),
    NM_DEVICE_STATE_REASON_MODEM_NOT_FOUND(43),
    NM_DEVICE_STATE_REASON_BT_FAILED(44),
    NM_DEVICE_STATE_REASON_GSM_SIM_NOT_INSERTED(45),
    NM_DEVICE_STATE_REASON_GSM_SIM_PIN_REQUIRED(46),
    NM_DEVICE_STATE_REASON_GSM_SIM_PUK_REQUIRED(47),
    NM_DEVICE_STATE_REASON_GSM_SIM_WRONG(48),
    NM_DEVICE_STATE_REASON_INFINIBAND_MODE(49),
    NM_DEVICE_STATE_REASON_DEPENDENCY_FAILED(50),
    NM_DEVICE_STATE_REASON_BR2684_FAILED(51),
    NM_DEVICE_STATE_REASON_MODEM_MANAGER_UNAVAILABLE(52),
    NM_DEVICE_STATE_REASON_SSID_NOT_FOUND(53),
    NM_DEVICE_STATE_REASON_SECONDARY_CONNECTION_FAILED(54),
    NM_DEVICE_STATE_REASON_DCB_FCOE_FAILED(55),
    NM_DEVICE_STATE_REASON_TEAMD_CONTROL_FAILED(56),
    NM_DEVICE_STATE_REASON_MODEM_FAILED(57),
    NM_DEVICE_STATE_REASON_MODEM_AVAILABLE(58),
    NM_DEVICE_STATE_REASON_SIM_PIN_INCORRECT(59),
    NM_DEVICE_STATE_REASON_NEW_ACTIVATION(60),
    NM_DEVICE_STATE_REASON_PARENT_CHANGED(61),
    NM_DEVICE_STATE_REASON_PARENT_MANAGED_CHANGED(62),
    NM_DEVICE_STATE_REASON_OVSDB_FAILED(63),
    NM_DEVICE_STATE_REASON_IP_ADDRESS_DUPLICATE(64),
    NM_DEVICE_STATE_REASON_IP_METHOD_UNSUPPORTED(65),
    NM_DEVICE_STATE_REASON_SRIOV_CONFIGURATION_FAILED(66),
    NM_DEVICE_STATE_REASON_PEER_NOT_FOUND(67);

    private int value;

    private NMDeviceStateReason(int value) {
        this.value = value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static NMDeviceStateReason fromUInt32(UInt32 uValue) {
        switch (uValue.intValue()) {
        case 0:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_NONE;
        case 2:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_NOW_MANAGED;
        case 3:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_NOW_UNMANAGED;
        case 4:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_CONFIG_FAILED;
        case 5:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_CONFIG_UNAVAILABLE;
        case 6:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_CONFIG_EXPIRED;
        case 7:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_NO_SECRETS;
        case 8:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_DISCONNECT;
        case 9:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_CONFIG_FAILED;
        case 10:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_FAILED;
        case 11:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_TIMEOUT;
        case 12:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_PPP_START_FAILED;
        case 13:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_PPP_DISCONNECT;
        case 14:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_PPP_FAILED;
        case 15:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_DHCP_START_FAILED;
        case 16:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_DHCP_ERROR;
        case 17:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_DHCP_FAILED;
        case 18:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SHARED_START_FAILED;
        case 19:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SHARED_FAILED;
        case 20:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_AUTOIP_START_FAILED;
        case 21:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_AUTOIP_ERROR;
        case 22:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_AUTOIP_FAILED;
        case 23:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_BUSY;
        case 24:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_NO_DIAL_TONE;
        case 25:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_NO_CARRIER;
        case 26:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_DIAL_TIMEOUT;
        case 27:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_DIAL_FAILED;
        case 28:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_INIT_FAILED;
        case 29:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_APN_FAILED;
        case 30:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_NOT_SEARCHING;
        case 31:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_DENIED;
        case 32:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_TIMEOUT;
        case 33:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_FAILED;
        case 34:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_PIN_CHECK_FAILED;
        case 35:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_FIRMWARE_MISSING;
        case 36:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_REMOVED;
        case 37:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SLEEPING;
        case 38:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_CONNECTION_REMOVED;
        case 39:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_USER_REQUESTED;
        case 40:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_CARRIER;
        case 41:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_CONNECTION_ASSUMED;
        case 42:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_AVAILABLE;
        case 43:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_NOT_FOUND;
        case 44:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_BT_FAILED;
        case 45:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_NOT_INSERTED;
        case 46:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_PIN_REQUIRED;
        case 47:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_PUK_REQUIRED;
        case 48:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_WRONG;
        case 49:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_INFINIBAND_MODE;
        case 50:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_DEPENDENCY_FAILED;
        case 51:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_BR2684_FAILED;
        case 52:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_MANAGER_UNAVAILABLE;
        case 53:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SSID_NOT_FOUND;
        case 54:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SECONDARY_CONNECTION_FAILED;
        case 55:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_DCB_FCOE_FAILED;
        case 56:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_TEAMD_CONTROL_FAILED;
        case 57:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_FAILED;
        case 58:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_AVAILABLE;
        case 59:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SIM_PIN_INCORRECT;
        case 60:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_NEW_ACTIVATION;
        case 61:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_PARENT_CHANGED;
        case 62:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_PARENT_MANAGED_CHANGED;
        case 63:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_OVSDB_FAILED;
        case 64:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_ADDRESS_DUPLICATE;
        case 65:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_METHOD_UNSUPPORTED;
        case 66:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_SRIOV_CONFIGURATION_FAILED;
        case 67:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_PEER_NOT_FOUND;
        case 1:
        default:
            return NMDeviceStateReason.NM_DEVICE_STATE_REASON_UNKNOWN;
        }

    }

}
