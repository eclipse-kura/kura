/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;

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

        appendDNSServers(config, sb);

        appendInterfaceName(config, sb);
        appendRouterAddress(config, sb);
        appendPassDNS(config, sb);
        appendLeaseTime(config, sb);
        appendPoolRange(config, sb);

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private void appendPoolRange(DhcpServerConfig config, StringBuilder sb) {
        sb.append("    pool {\n");
        sb.append("        range " + config.getRangeStart().getHostAddress() + " "
                + config.getRangeEnd().getHostAddress() + ";\n");
    }

    private void appendLeaseTime(DhcpServerConfig config, StringBuilder sb) {
        sb.append("    default-lease-time " + config.getDefaultLeaseTime() + ";\n");
        if (config.getMaximumLeaseTime() > -1) {
            sb.append("    max-lease-time " + config.getMaximumLeaseTime() + ";\n");
        }
    }

    private void appendPassDNS(DhcpServerConfig config, StringBuilder sb) {
        if (!config.isPassDns() || isNull(config.getRouterAddress())) {
            sb.append("    ddns-update-style none;\n");
            sb.append("    ddns-updates off;\n");
        }
    }

    private void appendRouterAddress(DhcpServerConfig config, StringBuilder sb) {
        if (!isNull(config.getRouterAddress())) {
            sb.append("    option routers " + config.getRouterAddress().getHostAddress() + ";\n");
        }
    }

    private void appendInterfaceName(DhcpServerConfig config, StringBuilder sb) {
        if (!isNull(config.getInterfaceName())) {
            sb.append("    interface " + config.getInterfaceName() + ";\n");
        }
    }

    private void appendDNSServers(DhcpServerConfig config, StringBuilder sb) {
        if (config.isPassDns() && !isNull(config.getDnsServers()) && !config.getDnsServers().isEmpty()
                && !isNull(config.getRouterAddress())) {
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
    }

}
