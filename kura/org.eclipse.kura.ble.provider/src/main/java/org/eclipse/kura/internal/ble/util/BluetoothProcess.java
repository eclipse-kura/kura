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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;

public class BluetoothProcess {

    private static final Logger logger = LogManager.getLogger(BluetoothProcess.class);
    private static final ExecutorService streamGobblers = Executors.newCachedThreadPool();

    private Process process;
    private Future<?> futureInputGobbler;
    private Future<?> futureErrorGobbler;
    private BufferedWriter bufferedWriter;

    private BTSnoopParser parser;
    private boolean btSnoopReady;

    public BufferedWriter getWriter() {
        return this.bufferedWriter;
    }

    void exec(String[] cmdArray, final BluetoothProcessListener listener) throws IOException {
        logger.debug("Executing: {}", Arrays.toString(cmdArray));
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        this.process = pb.start();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream()));

        // process the input stream
        this.futureInputGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess Input Stream Gobbler");
            try {
                readInputStreamFully(BluetoothProcess.this.process.getInputStream(), listener);
            } catch (IOException | KuraException e) {
                logger.error("Process input stream failed", e);
            }
        });

        // process the error stream
        this.futureErrorGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess ErrorStream Gobbler");
            try {
                readErrorStreamFully(BluetoothProcess.this.process.getErrorStream(), listener);
            } catch (IOException | KuraException e) {
                logger.error("Process error stream failed", e);
            }
        });
    }

    void execSnoop(String[] cmdArray, final BTSnoopListener listener) throws IOException {
        this.btSnoopReady = true;
        if (this.parser == null) {
            this.parser = new BTSnoopParser();
        }

        logger.debug("Executing: {}", Arrays.toString(cmdArray));
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        this.process = pb.start();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream()));

        this.futureInputGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess BTSnoop Gobbler");
            try {
                readBTSnoopStreamFully(BluetoothProcess.this.process.getInputStream(), listener);
            } catch (IOException e) {
                logger.error("Process snoop input stream failed", e);
            }
        });

        // process the error stream
        this.futureErrorGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess BTSnoop ErrorStream Gobbler");
            try {
                readBTErrorStreamFully(BluetoothProcess.this.process.getErrorStream(), listener);
            } catch (IOException e) {
                logger.error("Process snoop error stream failed", e);
            }
        });
    }

    public void destroy() {
        if (this.process != null) {
            closeStreams();
            this.process.destroy();
            this.process = null;
        }
    }

    public void destroyBTSnoop() {
        if (this.process != null) {
            this.btSnoopReady = false;
        }
    }

    private String readStream(InputStream is) throws IOException {
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        while ((line = br.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }
        logger.debug("End of stream!");
        return stringBuilder.toString();
    }

    private void readInputStreamFully(InputStream is, BluetoothProcessListener listener)
            throws IOException, KuraException {
        listener.processInputStream(readStream(is));
    }

    private void readBTSnoopStreamFully(InputStream is, BTSnoopListener listener) throws IOException {
        this.parser.setInputStream(is);
        while (this.btSnoopReady) {
            if (is != null) {
                byte[] packet = this.parser.readRecord();
                listener.processBTSnoopRecord(packet);
            }
        }
        closeStreams();
        this.process.destroy();
        this.process = null;
        logger.debug("End of stream!");
    }

    private void readErrorStreamFully(InputStream is, BluetoothProcessListener listener)
            throws IOException, KuraException {
        listener.processErrorStream(readStream(is));
    }

    private void readBTErrorStreamFully(InputStream is, BTSnoopListener listener) throws IOException {
        listener.processBTSnoopErrorStream(readStream(is));
    }

    private void closeStreams() {
        logger.info("Closing streams and killing...");
        if (this.futureInputGobbler != null) {
            this.futureInputGobbler.cancel(true);
        }
        if (this.futureErrorGobbler != null) {
            this.futureErrorGobbler.cancel(true);
        }
        closeQuietly(this.process.getErrorStream());
        closeQuietly(this.process.getOutputStream());
        closeQuietly(this.process.getInputStream());
    }

    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                logger.warn("Failed to close process input stream", e);
            }
        }
    }

    private void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                logger.warn("Failed to close process output stream", e);
            }
        }
    }

}
