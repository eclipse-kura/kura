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
package org.eclipse.kura.web.server.net2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.web.server.net2.configuration.NetworkConfigurationServiceAdapter;
import org.eclipse.kura.web.server.net2.status.NetworkStatusServiceAdapter;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtNetworkServiceImpl {

    private GwtNetworkServiceImpl() {

    }

    private static final Logger logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);

    public static List<GwtNetInterfaceConfig> findNetInterfaceConfigurations(boolean recompute)
            throws GwtKuraException {
        try {
            NetworkConfigurationServiceAdapter configuration = new NetworkConfigurationServiceAdapter();
            NetworkStatusServiceAdapter status = new NetworkStatusServiceAdapter();

            List<GwtNetInterfaceConfig> result = new LinkedList<>();
            for (String ifname : configuration.getNetInterfaces()) {
                GwtNetInterfaceConfig gwtConfig = configuration.getGwtNetInterfaceConfig(ifname);
                GwtNetInterfaceConfig gwtStatus = status.getGwtNetInterfaceConfig(ifname, gwtConfig.getHwType(),
                        gwtConfig.getConfigMode());

                gwtConfig.getProperties().putAll(gwtStatus.getProperties());
                logger.debug("GWT Network Configuration for interface {}:\n{}\n", ifname, gwtConfig.getProperties());

                result.add(gwtConfig);
            }

            return result;
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static List<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards() throws GwtKuraException {
        FirewallConfigurationService fcs = ServiceLocator.getInstance().getService(FirewallConfigurationService.class);
        List<GwtFirewallPortForwardEntry> gwtPortForwardEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = fcs.getFirewallConfiguration().getConfigs();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallPortForwardConfigIP4) {
                        logger.debug("findDeviceFirewallPortForwards() :: adding new Port Forward Entry");
                        GwtFirewallPortForwardEntry entry = new GwtFirewallPortForwardEntry();
                        entry.setInboundInterface(((FirewallPortForwardConfigIP4) netConfig).getInboundInterface());
                        entry.setOutboundInterface(((FirewallPortForwardConfigIP4) netConfig).getOutboundInterface());
                        entry.setAddress(((FirewallPortForwardConfigIP4) netConfig).getAddress().getHostAddress());
                        entry.setProtocol(((FirewallPortForwardConfigIP4) netConfig).getProtocol().toString());
                        entry.setInPort(((FirewallPortForwardConfigIP4) netConfig).getInPort());
                        entry.setOutPort(((FirewallPortForwardConfigIP4) netConfig).getOutPort());
                        String masquerade = ((FirewallPortForwardConfigIP4) netConfig).isMasquerade() ? "yes" : "no";
                        entry.setMasquerade(masquerade);
                        entry.setPermittedNetwork(
                                ((FirewallPortForwardConfigIP4) netConfig).getPermittedNetwork().toString());
                        entry.setPermittedMAC(((FirewallPortForwardConfigIP4) netConfig).getPermittedMac());
                        entry.setSourcePortRange(((FirewallPortForwardConfigIP4) netConfig).getSourcePortRange());

                        gwtPortForwardEntries.add(entry);
                    }
                }
            }

            return new ArrayList<>(gwtPortForwardEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static ArrayList<GwtFirewallNatEntry> findDeviceFirewallNATs() throws GwtKuraException {
        FirewallConfigurationService fcs = ServiceLocator.getInstance().getService(FirewallConfigurationService.class);
        List<GwtFirewallNatEntry> gwtNatEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = fcs.getFirewallConfiguration().getConfigs();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallNatConfig) {
                        logger.debug("findDeviceFirewallNATs() :: adding new NAT Entry");
                        GwtFirewallNatEntry entry = new GwtFirewallNatEntry();
                        entry.setInInterface(((FirewallNatConfig) netConfig).getSourceInterface());
                        entry.setOutInterface(((FirewallNatConfig) netConfig).getDestinationInterface());
                        entry.setProtocol(((FirewallNatConfig) netConfig).getProtocol());
                        entry.setSourceNetwork(((FirewallNatConfig) netConfig).getSource());
                        entry.setDestinationNetwork(((FirewallNatConfig) netConfig).getDestination());
                        String masquerade = ((FirewallNatConfig) netConfig).isMasquerade() ? "yes" : "no";
                        entry.setMasquerade(masquerade);
                        gwtNatEntries.add(entry);
                    }
                }
            }

            return new ArrayList<>(gwtNatEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static List<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts() throws GwtKuraException {
        FirewallConfigurationService fcs = ServiceLocator.getInstance().getService(FirewallConfigurationService.class);
        List<GwtFirewallOpenPortEntry> gwtOpenPortEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = fcs.getFirewallConfiguration().getConfigs();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallOpenPortConfigIP4) {
                        logger.debug("findDeviceFirewallOpenPorts() :: adding new Open Port Entry: {}",
                                ((FirewallOpenPortConfigIP4) netConfig).getPort());
                        GwtFirewallOpenPortEntry entry = new GwtFirewallOpenPortEntry();
                        if (((FirewallOpenPortConfigIP4) netConfig).getPortRange() != null) {
                            entry.setPortRange(((FirewallOpenPortConfigIP4) netConfig).getPortRange());
                        } else {
                            entry.setPortRange(String.valueOf(((FirewallOpenPortConfigIP4) netConfig).getPort()));
                        }
                        entry.setProtocol(((FirewallOpenPortConfigIP4) netConfig).getProtocol().toString());
                        entry.setPermittedNetwork(((FirewallOpenPortConfigIP4) netConfig).getPermittedNetwork()
                                .getIpAddress().getHostAddress() + "/"
                                + ((FirewallOpenPortConfigIP4) netConfig).getPermittedNetwork().getPrefix());
                        entry.setPermittedInterfaceName(
                                ((FirewallOpenPortConfigIP4) netConfig).getPermittedInterfaceName());
                        entry.setUnpermittedInterfaceName(
                                ((FirewallOpenPortConfigIP4) netConfig).getUnpermittedInterfaceName());
                        entry.setPermittedMAC(((FirewallOpenPortConfigIP4) netConfig).getPermittedMac());
                        entry.setSourcePortRange(((FirewallOpenPortConfigIP4) netConfig).getSourcePortRange());

                        gwtOpenPortEntries.add(entry);
                    }
                }
            }

            return new ArrayList<>(gwtOpenPortEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }
}
