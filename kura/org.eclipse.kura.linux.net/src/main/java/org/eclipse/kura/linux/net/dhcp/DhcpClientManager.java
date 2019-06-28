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

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpClientManager {

    private static final Logger logger = LoggerFactory.getLogger(DhcpClientManager.class);

    private static DhcpClientTool dhcpClientTool = DhcpClientTool.NONE;
    private static final String PID_FILE_DIR = "/var/run";
    private CommandExecutorService executorService;

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
        if ((Integer) status.getExitStatus().getExitValue() != 0) {
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
        if ((Integer) status.getExitStatus().getExitValue() != 0) {
            throw new KuraProcessExecutionErrorException("Failed to release current lease");
        }
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

    private static String formCommand(String interfaceName, boolean useLeasesFile, boolean usePidFile,
            boolean dontWait) {
        StringBuilder sb = new StringBuilder();

        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            sb.append(DhcpClientTool.DHCLIENT.getValue());
            sb.append(' ');
            if (dontWait) {
                sb.append("-nw");
                sb.append(' ');
            }
            if (useLeasesFile) {
                sb.append(formLeasesOption(interfaceName));
                sb.append(' ');
            }
            if (usePidFile) {
                sb.append("-pf ");
                sb.append(getPidFilename(interfaceName));
                sb.append(' ');
            }
            sb.append(interfaceName);
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            sb.append(DhcpClientTool.UDHCPC.getValue());
            sb.append(" -i ");
            sb.append(interfaceName);
            sb.append(' ');
            if (usePidFile) {
                sb.append(getPidFilename(interfaceName));
                sb.append(' ');
            }
            sb.append(" -S");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String formReleaseCurrentLeaseCommand(String interfaceName) {

        StringBuilder sb = new StringBuilder();
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            sb.append(DhcpClientTool.DHCLIENT.getValue());
            sb.append(" -r ");
            sb.append(interfaceName);
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            sb.append(DhcpClientTool.UDHCPC.getValue());
            sb.append(" -R ");
            sb.append("-i ");
            sb.append(interfaceName);
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String formLeasesOption(String interfaceName) {

        StringBuilder sb = new StringBuilder();
        sb.append("-lf ");
        sb.append(DhcpClientLeases.getInstance().getDhclientLeasesFilePath(interfaceName));
        return sb.toString();
    }

    private List<Pid> getPid(String interfaceName) {
        List<Pid> pids = new ArrayList<>();
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
            pids = this.executorService.getPids(DhcpClientTool.DHCLIENT.getValue() + " " + interfaceName, false);
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
            pids = this.executorService.getPids(DhcpClientTool.UDHCPC.getValue() + " " + interfaceName, false);
        }
        return pids;
    }

    private boolean isRunning(String interfaceName) {
        return !getPid(interfaceName).isEmpty();
    }
}
