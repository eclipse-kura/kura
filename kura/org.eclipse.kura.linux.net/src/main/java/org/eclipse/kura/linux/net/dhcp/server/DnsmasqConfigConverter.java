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

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpServerConfigConverter;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;

public class DnsmasqConfigConverter implements DhcpServerConfigConverter {

    private static final String DHCP_OPTION_KEY = "dhcp-option=";

    @Override
    public String convert(DhcpServerConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("interface=").append(config.getInterfaceName()).append("\n");

        StringBuilder dhcpRangeProp = new StringBuilder("dhcp-range=").append(config.getInterfaceName()).append(",")
                .append(config.getRangeStart()).append(",").append(config.getRangeEnd())
                .append(",").append(config.getDefaultLeaseTime()).append("s");
        sb.append(dhcpRangeProp.toString()).append("\n");

        sb.append(DHCP_OPTION_KEY + config.getInterfaceName()).append(",1,")
                .append(NetworkUtil.getNetmaskStringForm(config.getPrefix())).append("\n");
        // router property
        sb.append(DHCP_OPTION_KEY).append(config.getInterfaceName()).append(",3,")
                .append(config.getRouterAddress().toString()).append("\n");

        if (config.isPassDns()) {
            // announce DNS servers on this device
            sb.append(DHCP_OPTION_KEY).append(config.getInterfaceName()).append(",6,0.0.0.0").append("\n");
            ;
        } else {
            // leaving the option without value disables it
            sb.append(DHCP_OPTION_KEY).append(config.getInterfaceName()).append(",6").append("\n");
            sb.append("dhcp-ignore-names=").append(config.getInterfaceName()).append("\n");
        }

        // all subnets are local
        sb.append(DHCP_OPTION_KEY).append(config.getInterfaceName()).append(",27,1").append("\n");

        return sb.toString();
    }

}
