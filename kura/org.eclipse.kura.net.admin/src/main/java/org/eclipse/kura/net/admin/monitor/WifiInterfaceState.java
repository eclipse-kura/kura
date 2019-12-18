/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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