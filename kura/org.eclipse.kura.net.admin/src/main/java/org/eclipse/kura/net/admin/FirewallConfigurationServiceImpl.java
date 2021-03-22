/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
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
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.security.FloodingProtectionConfigurationChangeEvent;
import org.eclipse.kura.security.FloodingProtectionConfigurationService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallConfigurationServiceImpl
        implements FirewallConfigurationService, SelfConfiguringComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(FirewallConfigurationServiceImpl.class);

    private EventAdmin eventAdmin;
    private ServiceRegistration<?> serviceRegistration;
    private LinuxFirewall firewall;
    private CommandExecutorService executorService;
    private FloodingProtectionConfigurationService floodingConfiguration;

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        if (this.eventAdmin == eventAdmin) {
            this.eventAdmin = null;
        }
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        if (this.executorService == executorService) {
            this.executorService = null;
        }
    }

    public void setFloodingProtectionConfigurationService(
            FloodingProtectionConfigurationService floodingConfiguration) {
        this.floodingConfiguration = floodingConfiguration;
    }

    public void unsetFloodingProtectionConfigurationService(
            FloodingProtectionConfigurationService floodingConfiguration) {
        if (this.floodingConfiguration == floodingConfiguration) {
            this.floodingConfiguration = null;
        }
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("activate()");

        // we are intentionally ignoring the properties from ConfigAdmin at startup
        if (properties == null) {
            logger.debug("activate() :: Got null properties...");
        } else {
            for (Entry<String, Object> entry : properties.entrySet()) {
                logger.debug("activate() :: Props... {}={}", entry.getKey(), entry.getValue());
            }
        }

        this.firewall = getLinuxFirewall();

        Dictionary<String, Object> props = new Hashtable<>();
        String[] eventTopics = { FloodingProtectionConfigurationChangeEvent.FP_EVENT_CONFIG_CHANGE_TOPIC };
        props.put(EventConstants.EVENT_TOPIC, eventTopics);
        this.serviceRegistration = componentContext.getBundleContext().registerService(EventHandler.class.getName(),
                this, props);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("deactivate()");
        this.serviceRegistration.unregister();
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

        setFloodingProtectionConfiguration();

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
            if (localRule.getPortRange() != null) {
                logger.debug("getFirewallConfiguration() :: Adding local rule for {}", localRule.getPortRange());
                firewallConfiguration.addConfig(new FirewallOpenPortConfigIP4(localRule.getPortRange(),
                        NetProtocol.valueOf(localRule.getProtocol()), localRule.getPermittedNetwork(),
                        localRule.getPermittedInterfaceName(), localRule.getUnpermittedInterfaceName(),
                        localRule.getPermittedMAC(), localRule.getSourcePortRange()));
            } else {
                logger.debug("getFirewallConfiguration() :: Adding local rule for {}", localRule.getPort());
                firewallConfiguration.addConfig(new FirewallOpenPortConfigIP4(localRule.getPort(),
                        NetProtocol.valueOf(localRule.getProtocol()), localRule.getPermittedNetwork(),
                        localRule.getPermittedInterfaceName(), localRule.getUnpermittedInterfaceName(),
                        localRule.getPermittedMAC(), localRule.getSourcePortRange()));
            }
        }
        Iterator<PortForwardRule> portForwardRules = getPortForwardRules().iterator();
        while (portForwardRules.hasNext()) {
            PortForwardRule portForwardRule = portForwardRules.next();
            try {
                logger.debug("getFirewallConfiguration() :: Adding port forwarding - inbound iface is {}",
                        portForwardRule.getInboundIface());
                firewallConfiguration.addConfig(new FirewallPortForwardConfigIP4(portForwardRule.getInboundIface(),
                        portForwardRule.getOutboundIface(),
                        (IP4Address) IPAddress.parseHostAddress(portForwardRule.getAddress()),
                        NetProtocol.valueOf(portForwardRule.getProtocol()), portForwardRule.getInPort(),
                        portForwardRule.getOutPort(), portForwardRule.isMasquerade(),
                        new NetworkPair<>(
                                (IP4Address) IPAddress.parseHostAddress(portForwardRule.getPermittedNetwork()),
                                (short) portForwardRule.getPermittedNetworkMask()),
                        portForwardRule.getPermittedMAC(), portForwardRule.getSourcePortRange()));
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
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
            FirewallConfiguration firewallConfiguration = getFirewallConfiguration();
            return new ComponentConfigurationImpl(PID, getDefinition(),
                    firewallConfiguration.getConfigurationProperties());
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
            if (openPortEntry.getPermittedNetwork() == null
                    || openPortEntry.getPermittedNetwork().getIpAddress() == null) {
                try {
                    openPortEntry.setPermittedNetwork(getNetworkPair00());
                } catch (UnknownHostException e) {
                    logger.info(e.getMessage(), e);
                }
            }

            try {
                LocalRule localRule;
                if (openPortEntry.getPortRange() != null) {
                    logger.debug("setFirewallOpenPortConfiguration() :: Adding local rule for: {}",
                            openPortEntry.getPortRange());
                    localRule = new LocalRule(openPortEntry.getPortRange(), openPortEntry.getProtocol().name(),
                            new NetworkPair(
                                    IPAddress.parseHostAddress(
                                            openPortEntry.getPermittedNetwork().getIpAddress().getHostAddress()),
                                    openPortEntry.getPermittedNetwork().getPrefix()),
                            openPortEntry.getPermittedInterfaceName(), openPortEntry.getUnpermittedInterfaceName(),
                            openPortEntry.getPermittedMac(), openPortEntry.getSourcePortRange());
                } else {
                    logger.debug("setFirewallOpenPortConfiguration() :: Adding local rule for: {}",
                            openPortEntry.getPort());
                    localRule = new LocalRule(openPortEntry.getPort(), openPortEntry.getProtocol().name(),
                            new NetworkPair(
                                    IPAddress.parseHostAddress(
                                            openPortEntry.getPermittedNetwork().getIpAddress().getHostAddress()),
                                    openPortEntry.getPermittedNetwork().getPrefix()),
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

    @Override
    public void setFirewallPortForwardingConfiguration(
            List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException {

        logger.debug("setFirewallPortForwardingConfiguration() :: Deleting port forward rules");
        deleteAllPortForwardRules();

        ArrayList<PortForwardRule> portForwardRules = new ArrayList<>();
        for (FirewallPortForwardConfigIP<? extends IPAddress> portForwardEntry : firewallConfiguration) {
            logger.debug("setFirewallPortForwardingConfiguration() :: Adding port forward rule for: {}",
                    portForwardEntry.getInPort());

            if (portForwardEntry.getPermittedNetwork() == null
                    || portForwardEntry.getPermittedNetwork().getIpAddress() == null) {
                try {
                    portForwardEntry.setPermittedNetwork(getNetworkPair00());
                } catch (UnknownHostException e) {
                    logger.info(e.getMessage(), e);
                }
            }

            PortForwardRule portForwardRule = new PortForwardRule().inboundIface(portForwardEntry.getInboundInterface())
                    .outboundIface(portForwardEntry.getOutboundInterface())
                    .address(portForwardEntry.getAddress().getHostAddress())
                    .protocol(portForwardEntry.getProtocol().name()).inPort(portForwardEntry.getInPort())
                    .outPort(portForwardEntry.getOutPort()).masquerade(portForwardEntry.isMasquerade())
                    .permittedNetwork(portForwardEntry.getPermittedNetwork().getIpAddress().getHostAddress())
                    .permittedNetworkMask(portForwardEntry.getPermittedNetwork().getPrefix())
                    .permittedMAC(portForwardEntry.getPermittedMac())
                    .sourcePortRange(portForwardEntry.getSourcePortRange());
            portForwardRules.add(portForwardRule);
        }

        addPortForwardRules(portForwardRules);
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

    @Override
    public void handleEvent(Event event) {
        logger.debug("Received event: {}", event.getTopic());
        if (event.getTopic().equals(FloodingProtectionConfigurationChangeEvent.FP_EVENT_CONFIG_CHANGE_TOPIC)) {
            setFloodingProtectionConfiguration();
        }
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
        tad.setCardinality(10000);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_OPEN_PORTS_VALUE);
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.PLATFORM_INTERFACES));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.PORT_FORWARDING_PROP_NAME);
        tad.setName(FirewallConfiguration.PORT_FORWARDING_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_PORT_FORWARDING_VALUE);
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.PLATFORM_INTERFACES));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.NAT_PROP_NAME);
        tad.setName(FirewallConfiguration.NAT_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_NAT_VALUE);
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.PLATFORM_INTERFACES));
        tocd.addAD(tad);

        return tocd;
    }

    private NetworkPair getNetworkPair00() throws UnknownHostException {
        return new NetworkPair(IPAddress.parseHostAddress("0.0.0.0"), (short) 0);
    }

    private void setFloodingProtectionConfiguration() {
        if (this.floodingConfiguration != null) {
            try {
                this.firewall.setAdditionalRules(this.floodingConfiguration.getFloodingProtectionFilterRules(),
                        this.floodingConfiguration.getFloodingProtectionNatRules(),
                        this.floodingConfiguration.getFloodingProtectionMangleRules());
            } catch (KuraException e) {
                logger.error("Failed to set Firewall Flooding Protection Configuration", e);
            }
        }
    }
}
