/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsmasqTool implements DhcpLinuxTool {

    private static final Logger logger = LoggerFactory.getLogger(DnsmasqTool.class);

    private static final String DNSMASQ_GLOBAL_CONFIG_FILE = "/etc/dnsmasq.d/dnsmasq-globals.conf";
    private static final String GLOBAL_CONFIGURATION = "port=0\nbind-interfaces\n";

    private CommandExecutorService executorService;
    private Map<String, byte[]> configsLastHash = Collections.synchronizedMap(new HashMap<>());

    public DnsmasqTool(CommandExecutorService service) {
        this.executorService = service;
    }

    @Override
    public boolean isRunning(String interfaceName) throws KuraProcessExecutionErrorException {
        CommandStatus status = this.executorService.execute(
                new Command(new String[] { "systemctl", "is-active", "--quiet", DhcpServerTool.DNSMASQ.getValue() }));

        boolean isRunning;
        try {
            isRunning = status.getExitStatus().isSuccessful() && !isConfigFileAlteredOrNonExistent(interfaceName);
        } catch (Exception e) {
            throw new KuraProcessExecutionErrorException(e, "Failed to start DHCP server: " + e.getMessage());
        }
        logger.debug("DNSMASQ - Is dnsmasq running updated for interface {}? {}", interfaceName, isRunning);

        return isRunning;
    }

    @Override
    public CommandStatus startInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        logger.debug("DNSMASQ - starting dnsmasq service for interface {}.", interfaceName);

        try {
            this.configsLastHash.put(interfaceName,
                    sha1(Paths.get(DhcpServerManager.getConfigFilename(interfaceName))));
        } catch (Exception e) {
            throw new KuraProcessExecutionErrorException(e, "Failed to start DHCP server: " + e.getMessage());
        }

        writeGlobalConfig();

        CommandStatus restartStatus = this.executorService.execute(systemctlRestartCommand());

        if (!restartStatus.getExitStatus().isSuccessful()) {
            removeInterfaceConfig(interfaceName);
            restartStatus = this.executorService.execute(systemctlRestartCommand());
        }

        return restartStatus;
    }

    @Override
    public boolean disableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        try {
            boolean isInterfaceDisabled = true;

            if (removeInterfaceConfig(interfaceName)) {
                CommandStatus status = this.executorService.execute(systemctlRestartCommand());
                isInterfaceDisabled = status.getExitStatus().isSuccessful();
            }

            this.configsLastHash.remove(interfaceName);

            logger.debug("DNSMASQ - Disabled dhcp server on interface {}. Success? {}", interfaceName,
                    isInterfaceDisabled);

            return isInterfaceDisabled;
        } catch (Exception e) {
            throw new KuraProcessExecutionErrorException(e, "Failed to disable DHCP server: " + e.getMessage());
        }
    }

    private boolean isConfigFileAlteredOrNonExistent(String interfaceName)
            throws NoSuchAlgorithmException, IOException {

        File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));

        if (!configFile.exists()) {
            return true;
        }

        byte[] currentHash = sha1(Paths.get(DhcpServerManager.getConfigFilename(interfaceName)));

        if (this.configsLastHash.containsKey(interfaceName)) {
            return !Arrays.equals(currentHash, this.configsLastHash.get(interfaceName));
        } else {
            return true;
        }
    }

    private Command systemctlRestartCommand() {
        return new Command(new String[] { "systemctl", "restart", DhcpServerTool.DNSMASQ.getValue() });
    }

    private boolean removeInterfaceConfig(String interfaceName) {
        try {
            File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
            return Files.deleteIfExists(configFile.toPath());
        } catch (Exception e) {
            return false;
        }
    }

    private void writeGlobalConfig() {
        try {
            Path dnsmasqGlobalsPath = Paths.get(DNSMASQ_GLOBAL_CONFIG_FILE);
            if (Files.notExists(dnsmasqGlobalsPath)) {
                Files.write(dnsmasqGlobalsPath, GLOBAL_CONFIGURATION.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.warn("DNSMASQ - Failed setting in DHCP-only mode.", e);
        }
    }

    private byte[] sha1(Path filepath) throws NoSuchAlgorithmException, IOException {

        byte[] fileContent = Files.readAllBytes(filepath);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();

        return digest.digest(fileContent);
    }
}
