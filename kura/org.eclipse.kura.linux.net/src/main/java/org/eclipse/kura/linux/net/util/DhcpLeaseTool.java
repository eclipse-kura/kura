/*******************************************************************************
 * Copyright (c) 2022, 2023 Sterwen Technology and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Sterwen-Technology
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerLeaseReader;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpLeaseTool {

    private static final Logger logger = LoggerFactory.getLogger(DhcpLeaseTool.class);

    private DhcpLeaseTool() {

    }

    public static List<DhcpLease> probeLeases(String interfaceName, CommandExecutorService executorService) {
        List<DhcpLease> leases = new ArrayList<>();
        Optional<DhcpServerLeaseReader> dhcpServerLeaseParser = DhcpServerManager.getLeaseReader();
        if (dhcpServerLeaseParser.isPresent()) {
            try {
                leases = dhcpServerLeaseParser.get().readLeases(interfaceName, executorService);
            } catch (KuraException e) {
                logger.debug("Cannot parse DHCP server leases", e);
            }
        }
        return leases;
    }

}
