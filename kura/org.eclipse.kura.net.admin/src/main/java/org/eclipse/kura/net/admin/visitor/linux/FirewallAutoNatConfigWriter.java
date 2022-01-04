/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.visitor.linux;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallAutoNatConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(FirewallAutoNatConfigWriter.class);

    private CommandExecutorService executorService;

    public FirewallAutoNatConfigWriter() {
        // Do nothing...
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        if (this.executorService == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "The CommandExecutorService cannot be null");
        }

        applyNatConfig(config);
    }

    protected void applyNatConfig(NetworkConfiguration networkConfig) throws KuraException {
        LinuxFirewall firewall = LinuxFirewall.getInstance(this.executorService);
        firewall.replaceAllNatRules(getNatConfigs(networkConfig));
    }

    private LinkedHashSet<NATRule> getNatConfigs(NetworkConfiguration networkConfig) {
        LinkedHashSet<NATRule> natConfigs = new LinkedHashSet<>();

        if (networkConfig != null) {
            ArrayList<String> wanList = new ArrayList<>();
            ArrayList<String> natList = new ArrayList<>();

            // get relevant interfaces
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : networkConfig
                    .getNetInterfaceConfigs()) {
                String interfaceName = netInterfaceConfig.getName();
                NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
                boolean isNat = false;

                List<NetConfig> netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
                for (NetConfig netConfig : netConfigs) {
                    if (netConfig instanceof NetConfigIP4) {
                        status = ((NetConfigIP4) netConfig).getStatus();
                    } else if (netConfig instanceof FirewallAutoNatConfig) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("getNatConfigs() :: FirewallAutoNatConfig: {}", netConfig);
                        }
                        isNat = true;
                    } else if (netConfig instanceof FirewallNatConfig && logger.isDebugEnabled()) {
                        logger.debug("getNatConfigs() ::  FirewallNatConfig: {}", netConfig);
                    }
                }

                if (NetInterfaceStatus.netIPv4StatusEnabledWAN.equals(status)) {
                    wanList.add(interfaceName);
                } else if (NetInterfaceStatus.netIPv4StatusEnabledLAN.equals(status) && isNat) {
                    natList.add(interfaceName);
                }
            }

            // create a nat rule for each interface to all potential wan interfaces
            for (String sourceInterface : natList) {
                for (String destinationInterface : wanList) {
                    logger.debug("Got NAT rule for source: {}, destination: {}", sourceInterface, destinationInterface);
                    natConfigs.add(new NATRule(sourceInterface, destinationInterface, true, RuleType.GENERIC));
                }
            }
        }

        return natConfigs;
    }
}
