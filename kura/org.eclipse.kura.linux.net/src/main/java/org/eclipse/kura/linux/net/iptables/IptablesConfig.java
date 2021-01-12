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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptablesConfig extends IptablesConfigConstants {

    private static final Logger logger = LoggerFactory.getLogger(IptablesConfig.class);

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
            writer.println(STAR_NAT);
            writer.println(COMMIT);
            writer.println(STAR_FILTER);
            writer.println(COMMIT);

            File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
            if (configFile.exists()) {
                restore(FIREWALL_TMP_CONFIG_FILE_NAME);
            }
        } catch (IOException e) {
            throw new KuraIOException(e, "clear() :: failed to clear all chains");
        }
    }

    /*
     * Apply a minimal configuration
     */
    public void applyBlockPolicy() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println(STAR_NAT);
            writer.println(COMMIT);
            writer.println(STAR_FILTER);
            writer.println(ALLOW_ALL_TRAFFIC_TO_LOOPBACK);
            writer.println(ALLOW_ONLY_INCOMING_TO_OUTGOING);
            writer.println(COMMIT);

            File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
            if (configFile.exists()) {
                restore(FIREWALL_TMP_CONFIG_FILE_NAME);
            }
        } catch (IOException e) {
            throw new KuraIOException(e, "applyBlockPolicy() :: failed to clear all chains");
        }
    }

    /*
     * Clears all Kura chains
     */
    public void clearAllKuraChains() {
        internalFlush(INPUT_KURA_CHAIN, FILTER);
        internalFlush(OUTPUT_KURA_CHAIN, FILTER);
        internalFlush(FORWARD_KURA_CHAIN, FILTER);
        internalFlush(INPUT_KURA_CHAIN, NAT);
        internalFlush(OUTPUT_KURA_CHAIN, NAT);
        internalFlush(PREROUTING_KURA_CHAIN, NAT);
        internalFlush(POSTROUTING_KURA_CHAIN, NAT);
    }

    private void internalFlush(String chain, String table) {
        int exitValue = -1;
        CommandStatus status;
        if (this.executorService != null) {
            status = execute(new String[] { IPTABLES_COMMAND, "-F", chain, "-t", table });
            if (status.getExitStatus().getExitCode() != 0) {
                logger.error("Failed to flush rules from chain {} in table {}", chain, table);
            }
        }

        logger.debug("iptables flush() :: completed!, status={}", exitValue);
    }

    /*
     * Saves (using iptables-save) the current iptables config into /etc/sysconfig/iptables
     */
    public void save() throws KuraException {
        internalSave(null);
    }

    /*
     * Saves rules from the localRules, portForwardRules, natRules, and autoNatRules into the Kura chains in
     * /etc/sysconfig/iptables
     */
    public void saveKuraChains() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
                PrintWriter writer = new PrintWriter(fos)) {
            writer.println(STAR_FILTER);
            writer.println(INPUT_DROP_POLICY);
            writer.println(FORWARD_DROP_POLICY);
            writer.println(OUTPUT_ACCEPT_POLICY);
            writer.println(INPUT_KURA_POLICY);
            writer.println(OUTPUT_KURA_POLICY);
            writer.println(FORWARD_KURA_POLICY);
            writer.println(ADD_INPUT_KURA_CHAIN);
            writer.println(ADD_OUTPUT_KURA_CHAIN);
            writer.println(ADD_FORWARD_KURA_CHAIN);
            saveFilterTable(writer);
            writer.println(COMMIT);
            writer.println(STAR_NAT);
            writer.println(INPUT_ACCEPT_POLICY);
            writer.println(OUTPUT_ACCEPT_POLICY);
            writer.println(PREROUTING_ACCEPT_POLICY);
            writer.println(POSTROUTING_ACCEPT_POLICY);
            writer.println(PREROUTING_KURA_POLICY);
            writer.println(POSTROUTING_KURA_POLICY);
            writer.println(INPUT_KURA_POLICY);
            writer.println(OUTPUT_KURA_POLICY);
            writer.println(ADD_PREROUTING_KURA_CHAIN);
            writer.println(ADD_POSTROUTING_KURA_CHAIN);
            writer.println(ADD_INPUT_KURA_CHAIN);
            writer.println(ADD_OUTPUT_KURA_CHAIN);
            saveNatTable(writer);
            writer.println(COMMIT);
        } catch (IOException e) {
            throw new KuraIOException(e, "save() :: failed to create rules file");
        }

        File file = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
        if (file.exists()) {
            try {
                Files.move(file.toPath(), new File(FIREWALL_CONFIG_FILE_NAME).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new KuraIOException("save() :: failed to save rules on file");
            }
        }

    }

    private void internalSave(String path) {
        int exitValue = -1;
        CommandStatus status;
        if (this.executorService != null) {
            if (path == null) {
                path = FIREWALL_CONFIG_FILE_NAME;
            }
            status = execute(new String[] { "iptables-save", ">", path });
            exitValue = status.getExitStatus().getExitCode();
        } else {
            logger.error(COMMAND_EXECUTOR_SERVICE_MESSAGE);
            throw new IllegalArgumentException(COMMAND_EXECUTOR_SERVICE_MESSAGE);
        }

        logger.debug("iptablesSave() :: completed!, status={}", exitValue);
    }

    private CommandStatus execute(String[] commandLine) {
        Command command = new Command(commandLine);
        command.setExecuteInAShell(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        command.setErrorStream(err);
        command.setOutputStream(out);
        CommandStatus status = this.executorService.execute(command);
        if (logger.isDebugEnabled()) {
            logger.debug("execute command {} :: exited with code - {}", command, status.getExitStatus().getExitCode());
            logger.debug("execute stderr {}", new String(err.toByteArray(), Charsets.UTF_8));
            logger.debug("execute stdout {}", new String(out.toByteArray(), Charsets.UTF_8));
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
     * Saves current configurations from the localRules, portForwardRules, natRules, and autoNatRules
     * into specified temporary file
     */
    public void save(String filename) throws KuraException {
        internalSave(filename);
    }

    private void saveFilterTable(PrintWriter writer) {
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
        writeLocalRulesToFilterTable(writer);
        writePortForwardRulesToFilterTable(writer);
        writeAutoNatRulesToFilterTable(writer);
        writeNatRulesToFilterTable(writer);
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
        writer.println(RETURN_FORWARD_KURA_CHAIN);
    }

    private void writeNatRulesToFilterTable(PrintWriter writer) {
        if (this.natRules != null && !this.natRules.isEmpty()) {
            for (NATRule natRule : this.natRules) {
                List<String> filterForwardChainRules = natRule.getFilterForwardChainRule().toStrings();
                if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                    filterForwardChainRules.stream().forEach(filterForwardChainRule -> {
                        if (writer == null) {
                            execute((IPTABLES_COMMAND + " " + filterForwardChainRule).split(" "));
                        } else {
                            writer.println(filterForwardChainRule);
                        }
                    });
                }
            }
        }
    }

    private void writeLocalRulesToFilterTable(PrintWriter writer) {
        if (this.localRules != null && !this.localRules.isEmpty()) {
            for (LocalRule lr : this.localRules) {
                if (writer == null) {
                    execute((IPTABLES_COMMAND + " " + lr).split(" "));
                } else {
                    writer.println(lr);
                }
            }
        }
    }

    private void writeAutoNatRulesToFilterTable(PrintWriter writer) {
        if (this.autoNatRules != null && !this.autoNatRules.isEmpty()) {
            for (NATRule autoNatRule : this.autoNatRules) {
                List<String> filterForwardChainRules = autoNatRule.getFilterForwardChainRule().toStrings();
                if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                    filterForwardChainRules.stream().forEach(filterForwardChainRule -> {
                        if (writer == null) {
                            execute((IPTABLES_COMMAND + " " + filterForwardChainRule).split(" "));
                        } else {
                            writer.println(filterForwardChainRule);
                        }
                    });
                }
            }
        }
    }

    private void writePortForwardRulesToFilterTable(PrintWriter writer) {
        if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
            for (PortForwardRule portForwardRule : this.portForwardRules) {
                List<String> filterForwardChainRules = portForwardRule.getFilterForwardChainRule().toStrings();
                if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                    filterForwardChainRules.stream().forEach(filterForwardChainRule -> {
                        if (writer == null) {
                            execute((IPTABLES_COMMAND + " " + filterForwardChainRule).split(" "));
                        } else {
                            writer.println(filterForwardChainRule);
                        }
                    });
                }
            }
        }
    }

    private void saveNatTable(PrintWriter writer) {
        writePortForwardRulesToNatTable(writer);
        writeAutoNatRulesToNatTable(writer);
        writeNatRulesToNatTable(writer);
        writer.println(RETURN_POSTROUTING_KURA_CHAIN);
        writer.println(RETURN_PREROUTING_KURA_CHAIN);
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
    }

    private void writeNatRulesToNatTable(PrintWriter writer) {
        if (this.natRules != null && !this.natRules.isEmpty()) {
            for (NATRule natRule : this.natRules) {
                if (writer == null) {
                    execute((IPTABLES_COMMAND + " -t " + NAT + natRule.getNatPostroutingChainRule()).split(" "));
                } else {
                    writer.println(natRule.getNatPostroutingChainRule());
                }
            }
        }
    }

    private void writeAutoNatRulesToNatTable(PrintWriter writer) {
        if (this.autoNatRules != null && !this.autoNatRules.isEmpty()) {
            List<NatPostroutingChainRule> appliedNatPostroutingChainRules = new ArrayList<>();
            for (NATRule autoNatRule : this.autoNatRules) {
                boolean found = false;
                NatPostroutingChainRule natPostroutingChainRule = autoNatRule.getNatPostroutingChainRule();

                found = appliedNatPostroutingChainRules.stream()
                        .filter(appliedNatPostroutingChainRule -> appliedNatPostroutingChainRule
                                .equals(natPostroutingChainRule))
                        .count() > 0;
                if (!found) {
                    if (writer == null) {
                        execute((IPTABLES_COMMAND + " -t " + NAT + autoNatRule.getNatPostroutingChainRule())
                                .split(" "));
                    } else {
                        writer.println(autoNatRule.getNatPostroutingChainRule());
                    }
                    appliedNatPostroutingChainRules.add(natPostroutingChainRule);
                }
            }
        }
    }

    private void writePortForwardRulesToNatTable(PrintWriter writer) {
        if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
            for (PortForwardRule portForwardRule : this.portForwardRules) {
                if (writer == null) {
                    execute((IPTABLES_COMMAND + " -t " + NAT + portForwardRule.getNatPreroutingChainRule()).split(" "));
                    execute((IPTABLES_COMMAND + " -t " + NAT + portForwardRule.getNatPostroutingChainRule())
                            .split(" "));
                } else {
                    writer.println(portForwardRule.getNatPreroutingChainRule());
                    writer.println(portForwardRule.getNatPostroutingChainRule());
                }
            }
        }
    }

    /*
     * Populates the localRules, portForwardRules, natRules, and autoNatRules by parsing
     * the iptables configuration file. Only Kura chains are used.
     */
    public void restore() throws KuraException {
        try (FileReader fr = new FileReader(FIREWALL_CONFIG_FILE_NAME); BufferedReader br = new BufferedReader(fr)) {
            List<NatPreroutingChainRule> natPreroutingChain = new ArrayList<>();
            List<NatPostroutingChainRule> natPostroutingChain = new ArrayList<>();
            List<FilterForwardChainRule> filterForwardChain = new ArrayList<>();

            String line = null;
            boolean readingNatTable = false;
            boolean readingFilterTable = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // skip any predefined lines or comment lines
                if ("".equals(line) || line.startsWith("#") || line.startsWith(":") || line.contains(RETURN)) {
                    continue;
                }
                if (STAR_NAT.equals(line)) {
                    readingNatTable = true;
                } else if (STAR_FILTER.equals(line)) {
                    readingFilterTable = true;
                } else if (COMMIT.equals(line)) {
                    if (readingNatTable) {
                        readingNatTable = false;
                    }
                    if (readingFilterTable) {
                        readingFilterTable = false;
                    }
                } else if (readingNatTable && line.startsWith("-A prerouting-kura")) {
                    natPreroutingChain.add(new NatPreroutingChainRule(line));
                } else if (readingNatTable && line.startsWith("-A postrouting-kura")) {
                    natPostroutingChain.add(new NatPostroutingChainRule(line));
                } else if (readingFilterTable && line.startsWith("-A forward-kura")) {
                    filterForwardChain.add(new FilterForwardChainRule(line));
                } else if (readingFilterTable && line.startsWith("-A input-kura")) {
                    if (ALLOW_ALL_TRAFFIC_TO_LOOPBACK.equals(line)) {
                        continue;
                    }
                    if (ALLOW_ONLY_INCOMING_TO_OUTGOING.equals(line)) {
                        continue;
                    }
                    final String lineFinal = line;
                    String match = Arrays.stream(ALLOW_ICMP).filter(s -> s.equals(lineFinal)).findFirst().orElse("");
                    if (match != null && !match.isEmpty()) {
                        this.allowIcmp = true;
                        continue;
                    }
                    match = Arrays.stream(DO_NOT_ALLOW_ICMP).filter(s -> s.equals(lineFinal)).findFirst().orElse("");
                    if (match != null && !match.isEmpty()) {
                        this.allowIcmp = false;
                        continue;
                    }
                    readLocalRule(line);
                }
            }

            // ! done parsing !
            parsePortForwardingRules(natPreroutingChain, natPostroutingChain);

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
        } catch (IOException e) {
            throw new KuraIOException(e, "restore() :: failed to read configuration file");
        }
    }

    private void readLocalRule(String line) {
        try {
            LocalRule localRule = new LocalRule(line);
            logger.debug("parseFirewallConfigurationFile() :: Adding local rule: {}", localRule);
            this.localRules.add(localRule);
        } catch (KuraException e) {
            logger.error("Failed to parse Local Rule: {} ", line, e);
        }
    }

    private void parsePortForwardingRules(List<NatPreroutingChainRule> natPreroutingChain,
            List<NatPostroutingChainRule> natPostroutingChain) {
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
                StringBuilder sbSport = new StringBuilder().append(natPreroutingChainRule.getSrcPortFirst()).append(':')
                        .append(natPreroutingChainRule.getSrcPortLast());
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
                    protocol, inPort, outPort, masquerade, permittedNetwork, permittedNetworkMask, permittedMac, sport);
            logger.debug("Adding port forward rule: {}", portForwardRule);
            this.portForwardRules.add(portForwardRule);
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

    /*
     * Applies the rules contained in the localRules, portForwardRules, natRules, and autoNatRules
     * and force the polices for input and forward chains
     */
    public void applyRules() {
        applyPolicies();
        createKuraChains();
        writeLocalRulesToFilterTable(null);
        writePortForwardRulesToFilterTable(null);
        writeAutoNatRulesToFilterTable(null);
        writeNatRulesToFilterTable(null);
        writePortForwardRulesToNatTable(null);
        writeAutoNatRulesToNatTable(null);
        writeNatRulesToNatTable(null);
        createKuraChainsReturnRules();
    }

    private void applyPolicies() {
        if (execute(IPTABLES_INPUT_DROP_POLICY).getExitStatus().getExitCode() != 0) {
            logger.error("Failed to apply policy to chain INPUT");
        }
        if (execute(IPTABLES_FORWARD_DROP_POLICY).getExitStatus().getExitCode() != 0) {
            logger.error("Failed to apply policy to chain FORWARD");
        }
    }

    private void createKuraChains() {
        execute(IPTABLES_CREATE_INPUT_KURA_CHAIN);
        String rule = IPTABLES_COMMAND + " " + ADD_INPUT_KURA_CHAIN + " -t " + FILTER;
        if (execute(IPTABLES_CHECK_INPUT_KURA_CHAIN).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_OUTPUT_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_OUTPUT_KURA_CHAIN + " -t " + FILTER;
        if (execute(IPTABLES_CHECK_OUTPUT_KURA_CHAIN).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_FORWARD_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_FORWARD_KURA_CHAIN + " -t " + FILTER;
        if (execute(IPTABLES_CHECK_FORWARD_KURA_CHAIN).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_INPUT_KURA_CHAIN_NAT);
        rule = IPTABLES_COMMAND + " " + ADD_INPUT_KURA_CHAIN + " -t " + NAT;
        if (execute(IPTABLES_CHECK_INPUT_KURA_CHAIN_NAT).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_OUTPUT_KURA_CHAIN_NAT);
        rule = IPTABLES_COMMAND + " " + ADD_OUTPUT_KURA_CHAIN + " -t " + NAT;
        if (execute(IPTABLES_CHECK_OUTPUT_KURA_CHAIN_NAT).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_PREROUTING_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_PREROUTING_KURA_CHAIN + " -t " + NAT;
        if (execute(IPTABLES_CHECK_PREROUTING_KURA_CHAIN).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_POSTROUTING_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_POSTROUTING_KURA_CHAIN + " -t " + NAT;
        if (execute(IPTABLES_CHECK_POSTROUTING_KURA_CHAIN).getExitStatus().getExitCode() != 0
                && execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }
    }

    private void createKuraChainsReturnRules() {
        if (execute((IPTABLES_COMMAND + " " + RETURN_INPUT_KURA_CHAIN).split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        if (execute((IPTABLES_COMMAND + " " + RETURN_OUTPUT_KURA_CHAIN).split(" ")).getExitStatus()
                .getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        if (execute((IPTABLES_COMMAND + " " + RETURN_FORWARD_KURA_CHAIN).split(" ")).getExitStatus()
                .getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        String rule = IPTABLES_COMMAND + " " + RETURN_INPUT_KURA_CHAIN + " -t " + NAT;
        if (execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_OUTPUT_KURA_CHAIN + " -t " + NAT;
        if (execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_PREROUTING_KURA_CHAIN + " -t " + NAT;
        if (execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_POSTROUTING_KURA_CHAIN + " -t " + NAT;
        if (execute(rule.split(" ")).getExitStatus().getExitCode() != 0) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
    }
}
