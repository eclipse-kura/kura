/*******************************************************************************
 * Copyright (c) 2016, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LocalRule;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.linux.net.iptables.PortForwardRule;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.admin.event.FirewallConfigurationChangeEvent;
import org.eclipse.kura.net.configuration.NetworkConfigurationMessages;
import org.eclipse.kura.net.configuration.NetworkConfigurationPropertyNames;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4.FirewallOpenPortConfigIP4Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4.FirewallPortForwardConfigIP4Builder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallConfigurationServiceImpl implements FirewallConfigurationService, SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(FirewallConfigurationServiceImpl.class);

    private EventAdmin eventAdmin;
    private LinuxFirewall firewall;
    private CommandExecutorService executorService;

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating FirewallConfigurationService...");

        this.firewall = getLinuxFirewall();
        updated(properties);

        logger.info("Activating FirewallConfigurationService... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating FirewallConfigurationService...");
        logger.info("Deactivating FirewallConfigurationService... Done.");
    }

    public synchronized void updated(Map<String, Object> properties) {
        logger.debug("updated()");
        for (Entry<String, Object> entry : properties.entrySet()) {
            logger.debug("updated() :: Props... {}={}", entry.getKey(), entry.getValue());
        }

        FirewallConfiguration firewallConfiguration = new FirewallConfiguration(properties);
        try {
            setFirewallOpenPortConfiguration(firewallConfiguration.getOpenPortConfigs());
        } catch (KuraException e) {
            logger.error("Failed to set Firewall Open Ports Configuration", e);
        }
        try {
            setFirewallPortForwardingConfiguration(firewallConfiguration.getPortForwardConfigs());
        } catch (KuraException e) {
            logger.error("Failed to set Firewall Port Forwarding Configuration", e);
        }
        try {
            setFirewallNatConfiguration(firewallConfiguration.getNatConfigs());
        } catch (KuraException e) {
            logger.error("Failed to set Firewall NAT Configuration", e);
        }

        // raise the event because there was a change
        this.eventAdmin.postEvent(new FirewallConfigurationChangeEvent(properties));
    }

    @Override
    public FirewallConfiguration getFirewallConfiguration() throws KuraException {
        logger.debug("getting the firewall configuration");

        FirewallConfiguration firewallConfiguration = new FirewallConfiguration();

        Iterator<LocalRule> localRules = getLocalRules().iterator();
        while (localRules.hasNext()) {
            LocalRule localRule = localRules.next();
            FirewallOpenPortConfigIP4Builder builder = FirewallOpenPortConfigIP4.builder();
            try {
                if (localRule.getPortRange() != null) {
                    logger.debug("getFirewallConfiguration() :: Adding local rule for {}", localRule.getPortRange());
                    builder.withPortRange(localRule.getPortRange())
                            .withProtocol(NetProtocol.valueOf(localRule.getProtocol()))
                            .withPermittedNetwork((NetworkPair<IP4Address>) localRule.getPermittedNetwork())
                            .withPermittedInterfaceName(localRule.getPermittedInterfaceName())
                            .withUnpermittedInterfaceName(localRule.getUnpermittedInterfaceName())
                            .withPermittedMac(localRule.getPermittedMAC())
                            .withSourcePortRange(localRule.getSourcePortRange());
                    firewallConfiguration.addConfig(builder.build());
                } else {
                    logger.debug("getFirewallConfiguration() :: Adding local rule for {}", localRule.getPort());
                    builder.withPort(localRule.getPort()).withProtocol(NetProtocol.valueOf(localRule.getProtocol()))
                            .withPermittedNetwork((NetworkPair<IP4Address>) localRule.getPermittedNetwork())
                            .withPermittedInterfaceName(localRule.getPermittedInterfaceName())
                            .withUnpermittedInterfaceName(localRule.getUnpermittedInterfaceName())
                            .withPermittedMac(localRule.getPermittedMAC())
                            .withSourcePortRange(localRule.getSourcePortRange());
                    firewallConfiguration.addConfig(builder.build());
                }
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
            }
        }
        Iterator<PortForwardRule> portForwardRules = getPortForwardRules().iterator();
        while (portForwardRules.hasNext()) {
            PortForwardRule portForwardRule = portForwardRules.next();
            try {
                logger.debug("getFirewallConfiguration() :: Adding port forwarding - inbound iface is {}",
                        portForwardRule.getInboundIface());
                FirewallPortForwardConfigIP4Builder builder = FirewallPortForwardConfigIP4.builder();
                builder.withInboundIface(portForwardRule.getInboundIface())
                        .withOutboundIface(portForwardRule.getOutboundIface())
                        .withAddress((IP4Address) IPAddress.parseHostAddress(portForwardRule.getAddress()))
                        .withProtocol(NetProtocol.valueOf(portForwardRule.getProtocol()))
                        .withInPort(portForwardRule.getInPort()).withOutPort(portForwardRule.getOutPort())
                        .withMasquerade(portForwardRule.isMasquerade())
                        .withPermittedNetwork(new NetworkPair<>(
                                (IP4Address) IPAddress.parseHostAddress(portForwardRule.getPermittedNetwork()),
                                (short) portForwardRule.getPermittedNetworkMask()))
                        .withPermittedMac(portForwardRule.getPermittedMAC())
                        .withSourcePortRange(portForwardRule.getSourcePortRange());
                firewallConfiguration.addConfig(builder.build());
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
            }
        }
        Iterator<NATRule> autoNatRules = getAutoNatRules().iterator();
        while (autoNatRules.hasNext()) {
            NATRule autoNatRule = autoNatRules.next();
            logger.debug("getFirewallConfiguration() :: Adding auto NAT rules {}", autoNatRule.getSourceInterface());
            firewallConfiguration.addConfig(new FirewallAutoNatConfig(autoNatRule.getSourceInterface(),
                    autoNatRule.getDestinationInterface(), autoNatRule.isMasquerade()));
        }

        Iterator<NATRule> natRules = getNatRules().iterator();
        while (natRules.hasNext()) {
            NATRule natRule = natRules.next();
            logger.debug("getFirewallConfiguration() :: Adding NAT rules {}", natRule.getSourceInterface());
            firewallConfiguration.addConfig(new FirewallNatConfig(natRule.getSourceInterface(),
                    natRule.getDestinationInterface(), natRule.getProtocol(), natRule.getSource(),
                    natRule.getDestination(), natRule.isMasquerade(), natRule.getRuleType()));
        }

        return firewallConfiguration;
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        logger.debug("getConfiguration()");
        try {
            Map<String, Object> firewallConfigurationProperties = getFirewallConfiguration()
                    .getConfigurationProperties();
            firewallConfigurationProperties.put(KURA_SERVICE_PID, PID);
            firewallConfigurationProperties.put(SERVICE_PID, PID);
            return new ComponentConfigurationImpl(PID, getDefinition(), firewallConfigurationProperties);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void setFirewallOpenPortConfiguration(
            List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException {

        logger.debug("setFirewallOpenPortConfiguration() :: Deleting local rules");
        deleteAllLocalRules();

        ArrayList<LocalRule> localRules = new ArrayList<>();
        for (FirewallOpenPortConfigIP<? extends IPAddress> openPortEntry : firewallConfiguration) {
            try {
                LocalRule localRule;
                if (openPortEntry.getPortRange() != null) {
                    logger.debug("setFirewallOpenPortConfiguration() :: Adding local rule for: {}",
                            openPortEntry.getPortRange());
                    localRule = new LocalRule(openPortEntry.getPortRange(), openPortEntry.getProtocol().name(),
                            convertNetworkPair(openPortEntry.getPermittedNetworkString()),
                            openPortEntry.getPermittedInterfaceName(), openPortEntry.getUnpermittedInterfaceName(),
                            openPortEntry.getPermittedMac(), openPortEntry.getSourcePortRange());
                } else {
                    logger.debug("setFirewallOpenPortConfiguration() :: Adding local rule for: {}",
                            openPortEntry.getPort());
                    localRule = new LocalRule(openPortEntry.getPort(), openPortEntry.getProtocol().name(),
                            convertNetworkPair(openPortEntry.getPermittedNetworkString()),
                            openPortEntry.getPermittedInterfaceName(), openPortEntry.getUnpermittedInterfaceName(),
                            openPortEntry.getPermittedMac(), openPortEntry.getSourcePortRange());
                }
                localRules.add(localRule);
            } catch (Exception e) {
                logger.error("setFirewallOpenPortConfiguration() :: Failed to add local rule for: {}",
                        openPortEntry.getPort(), e);
            }
        }

        addLocalRules(localRules);
    }

    private NetworkPair<IP4Address> convertNetworkPair(String permittedNetwork) throws UnknownHostException {
        if (permittedNetwork == null || permittedNetwork.isEmpty()) {
            return new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0);
        } else {
            String[] split = permittedNetwork.split("/");
            return new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
        }
    }

    public void setFirewallPortForwardingConfiguration(
            List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException {

        logger.debug("setFirewallPortForwardingConfiguration() :: Deleting port forward rules");
        deleteAllPortForwardRules();

        ArrayList<PortForwardRule> portForwardRules = new ArrayList<>();
        for (FirewallPortForwardConfigIP<? extends IPAddress> portForwardEntry : firewallConfiguration) {
            logger.debug("setFirewallPortForwardingConfiguration() :: Adding port forward rule for: {}",
                    portForwardEntry.getInPort());

            try {
                PortForwardRule portForwardRule = new PortForwardRule()
                        .inboundIface(portForwardEntry.getInboundInterface())
                        .outboundIface(portForwardEntry.getOutboundInterface())
                        .address(portForwardEntry.getIPAddress().getHostAddress())
                        .protocol(portForwardEntry.getProtocol().name()).inPort(portForwardEntry.getInPort())
                        .outPort(portForwardEntry.getOutPort()).masquerade(portForwardEntry.isMasquerade())
                        .permittedNetwork(convertNetworkPairToString(portForwardEntry.getPermittedNetwork()))
                        .permittedNetworkMask(portForwardEntry.getPermittedNetwork().getPrefix())
                        .permittedMAC(portForwardEntry.getPermittedMac())
                        .sourcePortRange(portForwardEntry.getSourcePortRange());
                portForwardRules.add(portForwardRule);
            } catch (Exception e) {
                logger.error("setFirewallPortForwardingConfiguration() :: Failed to add port forward rule for: {}",
                        portForwardEntry.getInPort(), e);
            }
        }

        addPortForwardRules(portForwardRules);
    }

    private String convertNetworkPairToString(NetworkPair<? extends IPAddress> permittedNetwork)
            throws UnknownHostException {
        if (permittedNetwork == null || permittedNetwork.getIpAddress() == null) {
            return new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0).getIpAddress()
                    .getHostAddress();
        } else {
            return new NetworkPair<>(IPAddress.parseHostAddress(permittedNetwork.getIpAddress().getHostAddress()),
                    permittedNetwork.getPrefix()).getIpAddress().getHostAddress();
        }
    }

    @Override
    public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException {

        deleteAllNatRules();

        ArrayList<NATRule> natRules = new ArrayList<>();
        for (FirewallNatConfig natConfig : natConfigs) {
            NATRule natRule = new NATRule(natConfig.getSourceInterface(), natConfig.getDestinationInterface(),
                    natConfig.getProtocol(), natConfig.getSource(), natConfig.getDestination(),
                    natConfig.isMasquerade(), natConfig.getRuleType());
            natRules.add(natRule);
        }

        addNatRules(natRules);
    }

    protected void addLocalRules(ArrayList<LocalRule> localRules) throws KuraException {
        this.firewall.addLocalRules(localRules);
    }

    protected void addNatRules(ArrayList<NATRule> natRules) throws KuraException {
        this.firewall.addNatRules(natRules);
    }

    protected void addPortForwardRules(ArrayList<PortForwardRule> portForwardRules) throws KuraException {
        this.firewall.addPortForwardRules(portForwardRules);
    }

    protected void deleteAllLocalRules() throws KuraException {
        this.firewall.deleteAllLocalRules();
    }

    protected void deleteAllNatRules() throws KuraException {
        this.firewall.deleteAllNatRules();
    }

    protected void deleteAllPortForwardRules() throws KuraException {
        this.firewall.deleteAllPortForwardRules();
    }

    protected Set<NATRule> getAutoNatRules() throws KuraException {
        return this.firewall.getAutoNatRules();
    }

    protected Set<LocalRule> getLocalRules() throws KuraException {
        return this.firewall.getLocalRules();
    }

    protected Set<NATRule> getNatRules() throws KuraException {
        return this.firewall.getNatRules();
    }

    protected Set<PortForwardRule> getPortForwardRules() throws KuraException {
        return this.firewall.getPortForwardRules();
    }

    protected LinuxFirewall getLinuxFirewall() {
        if (this.firewall == null) {
            this.firewall = LinuxFirewall.getInstance(this.executorService);
        }

        return this.firewall;
    }

    private Tocd getDefinition() throws KuraException {

        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("FirewallConfigurationService");
        tocd.setId("org.eclipse.kura.net.admin.FirewallConfigurationService");
        tocd.setDescription("Firewall Configuration Service");

        Tad tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.OPEN_PORTS_PROP_NAME);
        tad.setName(FirewallConfiguration.OPEN_PORTS_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_OPEN_PORTS_VALUE);
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_OPEN_PORTS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.PORT_FORWARDING_PROP_NAME);
        tad.setName(FirewallConfiguration.PORT_FORWARDING_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_PORT_FORWARDING_VALUE);
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_PORT_FORWARDING));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.NAT_PROP_NAME);
        tad.setName(FirewallConfiguration.NAT_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_NAT_VALUE);
        tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_NAT));
        tocd.addAD(tad);

        return tocd;
    }

    @Override
    public void addFloodingProtectionRules(Set<String> floodingRules) {
        // The flooding protection rules are applied only to the mangle table.
        addFloodingProtectionRules(new LinkedHashSet<>(), new LinkedHashSet<>(), floodingRules);
    }

    @Override
    public void addFloodingProtectionRules(Set<String> filterFloodingRules, Set<String> natFloodingRules,
            Set<String> mangleFloodingRules) {
        try {
            this.firewall.setAdditionalRules(filterFloodingRules, natFloodingRules, mangleFloodingRules);
        } catch (KuraException e) {
            logger.error("Failed to set Firewall Flooding Protection Configuration", e);
        }
    }
}
