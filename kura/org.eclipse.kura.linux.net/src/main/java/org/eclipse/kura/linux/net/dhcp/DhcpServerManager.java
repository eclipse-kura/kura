/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.dhcp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServerManager.class);

    private static final String FILE_DIR = "/etc/";
    private static final String PID_FILE_DIR = "/var/run/";
    private static DhcpServerTool dhcpServerTool = DhcpServerTool.NONE;
    private CommandExecutorService executorService;

    static {
        dhcpServerTool = getTool();
    }

    public DhcpServerManager(CommandExecutorService service) {
        this.executorService = service;
    }

    public static DhcpServerTool getTool() {
        if (dhcpServerTool == DhcpServerTool.NONE) {
            if (LinuxNetworkUtil.toolExists(DhcpServerTool.DHCPD.getValue())) {
                dhcpServerTool = DhcpServerTool.DHCPD;
            } else if (LinuxNetworkUtil.toolExists(DhcpServerTool.UDHCPD.getValue())) {
                dhcpServerTool = DhcpServerTool.UDHCPD;
            }
        }
        return dhcpServerTool;
    }

    public boolean isRunning(String interfaceName) {
        // Check if DHCP server is running
        return this.executorService.isRunning(DhcpServerManager.formDhcpdCommand(interfaceName));
    }

    public boolean enable(String interfaceName) throws KuraException {
        // Check if DHCP server is running
        if (isRunning(interfaceName)) {
            // If so, disable it
            logger.error("DHCP server is already running for {}, bringing it down...", interfaceName);
            disable(interfaceName);
        }
        // Start DHCP server
        File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
        if (configFile.exists()) {
            CommandStatus status = this.executorService
                    .execute(new Command(DhcpServerManager.formDhcpdCommand(interfaceName)));
            if ((Integer) status.getExitStatus().getExitValue() == 0) {
                logger.debug("DHCP server started.");
                return true;
            } else {
                logger.debug("Can't start DHCP server, config file does not exist: {}", configFile.getAbsolutePath());
            }
        }

        return false;
    }

    public boolean disable(String interfaceName) throws KuraException {
        logger.debug("Disable DHCP server for {}", interfaceName);

        Map<String, Pid> pids = this.executorService.getPids(DhcpServerManager.formDhcpdCommand(interfaceName));
        for (Pid pid : pids.values()) {
            if (this.executorService.stop(pid, LinuxSignal.SIGTERM)) {
                DhcpServerManager.removePidFile(interfaceName);
            } else {
                logger.debug("Failed to stop process...try to kill");
                if (this.executorService.stop(pid, LinuxSignal.SIGKILL)) {
                    DhcpServerManager.removePidFile(interfaceName);
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

    public static String getConfigFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(FILE_DIR);
        if (dhcpServerTool == DhcpServerTool.DHCPD || dhcpServerTool == DhcpServerTool.UDHCPD) {
            sb.append(dhcpServerTool.getValue());
            sb.append('-');
            sb.append(interfaceName);
            sb.append(".conf");
        }
        return sb.toString();
    }

    private static boolean removePidFile(String interfaceName) {
        boolean ret = true;
        File pidFile = new File(DhcpServerManager.getPidFilename(interfaceName));
        if (pidFile.exists()) {
            ret = pidFile.delete();
        }
        return ret;
    }

    public static String getPidFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(PID_FILE_DIR);
        if (dhcpServerTool == DhcpServerTool.DHCPD || dhcpServerTool == DhcpServerTool.UDHCPD) {
            sb.append(dhcpServerTool.getValue());
            sb.append('-');
            sb.append(interfaceName);
            sb.append(".pid");
        }
        return sb.toString();
    }

    private static String[] formDhcpdCommand(String interfaceName) {
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
}
