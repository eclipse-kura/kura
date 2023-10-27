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
package org.eclipse.kura.linux.net.dhcp.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerLeaseReader;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.net.dhcp.DhcpLease;

public class UdhcpdLeaseReader extends AbstractDhcpLeaseReader implements DhcpServerLeaseReader {

    @Override
    public List<DhcpLease> readLeases(String interfaceName, CommandExecutorService commandService)
            throws KuraException {
        String[] commandLine = new String[] { "dumpleases", "-f", DhcpServerManager.getLeasesFilename(interfaceName) };
        return getDhcpLeases(commandLine, commandService);
    }

    protected List<DhcpLease> parseDhcpLeases(final InputStream in) {
        List<DhcpLease> leases = new ArrayList<>();
        String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        for (String line : result.split("\n")) {
            if (line.startsWith("Mac Address")) {
                continue;
            }
            String[] items = line.split("\\s+");
            if (items.length >= 3) {
                DhcpLease dl = new DhcpLease(items[0], items[1], items[2]);
                leases.add(dl);
            }
        }
        return leases;
    }

}
