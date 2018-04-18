/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.ppp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.linux.util.ProcessStats;
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

    public static void connect(String iface, String port) throws KuraException {

        String cmd = formConnectCommand(iface, port);
        try {
            int status = LinuxProcessUtil.start(cmd);
            if (status != 0) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, cmd + " command failed");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    public static void disconnect(String iface, String port) throws KuraException {

        int pid = getPid(iface, port);
        if (pid >= 0) {
            logger.info("stopping {} pid={}", iface, pid);

            LinuxProcessUtil.stopAndKill(pid);

            if (LinuxProcessUtil.stop(pid)) {
                logger.warn("Failed to disconnect {}", iface);
            } else {
                deleteLock(port);
            }
        }
    }

    public static boolean isPppProcessRunning(String iface, String port) throws KuraException {

        return getPid(iface, port) > 0 ? true : false;
    }

    public static boolean isPppProcessRunning(String iface, String port, int tout) throws KuraException {

        if (tout <= 0L) {
            return isPppProcessRunning(iface, port);
        }

        boolean isPppRunning = false;
        long timeout = tout * 1000L;

        long now = System.currentTimeMillis();
        long startDelay = now;
        long dif = now - startDelay;

        while (dif < timeout) {

            isPppRunning = isPppProcessRunning(iface, port);
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

    private static int getPid(String iface, String port) throws KuraException {
        int pid;
        synchronized (lock) {
            String[] pgrepCmd = { "pgrep", "-f", "" };
            pgrepCmd[2] = formConnectCommand(iface, port);
            try {
                ProcessStats processStats = LinuxProcessUtil.startWithStats(pgrepCmd);
                pid = parseGetPid(processStats, iface);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
            }
        }
        return pid;
    }

    private static int parseGetPid(ProcessStats processStats, String iface) throws KuraException {
        int pid = -1;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(processStats.getInputStream()))) {
            String line = br.readLine();
            if (line != null && line.length() > 0) {
                pid = Integer.parseInt(line);
                logger.trace("getPid() :: pppd pid={} for {}", pid, iface);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
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

    private static String formConnectCommand(String peer, String port) {
        StringBuilder sb = new StringBuilder();
        sb.append(PPP_DAEMON).append(' ').append(port).append(' ').append("call").append(' ').append(peer);
        return sb.toString();
    }
}
