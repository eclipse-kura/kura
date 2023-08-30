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
package org.eclipse.kura.linux.net.iptables;

import java.io.File;
import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux firewall implementation
 *
 * @author eurotech
 */
public class LinuxFirewallIPv6 extends AbstractLinuxFirewall {

    private static final Logger logger = LoggerFactory.getLogger(LinuxFirewallIPv6.class);

    private static LinuxFirewallIPv6 linuxFirewall;
    private static final String IPV6_FORWARD_FILE_NAME = "/proc/sys/net/ipv6/conf/all/forwarding";

    // The LinuxFirewall is a singleton, since at the creation of the object
    // it reads the iptables configuration from the filesystem once, instead of
    // reading it at every call of the constructor.
    // However, it is dangerous in a multi-thread system because the CommandExecutorService
    // is passed as an argument.
    public static LinuxFirewallIPv6 getInstance(CommandExecutorService executorService) {
        if (linuxFirewall == null) {
            linuxFirewall = new LinuxFirewallIPv6(executorService);
        } else {
            linuxFirewall.setExecutorService(executorService);
        }
        return linuxFirewall;
    }

    private LinuxFirewallIPv6(CommandExecutorService executorService) {
        this.executorService = executorService;
        this.iptables = new IptablesConfigIPv6(this.executorService);
        try {
            File cfgFile = new File(this.iptables.getFirewallConfigFileName());
            if (!cfgFile.exists()) {
                IptablesConfig minimalConfig = new IptablesConfigIPv6(executorService);
                minimalConfig.applyRules();
            }
            initialize();
        } catch (KuraException e) {
            logger.error("failed to initialize LinuxFirewall", e);
        }
    }

    @Override
    protected IPAddress getDefaultAddress() throws UnknownHostException {
        return IP6Address.getDefaultAddress();
    }

    @Override
    protected String getIpForwardFileName() {
        return IPV6_FORWARD_FILE_NAME;
    }
}
