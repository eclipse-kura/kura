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
package org.eclipse.kura.nm.configuration;

import org.freedesktop.dbus.types.UInt32;

public enum NMDeviceType {

    NM_DEVICE_TYPE_UNKNOWN,
    NM_DEVICE_TYPE_GENERIC,
    NM_DEVICE_TYPE_ETHERNET,
    NM_DEVICE_TYPE_WIFI,
    NM_DEVICE_TYPE_UNUSED1,
    NM_DEVICE_TYPE_UNUSED2,
    NM_DEVICE_TYPE_BT,
    NM_DEVICE_TYPE_OLPC_MESH,
    NM_DEVICE_TYPE_WIMAX,
    NM_DEVICE_TYPE_MODEM,
    NM_DEVICE_TYPE_INFINIBAND,
    NM_DEVICE_TYPE_BOND,
    NM_DEVICE_TYPE_VLAN,
    NM_DEVICE_TYPE_ADSL,
    NM_DEVICE_TYPE_BRIDGE,
    NM_DEVICE_TYPE_TEAM,
    NM_DEVICE_TYPE_TUN,
    NM_DEVICE_TYPE_IP_TUNNEL,
    NM_DEVICE_TYPE_MACVLAN,
    NM_DEVICE_TYPE_VXLAN,
    NM_DEVICE_TYPE_VETH,
    NM_DEVICE_TYPE_MACSEC,
    NM_DEVICE_TYPE_DUMMY,
    NM_DEVICE_TYPE_PPP,
    NM_DEVICE_TYPE_OVS_INTERFACE,
    NM_DEVICE_TYPE_OVS_PORT,
    NM_DEVICE_TYPE_OVS_BRIDGE,
    NM_DEVICE_TYPE_WPAN,
    NM_DEVICE_TYPE_6LOWPAN,
    NM_DEVICE_TYPE_WIREGUARD,
    NM_DEVICE_TYPE_WIFI_P2P,
    NM_DEVICE_TYPE_VRF;

    public static NMDeviceType fromUInt32(UInt32 type) {
        switch (type.intValue()) {
        case 0:
            return NM_DEVICE_TYPE_UNKNOWN;
        case 14:
            return NM_DEVICE_TYPE_GENERIC;
        case 1:
            return NM_DEVICE_TYPE_ETHERNET;
        case 2:
            return NM_DEVICE_TYPE_WIFI;
        case 3:
            return NM_DEVICE_TYPE_UNUSED1;
        case 4:
            return NM_DEVICE_TYPE_UNUSED2;
        case 5:
            return NM_DEVICE_TYPE_BT;
        case 6:
            return NM_DEVICE_TYPE_OLPC_MESH;
        case 7:
            return NM_DEVICE_TYPE_WIMAX;
        case 8:
            return NM_DEVICE_TYPE_MODEM;
        case 9:
            return NM_DEVICE_TYPE_INFINIBAND;
        case 10:
            return NM_DEVICE_TYPE_BOND;
        case 11:
            return NM_DEVICE_TYPE_VLAN;
        case 12:
            return NM_DEVICE_TYPE_ADSL;
        case 13:
            return NM_DEVICE_TYPE_BRIDGE;
        case 15:
            return NM_DEVICE_TYPE_TEAM;
        case 16:
            return NM_DEVICE_TYPE_TUN;
        case 17:
            return NM_DEVICE_TYPE_IP_TUNNEL;
        case 18:
            return NM_DEVICE_TYPE_MACVLAN;
        case 19:
            return NM_DEVICE_TYPE_VXLAN;
        case 20:
            return NM_DEVICE_TYPE_VETH;
        case 21:
            return NM_DEVICE_TYPE_MACSEC;
        case 22:
            return NM_DEVICE_TYPE_DUMMY;
        case 23:
            return NM_DEVICE_TYPE_PPP;
        case 24:
            return NM_DEVICE_TYPE_OVS_INTERFACE;
        case 25:
            return NM_DEVICE_TYPE_OVS_PORT;
        case 26:
            return NM_DEVICE_TYPE_OVS_BRIDGE;
        case 27:
            return NM_DEVICE_TYPE_WPAN;
        case 28:
            return NM_DEVICE_TYPE_6LOWPAN;
        case 29:
            return NM_DEVICE_TYPE_WIREGUARD;
        case 30:
            return NM_DEVICE_TYPE_WIFI_P2P;
        case 31:
            return NM_DEVICE_TYPE_VRF;
        default:
            return null;
        }
    }
}
