/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BluetoothProcessUtil {

    private static final Logger logger = LogManager.getLogger(BluetoothProcessUtil.class);

    private static final ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    public static BluetoothSafeProcess exec(String command) throws IOException {
        // Use StringTokenizer since this is the method documented by Runtime
        StringTokenizer st = new StringTokenizer(command);
        int count = st.countTokens();
        String[] cmdArray = new String[count];

        for (int i = 0; i < count; i++) {
            cmdArray[i] = st.nextToken();
        }

        return exec(cmdArray);
    }

    public static BluetoothSafeProcess exec(final String[] cmdarray) throws IOException {
        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothSafeProcess> futureSafeProcess = processExecutor.submit(() -> {
            Thread.currentThread().setName("SafeProcessExecutor");
            BluetoothSafeProcess safeProcess = new BluetoothSafeProcess();
            safeProcess.exec(cmdarray);
            return safeProcess;
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
    }

    public static void destroy(BluetoothSafeProcess proc) {
        proc.destroy();
    }
}
