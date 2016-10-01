/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
import java.io.IOException;
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

    private static final Logger s_logger = LoggerFactory.getLogger(PppLinux.class);
    private static Object s_lock = new Object();
    private static final String PPP_DAEMON = "/usr/sbin/pppd";

    public static void connect(String iface, String port) throws KuraException {

        String cmd = formConnectCommand(iface, port);
        try {
            int status = LinuxProcessUtil.start(cmd);
            if (status != 0) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, cmd + " command failed");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static void disconnect(String iface, String port) throws KuraException {

        int pid = getPid(iface, port);
        if (pid >= 0) {
            s_logger.info("stopping {} pid={}", iface, pid);

            boolean exists = LinuxProcessUtil.stop(pid);
            if (!exists) {
                s_logger.warn("stopping {} pid={} failed", iface, pid);
            } else {
                exists = waitProcess(pid, 500, 5000);
            }

            if (exists) {
                s_logger.info("killing {} pid={}", iface, pid);
                exists = LinuxProcessUtil.kill(pid);
                if (!exists) {
                    s_logger.warn("killing {} pid={} failed", iface, pid);
                } else {
                    exists = waitProcess(pid, 500, 5000);
                }
            }

            if (exists) {
                s_logger.warn("Failed to disconnect {}", iface);
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
        long timeout = tout * 1000;

        long now = System.currentTimeMillis();
        long startDelay = now;
        long dif = now - startDelay;

        while (dif < timeout) {

            isPppRunning = isPppProcessRunning(iface, port);
            if (isPppRunning) {
                break;
            }
            s_logger.info("Waiting {} ms for pppd to launch", timeout - dif);
            try {
                Thread.sleep(timeout - dif);
            } catch (InterruptedException e) {
                // ignore
            }

            now = System.currentTimeMillis();
            dif = now - startDelay;
        }

        return isPppRunning;
    }

    private static int getPid(String iface, String port) throws KuraException {
        int pid = -1;
        synchronized (s_lock) {
            String[] pgrepCmd = { "pgrep", "-f", "" };
            pgrepCmd[2] = formConnectCommand(iface, port);

            BufferedReader br = null;
            try {
                ProcessStats processStats = LinuxProcessUtil.startWithStats(pgrepCmd);
                br = new BufferedReader(new InputStreamReader(processStats.getInputStream()));
                String line = br.readLine();
                if (line != null && line.length() > 0) {
                    pid = Integer.parseInt(line);
                    s_logger.trace("getPid() :: pppd pid={} for {}", pid, iface);
                }
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    s_logger.warn("Error closing input stream", e);
                }
            }
        }
        return pid;
    }

    // Only call this method after a call to stop or kill.
    // FIXME: this is an utility method that should be moved in a suitable package.
    private static boolean waitProcess(int pid, long poll, long timeout) {
        boolean exists = true;
        try {
            final long startTime = System.currentTimeMillis();
            long now;
            do {
                Thread.sleep(poll);
                exists = LinuxProcessUtil.stop(pid);
                now = System.currentTimeMillis();
            } while (exists && now - startTime < timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            s_logger.warn("Interrupted waiting for pid {} to exit", pid);
        }

        return exists;
    }

    private static void deleteLock(String port) {
        String portName = port;
        if (portName.startsWith("/dev/")) {
            portName = portName.substring("/dev/".length());
        }
        File fLock = new File("/var/lock/LCK.." + portName);
        if (fLock.exists()) {
            s_logger.warn("Deleting stale lock file {}", portName);
            if (!fLock.delete()) {
                s_logger.warn("Failed to delete {}", fLock);
            }
        }
    }

    private static String formConnectCommand(String peer, String port) {
        StringBuilder sb = new StringBuilder();
        sb.append(PPP_DAEMON).append(' ').append(port).append(' ').append("call").append(' ').append(peer);
        return sb.toString();
    }
}
