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

import org.eclipse.kura.linux.net.dhcp.DhcpServerConfigConverter;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;

public class UdhcpdConfigConverter implements DhcpServerConfigConverter {

    @Override
    public String convert(DhcpServerConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("start ").append(config.getRangeStart().getHostAddress())
                .append("end ").append(config.getRangeEnd().getHostAddress())
                .append("interface ").append(config.getInterfaceName())
                .append("pidfile ").append(DhcpServerManager.getPidFilename(config.getInterfaceName()))
                .append("lease_file ").append(DhcpServerManager.getLeasesFilename(config.getInterfaceName()))
                .append("max_leases ").append(getMaxLeases(config))
                .append("auto_time 0")
                .append("decline_time ").append(config.getDefaultLeaseTime())
                .append("conflict_time ").append(config.getDefaultLeaseTime())
                .append("offer_time ").append(config.getDefaultLeaseTime())
                .append("min_lease ").append(config.getDefaultLeaseTime())
                .append("opt subnet ").append(config.getSubnetMask().getHostAddress())
                .append("opt router ").append(config.getRouterAddress().getHostAddress())
                .append("opt lease ").append(config.getDefaultLeaseTime());
        if (!config.getDnsServers().isEmpty()) {
            sb.append(addDNSServersOption(config));
        }
        return sb.toString();
    }

    private String addDNSServersOption(DhcpServerConfig config) {
        StringBuilder sb = new StringBuilder();
        config.getDnsServers().stream().filter(address -> address != null)
                .forEach(address -> sb.append(address.getHostAddress()).append(" "));

        return "opt dns " + sb.toString().trim();
    }

    private String getMaxLeases(DhcpServerConfig config) {
        return String.valueOf(ip2int(config.getRangeEnd()) - ip2int(config.getRangeStart()));
    }

    private int ip2int(IPAddress ip) {
        int result = 0;
        for (byte b : ip.getAddress()) {
            result = result << 8 | b & 0xFF;
        }
        return result;
    }
}
