/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

public enum NetInterfaceConfigMode {

    /** network interface is configured as DHCP client **/
    netIPv4ConfigModeDhcp,

    /** network interface is configured with static IP address **/
    netIPv4ConfigModeStatic,

    /**
     * network interface is configured in 'manual' or 'BOOTPROTO=none' mode
     * and is not managed by Linux.
     */
    netIPv4ConfigModeManual;
}
