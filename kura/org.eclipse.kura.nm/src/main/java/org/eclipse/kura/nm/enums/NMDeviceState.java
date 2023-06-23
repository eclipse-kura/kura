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

import java.util.Arrays;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;

public enum NMDeviceState {

    NM_DEVICE_STATE_UNKNOWN,
    NM_DEVICE_STATE_UNMANAGED,
    NM_DEVICE_STATE_UNAVAILABLE,
    NM_DEVICE_STATE_DISCONNECTED,
    NM_DEVICE_STATE_PREPARE,
    NM_DEVICE_STATE_CONFIG,
    NM_DEVICE_STATE_NEED_AUTH,
    NM_DEVICE_STATE_IP_CONFIG,
    NM_DEVICE_STATE_IP_CHECK,
    NM_DEVICE_STATE_SECONDARIES,
    NM_DEVICE_STATE_ACTIVATED,
    NM_DEVICE_STATE_DEACTIVATING,
    NM_DEVICE_STATE_FAILED;

    private static final List<NMDeviceState> DISCONNECTED_STATES = Arrays.asList(NMDeviceState.NM_DEVICE_STATE_UNKNOWN,
            NMDeviceState.NM_DEVICE_STATE_UNMANAGED, NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE,
            NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);

    public static Boolean isConnected(NMDeviceState state) {
        return !DISCONNECTED_STATES.contains(state);
    }

    public static NMDeviceState fromUInt32(UInt32 type) {
        switch (type.intValue()) {
        case 0:
            return NMDeviceState.NM_DEVICE_STATE_UNKNOWN;
        case 10:
            return NMDeviceState.NM_DEVICE_STATE_UNMANAGED;
        case 20:
            return NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE;
        case 30:
            return NMDeviceState.NM_DEVICE_STATE_DISCONNECTED;
        case 40:
            return NMDeviceState.NM_DEVICE_STATE_PREPARE;
        case 50:
            return NMDeviceState.NM_DEVICE_STATE_CONFIG;
        case 60:
            return NMDeviceState.NM_DEVICE_STATE_NEED_AUTH;
        case 70:
            return NMDeviceState.NM_DEVICE_STATE_IP_CONFIG;
        case 80:
            return NMDeviceState.NM_DEVICE_STATE_IP_CHECK;
        case 90:
            return NMDeviceState.NM_DEVICE_STATE_SECONDARIES;
        case 100:
            return NMDeviceState.NM_DEVICE_STATE_ACTIVATED;
        case 110:
            return NMDeviceState.NM_DEVICE_STATE_DEACTIVATING;
        case 120:
            return NMDeviceState.NM_DEVICE_STATE_FAILED;
        default:
            return NMDeviceState.NM_DEVICE_STATE_UNKNOWN;
        }
    }

    public static UInt32 toUInt32(NMDeviceState state) {
        switch (state) {
        case NM_DEVICE_STATE_UNMANAGED:
            return new UInt32(10);
        case NM_DEVICE_STATE_UNAVAILABLE:
            return new UInt32(20);
        case NM_DEVICE_STATE_DISCONNECTED:
            return new UInt32(30);
        case NM_DEVICE_STATE_PREPARE:
            return new UInt32(40);
        case NM_DEVICE_STATE_CONFIG:
            return new UInt32(50);
        case NM_DEVICE_STATE_NEED_AUTH:
            return new UInt32(60);
        case NM_DEVICE_STATE_IP_CONFIG:
            return new UInt32(70);
        case NM_DEVICE_STATE_IP_CHECK:
            return new UInt32(80);
        case NM_DEVICE_STATE_SECONDARIES:
            return new UInt32(90);
        case NM_DEVICE_STATE_ACTIVATED:
            return new UInt32(100);
        case NM_DEVICE_STATE_DEACTIVATING:
            return new UInt32(110);
        case NM_DEVICE_STATE_FAILED:
            return new UInt32(120);
        case NM_DEVICE_STATE_UNKNOWN:
        default:
            return new UInt32(0);
        }
    }
}
