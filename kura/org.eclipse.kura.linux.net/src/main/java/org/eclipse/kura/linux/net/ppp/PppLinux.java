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
package org.eclipse.kura.linux.net.ppp;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxPid;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.util.ProcessStopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PPP support for Linux OS
 *
 * @author ilya.binshtok
 *
 */
public class PppLinux {

    private static final Logger logger = LoggerFactory.getLogger(PppLinux.class);
    private static Object lock = new Object();
    private static final String PPP_DAEMON = "/usr/sbin/pppd";
    private final CommandExecutorService executorService;

    public PppLinux(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void connect(String iface, String port) throws KuraException {
        connect(iface, port, OptionalInt.empty());
    }

    public void connect(String iface, String port, OptionalInt pppNetInterfaceNumber) throws KuraException {

        String[] cmd = formConnectCommand(iface, port, pppNetInterfaceNumber);
        Command command = new Command(cmd);
        command.setTimeout(60);
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, String.join(" ", cmd) + " command failed");
        }
    }

    public void disconnect(String iface, String port) throws KuraException {

        disconnect(iface, port, OptionalInt.empty());
    }

    public void disconnect(String iface, String port, OptionalInt pppNetInterfaceNumber) throws KuraException {

        Pid pid = getPid(iface, port, pppNetInterfaceNumber);
        if (pid.getPid() >= 0) {
            logger.info("stopping {} pid={}", iface, pid);

            if (ProcessStopUtil.stopAndKill(executorService, pid)) {
                deleteLock(port);
            } else {
                logger.warn("Failed to disconnect {}", iface);
            }
        }
    }

    public boolean isPppProcessRunning(String iface, String port) {

        return isPppProcessRunning(iface, port, OptionalInt.empty());
    }

    public boolean isPppProcessRunning(String iface, String port, OptionalInt pppNetInterfaceNumber) {

        return getPid(iface, port, pppNetInterfaceNumber).getPid() > 0;
    }

    public boolean isPppProcessRunning(String iface, String port, int tout) {
        return isPppProcessRunning(iface, port, OptionalInt.empty(), tout);
    }

    public boolean isPppProcessRunning(String iface, String port, OptionalInt pppNetInterfaceNumber, int tout) {

        if (tout <= 0L) {
            return isPppProcessRunning(iface, port, pppNetInterfaceNumber);
        }

        boolean isPppRunning = false;
        long timeout = tout * 1000L;

        long now = System.currentTimeMillis();
        long startDelay = now;
        long dif = now - startDelay;

        while (dif < timeout) {

            isPppRunning = isPppProcessRunning(iface, port, pppNetInterfaceNumber);
            if (isPppRunning) {
                break;
            }
            logger.info("Waiting {} ms for pppd to launch", timeout - dif);
            try {
                Thread.sleep(timeout - dif);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            now = System.currentTimeMillis();
            dif = now - startDelay;
        }

        return isPppRunning;
    }

    private Pid getPid(String iface, String port, OptionalInt pppNetInterfaceNumber) {
        Pid pid = new LinuxPid(-1);
        synchronized (lock) {
            String[] command = formConnectCommand(iface, port, pppNetInterfaceNumber);
            // Filter the pid whose command exactly matches the connectCommand
            List<Pid> pids = this.executorService.getPids(command).entrySet().stream().filter(entry -> {
                if (pppNetInterfaceNumber.isPresent()) {
                    return entry.getKey().equals(String.join(" ", command));
                } else {
                    return entry.getKey().startsWith(String.join(" ", command));
                }
            }).map(Map.Entry::getValue).collect(Collectors.toList());
            if (!pids.isEmpty()) {
                pid = pids.get(0);
            }
        }
        return pid;
    }

    private static void deleteLock(String port) {
        String portName = port;
        if (portName.startsWith("/dev/")) {
            portName = portName.substring("/dev/".length());
        }
        File fLock = new File("/var/lock/LCK.." + portName);
        if (fLock.exists()) {
            logger.warn("Deleting stale lock file {}", portName);
            if (!fLock.delete()) {
                logger.warn("Failed to delete {}", fLock);
            }
        }
    }

    private static String[] formConnectCommand(String peer, String port, OptionalInt pppNetInterfaceNumber) {
        if (pppNetInterfaceNumber.isPresent()) {
            return new String[] { PPP_DAEMON, port, "call", peer, "unit",
                    Integer.toString(pppNetInterfaceNumber.getAsInt()) };
        } else {
            return new String[] { PPP_DAEMON, port, "call", peer };
        }
    }
}
