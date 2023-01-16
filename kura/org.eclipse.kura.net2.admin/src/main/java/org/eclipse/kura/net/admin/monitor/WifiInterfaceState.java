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
 *******************************************************************************/
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.net.IPAddress;

public class WifiInterfaceState extends InterfaceState {

    /**
     *
     * @param interfaceName
     *            interface name as {@link String}
     * @param up
     *            if true the interface is up
     * @param link
     *            if true the interface has link
     * @param ipAddress
     *            the {@link IPAddress} assigned to the interface
     * @param carrierChanges
     *            the number of times the network has been connected or disconnected
     */
    public WifiInterfaceState(String interfaceName, boolean up, boolean link, IPAddress ipAddress, int carrierChanges) {
        super(interfaceName, up, link, ipAddress, carrierChanges);
    }

}