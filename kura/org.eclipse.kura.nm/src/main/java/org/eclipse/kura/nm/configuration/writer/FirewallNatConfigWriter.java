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
 ******************************************************************************/
package org.eclipse.kura.nm.configuration.writer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LinuxFirewallIPv6;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallNatConfigWriter {

    private static final Logger logger = LoggerFactory.getLogger(FirewallNatConfigWriter.class);

    private final CommandExecutorService executorService;
    private final LinuxFirewall firewall;
    private final LinuxFirewallIPv6 firewallIpv6;
    private final List<String> wanInterfaceNames;
    private final List<String> natInterfaceNames;

    public FirewallNatConfigWriter(CommandExecutorService executorService, List<String> wanInterfaceNames,
            List<String> natInterfaceNames) {
        this.executorService = executorService;
        this.wanInterfaceNames = wanInterfaceNames;
        this.natInterfaceNames = natInterfaceNames;
        this.firewall = LinuxFirewall.getInstance(this.executorService);
        this.firewallIpv6 = LinuxFirewallIPv6.getInstance(this.executorService);
    }

    public void writeConfiguration(boolean isIp4) throws KuraException {
        try {
            Set<NATRule> natRules = new HashSet<>();

            for (String source : this.natInterfaceNames) {
                for (String destination : this.wanInterfaceNames) {
                    if (!source.equalsIgnoreCase(destination)) {
                        natRules.add(createNATRule(source, destination, isIp4));
                    }
                }
            }
            if (isIp4) {
                this.firewall.replaceAllNatRules(natRules);
            } else {
                this.firewallIpv6.replaceAllNatRules(natRules);
            }

        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "Failed to replace all NAT rules with new ones for interfaces: " + this.natInterfaceNames, e);
        }
    }

    private NATRule createNATRule(String source, String destination, boolean isIp4) {
        NATRule natRule = new NATRule();
        natRule.setSourceInterface(source);
        natRule.setDestinationInterface(destination);
        natRule.setMasquerade(true);

        if (isIp4) {
            logger.info("Applying Ip4 NAT rule {} -> {}: {}", source, destination, natRule);
        } else {
            logger.info("Applying Ip6 NAT rule {} -> {}: {}", source, destination, natRule);
        }

        return natRule;
    }

}
