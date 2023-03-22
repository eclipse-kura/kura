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
 ******************************************************************************/
package org.eclipse.kura.net.status;

/**
 * The type of a network interface.
 *
 */
public enum NetworkInterfaceType {

    /** The device type is unknown. */
    UNKNOWN,
    /** The device is a wired Ethernet device. */
    ETHERNET,
    /** The device is an 802.11 WiFi device. */
    WIFI,
    /** Unused */
    UNUSED1,
    /** Unused */
    UNUSED2,
    /** The device is a Bluetooth device. */
    BT,
    /** The device is an OLPC mesh networking device. */
    OLPC_MESH,
    /** The device is an 802.16e Mobile WiMAX device. */
    WIMAX,
    /**
     * The device is a modem supporting one or more of analog telephone, CDMA/EVDO,
     * GSM/UMTS/HSPA, or LTE standards to access a cellular or wireline data
     * network.
     */
    MODEM,
    /** The device is an IP-capable InfiniBand interface. */
    INFINIBAND,
    /** The device is a bond master interface. */
    BOND,
    /** The device is a VLAN interface. */
    VLAN,
    /** The device is an ADSL device. */
    ADSL,
    /** The device is a bridge master interface. */
    BRIDGE,
    /** This is a generic support for unrecognized device types. */
    GENERIC,
    /** The device is a team master interface. */
    TEAM,
    /** The device is a TUN or TAP interface. */
    TUN,
    /** The device is an IP tunnel interface. */
    TUNNEL,
    /** The device is a MACVLAN interface. */
    MACVLAN,
    /** The device is a VXLAN interface. */
    VXLAN,
    /** The device is a VETH interface. */
    VETH,
    /** The device is a MACsec interface. */
    MACSEC,
    /** The device is a dummy interface. */
    DUMMY,
    /** The device is a PPP interface. */
    PPP,
    /** The device is a Open vSwitch interface. */
    OVS_INTERFACE,
    /** The device is a Open vSwitch port. */
    OVS_PORT,
    /** The device is a Open vSwitch bridge. */
    OVS_BRIDGE,
    /** The device is a IEEE 802.15.4 (WPAN) MAC Layer Device. */
    WPAN,
    /** The device is a 6LoWPAN interface. */
    SIXLOWPAN,
    /** The device is a WireGuard interface. */
    WIREGUARD,
    /** The device is an 802.11 Wi-Fi P2P device. */
    WIFI_P2P,
    /** The device is a VRF (Virtual Routing and Forwarding) interface. */
    VRF,
    /** The device is a loopback device. */
    LOOPBACK;

}
