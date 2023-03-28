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
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    private CommandExecutorService executorService;
    private Set<String> activeInterfaces = Collections.synchronizedSet(new HashSet<>());

    public DnsmasqTool(CommandExecutorService service) {
        this.executorService = service;
    }

    @Override
    public boolean isRunning(String interfaceName) {
        CommandStatus status = this.executorService.execute(
                new Command(new String[] { "systemctl", "is-active", "--quiet", DhcpServerTool.DNSMASQ.getValue() }));
        boolean isRunning = status.getExitStatus().isSuccessful() && this.activeInterfaces.contains(interfaceName);

        logger.debug("DNSMASQ - Is dnsmasq running for interface {}? {}", interfaceName, isRunning);

        return isRunning;
    }

    @Override
    public CommandStatus startInterface(String interfaceName) {
        logger.debug("DNSMASQ - starting dnsmasq service for interface {}.", interfaceName);
        this.activeInterfaces.add(interfaceName);

        return this.executorService.execute(systemctlRestartCommand());
    }

    @Override
    public boolean disableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        try {
            File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));

            boolean isInterfaceDisabled = true;

            if (Files.deleteIfExists(configFile.toPath())) {
                CommandStatus status = this.executorService.execute(systemctlRestartCommand());
                isInterfaceDisabled = status.getExitStatus().isSuccessful();
            }
            
            this.activeInterfaces.remove(interfaceName);

            logger.debug("DNSMASQ - Disabled dhcp server on interface {}. Success? {}", interfaceName,
                    isInterfaceDisabled);
    
            return isInterfaceDisabled;
        } catch (Exception e) {
            throw new KuraProcessExecutionErrorException("Failed to disable DHCP server: " + e.getMessage());
        }
    }

    private Command systemctlRestartCommand() {
        return new Command(new String[] { "systemctl", "restart", DhcpServerTool.DNSMASQ.getValue() });
    }

}
