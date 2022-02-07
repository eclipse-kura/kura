/*******************************************************************************
 * Copyright (c) 2022 Sterwen Technology and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.dhcp.DhcpLease;

public class DhcpLeaseTool {
    private static final Pattern DHCP_LEASES_PATTERN = Pattern.compile("MAC (.*) IP (.*) HOSTNAME (.*) BEGIN .*");

    protected DhcpLeaseTool() {
        
    }

    public static List<DhcpLease> probeLeases(CommandExecutorService executorService) throws KuraException {
        try {
            List<DhcpLease> leases = new ArrayList<>();
            parseDhcpLeases(IwCapabilityTool.exec(new String[] { "dhcp-lease-list", "--parsable" }, executorService), leases);
            return leases;
        } catch (final KuraException e) {
            throw e;
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    private static void parseDhcpLeases(final InputStream in, List<DhcpLease> leases) {
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
    }
}
