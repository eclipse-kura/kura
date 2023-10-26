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
import java.util.Objects;

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

    private String globalConfigFilename = "/etc/dnsmasq.d/dnsmasq-globals.conf";
    private static final String GLOBAL_CONFIGURATION = "port=0\nbind-interfaces\ndhcp-leasefile=/var/lib/dhcp/dnsmasq.leases\n";

    static final Command IS_ACTIVE_COMMAND = new Command(new String[] { "systemctl", "is-active", "--quiet",
            DhcpServerTool.DNSMASQ.getValue() });
    static final Command RESTART_COMMAND = new Command(new String[] { "systemctl", "restart",
            DhcpServerTool.DNSMASQ.getValue() });

    private CommandExecutorService executorService;
    private Map<String, byte[]> configsLastHash = Collections.synchronizedMap(new HashMap<>());
    private byte[] globalConfigHash;

    public DnsmasqTool(CommandExecutorService service) {
        this.executorService = service;
    }

    @Override
    public boolean isRunning(String interfaceName) throws KuraProcessExecutionErrorException {
        CommandStatus status = this.executorService.execute(IS_ACTIVE_COMMAND);

        boolean isRunning;
        try {
            isRunning = status.getExitStatus().isSuccessful() && !isConfigFileAlteredOrNonExistent(interfaceName)
                    && !shouldWriteGlobalConfig();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new KuraProcessExecutionErrorException(e,
                    "Failed to check if DHCP server is running for interface: " + interfaceName);
        }
        logger.debug("Is dnsmasq running updated for interface {}? {}", interfaceName, isRunning);

        return isRunning;
    }

    @Override
    public CommandStatus startInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        logger.debug("Starting dnsmasq service for interface {}.", interfaceName);

        try {
            this.configsLastHash.put(interfaceName,
                    sha1(Paths.get(DhcpServerManager.getConfigFilename(interfaceName))));
            writeGlobalConfig();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new KuraProcessExecutionErrorException(e,
                    "Failed to start DHCP server for interface: " + interfaceName);
        }

        CommandStatus restartStatus = this.executorService.execute(RESTART_COMMAND);

        if (!restartStatus.getExitStatus().isSuccessful()) {
            removeInterfaceConfig(interfaceName);
            restartStatus = this.executorService.execute(RESTART_COMMAND);
        }

        return restartStatus;
    }

    @Override
    public boolean disableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        boolean isInterfaceDisabled = true;

        if (removeInterfaceConfig(interfaceName)) {
            CommandStatus status = this.executorService.execute(RESTART_COMMAND);
            isInterfaceDisabled = status.getExitStatus().isSuccessful();
        }

        this.configsLastHash.remove(interfaceName);

        logger.debug("Disabled DHCP server on interface {}. Success? {}", interfaceName, isInterfaceDisabled);

        if (!isInterfaceDisabled) {
            throw new KuraProcessExecutionErrorException("Failed to stop DHCP server for interface " + interfaceName);
        }

        return isInterfaceDisabled;
    }

    public void setDnsmasqGlobalConfigFile(String globalConfigFilename) {
        this.globalConfigFilename = globalConfigFilename;
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

    private boolean removeInterfaceConfig(String interfaceName) {
        try {
            File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
            return Files.deleteIfExists(configFile.toPath());
        } catch (Exception e) {
            return false;
        }
    }

    private void writeGlobalConfig() throws NoSuchAlgorithmException, IOException {
        Path dnsmasqGlobalsPath = Paths.get(this.globalConfigFilename);

        if (shouldWriteGlobalConfig()) {
            Files.write(dnsmasqGlobalsPath, GLOBAL_CONFIGURATION.getBytes(StandardCharsets.UTF_8));
            this.globalConfigHash = sha1(dnsmasqGlobalsPath);
            logger.debug("Global configuration '{}' written.", dnsmasqGlobalsPath);
        }
    }

    private boolean shouldWriteGlobalConfig() throws NoSuchAlgorithmException, IOException {
        Path dnsmasqGlobalsPath = Paths.get(this.globalConfigFilename);

        if (Objects.isNull(this.globalConfigHash)) {
            return true;
        }

        if (Files.exists(dnsmasqGlobalsPath)) {
            byte[] globalConfigCurrentHash = sha1(dnsmasqGlobalsPath);
            return !Arrays.equals(globalConfigCurrentHash, this.globalConfigHash);
        } else {
            return true;
        }
    }

    private byte[] sha1(Path filepath) throws NoSuchAlgorithmException, IOException {

        byte[] fileContent = Files.readAllBytes(filepath);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();

        return digest.digest(fileContent);
    }
}
