/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net;

/**
 * Used to store network interface types
 */
public enum NetInterfaceType {

    /** The device type is unknown. */
    UNKNOWN,
    /** The device is wired Ethernet device. */
    ETHERNET,
    /** The device is an 802.11 WiFi device. */
    WIFI,
    /** Unused */
    UNUSED1,
    /** Unused */
    UNUSED2,
    /** The device is Bluetooth device that provides PAN or DUN capabilities. */
    BT,
    /** The device is an OLPC mesh networking device. */
    OLPC_MESH,
    /** The device is an 802.16e Mobile WiMAX device. */
    WIMAX,
    /**
     * The device is a modem supporting one or more of analog telephone, CDMA/EVDO, GSM/UMTS/HSPA, or LTE
     * standards to access a cellular or wireline data network.
     */
    MODEM,
    /** The device is an IP-capable InfiniBand interface. */
    INFINIBAND,
    /** The device is a bond master interface. */
    BOND,
    /** The device is a VLAN interface. */
    VLAN,
    /** The device is an ADSL device supporting PPPoE and PPPoATM protocols. */
    ADSL,
    /** The device is a loopback device. */
    LOOPBACK;
}
