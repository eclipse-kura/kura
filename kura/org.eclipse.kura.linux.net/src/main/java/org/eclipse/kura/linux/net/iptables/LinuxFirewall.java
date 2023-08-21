/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux firewall implementation
 *
 * @author eurotech
 */
public class LinuxFirewall {

    private static final Logger logger = LoggerFactory.getLogger(LinuxFirewall.class);

    private static LinuxFirewall linuxFirewall;
    private static Object lock = new Object();

    private static final String IP_FORWARD_FILE_NAME = "/proc/sys/net/ipv4/ip_forward";

    private Set<LocalRule> localRules = new LinkedHashSet<>();
    private Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
    private Set<NATRule> autoNatRules = new LinkedHashSet<>();
    private Set<NATRule> natRules = new LinkedHashSet<>();
    private boolean allowIcmp = false;
    private final Set<String> additionalFilterRules = new HashSet<>();
    private final Set<String> additionalNatRules = new HashSet<>();
    private final Set<String> additionalMangleRules = new HashSet<>();
    private final IptablesConfig iptables;
    private CommandExecutorService executorService;

    // The LinuxFirewall is a singleton, since at the creation of the object
    // it reads the iptables configuration from the filesystem once, instead of
    // reading it at every call of the constructor.
    // However, it is dangerous in a multi-thread system because the CommandExecutorService
    // is passed as an argument.
    public static LinuxFirewall getInstance(CommandExecutorService executorService) {
        if (linuxFirewall == null) {
            linuxFirewall = new LinuxFirewall(executorService);
        } else {
            linuxFirewall.setExecutorService(executorService);
        }
        return linuxFirewall;
    }

    private LinuxFirewall(CommandExecutorService executorService) {
        this.executorService = executorService;
        this.iptables = new IptablesConfig(this.executorService);
        try {
            File cfgFile = new File(IptablesConfigConstants.FIREWALL_CONFIG_FILE_NAME);
            if (!cfgFile.exists()) {
                IptablesConfig minimalConfig = new IptablesConfig(new LinkedHashSet<>(), new LinkedHashSet<>(),
                        new LinkedHashSet<>(), new LinkedHashSet<>(), false, executorService);
                minimalConfig.applyRules();
            }
            initialize();
        } catch (KuraException e) {
            logger.error("failed to initialize LinuxFirewall", e);
        }
    }

    public void initialize() throws KuraException {
        logger.debug("initialize() :: initializing firewall ...");
        this.iptables.restore();
        this.localRules = this.iptables.getLocalRules();
        this.portForwardRules = this.iptables.getPortForwardRules();
        this.autoNatRules = this.iptables.getAutoNatRules();
        this.natRules = this.iptables.getNatRules();
        this.allowIcmp = true;
        logger.debug("initialize() :: Parsing current firewall configuration");
    }

