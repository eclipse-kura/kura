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
 * Used to track interface configuration status
 */
public enum NetInterfaceStatus {
    /** IPv4 configuration is disabled **/
    netIPv4StatusDisabled,

    /**
     * IPv4 configuration is not managed by Kura
     *
     * @since 1.4
     **/
    netIPv4StatusUnmanaged,

    /**
     * IPv4 configuration only at Layer 2 of the OSI model
     *
     * @since 1.4
     **/
    netIPv4StatusL2Only,

    /** IPv4 configuration is enabled as a LAN interface **/
    netIPv4StatusEnabledLAN,

    /** IPv4 configuration is enabled as a WAN interface **/
    netIPv4StatusEnabledWAN,

    /** IPv4 configuration is unknown **/
    netIPv4StatusUnknown,

    /** IPv6 configuration is disabled **/
    netIPv6StatusDisabled,

    /**
     * IPv6 configuration is not managed by Kura
     *
     * @since 1.4
     **/
    netIPv6StatusUnmanaged,

    /**
     * IPv6 configuration only at Layer 2 of the OSI model
     *
     * @since 1.4
     **/
    netIPv6StatusL2Only,

    /** IPv6 configuration is enabled as a LAN interface **/
    netIPv6StatusEnabledLAN,

    /** IPv6 configuration is enabled as a WAN interface **/
    netIPv6StatusEnabledWAN,

    /** IPv6 configuration is unknown **/
    netIPv6StatusUnknown;
}
