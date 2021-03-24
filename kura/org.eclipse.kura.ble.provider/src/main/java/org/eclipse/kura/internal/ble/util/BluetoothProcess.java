/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Scott Ware
 *******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;

public class BluetoothProcess {

    private static final Logger logger = LogManager.getLogger(BluetoothProcess.class);

    @SuppressWarnings("checkstyle:constantName")
    private static final ExecutorService streamGobblers = Executors.newCachedThreadPool();

    private Future<?> futureInputGobbler;
    private Future<?> futureErrorGobbler;
    private BufferedWriter bufferedWriter;
    private final PipedInputStream readOutputStream = new PipedInputStream();
    private final PipedInputStream readErrorStream = new PipedInputStream();
    private final PipedOutputStream outputStream = new PipedOutputStream();
    private final PipedOutputStream errorStream = new PipedOutputStream();

    private BTSnoopParser parser;
    private boolean btSnoopReady;
    private final CommandExecutorService executorService;

    public BluetoothProcess(CommandExecutorService executorService) {
        this.executorService = executorService;
        try {
            this.outputStream.connect(this.readOutputStream);
            this.errorStream.connect(this.readErrorStream);
        } catch (IOException e) {
            logger.error("Failed to connect streams", e);
        }
    }

    public BufferedWriter getWriter() {
        return this.bufferedWriter;
    }

    void exec(String[] cmdArray, final BluetoothProcessListener listener) {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing: {}", Arrays.toString(cmdArray));
        }
        Consumer<CommandStatus> callback = status -> logger.debug("Command ended with exit value {}",
                status.getExitStatus().getExitCode());
        Command command = new Command(cmdArray);
        command.setOutputStream(this.outputStream);
        command.setErrorStream(this.errorStream);
        this.executorService.execute(command, callback);

        // process the input stream
        this.futureInputGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess Input Stream Gobbler");
            try {
                readInputStreamFully(this.readOutputStream, listener);
            } catch (IOException | KuraException e) {
                logger.error("Process input stream failed", e);
            }
        });

        // process the error stream
        this.futureErrorGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess ErrorStream Gobbler");
            try {
                readErrorStreamFully(this.readErrorStream, listener);
            } catch (IOException | KuraException e) {
                logger.error("Process error stream failed", e);
            }
        });
    }

    void execSnoop(String[] cmdArray, final BTSnoopListener listener) {
        this.btSnoopReady = true;
        if (this.parser == null) {
            this.parser = new BTSnoopParser();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Executing: {}", Arrays.toString(cmdArray));
        }
        Consumer<CommandStatus> callback = status -> logger.debug("Command ended with exit value {}",
                status.getExitStatus().getExitCode());

        // Build command
        StringBuilder commandLine = new StringBuilder();
        commandLine.append("{ exec");
        Arrays.asList(cmdArray).stream().forEach(s -> {
            commandLine.append(" ");
            commandLine.append(s);
        });
        commandLine.append(" >/dev/null;");
        commandLine.append(" }");
        commandLine.append(" 3>&1");

        Command command = new Command(commandLine.toString().split("\\s+"));
        command.setOutputStream(this.outputStream);
        command.setErrorStream(this.errorStream);
        command.setExecuteInAShell(true);
        this.executorService.execute(command, callback);

        this.futureInputGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess BTSnoop Gobbler");
            try {
                readBTSnoopStreamFully(this.readOutputStream, listener);
            } catch (IOException e) {
                logger.debug("Process snoop input stream failed", e);
            }
        });

        // process the error stream
        this.futureErrorGobbler = streamGobblers.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess BTSnoop ErrorStream Gobbler");
            try {
                readBTErrorStreamFully(this.readErrorStream, listener);
            } catch (IOException e) {
                logger.debug("Process snoop error stream failed", e);
            }
        });
    }

    public void destroy() {
        closeStreams();
    }

    public void destroyBTSnoop() {
        this.btSnoopReady = false;
        closeStreams();
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
                if (packet.length > 0) {
                    listener.processBTSnoopRecord(packet);
                }
            }
        }
        logger.debug("End of btsnoop stream!");
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
        closeQuietly(this.outputStream);
        closeQuietly(this.errorStream);
        closeQuietly(this.readOutputStream);
        closeQuietly(this.readErrorStream);
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
