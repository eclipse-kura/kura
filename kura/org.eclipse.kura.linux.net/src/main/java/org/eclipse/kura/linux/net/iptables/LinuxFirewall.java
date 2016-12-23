/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.iptables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
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

    private static final Logger s_logger = LoggerFactory.getLogger(LinuxFirewall.class);

    private static LinuxFirewall s_linuxFirewall;

    private static Object s_lock = new Object();

    private static final String IP_FORWARD_FILE_NAME = "/proc/sys/net/ipv4/ip_forward";
    private static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
    private static final String CUSTOM_FIREWALL_SCRIPT_NAME = "/etc/init.d/firewall_cust";

    private LinkedHashSet<LocalRule> m_localRules;
    private LinkedHashSet<PortForwardRule> m_portForwardRules;
    private LinkedHashSet<NATRule> m_autoNatRules;
    private LinkedHashSet<NATRule> m_natRules;
    private boolean m_allowIcmp;
    private boolean m_allowForwarding;

    private LinuxFirewall() {
        try {
            File cfgFile = new File(FIREWALL_CONFIG_FILE_NAME);
            if (!cfgFile.exists()) {
                IptablesConfig.applyBlockPolicy();
                IptablesConfig.save();
            } else {
                s_logger.debug("{} file already exists", cfgFile);
            }
        } catch (Exception e) {
            s_logger.error("cannot create or read file", e);// File did not exist and was created
        }
        try {
            initialize();
        } catch (KuraException e) {
            s_logger.error("failed to initialize LinuxFirewall", e);
        }
    }

    public static LinuxFirewall getInstance() {
        if (s_linuxFirewall == null) {
            s_linuxFirewall = new LinuxFirewall();
        }

        return s_linuxFirewall;
    }

    public void initialize() throws KuraException {
        s_logger.debug("initialize() :: initializing firewall ...");
        IptablesConfig iptables = new IptablesConfig();
        iptables.restore();
        this.m_localRules = iptables.getLocalRules();
        this.m_portForwardRules = iptables.getPortForwardRules();
        this.m_autoNatRules = iptables.getAutoNatRules();
        this.m_natRules = iptables.getNatRules();
        this.m_allowIcmp = true;
        this.m_allowForwarding = false;
        s_logger.debug("initialize() :: Parsing current firewall configuraion");
    }

    public void addLocalRule(int port, String protocol, String permittedNetwork, String permittedNetworkPrefix,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC, String sourcePortRange)
                    throws KuraException {
        try {
            LocalRule newLocalRule = null;
            if (permittedNetwork != null && permittedNetworkPrefix != null) {
                s_logger.debug("permittedNetwork: {}", permittedNetwork);
                s_logger.debug("permittedNetworkPrefix: {}", permittedNetworkPrefix);

                newLocalRule = new LocalRule(port, protocol,
                        new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(permittedNetwork),
                                Short.parseShort(permittedNetworkPrefix)),
                        permittedInterfaceName, unpermittedInterfaceName, permittedMAC, sourcePortRange);
            } else {
                newLocalRule = new LocalRule(port, protocol,
                        new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0),
                        permittedInterfaceName, permittedInterfaceName, permittedMAC, sourcePortRange);
            }

            ArrayList<LocalRule> localRules = new ArrayList<LocalRule>();
            localRules.add(newLocalRule);
            addLocalRules(localRules);
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
                for (LocalRule localRule : this.m_localRules) {
                    if (newLocalRule.equals(localRule)) {
                        addRule = false;
                        break;
                    }
                }
                if (addRule) {
                    s_logger.info("Adding local rule to firewall configuration: {}", newLocalRule.toString());
                    this.m_localRules.add(newLocalRule);
                    doUpdate = true;
                } else {
                    s_logger.warn("Not adding local rule that is already present: {}", newLocalRule.toString());
                }
            }
            if (doUpdate) {
                update();
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void addPortForwardRule(String inboundIface, String outboundIface, String address, String protocol,
            int inPort, int outPort, boolean masquerade, String permittedNetwork, String permittedNetworkPrefix,
            String permittedMAC, String sourcePortRange) throws KuraException {
        try {
            PortForwardRule newPortForwardRule = null;
            if (permittedNetworkPrefix != null) {
                newPortForwardRule = new PortForwardRule(inboundIface, outboundIface, address, protocol, inPort,
                        outPort, masquerade, permittedNetwork, Short.parseShort(permittedNetworkPrefix), permittedMAC,
                        sourcePortRange);
            } else {
                newPortForwardRule = new PortForwardRule(inboundIface, outboundIface, address, protocol, inPort,
                        outPort, masquerade, permittedNetwork, -1, permittedMAC, sourcePortRange);
            }

            ArrayList<PortForwardRule> portForwardRules = new ArrayList<PortForwardRule>();
            portForwardRules.add(newPortForwardRule);
            addPortForwardRules(portForwardRules);
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
                for (PortForwardRule portForwardRule : this.m_portForwardRules) {
                    if (newPortForwardRule.equals(portForwardRule)) {
                        addRule = false;
                        break;
                    }
                }
                if (addRule) {
                    s_logger.info("Adding port forward rule to firewall configuration: {}",
                            newPortForwardRule.toString());
                    this.m_portForwardRules.add(newPortForwardRule);
                    doUpdate = true;
                } else {
                    s_logger.warn("Not adding port forward rule that is already present: {}",
                            newPortForwardRule.toString());
                }
            }
            if (doUpdate) {
                this.m_allowForwarding = true;
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
                s_logger.warn("Can't add auto NAT rule - source interface not specified");
                return;
            } else if (destinationInterface == null || destinationInterface.isEmpty()) {
                s_logger.warn("Can't add auto NAT rule - destination interface not specified");
                return;
            }

            NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, masquerade);
            ArrayList<NATRule> natRules = new ArrayList<NATRule>();
            natRules.add(newNatRule);
            addAutoNatRules(natRules);
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
                s_logger.warn("Can't add NAT rule - source interface not specified");
                return;
            } else if (destinationInterface == null || destinationInterface.isEmpty()) {
                s_logger.warn("Can't add NAT rule - destination interface not specified");
                return;
            }

            NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, protocol, source, destination,
                    masquerade);

            ArrayList<NATRule> natRules = new ArrayList<NATRule>();
            natRules.add(newNatRule);
            addNatRules(natRules);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void addAutoNatRules(List<NATRule> newNatRules) throws KuraException {
        addNatRules(newNatRules, this.m_autoNatRules);
    }

    public void addNatRules(List<NATRule> newNatRules) throws KuraException {
        addNatRules(newNatRules, this.m_natRules);
    }

    private void addNatRules(List<NATRule> newNatRules, LinkedHashSet<NATRule> rules) throws KuraException {
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
                    s_logger.info("Adding auto NAT rule to firewall configuration: {}", newNatRule.toString());
                    rules.add(newNatRule);
                    doUpdate = true;
                } else {
                    s_logger.warn("Not adding auto nat rule that is already present: {}", newNatRule.toString());
                }
            }
            if (doUpdate) {
                this.m_allowForwarding = true;
                update();
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<LocalRule> getLocalRules() throws KuraException {
        try {
            return this.m_localRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<PortForwardRule> getPortForwardRules() throws KuraException {
        try {
            return this.m_portForwardRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<NATRule> getAutoNatRules() throws KuraException {
        try {
            return this.m_autoNatRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<NATRule> getNatRules() throws KuraException {
        try {
            return this.m_natRules;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteLocalRule(LocalRule rule) throws KuraException {
        try {
            this.m_localRules.remove(rule);
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deletePortForwardRule(PortForwardRule rule) throws KuraException {
        if (this.m_portForwardRules == null) {
            return;
        }
        try {
            this.m_portForwardRules.remove(rule);
            if (this.m_autoNatRules != null && this.m_autoNatRules.isEmpty() && this.m_natRules != null
                    && this.m_natRules.isEmpty() && this.m_portForwardRules != null
                    && this.m_portForwardRules.isEmpty()) {

                this.m_allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAutoNatRule(NATRule rule) throws KuraException {
        if (this.m_autoNatRules == null) {
            return;
        }
        try {
            this.m_autoNatRules.remove(rule);
            if (this.m_autoNatRules != null && this.m_autoNatRules.isEmpty() && this.m_natRules != null
                    && this.m_natRules.isEmpty() && this.m_portForwardRules != null
                    && this.m_portForwardRules.isEmpty()) {

                this.m_allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllLocalRules() throws KuraException {
        try {
            this.m_localRules.clear();
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllPortForwardRules() throws KuraException {
        try {
            this.m_portForwardRules.clear();
            if (this.m_autoNatRules != null && this.m_autoNatRules.isEmpty() && this.m_natRules != null
                    && this.m_natRules.isEmpty()) {

                this.m_allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void replaceAllNatRules(LinkedHashSet<NATRule> newNatRules) throws KuraException {
        try {
            this.m_autoNatRules = newNatRules;
            if (this.m_autoNatRules != null && !this.m_autoNatRules.isEmpty()
                    || this.m_natRules != null && !this.m_natRules.isEmpty()
                    || this.m_portForwardRules != null && !this.m_portForwardRules.isEmpty()) {

                this.m_allowForwarding = true;
            } else {
                this.m_allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllAutoNatRules() throws KuraException {
        try {
            this.m_autoNatRules.clear();
            if (this.m_natRules != null && this.m_natRules.isEmpty() && this.m_portForwardRules != null
                    && this.m_portForwardRules.isEmpty()) {

                this.m_allowForwarding = false;
            }
            update();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteAllNatRules() throws KuraException {
        try {
            this.m_natRules.clear();
            if (this.m_autoNatRules != null && this.m_autoNatRules.isEmpty() && this.m_portForwardRules != null
                    && this.m_portForwardRules.isEmpty()) {
                this.m_allowForwarding = false;
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
        if (this.m_portForwardRules != null && !this.m_portForwardRules.isEmpty()
                || this.m_autoNatRules != null && !this.m_autoNatRules.isEmpty()
                || this.m_natRules != null && !this.m_natRules.isEmpty()) {
            this.m_allowForwarding = true;
        }
        IptablesConfig iptables = new IptablesConfig(this.m_localRules, this.m_portForwardRules, this.m_autoNatRules,
                this.m_natRules, this.m_allowIcmp);
        iptables.save(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);
        IptablesConfig.restore(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);
        s_logger.debug("Managing port forwarding...");
        enableForwarding(this.m_allowForwarding);
        runCustomFirewallScript();
    }

    private static void enableForwarding(boolean allow) throws KuraException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(IP_FORWARD_FILE_NAME);
            if (allow) {
                fw.write('1');
            } else {
                fw.write('0');
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
            }
        }
    }

    /*
     * Runs custom firewall script
     */
    private static void runCustomFirewallScript() throws KuraException {
        SafeProcess proc = null;
        try {
            File file = new File(CUSTOM_FIREWALL_SCRIPT_NAME);
            if (file.exists()) {
                s_logger.info("Running custom firewall script - {}", CUSTOM_FIREWALL_SCRIPT_NAME);
                proc = ProcessUtil.exec("sh " + CUSTOM_FIREWALL_SCRIPT_NAME);
                proc.waitFor();
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    public void enable() throws KuraException {
        update();
    }

    public void disable() throws KuraException {
        IptablesConfig.clearAllChains();
    }

    public void allowIcmp() {
        this.m_allowIcmp = true;
    }

    public void disableIcmp() {
        this.m_allowIcmp = false;
    }

    public void enableForwarding() {
        this.m_allowForwarding = true;
    }

    public void disableForwarding() {
        this.m_allowForwarding = false;
    }

    private void update() throws KuraException {
        synchronized (s_lock) {
            applyRules();
            IptablesConfig.save();
        }
    }
}
