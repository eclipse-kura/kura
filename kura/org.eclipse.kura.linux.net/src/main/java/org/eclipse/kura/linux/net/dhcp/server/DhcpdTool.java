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
import java.util.Map;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpdTool implements DhcpLinuxTool {

    private static final Logger logger = LoggerFactory.getLogger(DhcpdTool.class);

    private CommandExecutorService executorService;
    private DhcpServerTool dhcpServerTool = DhcpServerTool.NONE;

    public DhcpdTool(CommandExecutorService service, DhcpServerTool tool) {
        this.executorService = service;
        this.dhcpServerTool = tool;
    }

    @Override
    public boolean isRunning(String interfaceName) {
        return this.executorService.isRunning(getDhcpdCommand(interfaceName));
    }

    @Override
    public CommandStatus startInterface(String interfaceName) {
        return this.executorService.execute(new Command(getDhcpdCommand(interfaceName)));
    }

    @Override
    public boolean disableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        Map<String, Pid> pids = this.executorService.getPids(getDhcpdCommand(interfaceName));
        for (Pid pid : pids.values()) {
            if (this.executorService.stop(pid, LinuxSignal.SIGTERM)) {
                removePidFile(interfaceName);
            } else {
                logger.debug("Failed to stop process...try to kill");
                if (this.executorService.stop(pid, LinuxSignal.SIGKILL)) {
                    removePidFile(interfaceName);
                } else {
                    throw new KuraProcessExecutionErrorException("Failed to disable DHCP server");
                }
            }
        }
        if (pids.isEmpty()) {
            logger.debug("tried to kill DHCP server for interface but it is not running");
            return false;
        }

        return true;
    }

    private String[] getDhcpdCommand(String interfaceName) {
        List<String> command = new ArrayList<>();
        if (dhcpServerTool == DhcpServerTool.DHCPD) {
            command.add(DhcpServerTool.DHCPD.getValue());
            command.add("-cf");
            command.add(DhcpServerManager.getConfigFilename(interfaceName));
            command.add("-pf");
            command.add(DhcpServerManager.getPidFilename(interfaceName));
        } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
            command.add(DhcpServerTool.UDHCPD.getValue());
            command.add("-f");
            command.add("-S");
            command.add(DhcpServerManager.getConfigFilename(interfaceName));
        }
        return command.toArray(new String[0]);
    }

    private boolean removePidFile(String interfaceName) {
        boolean ret = true;
        File pidFile = new File(DhcpServerManager.getPidFilename(interfaceName));
        if (pidFile.exists()) {
            ret = pidFile.delete();
        }
        return ret;
    }

}
