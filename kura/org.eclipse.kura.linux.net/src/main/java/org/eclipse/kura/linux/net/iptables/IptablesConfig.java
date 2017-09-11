/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptablesConfig {

    private static final Logger logger = LoggerFactory.getLogger(IptablesConfig.class);
    public static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
    public static final String FIREWALL_TMP_CONFIG_FILE_NAME = "/tmp/iptables";

    private static final String ALLOW_ALL_TRAFFIC_TO_LOOPBACK = "-A INPUT -i lo -j ACCEPT";
    private static final String ALLOW_ONLY_INCOMING_TO_OUTGOING = "-A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT";

    private static final String[] ALLOW_ICMP = {
            "-A INPUT -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT",
            "-A OUTPUT -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j ACCEPT" };

    private static final String[] DO_NOT_ALLOW_ICMP = {
            "-A INPUT -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j DROP",
            "-A OUTPUT -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j DROP" };

    private final Set<LocalRule> localRules;
    private final Set<PortForwardRule> portForwardRules;
    private final Set<NATRule> autoNatRules;
    private final Set<NATRule> natRules;
    private boolean allowIcmp;

    public IptablesConfig() {
        this.localRules = new LinkedHashSet<>();
        this.portForwardRules = new LinkedHashSet<>();
        this.autoNatRules = new LinkedHashSet<>();
        this.natRules = new LinkedHashSet<>();
    }

    public IptablesConfig(Set<LocalRule> localRules, Set<PortForwardRule> portForwardRules, Set<NATRule> autoNatRules,
            Set<NATRule> natRules, boolean allowIcmp) {
        this.localRules = localRules;
        this.portForwardRules = portForwardRules;
        this.autoNatRules = autoNatRules;
        this.natRules = natRules;
        this.allowIcmp = allowIcmp;
    }

    /*
     * Clears all chains
     */
    public static void clearAllChains() throws KuraException {
        FileOutputStream fos = null;
        PrintWriter writer = null;
        try {
            fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
            writer = new PrintWriter(fos);
            writer.println("*nat");
            writer.println("COMMIT");
            writer.println("*filter");
            writer.println("COMMIT");
        } catch (Exception e) {
            logger.error("clear() :: failed to clear all chains ", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error("clear() :: failed to close FileOutputStream ", e);
                }
            }
        }
        File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
        if (configFile.exists()) {
            restore(FIREWALL_TMP_CONFIG_FILE_NAME);
        }
    }

    public static void applyBlockPolicy() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println("*nat");
            writer.println("COMMIT");
            writer.println("*filter");
            writer.println(ALLOW_ALL_TRAFFIC_TO_LOOPBACK);
            writer.println(ALLOW_ONLY_INCOMING_TO_OUTGOING);
            writer.println("COMMIT");
        } catch (Exception e) {
            logger.error("applyBlockPolicy() :: failed to clear all chains ", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
        if (configFile.exists()) {
            restore(FIREWALL_TMP_CONFIG_FILE_NAME);
        }
    }

    /*
     * Saves (using iptables-save) the current iptables config into /etc/sysconfig/iptables
     */
    public static void save() throws KuraException {
        SafeProcess proc = null;

        try {
            int status = -1;
            proc = ProcessUtil.exec("iptables-save");
            status = proc.waitFor();
            if (status != 0) {
                logger.error("save() :: failed - {}", LinuxProcessUtil.getInputStreamAsString(proc.getErrorStream()));
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to execute the iptable-save command");
            }
            iptablesSave(proc);
            logger.debug("iptablesSave() :: completed!, status={}", status);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    private static void iptablesSave(SafeProcess proc) throws KuraException {
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                PrintWriter out = new PrintWriter(FIREWALL_CONFIG_FILE_NAME)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        } catch (Exception e) {
            logger.error("failed to write {} file ", FIREWALL_CONFIG_FILE_NAME, e);
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    /*
     * Restores (using iptables-restore) firewall settings from temporary iptables configuration file.
     * Temporary configuration file is deleted upon completion.
     */
    public static void restore(String filename) throws KuraException {
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec("iptables-restore " + filename);
            int status = proc.waitFor();
            if (status != 0) {
                logger.error("restore() :: failed - {}",
                        LinuxProcessUtil.getInputStreamAsString(proc.getErrorStream()));
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Failed to execute the iptable-restore command");
            }
        } catch (Exception e) {
            logger.error("restore() :: Failed to execute the iptable-restore command ", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
            File configFile = new File(filename);
            if (configFile.exists()) {
                if (configFile.delete()) {
                    logger.debug("restore() :: removing the {} file", filename);
                }
            }
        }
    }

    /*
     * Saves current configurations from the m_localRules, m_portForwardRules, m_natRules, and m_autoNatRules
     * into specified temporary file
     */
    public void save(String filename) throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println("*filter");
            writer.println(ALLOW_ALL_TRAFFIC_TO_LOOPBACK);
            writer.println(ALLOW_ONLY_INCOMING_TO_OUTGOING);
            if (this.allowIcmp) {
                for (String sAllowIcmp : ALLOW_ICMP) {
                    writer.println(sAllowIcmp);
                }
            } else {
                for (String sDoNotAllowIcmp : DO_NOT_ALLOW_ICMP) {
                    writer.println(sDoNotAllowIcmp);
                }
            }
            if (this.localRules != null && !this.localRules.isEmpty()) {
                for (LocalRule lr : this.localRules) {
                    writer.println(lr);
                }
            }
            if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
                for (PortForwardRule portForwardRule : this.portForwardRules) {
                    List<String> filterForwardChainRules = portForwardRule.getFilterForwardChainRule().toStrings();
                    if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                        for (String filterForwardChainRule : filterForwardChainRules) {
                            writer.println(filterForwardChainRule);
                        }
                    }
                }
            }
            if (this.autoNatRules != null && !this.autoNatRules.isEmpty()) {
                for (NATRule autoNatRule : this.autoNatRules) {
                    List<String> filterForwardChainRules = autoNatRule.getFilterForwardChainRule().toStrings();
                    if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                        for (String filterForwardChainRule : filterForwardChainRules) {
                            writer.println(filterForwardChainRule);
                        }
                    }
                }
            }
            if (this.natRules != null && !this.natRules.isEmpty()) {
                for (NATRule natRule : this.natRules) {
                    List<String> filterForwardChainRules = natRule.getFilterForwardChainRule().toStrings();
                    if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                        for (String filterForwardChainRule : filterForwardChainRules) {
                            writer.println(filterForwardChainRule);
                        }
                    }
                }
            }
            writer.println("COMMIT");
            writer.println("*nat");
            if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
                for (PortForwardRule portForwardRule : this.portForwardRules) {
                    writer.println(portForwardRule.getNatPreroutingChainRule());
                    writer.println(portForwardRule.getNatPostroutingChainRule());
                }
            }
            if (this.autoNatRules != null && !this.autoNatRules.isEmpty()) {
                List<NatPostroutingChainRule> appliedNatPostroutingChainRules = new ArrayList<NatPostroutingChainRule>();
                for (NATRule autoNatRule : this.autoNatRules) {
                    boolean found = false;
                    NatPostroutingChainRule natPostroutingChainRule = autoNatRule.getNatPostroutingChainRule();

                    for (NatPostroutingChainRule appliedNatPostroutingChainRule : appliedNatPostroutingChainRules) {

                        if (appliedNatPostroutingChainRule.equals(natPostroutingChainRule)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        writer.println(autoNatRule.getNatPostroutingChainRule());
                        appliedNatPostroutingChainRules.add(natPostroutingChainRule);
                    }
                }
            }
            if (this.natRules != null && !this.natRules.isEmpty()) {
                for (NATRule natRule : this.natRules) {
                    writer.println(natRule.getNatPostroutingChainRule());
                }
            }
            writer.println("COMMIT");
        } catch (Exception e) {
            logger.error("save() :: failed to clear all chains ", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    /*
     * Populates the m_localRules, m_portForwardRules, m_natRules, and m_autoNatRules by parsing
     * the iptables configuration file.
     */
    public void restore() throws KuraException {
        try (FileReader fr = new FileReader(FIREWALL_CONFIG_FILE_NAME); BufferedReader br = new BufferedReader(fr)) {
            List<NatPreroutingChainRule> natPreroutingChain = new ArrayList<>();
            List<NatPostroutingChainRule> natPostroutingChain = new ArrayList<>();
            List<FilterForwardChainRule> filterForwardChain = new ArrayList<>();

            String line = null;
            boolean readingNatTable = false;
            boolean readingFilterTable = false;
            lineloop: while ((line = br.readLine()) != null) {
                line = line.trim();
                // skip any predefined lines or comment lines
                if ("".equals(line)) {
                    continue;
                }
                if (line.startsWith("#") || line.startsWith(":")) {
                    continue;
                }
                if ("*nat".equals(line)) {
                    readingNatTable = true;
                } else if ("*filter".equals(line)) {
                    readingFilterTable = true;
                } else if ("COMMIT".equals(line)) {
                    if (readingNatTable) {
                        readingNatTable = false;
                    }
                    if (readingFilterTable) {
                        readingFilterTable = false;
                    }
                } else if (readingNatTable && line.startsWith("-A PREROUTING")) {
                    natPreroutingChain.add(new NatPreroutingChainRule(line));
                } else if (readingNatTable && line.startsWith("-A POSTROUTING")) {
                    natPostroutingChain.add(new NatPostroutingChainRule(line));
                } else if (readingFilterTable && line.startsWith("-A FORWARD")) {
                    filterForwardChain.add(new FilterForwardChainRule(line));
                } else if (readingFilterTable && line.startsWith("-A INPUT")) {
                    if (ALLOW_ALL_TRAFFIC_TO_LOOPBACK.equals(line)) {
                        continue;
                    }
                    if (ALLOW_ONLY_INCOMING_TO_OUTGOING.equals(line)) {
                        continue;
                    }
                    for (String allowIcmpProto : ALLOW_ICMP) {
                        if (allowIcmpProto.equals(line)) {
                            this.allowIcmp = true;
                            continue lineloop;
                        }
                    }
                    for (String allowIcmpProto : DO_NOT_ALLOW_ICMP) {
                        if (allowIcmpProto.equals(line)) {
                            this.allowIcmp = false;
                            continue lineloop;
                        }
                    }
                    try {
                        LocalRule localRule = new LocalRule(line);
                        logger.debug("parseFirewallConfigurationFile() :: Adding local rule: {}", localRule);
                        this.localRules.add(localRule);
                    } catch (KuraException e) {
                        logger.error("Failed to parse Local Rule: {} ", line, e);
                    }
                }
            }

            // ! done parsing !
            for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
                // found port forwarding rule ...
                String inboundIfaceName = natPreroutingChainRule.getInputInterface();
                String outboundIfaceName = null;
                String protocol = natPreroutingChainRule.getProtocol();
                int inPort = natPreroutingChainRule.getExternalPort();
                int outPort = natPreroutingChainRule.getInternalPort();
                boolean masquerade = false;
                String sport = null;
                if (natPreroutingChainRule.getSrcPortFirst() > 0
                        && natPreroutingChainRule.getSrcPortFirst() <= natPreroutingChainRule.getSrcPortLast()) {
                    StringBuilder sbSport = new StringBuilder().append(natPreroutingChainRule.getSrcPortFirst())
                            .append(':').append(natPreroutingChainRule.getSrcPortLast());
                    sport = sbSport.toString();
                }
                String permittedMac = natPreroutingChainRule.getPermittedMacAddress();
                String permittedNetwork = natPreroutingChainRule.getPermittedNetwork();
                int permittedNetworkMask = natPreroutingChainRule.getPermittedNetworkMask();
                String address = natPreroutingChainRule.getDstIpAddress();

                for (NatPostroutingChainRule natPostroutingChainRule : natPostroutingChain) {
                    if (natPreroutingChainRule.getDstIpAddress().equals(natPostroutingChainRule.getDstNetwork())) {
                        outboundIfaceName = natPostroutingChainRule.getDstInterface();
                        if (natPostroutingChainRule.isMasquerade()) {
                            masquerade = true;
                        }
                    }
                }
                if (permittedNetwork == null) {
                    permittedNetwork = "0.0.0.0";
                }
                PortForwardRule portForwardRule = new PortForwardRule(inboundIfaceName, outboundIfaceName, address,
                        protocol, inPort, outPort, masquerade, permittedNetwork, permittedNetworkMask, permittedMac,
                        sport);
                logger.debug("Adding port forward rule: {}", portForwardRule);
                this.portForwardRules.add(portForwardRule);
            }

            for (NatPostroutingChainRule natPostroutingChainRule : natPostroutingChain) {
                String destinationInterface = natPostroutingChainRule.getDstInterface();
                boolean masquerade = natPostroutingChainRule.isMasquerade();
                String protocol = natPostroutingChainRule.getProtocol();
                if (protocol != null) {
                    // found NAT rule, ... maybe
                    boolean isNATrule = false;
                    String source = natPostroutingChainRule.getSrcNetwork();
                    String destination = natPostroutingChainRule.getDstNetwork();
                    if (destination != null) {
                        StringBuilder sbDestination = new StringBuilder().append(destination).append('/')
                                .append(natPostroutingChainRule.getDstMask());
                        destination = sbDestination.toString();
                    } else {
                        isNATrule = true;
                    }

                    if (source != null) {
                        StringBuilder sbSource = new StringBuilder().append(source).append('/')
                                .append(natPostroutingChainRule.getSrcMask());
                        source = sbSource.toString();
                    }
                    if (!isNATrule) {
                        boolean matchFound = false;
                        for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
                            if (natPreroutingChainRule.getDstIpAddress()
                                    .equals(natPostroutingChainRule.getDstNetwork())) {
                                matchFound = true;
                                break;
                            }
                        }
                        if (!matchFound) {
                            isNATrule = true;
                        }
                    }
                    if (isNATrule) {
                        // match FORWARD rule to find out source interface ...
                        for (FilterForwardChainRule filterForwardChainRule : filterForwardChain) {
                            if (natPostroutingChainRule.isMatchingForwardChainRule(filterForwardChainRule)) {
                                String sourceInterface = filterForwardChainRule.getInputInterface();
                                logger.debug("parseFirewallConfigurationFile() :: Parsed NAT rule with"
                                        + "   sourceInterface: " + sourceInterface + "   destinationInterface: "
                                        + destinationInterface + "   masquerade: " + masquerade + "	protocol: "
                                        + protocol + "	source network/host: " + source + "	destination network/host "
                                        + destination);
                                NATRule natRule = new NATRule(sourceInterface, destinationInterface, protocol, source,
                                        destination, masquerade);
                                logger.debug("parseFirewallConfigurationFile() :: Adding NAT rule {}", natRule);
                                this.natRules.add(natRule);
                            }
                        }
                    }
                } else {
                    // found Auto NAT rule ...
                    // match FORWARD rule to find out source interface ...
                    for (FilterForwardChainRule filterForwardChainRule : filterForwardChain) {
                        if (natPostroutingChainRule.isMatchingForwardChainRule(filterForwardChainRule)) {
                            String sourceInterface = filterForwardChainRule.getInputInterface();
                            logger.debug("parseFirewallConfigurationFile() :: Parsed auto NAT rule with"
                                    + "   sourceInterface: " + sourceInterface + "   destinationInterface: "
                                    + destinationInterface + "   masquerade: " + masquerade);

                            NATRule natRule = new NATRule(sourceInterface, destinationInterface, masquerade);
                            logger.debug("parseFirewallConfigurationFile() :: Adding auto NAT rule {}", natRule);
                            this.autoNatRules.add(natRule);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public Set<LocalRule> getLocalRules() {
        return this.localRules;
    }

    public Set<PortForwardRule> getPortForwardRules() {
        return this.portForwardRules;
    }

    public Set<NATRule> getAutoNatRules() {
        return this.autoNatRules;
    }

    public Set<NATRule> getNatRules() {
        return this.natRules;
    }

    public boolean allowIcmp() {
        return this.allowIcmp;
    }
}
