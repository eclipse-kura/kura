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
package org.eclipse.kura.core.util;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated since {@link org.eclipse.kura.core.util} version 1.3 in favor of
 *             {@link org.eclipse.kura.executor.CommandExecutorService}
 */
@Deprecated
public class ProcessUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtil.class);

    private static ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    private ProcessUtil() {

    }

    public static SafeProcess exec(String command) throws IOException {
        // Use StringTokenizer since this is the method documented by Runtime
        StringTokenizer st = new StringTokenizer(command);
        int count = st.countTokens();
        String[] cmdArray = new String[count];

        for (int i = 0; i < count; i++) {
            cmdArray[i] = st.nextToken();
        }

        return exec(cmdArray);
    }

    public static SafeProcess exec(final String[] cmdarray) throws IOException {
        // Serialize process executions. One at a time so we can consume all streams.
        Future<SafeProcess> futureSafeProcess = processExecutor.submit(() -> {
            Thread.currentThread().setName("SafeProcessExecutor");
            SafeProcess safeProcess = new SafeProcess();
            safeProcess.exec(cmdarray);
            return safeProcess;
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            logger.error("Error waiting from SafeProcess output");
            throw new IOException(e);
        }
    }

    /**
     * @deprecated The method does nothing
     */
    @Deprecated
    public static void close(SafeProcess proc) {
    }

    public static void destroy(SafeProcess proc) {
        proc.destroy();
    }
}
