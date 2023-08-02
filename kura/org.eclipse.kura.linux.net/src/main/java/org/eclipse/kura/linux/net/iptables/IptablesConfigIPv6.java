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

import java.util.Set;

import org.eclipse.kura.executor.CommandExecutorService;

public class IptablesConfigIPv6 extends IptablesConfig {

    private static final String FIREWALL_IPV6_CONFIG_FILE_NAME = "/etc/sysconfig/ip6tables";
    private static final String FIREWALL_IPV6_TMP_CONFIG_FILE_NAME = "/tmp/ip6tables";
    private static final String IPTABLES_IPV6_COMMAND = "ip6tables";
    private static final String[] ALLOW_ICMP_IPV6 = {
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 1 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 2 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 3/0 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 3/1 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 4/0 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 4/1 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 4/2 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 128 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 129 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 144 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 145 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 146 -j ACCEPT",
            "-A input-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 147 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 130 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 131 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 132 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 133 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 134 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 135 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 136 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 141 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 142 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 148 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 149 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 151 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 152 -j ACCEPT",
            "-A input-kura -s fe80::/10 -p ipv6-icmp -m ipv6-icmp --icmpv6-type 153 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 1 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 2 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 3/0 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 3/1 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 4/0 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 4/1 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 4/2 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 144 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 145 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 146 -j ACCEPT",
            "-A forward-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 147 -j ACCEPT" };
    private static final String[] DO_NOT_ALLOW_ICMP_IPV6 = {}; // The INPUT and FORWARD chains are dropped by default

    public IptablesConfigIPv6() {
        super();
    }

    public IptablesConfigIPv6(CommandExecutorService executorService) {
        super(executorService);
    }

    public IptablesConfigIPv6(Set<LocalRule> localRules, Set<PortForwardRule> portForwardRules,
            Set<NATRule> autoNatRules, Set<NATRule> natRules, boolean allowIcmp,
            CommandExecutorService executorService) {
        super(localRules, portForwardRules, autoNatRules, natRules, allowIcmp, executorService);
    }

    @Override
    public String getFirewallConfigFileName() {
        return FIREWALL_IPV6_CONFIG_FILE_NAME;
    }

    @Override
    public String getFirewallConfigTmpFileName() {
        return FIREWALL_IPV6_TMP_CONFIG_FILE_NAME;
    }

    @Override
    protected String getIptablesCommand() {
        return IPTABLES_IPV6_COMMAND;
    }

    @Override
    protected String[] getAllowIcmp() {
        return ALLOW_ICMP_IPV6;
    }

    @Override
    protected String[] getNotAllowIcmp() {
        return DO_NOT_ALLOW_ICMP_IPV6;
    }
}
