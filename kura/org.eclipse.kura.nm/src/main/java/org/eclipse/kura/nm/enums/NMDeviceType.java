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
    NM_DEVICE_TYPE_VRF,
    NM_DEVICE_TYPE_LOOPBACK;

    public static NMDeviceType fromUInt32(UInt32 type) {
        switch (type.intValue()) {
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
        case 32:
            return NM_DEVICE_TYPE_LOOPBACK;
        case 0:
        default:
            return NM_DEVICE_TYPE_UNKNOWN;
        }
    }

    public static UInt32 toUInt32(NMDeviceType type) {
        switch (type) {
        case NM_DEVICE_TYPE_GENERIC:
            return new UInt32(14);
        case NM_DEVICE_TYPE_ETHERNET:
            return new UInt32(1);
        case NM_DEVICE_TYPE_WIFI:
            return new UInt32(2);
        case NM_DEVICE_TYPE_UNUSED1:
            return new UInt32(3);
        case NM_DEVICE_TYPE_UNUSED2:
            return new UInt32(4);
        case NM_DEVICE_TYPE_BT:
            return new UInt32(5);
        case NM_DEVICE_TYPE_OLPC_MESH:
            return new UInt32(6);
        case NM_DEVICE_TYPE_WIMAX:
            return new UInt32(7);
        case NM_DEVICE_TYPE_MODEM:
            return new UInt32(8);
        case NM_DEVICE_TYPE_INFINIBAND:
            return new UInt32(9);
        case NM_DEVICE_TYPE_BOND:
            return new UInt32(10);
        case NM_DEVICE_TYPE_VLAN:
            return new UInt32(11);
        case NM_DEVICE_TYPE_ADSL:
            return new UInt32(12);
        case NM_DEVICE_TYPE_BRIDGE:
            return new UInt32(13);
        case NM_DEVICE_TYPE_TEAM:
            return new UInt32(15);
        case NM_DEVICE_TYPE_TUN:
            return new UInt32(16);
        case NM_DEVICE_TYPE_IP_TUNNEL:
            return new UInt32(17);
        case NM_DEVICE_TYPE_MACVLAN:
            return new UInt32(18);
        case NM_DEVICE_TYPE_VXLAN:
            return new UInt32(19);
        case NM_DEVICE_TYPE_VETH:
            return new UInt32(20);
        case NM_DEVICE_TYPE_MACSEC:
            return new UInt32(21);
        case NM_DEVICE_TYPE_DUMMY:
            return new UInt32(22);
        case NM_DEVICE_TYPE_PPP:
            return new UInt32(23);
        case NM_DEVICE_TYPE_OVS_INTERFACE:
            return new UInt32(24);
        case NM_DEVICE_TYPE_OVS_PORT:
            return new UInt32(25);
        case NM_DEVICE_TYPE_OVS_BRIDGE:
            return new UInt32(26);
        case NM_DEVICE_TYPE_WPAN:
            return new UInt32(27);
        case NM_DEVICE_TYPE_6LOWPAN:
            return new UInt32(28);
        case NM_DEVICE_TYPE_WIREGUARD:
            return new UInt32(29);
        case NM_DEVICE_TYPE_WIFI_P2P:
            return new UInt32(30);
        case NM_DEVICE_TYPE_VRF:
            return new UInt32(31);
        case NM_DEVICE_TYPE_LOOPBACK:
            return new UInt32(32);
        case NM_DEVICE_TYPE_UNKNOWN:
        default:
            return new UInt32(0);
        }
    }
    
    public static NMDeviceType fromPropertiesString(String typeProperty) {
        switch (typeProperty) {
        case "UNKNOWN": 
            return NM_DEVICE_TYPE_UNKNOWN;
        case "ETHERNET": 
            return NM_DEVICE_TYPE_ETHERNET;
        case "WIFI": 
            return NM_DEVICE_TYPE_WIFI;
        case "UNUSED1": 
            return NM_DEVICE_TYPE_UNUSED1;
        case "UNUSED2": 
            return NM_DEVICE_TYPE_UNUSED2;
        case "BT": 
            return NM_DEVICE_TYPE_BT;
        case "OLPC_MESH": 
            return NM_DEVICE_TYPE_OLPC_MESH;
        case "WIMAX": 
            return NM_DEVICE_TYPE_WIMAX;
        case "MODEM": 
            return NM_DEVICE_TYPE_MODEM;
        case "INFINIBAND": 
            return NM_DEVICE_TYPE_INFINIBAND;
        case "BOND": 
            return NM_DEVICE_TYPE_BOND;
        case "VLAN": 
            return NM_DEVICE_TYPE_VLAN;
        case "ADSL": 
            return NM_DEVICE_TYPE_ADSL;
        case "LOOPBACK": 
            return NM_DEVICE_TYPE_LOOPBACK;
        //Todo: add types not in NetInterfaceType as needed
        default:
            return NM_DEVICE_TYPE_UNKNOWN;
        }
    }
}
