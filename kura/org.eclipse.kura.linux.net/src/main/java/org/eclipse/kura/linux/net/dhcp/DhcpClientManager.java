/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpClientManager {

    private static final Logger logger = LoggerFactory.getLogger(DhcpClientManager.class);

    private static final String PID_FILE_DIR = "/var/run";
    private static final String DHCLIENT_HOOK_SCRIPT_FILE = "/etc/kura-dhclient-resolv-hook";
    private static final String DHCLIENT_ROUTE_SCRIPT_FILE = "/etc/kura-dhclient-route-hook";
    private static DhcpClientTool dhcpClientTool = DhcpClientTool.NONE;
    private final CommandExecutorService executorService;

    static {
        dhcpClientTool = getTool();
    }

    public DhcpClientManager(CommandExecutorService service) {
        this.executorService = service;
    }

    public static DhcpClientTool getTool() {
        if (dhcpClientTool == DhcpClientTool.NONE) {
            if (LinuxNetworkUtil.toolExists(DhcpClientTool.DHCLIENT.getValue())) {
                dhcpClientTool = DhcpClientTool.DHCLIENT;
            } else if (LinuxNetworkUtil.toolExists(DhcpClientTool.UDHCPC.getValue())) {
                dhcpClientTool = DhcpClientTool.UDHCPC;
            }
        }
        return dhcpClientTool;
    }

    public void enable(String interfaceName) throws KuraProcessExecutionErrorException {
        if (isRunning(interfaceName)) {
            logger.info("enable() :: disabling DHCP client for {}", interfaceName);
            disable(interfaceName);
        }
        logger.info("enable() :: Starting DHCP client for {}", interfaceName);
        CommandStatus status = this.executorService.execute(new Command(formCommand(interfaceName, true, true, true)));
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraProcessExecutionErrorException("Failed to start dhcp client on interface " + interfaceName);
        }
    }

    public void disable(String interfaceName) throws KuraProcessExecutionErrorException {
        List<Pid> pids = getPid(interfaceName);
        if (!pids.isEmpty()) {
            for (Pid pid : pids) {
                logger.info("disable() :: killing DHCP client for {}", interfaceName);
                if (this.executorService.stop(pid, LinuxSignal.SIGKILL)) {
                    removePidFile(interfaceName);
                } else {
                    throw new KuraProcessExecutionErrorException(
                            "Failed to stop process with pid " + (Integer) pid.getPid());
                }
            }
        }
    }

    public void releaseCurrentLease(String interfaceName) throws KuraProcessExecutionErrorException {
        Command command = new Command(formReleaseCurrentLeaseCommand(interfaceName));
        command.setTimeout(60);
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraProcessExecutionErrorException("Failed to release current lease");
        }
    }

    public static String getResolvConfHookScriptFileName() {
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            return DHCLIENT_HOOK_SCRIPT_FILE;
        }
        return "";
    }

    public static String getRouteHookScriptFileName() {
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            return DHCLIENT_ROUTE_SCRIPT_FILE;
        }
        return "";
    }

    private static boolean removePidFile(String interfaceName) {
        boolean ret = true;
        File pidFile = new File(getPidFilename(interfaceName));
        if (pidFile.exists()) {
            ret = pidFile.delete();
        }
        return ret;
    }

    private static String getPidFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(PID_FILE_DIR);
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            sb.append('/');
            sb.append(DhcpClientTool.DHCLIENT.getValue());
            sb.append('.');
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            sb.append('/');
            sb.append(DhcpClientTool.UDHCPC.getValue());
            sb.append('-');
        }
        sb.append(interfaceName);
        sb.append(".pid");

        return sb.toString();
    }

    private static String[] formCommand(String interfaceName, boolean useLeasesFile, boolean usePidFile,
            boolean dontWait) {
        List<String> command = new ArrayList<>();

        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            command.add(DhcpClientTool.DHCLIENT.getValue());
            if (dontWait) {
                command.add("-nw");
            }
            if (useLeasesFile) {
                command.add("-lf");
                command.add(DhcpClientLeases.getInstance().getDhclientLeasesFilePath(interfaceName));
            }
            if (usePidFile) {
                command.add("-pf");
                command.add(getPidFilename(interfaceName));
            }
            command.add(interfaceName);
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            command.add(DhcpClientTool.UDHCPC.getValue());
            command.add("-i");
            command.add(interfaceName);
            if (usePidFile) {
                command.add("-p");
                command.add(getPidFilename(interfaceName));
            }
            command.add("-S");
        }
        return command.toArray(new String[0]);
    }

    private static String[] formReleaseCurrentLeaseCommand(String interfaceName) {

        List<String> command = new ArrayList<>();
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            command.add(DhcpClientTool.DHCLIENT.getValue());
            command.add("-r");
            command.add(interfaceName);
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            command.add(DhcpClientTool.UDHCPC.getValue());
            command.add("-R");
            command.add("-i");
            command.add(interfaceName);
        }
        return command.toArray(new String[0]);
    }

    private List<Pid> getPid(String interfaceName) {
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            return new ArrayList<>(this.executorService
                    .getPids(new String[] { DhcpClientTool.DHCLIENT.getValue(), interfaceName }).values());
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            return new ArrayList<>(this.executorService
                    .getPids(new String[] { DhcpClientTool.UDHCPC.getValue(), interfaceName }).values());
        } else {
            return new ArrayList<>();
        }
    }

    private boolean isRunning(String interfaceName) {
        return !getPid(interfaceName).isEmpty();
    }
}
