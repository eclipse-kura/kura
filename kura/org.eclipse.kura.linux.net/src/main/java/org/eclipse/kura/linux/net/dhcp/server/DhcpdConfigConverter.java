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
import org.eclipse.kura.net.dhcp.DhcpServerConfig;

public class DhcpdConfigConverter implements DhcpServerConfigConverter {

    @Override
    public String convert(DhcpServerConfig config) {
        StringBuilder sb = new StringBuilder();

        sb.append("# enabled? ").append(config.isEnabled()).append("\n");
        sb.append("# prefix: ").append(config.getPrefix()).append("\n");
        sb.append("# pass DNS? ").append(config.isPassDns()).append("\n\n");

        // Leases file
        sb.append("lease-file-name \"" + DhcpServerManager.getLeasesFilename(config.getInterfaceName()) + "\";\n\n");

        sb.append("subnet " + config.getSubnet().getHostAddress() + " netmask "
                + config.getSubnetMask().getHostAddress() + " {\n");

        // DNS servers
        if (config.isPassDns() && config.getDnsServers() != null && !config.getDnsServers().isEmpty()) {
            sb.append("    option domain-name-servers ");
            for (int i = 0; i < config.getDnsServers().size(); i++) {
                if (config.getDnsServers().get(i) != null) {
                    sb.append(config.getDnsServers().get(i).getHostAddress());
                }

                if (i + 1 == config.getDnsServers().size()) {
                    sb.append(";\n\n");
                } else {
                    sb.append(",");
                }
            }
        }
        // interface
        if (config.getInterfaceName() != null) {
            sb.append("    interface " + config.getInterfaceName() + ";\n");
        }
        // router address
        if (config.getRouterAddress() != null) {
            sb.append("    option routers " + config.getRouterAddress().getHostAddress() + ";\n");
        }
        // if DNS should not be forwarded, add the following lines
        if (!config.isPassDns()) {
            sb.append("    ddns-update-style none;\n");
            sb.append("    ddns-updates off;\n");
        }
        // Lease times
        sb.append("    default-lease-time " + config.getDefaultLeaseTime() + ";\n");
        if (config.getMaximumLeaseTime() > -1) {
            sb.append("    max-lease-time " + config.getMaximumLeaseTime() + ";\n");
        }
        // Add the pool and range
        sb.append("    pool {\n");
        sb.append("        range " + config.getRangeStart().getHostAddress() + " "
                + config.getRangeEnd().getHostAddress() + ";\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

}
