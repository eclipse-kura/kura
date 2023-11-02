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

public class DhcpdLeaseReader extends AbstractDhcpLeaseReader implements DhcpServerLeaseReader {

    @Override
    public List<DhcpLease> readLeases(String interfaceName, CommandExecutorService commandService)
            throws KuraException {
        String[] commandLine = new String[] { "dhcp-lease-list", "--parsable", "--lease",
                DhcpServerManager.getLeasesFilename(interfaceName) };
        return getDhcpLeases(commandLine, commandService);
    }

    protected List<DhcpLease> parseDhcpLeases(final InputStream in) {
        List<DhcpLease> leases = new ArrayList<>();
        String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        for (String line : result.split("\n")) {
            String[] items = line.split("\\s+");
            if (checkDhcpdLeaseEntry(items)) {
                String macAddress = items[1];
                String ipAddress = items[3];
                String hostname = items[5];
                DhcpLease dl = new DhcpLease(macAddress, ipAddress, hostname);
                leases.add(dl);
            }
        }
        return leases;
    }

    private boolean checkDhcpdLeaseEntry(String[] parsedLine) {
        // The DHPCD lease entry is as follows:
        // MAC <mac-address> IP <ip-address> HOSTNAME <host-name> BEGIN <start-date> END
        // <end-date> MANUFACTURER <manufacturer>
        return parsedLine.length >= 6 && parsedLine[0].equals("MAC") && parsedLine[2].equals("IP")
                && parsedLine[4].equals("HOSTNAME");
    }

}