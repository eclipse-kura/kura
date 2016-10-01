package org.eclipse.kura.linux.bluetooth.util;

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

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothProcessUtil {

    private static final Logger s_logger = LoggerFactory.getLogger(BluetoothProcessUtil.class);

    private static final ExecutorService s_processExecutor = Executors.newSingleThreadExecutor();

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
        Future<BluetoothSafeProcess> futureSafeProcess = s_processExecutor.submit(new Callable<BluetoothSafeProcess>() {

            @Override
            public BluetoothSafeProcess call() throws Exception {
                Thread.currentThread().setName("SafeProcessExecutor");
                BluetoothSafeProcess safeProcess = new BluetoothSafeProcess();
                safeProcess.exec(cmdarray);
                return safeProcess;
            }
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            s_logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
    }

    /**
     * @deprecated The method does nothing
     */
    @Deprecated
    public static void close(BluetoothSafeProcess proc) {
    }

    public static void destroy(BluetoothSafeProcess proc) {
        proc.destroy();
    }
}
