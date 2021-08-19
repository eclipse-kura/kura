/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.bluetooth.le.beacon.BTSnoopParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothProcess {

    private static final String END_OF_STREAM_MESSAGE = "End of stream!";
    private static final String INPUT_STREAM_MESSAGE = "Error in processing the input stream : ";
    private static final String ERROR_STREAM_MESSAGE = "Error in processing the error stream : ";
    private static final Logger logger = LoggerFactory.getLogger(BluetoothProcess.class);
    private static final ExecutorService STREAM_GOBBLERS = Executors.newCachedThreadPool();

    private Future<?> futureInputGobbler;
    private Future<?> futureErrorGobbler;
    private BufferedWriter bufferedWriter;
    private final PipedInputStream readOutputStream = new PipedInputStream();
    private final PipedInputStream readErrorStream = new PipedInputStream();
    private final PipedOutputStream writeInputStream = new PipedOutputStream();
    private final PipedOutputStream outputStream = new PipedOutputStream();
    private final PipedOutputStream errorStream = new PipedOutputStream();
    private final PipedInputStream inputStream = new PipedInputStream();

    private BTSnoopParser parser;
    private boolean btSnoopReady;
    private final CommandExecutorService executorService;

    public BluetoothProcess(CommandExecutorService executorService) {
        this.executorService = executorService;
        try {
            this.outputStream.connect(this.readOutputStream);
            this.errorStream.connect(this.readErrorStream);
            this.inputStream.connect(this.writeInputStream);
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.writeInputStream));
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
        command.setInputStream(this.inputStream);
        this.executorService.execute(command, callback);

        // process the input stream
        this.futureInputGobbler = STREAM_GOBBLERS.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess Input Stream Gobbler");
            readInputStreamFully(this.readOutputStream, listener);
        });

        // process the error stream
        this.futureErrorGobbler = STREAM_GOBBLERS.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess ErrorStream Gobbler");
            readErrorStreamFully(this.readErrorStream, listener);
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
        Command command = new Command(cmdArray);
        command.setOutputStream(this.outputStream);
        command.setErrorStream(this.errorStream);
        this.executorService.execute(command, callback);

        this.futureInputGobbler = STREAM_GOBBLERS.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess BTSnoop Gobbler");
            readBTSnoopStreamFully(this.readOutputStream, listener);
        });

        // process the error stream
        this.futureErrorGobbler = STREAM_GOBBLERS.submit(() -> {
            Thread.currentThread().setName("BluetoothProcess BTSnoop ErrorStream Gobbler");
            readBTErrorStreamFully(this.readErrorStream, listener);
        });

    }

    public void destroy() {
        closeStreams();
    }

    public void destroyBTSnoop() {
        this.btSnoopReady = false;
        closeStreams();
    }

    private void readInputStreamFully(InputStream is, BluetoothProcessListener listener) {
        int ch;
        String line;

        if (listener instanceof BluetoothGatt) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                while ((ch = br.read()) != -1) {
                    listener.processInputStream((char) ch);
                }
            } catch (IOException e) {
                logger.warn(INPUT_STREAM_MESSAGE, e);
            }
            logger.debug(END_OF_STREAM_MESSAGE);
        } else {
            StringBuilder stringBuilder = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
            } catch (IOException e) {
                logger.warn(INPUT_STREAM_MESSAGE, e);
            }
            listener.processInputStream(stringBuilder.toString());
            logger.debug(END_OF_STREAM_MESSAGE);
        }
    }

    private void readBTSnoopStreamFully(InputStream is, BTSnoopListener listener) {

        this.parser.setInputStream(is);

        try {
            while (this.btSnoopReady) {
                if (is != null) {
                    byte[] packet = this.parser.readRecord();
                    listener.processBTSnoopRecord(packet);
                }
            }
        } catch (IOException e) {
            logger.warn(INPUT_STREAM_MESSAGE, e);
        }

        logger.debug(END_OF_STREAM_MESSAGE);
    }

    private void readErrorStreamFully(InputStream is, BluetoothProcessListener listener) {
        int ch;

        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            while ((ch = br.read()) != -1) {
                stringBuilder.append((char) ch);
            }
        } catch (IOException e) {
            logger.warn(ERROR_STREAM_MESSAGE, e);
        }
        listener.processErrorStream(stringBuilder.toString());
        logger.debug(END_OF_STREAM_MESSAGE);
    }

    private void readBTErrorStreamFully(InputStream is, BTSnoopListener listener) {
        int ch;

        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            while ((ch = br.read()) != -1) {
                stringBuilder.append((char) ch);
            }
        } catch (IOException e) {
            logger.warn(ERROR_STREAM_MESSAGE, e);
        }
        listener.processErrorStream(stringBuilder.toString());
        logger.debug(END_OF_STREAM_MESSAGE);
    }

    private void closeStreams() {
        logger.info("Closing streams and killing...");
        closeQuietly(this.outputStream);
        closeQuietly(this.errorStream);
        closeQuietly(this.readOutputStream);
        closeQuietly(this.readErrorStream);
        closeQuietly(this.inputStream);
        closeQuietly(this.writeInputStream);
        if (this.futureInputGobbler != null) {
            this.futureInputGobbler.cancel(true);
        }
        if (this.futureErrorGobbler != null) {
            this.futureErrorGobbler.cancel(true);
        }
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
