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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptablesConfig {

    private static final Logger logger = LoggerFactory.getLogger(IptablesConfig.class);

    public static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
    public static final String FIREWALL_TMP_CONFIG_FILE_NAME = "/tmp/iptables";
    private static final String FILTER = "filter";
    private static final String NAT = "nat";
    private static final String STAR_NAT = "*" + NAT;
    private static final String STAR_FILTER = "*" + FILTER;
    private static final String COMMIT = "COMMIT";
    public static final String INPUT_KURA_CHAIN = "input-kura";
    public static final String OUTPUT_KURA_CHAIN = "output-kura";
    public static final String FORWARD_KURA_CHAIN = "forward-kura";
    public static final String PREROUTING_KURA_CHAIN = "prerouting-kura";
    public static final String POSTROUTING_KURA_CHAIN = "postrouting-kura";
    private static final String RETURN_PREROUTING_KURA_CHAIN = "-A prerouting-kura -j RETURN";
    private static final String RETURN_POSTROUTING_KURA_CHAIN = "-A postrouting-kura -j RETURN";
    private static final String RETURN_INPUT_KURA_CHAIN = "-A input-kura -j RETURN";
    private static final String RETURN_OUTPUT_KURA_CHAIN = "-A output-kura -j RETURN";
    private static final String RETURN_FORWARD_KURA_CHAIN = "-A forward-kura -j RETURN";
    private static final String ALLOW_ALL_TRAFFIC_TO_LOOPBACK = "-A input-kura -i lo -j ACCEPT";
    private static final String ALLOW_ONLY_INCOMING_TO_OUTGOING = "-A input-kura -m state --state RELATED,ESTABLISHED -j ACCEPT";
    private static final String COMMAND_EXECUTOR_SERVICE_MESSAGE = "CommandExecutorService not set.";

    private static final String[] ALLOW_ICMP = {
            "-A input-kura -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT",
            "-A output-kura -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j ACCEPT" };

    private static final String[] DO_NOT_ALLOW_ICMP = {
            "-A input-kura -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j DROP",
            "-A output-kura -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j DROP" };

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
     * Flush (delete) the rules in the Kura custom chains
     */
    public void flush() {
        try {
            internalFlush(INPUT_KURA_CHAIN, FILTER);
            internalFlush(OUTPUT_KURA_CHAIN, FILTER);
            internalFlush(FORWARD_KURA_CHAIN, FILTER);
            internalFlush(INPUT_KURA_CHAIN, NAT);
            internalFlush(OUTPUT_KURA_CHAIN, NAT);
            internalFlush(PREROUTING_KURA_CHAIN, NAT);
            internalFlush(POSTROUTING_KURA_CHAIN, NAT);
        } catch (KuraException e) {
            logger.error("Failed to flush rules", e);
        }
    }

    private void internalFlush(String chain, String table) throws KuraException {
        int exitValue = -1;
        CommandStatus status;
        if (this.executorService != null) {
            status = execute(new String[] { "iptables", "-F", chain, "-t", table });
            exitValue = status.getExitStatus().getExitCode();
        } else {
            throw new IllegalArgumentException(COMMAND_EXECUTOR_SERVICE_MESSAGE);
        }

        logger.debug("iptables flush() :: completed!, status={}", exitValue);
    }

    /*
     * Saves (using iptables-save) the current iptables config into /etc/sysconfig/iptables
     */
    public void save() throws KuraException {
        internalSave(null);
    }

    private void internalSave(String path) throws KuraException {
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

    private CommandStatus execute(String[] commandLine) throws KuraException {
        Command command = new Command(commandLine);
        command.setExecuteInAShell(true);
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
     * Saves current configurations from the localRules, portForwardRules, natRules, and autoNatRules
     * into specified temporary file
     */
    public void save(String filename) throws KuraException {
        internalSave(filename);
        try (Stream<String> lines = Files.lines(Paths.get(filename));
                FileOutputStream fos = new FileOutputStream(filename + "_tmp");
                PrintWriter writer = new PrintWriter(fos)) {
            AtomicBoolean readingNatTable = new AtomicBoolean(false);
            AtomicBoolean readingFilterTable = new AtomicBoolean(false);
            lines.forEach(line -> {
                line = line.trim();
                if (STAR_NAT.equals(line)) {
                    readingNatTable.set(true);
                } else if (STAR_FILTER.equals(line)) {
                    readingFilterTable.set(true);
                } else if (COMMIT.equals(line)) {
                    if (readingNatTable.get()) {
                        readingNatTable.set(false);
                        saveNatTable(writer);
                    }
                    if (readingFilterTable.get()) {
                        readingFilterTable.set(false);
                        saveFilterTable(writer);
                        writer.println(RETURN_FORWARD_KURA_CHAIN);
                    }
                }
                writer.println(line);
            });
        } catch (IOException e) {
            throw new KuraIOException(e, "save() :: failed to save rules on file");
        }

        try {
            File configFile = new File(filename);
            File tmpFile = new File(filename + "_tmp");
            Files.move(tmpFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new KuraIOException(e, "save() :: failed to rename temporary file");
        }
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
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
    }

    private void saveNatTable(PrintWriter writer) {
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
        writer.println(RETURN_POSTROUTING_KURA_CHAIN);
        writer.println(RETURN_PREROUTING_KURA_CHAIN);
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
    }

    /*
     * Populates the localRules, portForwardRules, natRules, and autoNatRules by parsing
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
                if ("".equals(line) || line.startsWith("#") || line.startsWith(":") || line.contains("RETURN")) {
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
}
