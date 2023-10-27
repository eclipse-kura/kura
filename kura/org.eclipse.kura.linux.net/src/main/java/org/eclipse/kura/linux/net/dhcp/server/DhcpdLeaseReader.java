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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerLeaseReader;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.net.dhcp.DhcpLease;

public class DhcpdLeaseReader extends AbstractDhcpLeaseReader implements DhcpServerLeaseReader {

    private static final Pattern DHCP_LEASES_PATTERN = Pattern.compile("MAC (.*) IP (.*) HOSTNAME (.*) BEGIN .*");

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
            Matcher m = DHCP_LEASES_PATTERN.matcher(line);
            if (m.matches()) {
                String macAddress = m.group(1);
                String ipAddress = m.group(2);
                String hostname = m.group(3);
                DhcpLease dl = new DhcpLease(macAddress, ipAddress, hostname);
                leases.add(dl);
            }
        }
        return leases;
    }

}
