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
package org.eclipse.kura.linux.net.dhcp;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.kura.KuraException;

public class DhcpServerFactory {

    private static Map<String, DhcpServerImpl> dhcpServers;

    private DhcpServerFactory() {
    }

    public static DhcpServerImpl getInstance(String interfaceName, boolean enabled, boolean passDns)
            throws KuraException {
        if (dhcpServers == null) {
            dhcpServers = new Hashtable<>();
        }

        DhcpServerImpl dhcpServer = dhcpServers.get(interfaceName);
        if (dhcpServer == null) {
            dhcpServer = new DhcpServerImpl(interfaceName, enabled, passDns);
            dhcpServers.put(interfaceName, dhcpServer);
        }

        return dhcpServer;
    }
}
