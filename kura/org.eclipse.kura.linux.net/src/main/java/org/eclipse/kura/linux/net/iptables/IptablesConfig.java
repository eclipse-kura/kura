/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptablesConfig extends IptablesConfigConstants {

    private static final Logger logger = LoggerFactory.getLogger(IptablesConfig.class);

    private Set<LocalRule> localRules;
    private Set<PortForwardRule> portForwardRules;
    private Set<NATRule> autoNatRules;
    private Set<NATRule> natRules;
    private boolean allowIcmp;
    private Set<String> additionalFilterRules;
    private Set<String> additionalNatRules;
    private Set<String> additionalMangleRules;
    private CommandExecutorService executorService;

    public IptablesConfig() {
        this.localRules = new LinkedHashSet<>();
        this.portForwardRules = new LinkedHashSet<>();
        this.autoNatRules = new LinkedHashSet<>();
        this.natRules = new LinkedHashSet<>();
        this.additionalFilterRules = new LinkedHashSet<>();
        this.additionalNatRules = new LinkedHashSet<>();
        this.additionalMangleRules = new LinkedHashSet<>();
        this.allowIcmp = true;
    }

    public IptablesConfig(CommandExecutorService executorService) {
        this();
        this.executorService = executorService;
    }

    public IptablesConfig(Set<LocalRule> localRules, Set<PortForwardRule> portForwardRules, Set<NATRule> autoNatRules,
            Set<NATRule> natRules, boolean allowIcmp, CommandExecutorService executorService) {
        this.localRules = localRules;
        this.portForwardRules = portForwardRules;
        this.autoNatRules = autoNatRules;
        this.natRules = natRules;
        this.allowIcmp = allowIcmp;
        this.additionalFilterRules = new LinkedHashSet<>();
        this.additionalNatRules = new LinkedHashSet<>();
        this.additionalMangleRules = new LinkedHashSet<>();
        this.executorService = executorService;
    }

    public Set<LocalRule> getLocalRules() {
        return this.localRules;
    }

    public void setLocalRules(Set<LocalRule> localRules) {
        this.localRules = localRules;
    }

    public Set<PortForwardRule> getPortForwardRules() {
        return this.portForwardRules;
    }

    public void setPortForwardRules(Set<PortForwardRule> portForwardRules) {
        this.portForwardRules = portForwardRules;
    }

    public Set<NATRule> getAutoNatRules() {
        return this.autoNatRules;
    }

    public void setAutoNatRules(Set<NATRule> autoNatRules) {
        this.autoNatRules = autoNatRules;
    }

    public Set<NATRule> getNatRules() {
        return this.natRules;
    }

    public void setNatRules(Set<NATRule> natRules) {
        this.natRules = natRules;
    }

    public void setAllowIcmp(boolean allowIcmp) {
        this.allowIcmp = allowIcmp;
    }

    public boolean allowIcmp() {
        return this.allowIcmp;
    }

    public Set<String> getAdditionalFilterRules() {
        return additionalFilterRules;
    }

    public void setAdditionalFilterRules(Set<String> additionalFilterRules) {
        this.additionalFilterRules = additionalFilterRules;
    }

    public Set<String> getAdditionalNatRules() {
        return additionalNatRules;
    }

    public void setAdditionalNatRules(Set<String> additionalNatRules) {
        this.additionalNatRules = additionalNatRules;
    }

    public Set<String> getAdditionalMangleRules() {
        return additionalMangleRules;
    }

    public void setAdditionalMangleRules(Set<String> additionalMangleRules) {
        this.additionalMangleRules = additionalMangleRules;
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
            writer.println(STAR_MANGLE);
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
            writer.println(STAR_MANGLE);
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
        internalFlush(FORWARD_KURA_PF_CHAIN, FILTER);
        internalFlush(FORWARD_KURA_IPF_CHAIN, FILTER);
        internalFlush(INPUT_KURA_CHAIN, NAT);
        internalFlush(OUTPUT_KURA_CHAIN, NAT);
        internalFlush(PREROUTING_KURA_CHAIN, NAT);
        internalFlush(PREROUTING_KURA_PF_CHAIN, NAT);
        internalFlush(POSTROUTING_KURA_CHAIN, NAT);
        internalFlush(POSTROUTING_KURA_PF_CHAIN, NAT);
        internalFlush(POSTROUTING_KURA_IPF_CHAIN, NAT);
        internalFlush(INPUT_KURA_CHAIN, MANGLE);
        internalFlush(OUTPUT_KURA_CHAIN, MANGLE);
        internalFlush(FORWARD_KURA_CHAIN, MANGLE);
        internalFlush(PREROUTING_KURA_CHAIN, MANGLE);
        internalFlush(POSTROUTING_KURA_CHAIN, MANGLE);
    }

    private void internalFlush(String chain, String table) {
        CommandStatus status;
        if (this.executorService != null) {
            status = execute(new String[] { IPTABLES_COMMAND, "-F", chain, "-t", table });
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to flush rules from chain {} in table {}", chain, table);
            }
        }
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
            writer.println(FORWARD_KURA_PF_POLICY);
            writer.println(FORWARD_KURA_IPF_POLICY);
            writer.println(ADD_INPUT_KURA_CHAIN);
            writer.println(ADD_OUTPUT_KURA_CHAIN);
            writer.println(ADD_FORWARD_KURA_CHAIN);
            writer.println(ADD_FORWARD_KURA_PF_CHAIN);
            writer.println(ADD_FORWARD_KURA_IPF_CHAIN);
            saveFilterTable(writer);
            writer.println(COMMIT);
            writer.println(STAR_NAT);
            writer.println(INPUT_ACCEPT_POLICY);
            writer.println(OUTPUT_ACCEPT_POLICY);
            writer.println(PREROUTING_ACCEPT_POLICY);
            writer.println(POSTROUTING_ACCEPT_POLICY);
            writer.println(PREROUTING_KURA_POLICY);
            writer.println(PREROUTING_KURA_PF_POLICY);
            writer.println(POSTROUTING_KURA_POLICY);
            writer.println(POSTROUTING_KURA_PF_POLICY);
            writer.println(POSTROUTING_KURA_IPF_POLICY);
            writer.println(INPUT_KURA_POLICY);
            writer.println(OUTPUT_KURA_POLICY);
            writer.println(ADD_PREROUTING_KURA_CHAIN);
            writer.println(ADD_PREROUTING_KURA_PF_CHAIN);
            writer.println(ADD_POSTROUTING_KURA_CHAIN);
            writer.println(ADD_POSTROUTING_KURA_PF_CHAIN);
            writer.println(ADD_POSTROUTING_KURA_IPF_CHAIN);
            writer.println(ADD_INPUT_KURA_CHAIN);
            writer.println(ADD_OUTPUT_KURA_CHAIN);
            saveNatTable(writer);
            writer.println(COMMIT);
            writer.println(STAR_MANGLE);
            writer.println(INPUT_ACCEPT_POLICY);
            writer.println(OUTPUT_ACCEPT_POLICY);
            writer.println(FORWARD_ACCEPT_POLICY);
            writer.println(PREROUTING_ACCEPT_POLICY);
            writer.println(POSTROUTING_ACCEPT_POLICY);
            writer.println(PREROUTING_KURA_POLICY);
            writer.println(POSTROUTING_KURA_POLICY);
            writer.println(INPUT_KURA_POLICY);
            writer.println(OUTPUT_KURA_POLICY);
            writer.println(FORWARD_KURA_POLICY);
            writer.println(ADD_PREROUTING_KURA_CHAIN);
            writer.println(ADD_POSTROUTING_KURA_CHAIN);
            writer.println(ADD_INPUT_KURA_CHAIN);
            writer.println(ADD_OUTPUT_KURA_CHAIN);
            writer.println(ADD_FORWARD_KURA_CHAIN);
            saveMangleTable(writer);
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
        CommandStatus status;
        if (this.executorService != null) {
            if (path == null) {
                path = FIREWALL_CONFIG_FILE_NAME;
            }
            status = execute(new String[] { "iptables-save", ">", path });
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to save rules in {}", path);
            }
        } else {
            logger.error(COMMAND_EXECUTOR_SERVICE_MESSAGE);
            throw new IllegalArgumentException(COMMAND_EXECUTOR_SERVICE_MESSAGE);
        }
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
        try {
            if (this.executorService != null) {
                CommandStatus status = execute(new String[] { "iptables-restore", filename });
                if (!status.getExitStatus().isSuccessful()) {
                    logger.error("Failed to restore rules from {}", filename);
                }
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
        writeAdditionalRulesToFilterTable(writer);
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
        writer.println(RETURN_FORWARD_KURA_CHAIN);
        writer.println(RETURN_FORWARD_KURA_PF_CHAIN);
        writer.println(RETURN_FORWARD_KURA_IPF_CHAIN);
    }

    private void writeNatRulesToFilterTable(PrintWriter writer) {
        if (this.natRules != null && !this.natRules.isEmpty()) {
            this.natRules.stream().forEach(natRule -> {
                List<String> filterForwardChainRules = natRule.getFilterForwardChainRule().toStrings();
                if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                    filterForwardChainRules.stream()
                            .forEach(filterForwardChainRule -> writeNatRulesInternal(writer, filterForwardChainRule));
                }
            });
        }
    }

    private void writeNatRulesInternal(PrintWriter writer, String filterForwardChainRule) {
        if (writer == null) {
            CommandStatus status = execute((IPTABLES_COMMAND + " " + filterForwardChainRule).split(" "));
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to apply forward rules to filter table");
            }
        } else {
            writer.println(filterForwardChainRule);
        }
    }

    private void writeLocalRulesToFilterTable(PrintWriter writer) {
        if (this.localRules != null && !this.localRules.isEmpty()) {
            for (LocalRule lr : this.localRules) {
                if (writer == null) {
                    CommandStatus status = execute((IPTABLES_COMMAND + " " + lr).split(" "));
                    if (!status.getExitStatus().isSuccessful()) {
                        logger.error("Failed to apply local rules to filter table");
                    }
                } else {
                    writer.println(lr);
                }
            }
        }
    }

    private void writeAutoNatRulesToFilterTable(PrintWriter writer) {
        if (this.autoNatRules != null && !this.autoNatRules.isEmpty()) {
            this.autoNatRules.stream().forEach(autoNatRule -> {
                List<String> filterForwardChainRules = autoNatRule.getFilterForwardChainRule().toStrings();
                if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                    filterForwardChainRules.stream().forEach(
                            filterForwardChainRule -> writeAutoNatRulesInternal(writer, filterForwardChainRule));
                }
            });
        }
    }

    private void writeAutoNatRulesInternal(PrintWriter writer, String filterForwardChainRule) {
        if (writer == null) {
            CommandStatus status = execute((IPTABLES_COMMAND + " " + filterForwardChainRule).split(" "));
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to apply auto nat rules");
            }
        } else {
            writer.println(filterForwardChainRule);
        }
    }

    private void writePortForwardRulesToFilterTable(PrintWriter writer) {
        if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
            this.portForwardRules.stream().forEach(portForwardRule -> {
                List<String> filterForwardChainRules = portForwardRule.getFilterForwardChainRule().toStrings();
                if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                    filterForwardChainRules.stream().forEach(
                            filterForwardChainRule -> writeForwardRulesInternal(writer, filterForwardChainRule));
                }
            });
        }
    }

    private void writeForwardRulesInternal(PrintWriter writer, String filterForwardChainRule) {
        if (writer == null) {
            CommandStatus status = execute((IPTABLES_COMMAND + " " + filterForwardChainRule).split(" "));
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to apply forward rules");
            }
        } else {
            writer.println(filterForwardChainRule);
        }
    }

    private void writeAdditionalRulesToFilterTable(PrintWriter writer) {
        for (String filterRule : this.additionalFilterRules) {
            if (writer == null) {
                CommandStatus status = execute((IPTABLES_COMMAND + " -t " + FILTER + " " + filterRule).split(" "));
                if (!status.getExitStatus().isSuccessful()) {
                    logger.error("Failed to apply additional rules to filter table");
                }
            } else {
                writer.println(filterRule);
            }
        }
    }

    private void saveNatTable(PrintWriter writer) {
        writePortForwardRulesToNatTable(writer);
        writeAutoNatRulesToNatTable(writer);
        writeNatRulesToNatTable(writer);
        writeAdditionalRulesToNatTable(writer);
        writer.println(RETURN_POSTROUTING_KURA_CHAIN);
        writer.println(RETURN_POSTROUTING_KURA_PF_CHAIN);
        writer.println(RETURN_POSTROUTING_KURA_IPF_CHAIN);
        writer.println(RETURN_PREROUTING_KURA_CHAIN);
        writer.println(RETURN_PREROUTING_KURA_PF_CHAIN);
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
    }

    private void writeNatRulesToNatTable(PrintWriter writer) {
        if (this.natRules != null && !this.natRules.isEmpty()) {
            this.natRules.stream().forEach(natRule -> writePostroutingNatRulesInternal(writer, natRule));
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
                    writePostroutingNatRulesInternal(writer, autoNatRule);
                    appliedNatPostroutingChainRules.add(natPostroutingChainRule);
                }
            }
        }
    }

    private void writePostroutingNatRulesInternal(PrintWriter writer, NATRule autoNatRule) {
        if (writer == null) {
            CommandStatus status = execute(
                    (IPTABLES_COMMAND + " -t " + NAT + " " + autoNatRule.getNatPostroutingChainRule()).split(" "));
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to apply postrouting rules to nat table");
            }
        } else {
            writer.println(autoNatRule.getNatPostroutingChainRule());
        }
    }

    private void writePortForwardRulesToNatTable(PrintWriter writer) {
        if (this.portForwardRules != null && !this.portForwardRules.isEmpty()) {
            this.portForwardRules.stream().forEach(portForwardRule -> {
                if (writer == null) {
                    CommandStatus statusPre = execute(
                            (IPTABLES_COMMAND + " -t " + NAT + " " + portForwardRule.getNatPreroutingChainRule())
                                    .split(" "));
                    CommandStatus statusPost = execute(
                            (IPTABLES_COMMAND + " -t " + NAT + " " + portForwardRule.getNatPostroutingChainRule())
                                    .split(" "));
                    if (!statusPre.getExitStatus().isSuccessful() || !statusPost.getExitStatus().isSuccessful()) {
                        logger.error("Failed to apply pre/postrouting rules to nat table");
                    }
                } else {
                    writer.println(portForwardRule.getNatPreroutingChainRule());
                    writer.println(portForwardRule.getNatPostroutingChainRule());
                }
            });
        }
    }

    private void writeAdditionalRulesToNatTable(PrintWriter writer) {
        for (String natRule : this.additionalNatRules) {
            if (writer == null) {
                CommandStatus status = execute((IPTABLES_COMMAND + " -t " + NAT + " " + natRule).split(" "));
                if (!status.getExitStatus().isSuccessful()) {
                    logger.error("Failed to apply additional rules to nat table");
                }
            } else {
                writer.println(natRule);
            }
        }
    }

    private void saveMangleTable(PrintWriter writer) {
        writeAdditionalRulesToMangleTable(writer);
        writer.println(RETURN_POSTROUTING_KURA_CHAIN);
        writer.println(RETURN_PREROUTING_KURA_CHAIN);
        writer.println(RETURN_INPUT_KURA_CHAIN);
        writer.println(RETURN_OUTPUT_KURA_CHAIN);
        writer.println(RETURN_FORWARD_KURA_CHAIN);
    }

    private void writeAdditionalRulesToMangleTable(PrintWriter writer) {
        for (String mangleRule : this.additionalMangleRules) {
            if (writer == null) {
                CommandStatus status = execute((IPTABLES_COMMAND + " -t " + MANGLE + " " + mangleRule).split(" "));
                if (!status.getExitStatus().isSuccessful()) {
                    logger.error("Failed to apply prerouting rules to mangle table");
                }
            } else {
                writer.println(mangleRule);
            }
        }
    }

    /*
     * Populates the localRules, portForwardRules, natRules, and autoNatRules by parsing
     * the iptables configuration file. Only Kura chains are used.
     */
    public void restore() throws KuraException {
        List<NatPreroutingChainRule> natPreroutingChain = new ArrayList<>();
        List<NatPostroutingChainRule> natPostroutingChain = new ArrayList<>();
        List<FilterForwardChainRule> filterForwardChain = new ArrayList<>();
        try (FileReader fr = new FileReader(FIREWALL_CONFIG_FILE_NAME); BufferedReader br = new BufferedReader(fr)) {
            parseIptablesRules(natPreroutingChain, natPostroutingChain, filterForwardChain, br);
            // ! done parsing !
            parsePortForwardingRules(natPreroutingChain, natPostroutingChain);
            parseIpForwardingRules(natPreroutingChain, natPostroutingChain, filterForwardChain);
        } catch (IOException e) {
            throw new KuraIOException(e, "restore() :: failed to read configuration file");
        }
    }

    private void parseIptablesRules(List<NatPreroutingChainRule> natPreroutingChain,
            List<NatPostroutingChainRule> natPostroutingChain, List<FilterForwardChainRule> filterForwardChain,
            BufferedReader br) throws IOException, KuraException {
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
            } else if (COMMIT.equals(line) && readingNatTable) {
                readingNatTable = false;
            } else if (COMMIT.equals(line) && readingFilterTable) {
                readingFilterTable = false;
            } else if (readingNatTable) {
                parseNatTable(line, natPreroutingChain, natPostroutingChain);
            } else if (readingFilterTable) {
                parseFilterTable(line, filterForwardChain);
            }
        }
    }

    private void parseNatTable(String line, List<NatPreroutingChainRule> natPreroutingChain,
            List<NatPostroutingChainRule> natPostroutingChain) throws KuraException {
        if (line.startsWith("-A prerouting-kura") && !line.startsWith("-A prerouting-kura -j prerouting-kura-")) {
            natPreroutingChain.add(new NatPreroutingChainRule(line));
        } else if (line.startsWith("-A postrouting-kura")
                && !line.startsWith("-A postrouting-kura -j postrouting-kura-")) {
            natPostroutingChain.add(new NatPostroutingChainRule(line));
        }
    }

    private void parseFilterTable(String line, List<FilterForwardChainRule> filterForwardChain) throws KuraException {
        if (line.startsWith("-A forward-kura") && !line.startsWith("-A forward-kura -j forward-kura-")) {
            filterForwardChain.add(new FilterForwardChainRule(line));
        } else if (line.startsWith("-A input-kura")) {
            readInputChain(line);
        }
    }

    private void readInputChain(String line) {
        if (ALLOW_ALL_TRAFFIC_TO_LOOPBACK.equals(line)) {
            return;
        }
        if (ALLOW_ONLY_INCOMING_TO_OUTGOING.equals(line)) {
            return;
        }
        // Ignore flooding protection rules
        if (line.contains("connlimit") || line.contains("tcp-flags") || line.contains("conntrack")) {
            return;
        }
        final String lineFinal = line;
        String match = Arrays.stream(ALLOW_ICMP).filter(s -> s.equals(lineFinal)).findFirst().orElse("");
        if (match != null && !match.isEmpty()) {
            this.allowIcmp = true;
            return;
        }
        match = Arrays.stream(DO_NOT_ALLOW_ICMP).filter(s -> s.equals(lineFinal)).findFirst().orElse("");
        if (match != null && !match.isEmpty()) {
            this.allowIcmp = false;
            return;
        }
        readLocalRule(line);
    }

    private void parseIpForwardingRules(List<NatPreroutingChainRule> natPreroutingChain,
            List<NatPostroutingChainRule> natPostroutingChain, List<FilterForwardChainRule> filterForwardChain) {
        natPostroutingChain.stream()
                .filter(natPostroutingChainRule -> natPostroutingChainRule.getType() != RuleType.PORT_FORWARDING)
                .forEach(natPostroutingChainRule -> {
                    String destinationInterface = natPostroutingChainRule.getDstInterface();
                    boolean masquerade = natPostroutingChainRule.isMasquerade();
                    String protocol = natPostroutingChainRule.getProtocol();
                    if (protocol != null) {
                        // found NAT rule, ... maybe
                        parseNatRule(natPreroutingChain, filterForwardChain, natPostroutingChainRule,
                                destinationInterface, masquerade, protocol);
                    } else {
                        // found Auto NAT rule ...
                        // match FORWARD rule to find out source interface ...
                        parseAutoNatRule(filterForwardChain, natPostroutingChainRule, destinationInterface, masquerade);
                    }
                });
    }

    private void parseAutoNatRule(List<FilterForwardChainRule> filterForwardChain,
            NatPostroutingChainRule natPostroutingChainRule, String destinationInterface, boolean masquerade) {
        filterForwardChain.stream()
                .filter(filterForwardChainRule -> natPostroutingChainRule.isMatchingForwardChainRule(
                        filterForwardChainRule) && filterForwardChainRule.getType() == RuleType.GENERIC)
                .forEach(filterForwardChainRule -> {
                    String sourceInterface = filterForwardChainRule.getInputInterface();
                    logger.debug(
                            "parseFirewallConfigurationFile() :: Parsed auto NAT rule with"
                                    + " sourceInterface: {}    destinationInterface: {}   masquerade: {}",
                            sourceInterface, destinationInterface, masquerade);

                    NATRule natRule = new NATRule(sourceInterface, destinationInterface, masquerade, RuleType.GENERIC);
                    logger.debug("parseFirewallConfigurationFile() :: Adding auto NAT rule {}", natRule);
                    this.autoNatRules.add(natRule);
                });
    }

    private void parseNatRule(List<NatPreroutingChainRule> natPreroutingChain,
            List<FilterForwardChainRule> filterForwardChain, NatPostroutingChainRule natPostroutingChainRule,
            String destinationInterface, boolean masquerade, String protocol) {
        boolean isNATrule = false;
        String source = formatIpAddress(natPostroutingChainRule.getSrcNetwork(), natPostroutingChainRule.getSrcMask());
        String destination = formatIpAddress(natPostroutingChainRule.getDstNetwork(),
                natPostroutingChainRule.getDstMask());
        if (destination == null) {
            isNATrule = true;
        }

        if (!isNATrule) {
            boolean matchFound = false;
            for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
                if (natPreroutingChainRule.getDstIpAddress().equals(natPostroutingChainRule.getDstNetwork())) {
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
            filterForwardChain.stream()
                    .filter(filterForwardChainRule -> natPostroutingChainRule.isMatchingForwardChainRule(
                            filterForwardChainRule) && filterForwardChainRule.getType() == RuleType.IP_FORWARDING)
                    .forEach(filterForwardChainRule -> {
                        String sourceInterface = filterForwardChainRule.getInputInterface();
                        logger.debug(
                                "parseFirewallConfigurationFile() :: Parsed NAT rule with"
                                        + "   sourceInterface: {}   destinationInterface: {}   masquerade: {}"
                                        + "protocol: {}  source network/host: {} destination network/host {}",
                                sourceInterface, destinationInterface, masquerade, protocol, source, destination);
                        NATRule natRule = new NATRule(sourceInterface, destinationInterface, protocol, source,
                                destination, masquerade, RuleType.IP_FORWARDING);
                        logger.debug("parseFirewallConfigurationFile() :: Adding NAT rule {}", natRule);
                        this.natRules.add(natRule);
                    });
        }
    }

    private String formatIpAddress(String address, Short mask) {
        String formatted = null;
        if (address != null) {
            formatted = new StringBuilder().append(address).append('/').append(mask).toString();
        }
        return formatted;
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
        natPreroutingChain.stream()
                .filter(natPreroutingChainRule -> natPreroutingChainRule.getType() == RuleType.PORT_FORWARDING)
                .forEach(natPreroutingChainRule -> {
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
                        if (natPreroutingChainRule.getDstIpAddress().equals(natPostroutingChainRule.getDstNetwork())
                                && natPostroutingChainRule.getType() == RuleType.PORT_FORWARDING) {
                            outboundIfaceName = natPostroutingChainRule.getDstInterface();
                            if (natPostroutingChainRule.isMasquerade()) {
                                masquerade = true;
                            }
                        }
                    }
                    if (permittedNetwork == null) {
                        permittedNetwork = "0.0.0.0";
                    }
                    PortForwardRule portForwardRule = new PortForwardRule().inboundIface(inboundIfaceName)
                            .outboundIface(outboundIfaceName).address(address).protocol(protocol).inPort(inPort)
                            .outPort(outPort).masquerade(masquerade).permittedNetwork(permittedNetwork)
                            .permittedNetworkMask(permittedNetworkMask).permittedMAC(permittedMac)
                            .sourcePortRange(sport);
                    logger.debug("Adding port forward rule: {}", portForwardRule);
                    this.portForwardRules.add(portForwardRule);
                });
    }

    /*
     * Applies the rules contained in the localRules, portForwardRules, natRules, and autoNatRules,
     * force the polices for input and forward chains and apply flooding protection rules if needed.
     */
    public void applyRules() {
        applyPolicies();
        createKuraChains();
        applyLoopbackRules();
        applyIncomingToOutcomingRules();
        applyIcmpRules();
        writeLocalRulesToFilterTable(null);
        writePortForwardRulesToFilterTable(null);
        writeAutoNatRulesToFilterTable(null);
        writeNatRulesToFilterTable(null);
        writePortForwardRulesToNatTable(null);
        writeAutoNatRulesToNatTable(null);
        writeNatRulesToNatTable(null);
        writeAdditionalRulesToFilterTable(null);
        writeAdditionalRulesToNatTable(null);
        writeAdditionalRulesToMangleTable(null);
        createKuraChainsReturnRules();
    }

    private void applyPolicies() {
        if (!execute(IPTABLES_INPUT_DROP_POLICY).getExitStatus().isSuccessful()) {
            logger.error("Failed to apply policy to chain INPUT");
        }
        if (!execute(IPTABLES_FORWARD_DROP_POLICY).getExitStatus().isSuccessful()) {
            logger.error("Failed to apply policy to chain FORWARD");
        }
    }

    private void createKuraChains() {
        createKuraFilterChains();
        createKuraNatChains();
    }

    private void createKuraNatChains() {
        String rule;
        execute(IPTABLES_CREATE_INPUT_KURA_CHAIN_NAT);
        rule = IPTABLES_COMMAND + " " + ADD_INPUT_KURA_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_INPUT_KURA_CHAIN_NAT).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_OUTPUT_KURA_CHAIN_NAT);
        rule = IPTABLES_COMMAND + " " + ADD_OUTPUT_KURA_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_OUTPUT_KURA_CHAIN_NAT).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_PREROUTING_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_PREROUTING_KURA_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_PREROUTING_KURA_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_PREROUTING_KURA_PF_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_PREROUTING_KURA_PF_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_PREROUTING_KURA_PF_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_POSTROUTING_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_POSTROUTING_KURA_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_POSTROUTING_KURA_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_POSTROUTING_KURA_PF_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_POSTROUTING_KURA_PF_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_POSTROUTING_KURA_PF_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_POSTROUTING_KURA_IPF_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_POSTROUTING_KURA_IPF_CHAIN + " -t " + NAT;
        if (!execute(IPTABLES_CHECK_POSTROUTING_KURA_IPF_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }
    }

    private void createKuraFilterChains() {
        execute(IPTABLES_CREATE_INPUT_KURA_CHAIN);
        String rule = IPTABLES_COMMAND + " " + ADD_INPUT_KURA_CHAIN + " -t " + FILTER;
        if (!execute(IPTABLES_CHECK_INPUT_KURA_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_OUTPUT_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_OUTPUT_KURA_CHAIN + " -t " + FILTER;
        if (!execute(IPTABLES_CHECK_OUTPUT_KURA_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_FORWARD_KURA_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_FORWARD_KURA_CHAIN + " -t " + FILTER;
        if (!execute(IPTABLES_CHECK_FORWARD_KURA_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_FORWARD_KURA_PF_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_FORWARD_KURA_PF_CHAIN + " -t " + FILTER;
        if (!execute(IPTABLES_CHECK_FORWARD_KURA_PF_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }

        execute(IPTABLES_CREATE_FORWARD_KURA_IPF_CHAIN);
        rule = IPTABLES_COMMAND + " " + ADD_FORWARD_KURA_IPF_CHAIN + " -t " + FILTER;
        if (!execute(IPTABLES_CHECK_FORWARD_KURA_IPF_CHAIN).getExitStatus().isSuccessful()
                && !execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_CREATION_FAILED_MESSAGE);
        }
    }

    private void applyLoopbackRules() {
        if (!execute((IPTABLES_COMMAND + " " + ALLOW_ALL_TRAFFIC_TO_LOOPBACK + " -t " + FILTER).split(" "))
                .getExitStatus().isSuccessful()) {
            logger.error("Failed to apply rules to loopback interface");
        }
    }

    private void applyIncomingToOutcomingRules() {
        if (!execute((IPTABLES_COMMAND + " " + ALLOW_ONLY_INCOMING_TO_OUTGOING + " -t " + FILTER).split(" "))
                .getExitStatus().isSuccessful()) {
            logger.error("Failed to apply icmp rules");
        }
    }

    private void applyIcmpRules() {
        if (this.allowIcmp) {
            for (String allowIcmpRule : ALLOW_ICMP) {
                if (!execute((IPTABLES_COMMAND + " " + allowIcmpRule + " -t " + FILTER).split(" ")).getExitStatus()
                        .isSuccessful()) {
                    logger.error("Failed to apply {} rule", allowIcmpRule);
                }
            }
        } else {
            for (String doNotAllowIcmpRule : DO_NOT_ALLOW_ICMP) {
                if (!execute((IPTABLES_COMMAND + " " + doNotAllowIcmpRule + " -t " + FILTER).split(" ")).getExitStatus()
                        .isSuccessful()) {
                    logger.error("Failed to apply {} rule", doNotAllowIcmpRule);
                }
            }
        }
    }

    private void createKuraChainsReturnRules() {
        createKuraChainsReturnFilterRules();
        createKuraChainsReturnNatRules();
        createKuraChainsReturnMangleRules();
    }

    private void createKuraChainsReturnMangleRules() {
        String rule;
        rule = IPTABLES_COMMAND + " " + RETURN_INPUT_KURA_CHAIN + " -t " + MANGLE;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_OUTPUT_KURA_CHAIN + " -t " + MANGLE;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_PREROUTING_KURA_CHAIN + " -t " + MANGLE;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_POSTROUTING_KURA_CHAIN + " -t " + MANGLE;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_FORWARD_KURA_CHAIN + " -t " + MANGLE;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
    }

    private void createKuraChainsReturnNatRules() {
        String rule = IPTABLES_COMMAND + " " + RETURN_INPUT_KURA_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_OUTPUT_KURA_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_PREROUTING_KURA_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_PREROUTING_KURA_PF_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_POSTROUTING_KURA_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_POSTROUTING_KURA_PF_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        rule = IPTABLES_COMMAND + " " + RETURN_POSTROUTING_KURA_IPF_CHAIN + " -t " + NAT;
        if (!execute(rule.split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
    }

    private void createKuraChainsReturnFilterRules() {
        if (!execute((IPTABLES_COMMAND + " " + RETURN_INPUT_KURA_CHAIN).split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        if (!execute((IPTABLES_COMMAND + " " + RETURN_OUTPUT_KURA_CHAIN).split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        if (!execute((IPTABLES_COMMAND + " " + RETURN_FORWARD_KURA_CHAIN).split(" ")).getExitStatus().isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        if (!execute((IPTABLES_COMMAND + " " + RETURN_FORWARD_KURA_PF_CHAIN).split(" ")).getExitStatus()
                .isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
        if (!execute((IPTABLES_COMMAND + " " + RETURN_FORWARD_KURA_IPF_CHAIN).split(" ")).getExitStatus()
                .isSuccessful()) {
            logger.error(CHAIN_RETURN_RULE_FAILED_MESSAGE);
        }
    }
}