    private void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public void addLocalRule(int port, String protocol, String permittedNetwork, String permittedNetworkPrefix,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC, String sourcePortRange)
            throws KuraException {
        try {
            LocalRule newLocalRule;
            if (permittedNetwork != null && permittedNetworkPrefix != null) {
                logger.debug("permittedNetwork: {}", permittedNetwork);
                logger.debug("permittedNetworkPrefix: {}", permittedNetworkPrefix);

                newLocalRule = new LocalRule(port, protocol,
                        new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(permittedNetwork),
                                Short.parseShort(permittedNetworkPrefix)),
                        permittedInterfaceName, unpermittedInterfaceName, permittedMAC, sourcePortRange);
            } else {
                newLocalRule = new LocalRule(port, protocol,
                        new NetworkPair<>(IP4Address.getDefaultAddress(), (short) 0), permittedInterfaceName,
                        unpermittedInterfaceName, permittedMAC, sourcePortRange);
            }

            ArrayList<LocalRule> locRules = new ArrayList<>();
            locRules.add(newLocalRule);
            addLocalRules(locRules);
        } catch (UnknownHostException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e, "Failed to parse host address");
        }
    }

    public void addLocalRules(List<LocalRule> newLocalRules) throws KuraException {
        boolean doUpdate = false;
        for (LocalRule newLocalRule : newLocalRules) {
            // make sure it is not already present
            boolean addRule = true;
            for (LocalRule localRule : this.localRules) {
                if (newLocalRule.equals(localRule)) {
                    addRule = false;
                    break;
                }
            }
            if (addRule) {
                logger.info("Adding local rule to firewall configuration: {}", newLocalRule);
                this.localRules.add(newLocalRule);
                doUpdate = true;
            } else {
                logger.warn("Not adding local rule that is already present: {}", newLocalRule);
            }
        }
        if (doUpdate) {
            update();
        }
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public void addPortForwardRule(String inboundIface, String outboundIface, String address, String protocol,
            int inPort, int outPort, boolean masquerade, String permittedNetwork, String permittedNetworkPrefix,
            String permittedMAC, String sourcePortRange) throws KuraException {

        short mask = permittedNetworkPrefix == null ? (short) -1 : Short.parseShort(permittedNetworkPrefix);
        PortForwardRule newPortForwardRule = new PortForwardRule().inboundIface(inboundIface)
                .outboundIface(outboundIface).address(address).protocol(protocol).inPort(inPort).outPort(outPort)
                .masquerade(masquerade).permittedNetwork(permittedNetwork).permittedNetworkMask(mask)
                .permittedMAC(permittedMAC).sourcePortRange(sourcePortRange);

        ArrayList<PortForwardRule> portFwdRules = new ArrayList<>();
        portFwdRules.add(newPortForwardRule);
        addPortForwardRules(portFwdRules);
    }

    public void addPortForwardRules(List<PortForwardRule> newPortForwardRules) throws KuraException {
        boolean doUpdate = false;
        for (PortForwardRule newPortForwardRule : newPortForwardRules) {
            // make sure it is not already present
            boolean addRule = true;
            for (PortForwardRule portForwardRule : this.portForwardRules) {
                if (newPortForwardRule.equals(portForwardRule)) {
                    addRule = false;
                    break;
                }
            }
            if (addRule) {
                logger.info("Adding port forward rule to firewall configuration: {}", newPortForwardRule);
                this.portForwardRules.add(newPortForwardRule);
                doUpdate = true;
            } else {
                logger.warn("Not adding port forward rule that is already present: {}", newPortForwardRule);
            }
        }
        if (doUpdate) {
            update();
        }
    }

    /**
     * Adds automatic NAT rule
     *
     * @param sourceInterface
     * @param destinationInterface
     * @param masquerade
     * @param type
     * @throws KuraException
     */
    public void addNatRule(String sourceInterface, String destinationInterface, boolean masquerade, RuleType type)
            throws KuraException {
        if (sourceInterface == null || sourceInterface.isEmpty()) {
            logger.warn("Can't add auto NAT rule - source interface not specified");
            return;
        } else if (destinationInterface == null || destinationInterface.isEmpty()) {
            logger.warn("Can't add auto NAT rule - destination interface not specified");
            return;
        }

        NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, masquerade, type);
        ArrayList<NATRule> natRuleList = new ArrayList<>();
        natRuleList.add(newNatRule);
        addAutoNatRules(natRuleList);
    }

    /**
     * Adds NAT Rule
     *
     * @param sourceInterface
     * @param destinationInterface
     * @param protocol
     * @param source
     * @param destination
     * @param masquerade
     * @param type
     * @throws KuraException
     */
    public void addNatRule(String sourceInterface, String destinationInterface, String protocol, String source,
            String destination, boolean masquerade, RuleType type) throws KuraException {

        if (sourceInterface == null || sourceInterface.isEmpty()) {
            logger.warn("Can't add NAT rule - source interface not specified");
            return;
        } else if (destinationInterface == null || destinationInterface.isEmpty()) {
            logger.warn("Can't add NAT rule - destination interface not specified");
            return;
        }

        NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, protocol, source, destination,
                masquerade, type);

        ArrayList<NATRule> natRuleList = new ArrayList<>();
        natRuleList.add(newNatRule);
        addNatRules(natRuleList);
    }

    public void addAutoNatRules(List<NATRule> newNatRules) throws KuraException {
        addNatRules(newNatRules, this.autoNatRules);
    }

    public void addNatRules(List<NATRule> newNatRules) throws KuraException {
        addNatRules(newNatRules, this.natRules);
    }

    private void addNatRules(List<NATRule> newNatRules, Set<NATRule> rules) throws KuraException {
        boolean doUpdate = false;
        for (NATRule newNatRule : newNatRules) {
            // make sure it is not already present
            boolean addRule = true;
            for (NATRule natRule : rules) {
                if (newNatRule.equals(natRule)) {
                    addRule = false;
                    break;
                }
            }
            if (addRule) {
                logger.info("Adding auto NAT rule to firewall configuration: {}", newNatRule);
                rules.add(newNatRule);
                doUpdate = true;
            } else {
                logger.warn("Not adding auto nat rule that is already present: {}", newNatRule);
            }
        }
        if (doUpdate) {
            update();
        }
    }

    public Set<LocalRule> getLocalRules() throws KuraException {
        return this.localRules;
    }

    public Set<PortForwardRule> getPortForwardRules() throws KuraException {
        return this.portForwardRules;
    }

    public Set<NATRule> getAutoNatRules() throws KuraException {
        return this.autoNatRules;
    }

    public Set<NATRule> getNatRules() throws KuraException {
        return this.natRules;
    }

    public void deleteLocalRule(LocalRule rule) throws KuraException {
        this.localRules.remove(rule);
        update();
    }

    public void deletePortForwardRule(PortForwardRule rule) throws KuraException {
        if (this.portForwardRules == null) {
            return;
        }
        this.portForwardRules.remove(rule);
        update();
    }

    public void deleteAutoNatRule(NATRule rule) throws KuraException {
        if (this.autoNatRules == null) {
            return;
        }
        this.autoNatRules.remove(rule);
        update();
    }

    public void deleteAllLocalRules() throws KuraException {
        this.localRules.clear();
        update();
    }

    public void deleteAllPortForwardRules() throws KuraException {
        this.portForwardRules.clear();
        update();
    }

    public void replaceAllNatRules(Set<NATRule> newNatRules) throws KuraException {
        this.autoNatRules = newNatRules;
        update();
    }

    public void deleteAllAutoNatRules() throws KuraException {
        this.autoNatRules.clear();
        update();
    }

    public void deleteAllNatRules() throws KuraException {
        this.natRules.clear();
        update();
    }

    public void blockAllPorts() throws KuraException {
        deleteAllRules();
    }

    public void unblockAllPorts() throws KuraException {
        deleteAllRules();
    }

    private void deleteAllRules() throws KuraException {
        deleteAllLocalRules();
        deleteAllPortForwardRules();
        deleteAllAutoNatRules();
        update();
    }

    private void applyRules() throws KuraException {
        this.iptables.applyRules();
        if (this.portForwardRules != null && !this.portForwardRules.isEmpty()
                || this.autoNatRules != null && !this.autoNatRules.isEmpty()
                || this.natRules != null && !this.natRules.isEmpty()) {

            try (FileWriter fw = new FileWriter(IP_FORWARD_FILE_NAME)) {
                fw.write('1');
            } catch (IOException e) {
                throw new KuraIOException(e, "Failed to enable ip forwarding");
            }
        }
    }

    public void enable() throws KuraException {
        update();
    }

    public void disable() throws KuraException {
        this.iptables.clearAllChains();
    }

    public void allowIcmp() {
        this.allowIcmp = true;
    }

    public void disableIcmp() {
        this.allowIcmp = false;
    }

    public void setAdditionalRules(Set<String> filterRules, Set<String> natRules, Set<String> mangleRules)
            throws KuraException {
        this.additionalFilterRules.clear();
        this.additionalFilterRules.addAll(filterRules);
        this.additionalNatRules.clear();
        this.additionalNatRules.addAll(natRules);
        this.additionalMangleRules.clear();
        this.additionalMangleRules.addAll(mangleRules);
        update();
    }

    private void update() throws KuraException {
        synchronized (lock) {
            updateIptablesConfig();
            this.iptables.clearAllKuraChains();
            applyRules();
            this.iptables.saveKuraChains();
        }
    }

    private void updateIptablesConfig() {
        this.iptables.setLocalRules(this.localRules);
        this.iptables.setPortForwardRules(this.portForwardRules);
        this.iptables.setNatRules(this.natRules);
        this.iptables.setAutoNatRules(this.autoNatRules);
        this.iptables.setAdditionalFilterRules(this.additionalFilterRules);
        this.iptables.setAdditionalNatRules(this.additionalNatRules);
        this.iptables.setAdditionalMangleRules(this.additionalMangleRules);
        this.iptables.setAllowIcmp(this.allowIcmp);
    }
}
