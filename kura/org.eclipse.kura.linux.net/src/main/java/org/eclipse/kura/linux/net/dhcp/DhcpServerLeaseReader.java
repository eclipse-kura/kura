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
package org.eclipse.kura.linux.net.dhcp;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.dhcp.DhcpLease;

public interface DhcpServerLeaseReader {

    /**
     * Reads the Leases provided by a DHCP Server
     * 
     * @param interfaceName  the network interface where the DHCP Server is running
     * @param commandService the commandExecutorService used to run system commands
     * @return a List of DhcpLeases
     * @throws KuraException
     */
    public List<DhcpLease> readLeases(String interfaceName, CommandExecutorService commandService) throws KuraException;
}
