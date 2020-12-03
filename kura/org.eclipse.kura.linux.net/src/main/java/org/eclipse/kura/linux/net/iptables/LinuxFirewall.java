/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux firewall implementation
 *
 * @author eurotech
 */
public class LinuxFirewall {

    private static final Logger logger = LoggerFactory.getLogger(LinuxFirewall.class);

    private static Object lock = new Object();

    private static final String IP_FORWARD_FILE_NAME = "/proc/sys/net/ipv4/ip_forward";
    private static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
    private static final String CUSTOM_FIREWALL_SCRIPT_NAME = "/etc/init.d/firewall_cust";

    private Set<LocalRule> localRules;
    private Set<PortForwardRule> portForwardRules;
    private Set<NATRule> autoNatRules;
    private Set<NATRule> natRules;
    private boolean allowIcmp;
    private boolean allowForwarding;
    private final IptablesConfig iptables;
    private final CommandExecutorService executorService;

    public LinuxFirewall(CommandExecutorService executorService) {
        this.executorService = executorService;
        this.iptables = new IptablesConfig(this.executorService);
        try {
            File cfgFile = new File(FIREWALL_CONFIG_FILE_NAME);
            if (!cfgFile.exists()) {
                this.iptables.applyBlockPolicy();
                this.iptables.save();
            } else {
                logger.debug("{} file already exists", cfgFile);
            }
        } catch (Exception e) {
            logger.error("cannot create or read file", e); // File did not exist and was created
        }
        try {
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
        this.allowForwarding = false;
        logger.debug("initialize() :: Parsing current firewall configuraion");
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
                        new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0),
                        permittedInterfaceName, permittedInterfaceName, permittedMAC, sourcePortRange);
            }

            ArrayList<LocalRule> locRules = new ArrayList<>();
            locRules.add(newLocalRule);
            addLocalRules(locRules);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void addLocalRules(List<LocalRule> newLocalRules) throws KuraException {
        try {
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
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public void addPortForwardRule(String inboundIface, String outboundIface, String address, String protocol,
            int inPort, int outPort, boolean masquerade, String permittedNetwork, String permittedNetworkPrefix,
            String permittedMAC, String sourcePortRange) throws KuraException {
        try {
            PortForwardRule newPortForwardRule;
            if (permittedNetworkPrefix != null) {
                newPortForwardRule = new PortForwardRule(inboundIface, outboundIface, address, protocol, inPort,
                        outPort, masquerade, permittedNetwork, Short.parseShort(permittedNetworkPrefix), permittedMAC,
                        sourcePortRange);
            } else {
                newPortForwardRule = new PortForwardRule(inboundIface, outboundIface, address, protocol, inPort,
                        outPort, masquerade, permittedNetwork, -1, permittedMAC, sourcePortRange);
            }

            ArrayList<PortForwardRule> portFwdRules = new ArrayList<>();
            portFwdRules.add(newPortForwardRule);
            addPortForwardRules(portFwdRules);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void addPortForwardRules(List<PortForwardRule> newPortForwardRules) throws KuraException {
        try {
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
                this.allowForwarding = true;
                update();
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * Adds automatic NAT rule
     *
     * @param sourceInterface
     * @param destinationInterface
     * @param masquerade
     * @throws EsfException
     */
    public void addNatRule(String sourceInterface, String destinationInterface, boolean masquerade)
            throws KuraException {
        try {
            if (sourceInterface == null || sourceInterface.isEmpty()) {
                logger.warn("Can't add auto NAT rule - source interface not specified");
                return;
            } else if (destinationInterface == null || destinationInterface.isEmpty()) {
                logger.warn("Can't add auto NAT rule - destination interface not specified");
                return;
            }

            NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, masquerade);
            ArrayList<NATRule> natRuleList = new ArrayList<>();
            natRuleList.add(newNatRule);
            addAutoNatRules(natRuleList);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
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
     * @throws EsfException
     */
    public void addNatRule(String sourceInterface, String destinationInterface, String protocol, String source,
            String destination, boolean masquerade) throws KuraException {

        try {
            if (sourceInterface == null || sourceInterface.isEmpty()) {
                logger.warn("Can't add NAT rule - source interface not specified");
                return;
            } else if (destinationInterface == null || destinationInterface.isEmpty()) {
                logger.warn("Can't add NAT rule - destination interface not specified");
                return;
            }

            NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, protocol, source, destination,
                    masquerade);

            ArrayList<NATRule> natRuleList = new ArrayList<>();
            natRuleList.add(newNatRule);
            addNatRules(natRuleList);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void addAutoNatRules(List<NATRule> newNatRules) throws KuraException {
        addNatRules(newNatRules, this.autoNatRules);
    }

    public void addNatRules(List<NATRule> newNatRules) throws KuraException {
        addNatRules(newNatRules, this.natRules);
    }

    private void addNatRules(List<NATRule> newNatRules, Set<NATRule> rules) throws KuraException {
        try {
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
                this.allowForwarding = true;
                update();
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<LocalRule> getLocalRules() throws KuraException {
        try {
            return this.localRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<PortForwardRule> getPortForwardRules() throws KuraException {
        try {
            return this.portForwardRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<NATRule> getAutoNatRules() throws KuraException {
        try {
            return this.autoNatRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<NATRule> getNatRules() throws KuraException {
        try {
            return this.natRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteLocalRule(LocalRule rule) throws KuraException {
        try {
            this.localRules.remove(rule);
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deletePortForwardRule(PortForwardRule rule) throws KuraException {
        if (this.portForwardRules == null) {
            return;
        }
        try {
            this.portForwardRules.remove(rule);
            if (this.autoNatRules != null && this.autoNatRules.isEmpty() && this.natRules != null
                    && this.natRules.isEmpty() && this.portForwardRules.isEmpty()) {

                this.allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAutoNatRule(NATRule rule) throws KuraException {
        if (this.autoNatRules == null) {
            return;
        }
        try {
            this.autoNatRules.remove(rule);
            if (this.autoNatRules.isEmpty() && this.natRules != null && this.natRules.isEmpty()
                    && this.portForwardRules != null && this.portForwardRules.isEmpty()) {

                this.allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllLocalRules() throws KuraException {
        try {
            this.localRules.clear();
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllPortForwardRules() throws KuraException {
        try {
            this.portForwardRules.clear();
            if (this.autoNatRules != null && this.autoNatRules.isEmpty() && this.natRules != null
                    && this.natRules.isEmpty()) {

                this.allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void replaceAllNatRules(Set<NATRule> newNatRules) throws KuraException {
        try {
            this.autoNatRules = newNatRules;
            if (this.autoNatRules != null && !this.autoNatRules.isEmpty()
                    || this.natRules != null && !this.natRules.isEmpty()
                    || this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
                this.allowForwarding = true;
            } else {
                this.allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllAutoNatRules() throws KuraException {
        try {
            this.autoNatRules.clear();
            if (this.natRules != null && this.natRules.isEmpty() && this.portForwardRules != null
                    && this.portForwardRules.isEmpty()) {

                this.allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllNatRules() throws KuraException {
        try {
            this.natRules.clear();
            if (this.autoNatRules != null && this.autoNatRules.isEmpty() && this.portForwardRules != null
                    && this.portForwardRules.isEmpty()) {
                this.allowForwarding = false;
            }
            update();
        } catch (KuraException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void blockAllPorts() throws KuraException {
        deleteAllLocalRules();
        deleteAllPortForwardRules();
        deleteAllAutoNatRules();
        update();
    }

    public void unblockAllPorts() throws KuraException {
        deleteAllLocalRules();
        deleteAllPortForwardRules();
        deleteAllAutoNatRules();
        update();
    }

    private void applyRules() throws KuraException {
        if (this.portForwardRules != null && !this.portForwardRules.isEmpty()
                || this.autoNatRules != null && !this.autoNatRules.isEmpty()
                || this.natRules != null && !this.natRules.isEmpty()) {
            this.allowForwarding = true;
        }
        IptablesConfig newIptables = new IptablesConfig(this.localRules, this.portForwardRules, this.autoNatRules,
                this.natRules, this.allowIcmp, this.executorService);
        newIptables.save(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);
        newIptables.restore(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);
        logger.debug("Managing port forwarding...");
        enableForwarding(this.allowForwarding);
        runCustomFirewallScript();
    }

    private static void enableForwarding(boolean allow) throws KuraException {
        try (FileWriter fw = new FileWriter(IP_FORWARD_FILE_NAME)) {
            if (allow) {
                fw.write('1');
            } else {
                fw.write('0');
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    /*
     * Runs custom firewall script
     */
    private void runCustomFirewallScript() throws KuraException {
        File file = new File(CUSTOM_FIREWALL_SCRIPT_NAME);
        if (file.exists()) {
            logger.info("Running custom firewall script - {}", CUSTOM_FIREWALL_SCRIPT_NAME);
            Command command = new Command(new String[] { "sh", CUSTOM_FIREWALL_SCRIPT_NAME });
            CommandStatus status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                throw new KuraProcessExecutionErrorException("Failed to apply custom firewall script");
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

    public void enableForwarding() {
        this.allowForwarding = true;
    }

    public void disableForwarding() {
        this.allowForwarding = false;
    }

    private void update() throws KuraException {
        synchronized (lock) {
            applyRules();
            this.iptables.save();
        }
    }
}
