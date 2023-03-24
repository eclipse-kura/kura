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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;

public class DnsmasqTool implements DhcpLinuxTool {

    private CommandExecutorService executorService;

    public DnsmasqTool(CommandExecutorService service) {
        this.executorService = service;
    }

    @Override
    public boolean isRunning(String interfaceName) {
        CommandStatus status = this.executorService.execute(
                new Command(new String[] { "systemctl", "is-active", "--quiet", DhcpServerTool.DNSMASQ.getValue() }));
        return status.getExitStatus().isSuccessful();
    }

    @Override
    public CommandStatus startInterface(String interfaceName) {
        List<String> command = new ArrayList<>();

        command.add("systemctl");
        command.add("start");
        command.add(DhcpServerTool.DNSMASQ.getValue());

        Command cmd = new Command(command.toArray(new String[0]));

        return this.executorService.execute(cmd);
    }

    @Override
    public boolean disableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
        configFile.delete();

        CommandStatus status = this.executorService.execute(
                new Command(new String[] { "systemctl", "restart", DhcpServerTool.DNSMASQ.getValue() }));
        return status.getExitStatus().isSuccessful();
    }

}
