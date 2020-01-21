/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptablesConfig {

    private static final String COMMAND_EXECUTOR_SERVICE_MESSAGE = "CommandExecutorService not set.";
    private static final Logger logger = LoggerFactory.getLogger(IptablesConfig.class);
    public static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
    public static final String FIREWALL_TMP_CONFIG_FILE_NAME = "/tmp/iptables";
    private static final String FILTER = "*filter";
    private static final String COMMIT = "COMMIT";
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
    private CommandExecutorService executorService;

    public IptablesConfig() {
        this.localRules = new LinkedHashSet<>();
        this.portForwardRules = new LinkedHashSet<>();
        this.autoNatRules = new LinkedHashSet<>();
        this.natRules = new LinkedHashSet<>();
    }

    public IptablesConfig(CommandExecutorService executorService) {
        this.localRules = new LinkedHashSet<>();
        this.portForwardRules = new LinkedHashSet<>();
        this.autoNatRules = new LinkedHashSet<>();
        this.natRules = new LinkedHashSet<>();
        this.executorService = executorService;
    }

    public IptablesConfig(Set<LocalRule> localRules, Set<PortForwardRule> portForwardRules, Set<NATRule> autoNatRules,
            Set<NATRule> natRules, boolean allowIcmp, CommandExecutorService executorService) {
        this.localRules = localRules;
        this.portForwardRules = portForwardRules;
        this.autoNatRules = autoNatRules;
        this.natRules = natRules;
        this.allowIcmp = allowIcmp;
        this.executorService = executorService;
    }

    /*
     * Clears all chains
     */
    public void clearAllChains() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println("*nat");
            writer.println(COMMIT);
            writer.println(FILTER);
            writer.println(COMMIT);

            File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
            if (configFile.exists()) {
                restore(FIREWALL_TMP_CONFIG_FILE_NAME);
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "clear() :: failed to clear all chains");
        }
    }

    public void applyBlockPolicy() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println("*nat");
            writer.println(COMMIT);
            writer.println(FILTER);
            writer.println(ALLOW_ALL_TRAFFIC_TO_LOOPBACK);
            writer.println(ALLOW_ONLY_INCOMING_TO_OUTGOING);
            writer.println(COMMIT);

            File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
            if (configFile.exists()) {
                restore(FIREWALL_TMP_CONFIG_FILE_NAME);
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e,
                    "applyBlockPolicy() :: failed to clear all chains");
        }
    }

    /*
     * Saves (using iptables-save) the current iptables config into /etc/sysconfig/iptables
     */
    public void save() throws KuraException {
        int exitValue = -1;
        if (this.executorService != null) {
            CommandStatus status = execute(new String[] { "iptables-save" });
            iptablesSave((ByteArrayOutputStream) status.getOutputStream());
            exitValue = status.getExitStatus().getExitCode();
        } else {
            logger.error(COMMAND_EXECUTOR_SERVICE_MESSAGE);
            throw new IllegalArgumentException(COMMAND_EXECUTOR_SERVICE_MESSAGE);
        }

        logger.debug("iptablesSave() :: completed!, status={}", exitValue);
    }

    private void iptablesSave(ByteArrayOutputStream out) throws KuraProcessExecutionErrorException {
        try (FileOutputStream outFile = new FileOutputStream(FIREWALL_CONFIG_FILE_NAME)) {
            out.writeTo(outFile);
        } catch (IOException e) {
            throw new KuraProcessExecutionErrorException(e, "Failed to write to firewall file");
        }
    }

    private CommandStatus execute(String[] commandLine) throws KuraException {
        Command command = new Command(commandLine);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        command.setErrorStream(err);
        command.setOutputStream(out);
        CommandStatus status = this.executorService.execute(command);
        int exitValue = status.getExitStatus().getExitCode();
        if (exitValue != 0) {
            if (logger.isErrorEnabled()) {
                logger.error("command {} :: failed - {}", command, new String(err.toByteArray(), Charsets.UTF_8));
            }
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to execute the {} command", command);
        }
        return status;
    }

    /*
     * Restores (using iptables-restore) firewall settings from temporary iptables configuration file.
     * Temporary configuration file is deleted upon completion.
     */
    public void restore(String filename) throws KuraException {
        int exitValue = -1;
        try {
            if (this.executorService != null) {
                CommandStatus status = execute(new String[] { "iptables-restore", filename });
                exitValue = status.getExitStatus().getExitCode();
            } else {
                logger.error(COMMAND_EXECUTOR_SERVICE_MESSAGE);
                throw new IllegalArgumentException(COMMAND_EXECUTOR_SERVICE_MESSAGE);
            }
        } finally {
            try {
                File configFile = new File(filename);
                if (Files.deleteIfExists(configFile.toPath())) {
                    logger.debug("restore() :: removing the {} file", filename);
                }
            } catch (IOException e) {
                logger.error("Cannot delete file {}", filename, e);
            }
        }

        logger.debug("iptablesRestore() :: completed!, status={}", exitValue);
    }

    /*
     * Saves current configurations from the m_localRules, m_portForwardRules, m_natRules, and m_autoNatRules
     * into specified temporary file
     */
    public void save(String filename) throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println(FILTER);
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
            writer.println(COMMIT);
            writer.println("*nat");
            if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
                for (PortForwardRule portForwardRule : this.portForwardRules) {
                    writer.println(portForwardRule.getNatPreroutingChainRule());
                    writer.println(portForwardRule.getNatPostroutingChainRule());
                }
            }
            if (this.autoNatRules != null && !this.autoNatRules.isEmpty()) {
                List<NatPostroutingChainRule> appliedNatPostroutingChainRules = new ArrayList<>();
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
            writer.println(COMMIT);
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
                } else if (FILTER.equals(line)) {
                    readingFilterTable = true;
                } else if (COMMIT.equals(line)) {
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
                                logger.debug(
                                        "parseFirewallConfigurationFile() :: Parsed NAT rule with"
                                                + "   sourceInterface: {}   destinationInterface: {}   masquerade: {}"
                                                + "protocol: {}  source network/host: {} destination network/host {}",
                                        sourceInterface, destinationInterface, masquerade, protocol, source,
                                        destination);
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
                            logger.debug(
                                    "parseFirewallConfigurationFile() :: Parsed auto NAT rule with"
                                            + " sourceInterface: {}    destinationInterface: {}   masquerade: {}",
                                    sourceInterface, destinationInterface, masquerade);

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
