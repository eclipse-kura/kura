/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.linux.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxProcessUtil {

    private static final Logger logger = LoggerFactory.getLogger(LinuxProcessUtil.class);

    private static final String PLATFORM_INTEL_EDISON = "intel-edison";
    private static volatile Boolean usingBusybox;

    public static int start(String command, boolean wait, boolean background) throws Exception {
        SafeProcess proc = null;
        try {
            logger.info("executing: " + command);
            proc = ProcessUtil.exec(command);
            // FIXME:MC this leads to a process leak when called with false
            if (wait) {
                try {
                    proc.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted exception - ", e);
                }

                logger.info(command + " returned with exit value:" + proc.exitValue());
                if (proc.exitValue() > 0) {
                    String stdout = getInputStreamAsString(proc.getInputStream());
                    String stderr = getInputStreamAsString(proc.getErrorStream());
                    logger.debug("stdout: {}", stdout);
                    logger.debug("stderr: {}", stderr);
                }
                return proc.exitValue();
            } else {
                return 0;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            // FIXME:MC this may lead to a process leak when called with false
            if (!background && proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    public static int start(String command) throws Exception {
        return LinuxProcessUtil.start(command, true, false);
    }

    public static int start(String command, boolean wait) throws Exception {
        return start(command, wait, false);
    }

    public static int start(String[] command, boolean wait) throws Exception {
        StringBuilder cmdBuilder = new StringBuilder();
        for (String cmd : command) {
            cmdBuilder.append(cmd).append(' ');
        }
        return start(cmdBuilder.toString(), wait);
    }

    public static int startBackground(String command, boolean wait) throws Exception {
        return start(command, wait, true);
    }

    public static ProcessStats startWithStats(String command) throws Exception {
        SafeProcess proc = null;
        try {
            logger.info("executing: " + command);
            proc = ProcessUtil.exec(command);

            try {
                int exitVal = proc.waitFor();
                logger.info(command + " returned with exit value:" + exitVal);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("error executing " + command + " command" + e);
            }

            ProcessStats stats = new ProcessStats(proc);
            // s_logger.info(command + " returned with exit value:" +
            // proc.exitValue());
            return stats;
        } catch (Exception e) {
            throw e;
        }
    }

    public static ProcessStats startWithStats(String[] command) throws Exception {
        SafeProcess proc = null;
        try {
            StringBuilder cmdBuilder = new StringBuilder();
            for (String cmd : command) {
                cmdBuilder.append(cmd).append(' ');
            }
            logger.debug("executing: {}", cmdBuilder);
            proc = ProcessUtil.exec(command);

            try {
                int exitVal = proc.waitFor();
                logger.debug("{} returned with exit value:{}", cmdBuilder, +exitVal);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("error executing " + command + " command" + e);
            }

            ProcessStats stats = new ProcessStats(proc);
            // s_logger.debug(cmdBuilder + " returned with exit value:" +
            // proc.exitValue());
            return stats;
        } catch (Exception e) {
            throw e;
        }
    }

    public static int getPid(String command) throws Exception {
        StringTokenizer st = null;
        String line = null;
        String pid = null;
        SafeProcess proc = null;
        BufferedReader br = null;
        try {

            if (command != null && !command.isEmpty()) {
                logger.trace("searching process list for {}", command);

                if (isUsingBusyBox()) {
                    proc = ProcessUtil.exec("ps");
                } else {
                    proc = ProcessUtil.exec("ps -ax");
                }
                proc.waitFor();

                // get the output
                br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                while ((line = br.readLine()) != null) {
                    st = new StringTokenizer(line);
                    pid = st.nextToken();
                    st.nextElement();
                    st.nextElement();
                    st.nextElement();

                    // get the remainder of the line showing the command that
                    // was issued
                    line = line.substring(line.indexOf(st.nextToken()));

                    // see if the line has our command
                    if (line.indexOf(command) >= 0) {
                        logger.trace("found pid {} for command: {}", pid, command);
                        return Integer.parseInt(pid);
                    }
                }
            }

            // return failure
            return -1;
        } catch (Exception e) {
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    public static int getPid(String command, String[] tokens) throws Exception {
        StringTokenizer st = null;
        String line = null;
        String pid = null;
        SafeProcess proc = null;
        BufferedReader br = null;
        try {
            if (command != null && !command.isEmpty()) {
                logger.trace("searching process list for {}", command);
                if (isUsingBusyBox()) {
                    proc = ProcessUtil.exec("ps");
                } else {
                    proc = ProcessUtil.exec("ps -ax");
                }
                proc.waitFor();

                // get the output
                br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                while ((line = br.readLine()) != null) {
                    st = new StringTokenizer(line);
                    pid = st.nextToken();
                    st.nextElement();
                    st.nextElement();
                    st.nextElement();

                    // get the remainder of the line showing the command that was issued
                    line = line.substring(line.indexOf(st.nextToken()));

                    // see if the line has our command
                    if (line.indexOf(command) >= 0) {
                        boolean allTokensPresent = true;
                        for (String token : tokens) {
                            if (!line.contains(token)) {
                                allTokensPresent = false;
                                break;
                            }
                        }
                        if (allTokensPresent) {
                            logger.trace("found pid {} for command: {}", pid, command);
                            return Integer.parseInt(pid);
                        }
                    }
                }
            }

            return -1;
        } catch (Exception e) {
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    public static int getKuraPid() throws Exception {

        int pid = -1;
        File kuraPidFile = new File("/var/run/kura.pid");
        if (kuraPidFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(kuraPidFile))) {
                pid = Integer.parseInt(br.readLine());
            }
        }
        return pid;
    }

    public static boolean stop(int pid) {
        return stop(pid, false);
    }

    public static boolean kill(int pid) {
        return stop(pid, true);
    }

    public static boolean killAll(String command) {
        try {
            logger.info("attempting to kill process " + command);
            if (start("killall " + command) == 0) {
                logger.info("successfully killed process " + command);
                return true;
            } else {
                logger.warn("failed to kill process " + command);
                return false;
            }
        } catch (Exception e) {
            logger.warn("failed to kill process " + command);
            return false;
        }
    }

    public static String getInputStreamAsString(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(stream));
            char[] cbuf = new char[1024];
            int len;
            while ((len = br.read(cbuf)) > 0) {
                sb.append(cbuf, 0, len);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return sb.toString();
    }

    /**
     * This method takes a pid and returns a boolean that defines if the corresponding process is running or not in the
     * host system.
     * 
     * @param pid
     *            integer representing the process id that has to be verified.
     * @return true if the process in running in the system, false otherwise.
     * @throws IOException
     *             if an I/O or execution error occurs
     */
    public static boolean isProcessRunning(int pid) throws IOException {
        boolean isRunning = false;

        SafeProcess proc = null;
        BufferedReader br = null;
        try {
            logger.trace("searching process list for pid{}", pid);
            if (isUsingBusyBox()) {
                proc = ProcessUtil.exec("ps");
            } else {
                proc = ProcessUtil.exec("ps -ax");
            }
            proc.waitFor();

            // get the output
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = br.readLine(); // skip first line: PID TTY STAT TIME COMMAND
            while ((line = br.readLine()) != null) {
                if (parsePid(line) == pid) {
                    isRunning = true;
                    break;
                }
            }
            return isRunning;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            if (br != null) {
                br.close();
            }
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    /**
     * This method tries first to stop a process specified by the passed PID. If, aster this first step, the process is
     * still alive, the code invokes a kill operation.
     *
     * @param pid
     *            An int representing the linux pid.
     * @throws KuraException
     *             Thrown if one of the executed operations generate an exception.
     * @since {@link org.eclipse.kura.core.linux.util} 1.1.0
     */
    public static boolean stopAndKill(int pid) throws KuraException {
        try {
            if (pid >= 0) {
                logger.info("stopping pid={}", pid);

                boolean exists = stop(pid);
                if (!exists) {
                    logger.warn("stopping pid={} has failed", pid);
                } else {
                    exists = waitProcess(pid, 500, 5000);
                }

                if (exists) {
                    logger.info("killing pid={}", pid);
                    exists = kill(pid);
                    if (!exists) {
                        logger.warn("killing pid={} has failed", pid);
                    } else {
                        exists = waitProcess(pid, 500, 5000);
                    }
                }

                if (exists) {
                    logger.warn("Failed to stop process with pid {}", pid);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    //
    // Private Methods
    //
    private static boolean isUsingBusyBox() {
        if (usingBusybox != null) {
            return usingBusybox;
        }

        final BundleContext ctx = FrameworkUtil.getBundle(LinuxProcessUtil.class).getBundleContext();

        final ServiceReference<SystemService> systemServiceRef = ctx.getServiceReference(SystemService.class);
        if (systemServiceRef == null) {
            throw new IllegalStateException("Unable to find instance of: " + SystemService.class.getName());
        }

        final SystemService service = ctx.getService(systemServiceRef);
        if (service == null) {
            throw new IllegalStateException("Unable to get instance of: " + SystemService.class.getName());
        }

        try {
            usingBusybox = PLATFORM_INTEL_EDISON.equals(service.getPlatform());
        } finally {
            ctx.ungetService(systemServiceRef);
        }

        return usingBusybox;
    }

    private static boolean stop(int pid, boolean kill) {
        boolean result = false;
        try {
            if (isProcessRunning(pid)) {
                StringBuffer cmd = new StringBuffer();
                cmd.append("kill ");
                if (kill) {
                    cmd.append("-9 ");
                }
                cmd.append(pid);

                if (kill) {
                    logger.info("attempting to kill -9 pid {}", pid);
                } else {
                    logger.info("attempting to kill pid {}", pid);
                }

                if (start(cmd.toString()) == 0) {
                    logger.info("successfully killed pid {}", pid);
                    result = true;
                } else {
                    logger.warn("failed to kill pid {}", pid);
                }
            }
        } catch (Exception e) {
            logger.warn("failed to kill pid {}", pid);
        }
        return result;
    }

    private static boolean waitProcess(int pid, long poll, long timeout) {
        boolean exists = false;
        try {
            final long startTime = System.currentTimeMillis();
            long now;
            do {
                Thread.sleep(poll);
                exists = isProcessRunning(pid);
                now = System.currentTimeMillis();
            } while (exists && now - startTime < timeout);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            logger.warn("Failed waiting for pid {} to exit - {}", pid, e);
        }

        return exists;
    }

    private static int parsePid(String line) {
        StringTokenizer st = new StringTokenizer(line);
        int processID = -1;
        try {
            processID = Integer.parseInt(st.nextToken());
        } catch (NumberFormatException e) {
            logger.warn("getPid() :: NumberFormatException reading PID - {}", e);
        }
        return processID;
    }
}
