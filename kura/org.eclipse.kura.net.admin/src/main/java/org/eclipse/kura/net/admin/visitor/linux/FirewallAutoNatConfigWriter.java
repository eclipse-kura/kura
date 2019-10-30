/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
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
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallAutoNatConfigWriter implements NetworkConfigurationVisitor {

    private static final String NET_INTERFACE = "net.interface.";

    private static final Logger logger = LoggerFactory.getLogger(FirewallAutoNatConfigWriter.class);

    private static FirewallAutoNatConfigWriter instance;
    private CommandExecutorService executorService;

    public static FirewallAutoNatConfigWriter getInstance() {

        if (instance == null) {
            instance = new FirewallAutoNatConfigWriter();
        }
        return instance;
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        if (this.executorService == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "The CommandExecutorService cannot be null");
        }

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                    || netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                writeConfig(netInterfaceConfig, getKuranetProperties());
            }
        }

        applyNatConfig(config);

        // After every visit, unset the executorService. This must be set before every call.
        this.executorService = null;
    }

    private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
            Properties kuraProps) throws KuraException {

        String interfaceName = netInterfaceConfig.getName();
        logger.debug("Writing NAT config for {}", interfaceName);
        boolean natEnabled = false;
        boolean useMasquerade = false;
        String srcIface = null;
        String dstIface = null;
        List<NetConfig> netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof FirewallAutoNatConfig) {
                natEnabled = true;
                srcIface = ((FirewallAutoNatConfig) netConfig).getSourceInterface();
                dstIface = ((FirewallAutoNatConfig) netConfig).getDestinationInterface();
                useMasquerade = ((FirewallAutoNatConfig) netConfig).isMasquerade();
            }
        }
        updateKuranetPropertied(kuraProps, interfaceName, natEnabled, useMasquerade, srcIface, dstIface);
    }

    private void updateKuranetPropertied(Properties kuraProps, String interfaceName, boolean natEnabled,
            boolean useMasquerade, String srcIface, String dstIface) throws KuraException {

        // set it all
        if (kuraProps == null) {
            logger.debug("kuraExtendedProps was null");
            kuraProps = new Properties();
        }

        StringBuilder sb = new StringBuilder().append(NET_INTERFACE).append(interfaceName)
                .append(".config.nat.enabled");
        kuraProps.put(sb.toString(), Boolean.toString(natEnabled));
        if (natEnabled && srcIface != null && dstIface != null) {
            sb = new StringBuilder().append(NET_INTERFACE).append(interfaceName).append(".config.nat.dst.interface");
            kuraProps.put(sb.toString(), dstIface);

            sb = new StringBuilder().append(NET_INTERFACE).append(interfaceName).append(".config.nat.masquerade");
            kuraProps.put(sb.toString(), Boolean.toString(useMasquerade));
        }

        // write it
        storeKuranetProperties(kuraProps);
    }

    protected void applyNatConfig(NetworkConfiguration networkConfig) throws KuraException {
        LinuxFirewall firewall = new LinuxFirewall(this.executorService);
        firewall.replaceAllNatRules(getNatConfigs(networkConfig));
        firewall.enable();
    }

    protected Properties getKuranetProperties() {
        return KuranetConfig.getProperties();
    }

    protected void storeKuranetProperties(Properties kuraProps) throws KuraException {
        if (kuraProps == null || kuraProps.isEmpty()) {
            return;
        }

        try {
            KuranetConfig.storeProperties(kuraProps);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
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
                            logger.debug("getNatConfigs() :: FirewallAutoNatConfig: {}",
                                    ((FirewallAutoNatConfig) netConfig));
                        }
                        isNat = true;
                    } else if (netConfig instanceof FirewallNatConfig && logger.isDebugEnabled()) {
                        logger.debug("getNatConfigs() ::  FirewallNatConfig: {}", ((FirewallNatConfig) netConfig));
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
                    natConfigs.add(new NATRule(sourceInterface, destinationInterface, true));
                }
            }
        }

        return natConfigs;
    }
}
