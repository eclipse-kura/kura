/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.util;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessStopUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProcessStopUtil.class);

    private ProcessStopUtil() {
    }

    public static boolean stopAndKill(final CommandExecutorService executorService, final Pid pid)
            throws KuraException {
        try {
            if (stop(executorService, pid, LinuxSignal.SIGTERM)) {
                return true;
            }

            return stop(executorService, pid, LinuxSignal.SIGKILL);

        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    private static boolean stop(final CommandExecutorService executorService, final Pid pid, final Signal signal) {
        logger.info("stopping pid={} with {}", pid, signal);
        final boolean signalSent = executorService.stop(pid, signal);

        final boolean result;

        if (!signalSent) {
            result = false;
        } else {
            result = !processExists(executorService, pid, 500, 5000);
        }

        if (result) {
            logger.info("stopped pid={} with {}", pid, signal);
        } else {
            logger.warn("failed to stop pid={} with {}", pid, signal);
        }

        return result;
    }

    private static boolean processExists(final CommandExecutorService executorService, final Pid pid, final long poll,
            final long timeout) {
        boolean exists = false;
        try {
            final long startTime = System.currentTimeMillis();
            long now;
            do {
                Thread.sleep(poll);
                exists = executorService.isRunning(pid);
                now = System.currentTimeMillis();
            } while (exists && now - startTime < timeout);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            logger.warn("Failed waiting for pid {} to exit - {}", pid.getPid(), e);
        }

        return exists;
    }
}
