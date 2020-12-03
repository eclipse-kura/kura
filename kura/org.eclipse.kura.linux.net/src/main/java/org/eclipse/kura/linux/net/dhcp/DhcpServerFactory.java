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
package org.eclipse.kura.linux.net.dhcp;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;

public class DhcpServerFactory {

    private static Map<String, DhcpServerImpl> dhcpServers;

    private DhcpServerFactory() {
    }

    public static DhcpServerImpl getInstance(String interfaceName, boolean enabled, boolean passDns,
            CommandExecutorService executorService) throws KuraException {
        if (dhcpServers == null) {
            dhcpServers = new Hashtable<>();
        }

        DhcpServerImpl dhcpServer = dhcpServers.get(interfaceName);
        if (dhcpServer == null) {
            dhcpServer = new DhcpServerImpl(interfaceName, enabled, passDns, executorService);
            dhcpServers.put(interfaceName, dhcpServer);
        }

        return dhcpServer;
    }
}
