/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.comm;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.StringJoiner;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;

public class CommConnectionImpl implements CommConnection, Closeable {

    private static final String SEND_MESSAGE = "sendMessage() - {}";
    private static final String JAVA_EXT_DIRS = "java.ext.dirs";
    private static final String KURA_EXT_DIR = "kura.ext.dir";

    private static final Logger logger = LogManager.getLogger(CommConnectionImpl.class);

    // set up the appropriate ext dir for RXTX extra device nodes
    static {
        String kuraExtDir = System.getProperty(KURA_EXT_DIR);
        if (kuraExtDir != null) {
            StringBuffer sb = new StringBuffer();
            String existingDirs = System.getProperty(JAVA_EXT_DIRS);
            if (existingDirs != null) {
                if (!existingDirs.contains(kuraExtDir)) {
                    sb.append(existingDirs).append(File.pathSeparator).append(kuraExtDir);
                    System.setProperty(JAVA_EXT_DIRS, sb.toString());
                }
            } else {
                sb.append(kuraExtDir);
                System.setProperty(JAVA_EXT_DIRS, sb.toString());
            }
        }
    }

    private final CommURI commUri;
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    public CommConnectionImpl(CommURI commUri, int mode, boolean timeouts)
            throws IOException, NoSuchPortException, PortInUseException {

        requireNonNull(commUri);

        this.commUri = commUri;

        final String port = this.commUri.getPort();
        final int baudRate = this.commUri.getBaudRate();
        final int dataBits = this.commUri.getDataBits();
        final int stopBits = this.commUri.getStopBits();
        final int parity = this.commUri.getParity();
        final int flowControl = this.commUri.getFlowControl();
        final int openTimeout = this.commUri.getOpenTimeout();
        final int receiveTimeout = this.commUri.getReceiveTimeout();

        final CommPortIdentifier commPortIdentifier = CommPortIdentifier.getPortIdentifier(port);

        final CommPort commPort = commPortIdentifier.open(this.getClass().getName(), openTimeout);

        if (commPort instanceof SerialPort) {
            this.serialPort = (SerialPort) commPort;
            try {
                this.serialPort.setSerialPortParams(baudRate, dataBits, stopBits, parity);
                this.serialPort.setFlowControlMode(flowControl);
                if (receiveTimeout > 0) {
                    this.serialPort.enableReceiveTimeout(receiveTimeout);
                    if (!this.serialPort.isReceiveTimeoutEnabled()) {
                        throw new IOException("Serial receive timeout not supported by driver");
                    }
                }
            } catch (UnsupportedCommOperationException e) {
                logger.error("Failed to configure COM port", e);
                throw new IOException(e);
            }
        } else {
            throw new IOException("Unsupported Port Type");
        }
    }

    @Override
    public CommURI getURI() {
        return this.commUri;
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    @Override
    public synchronized InputStream openInputStream() throws IOException {
        checkIfClosed();

        if (this.inputStream == null) {
            this.inputStream = this.serialPort.getInputStream();
        }
        return this.inputStream;
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    @Override
    public synchronized OutputStream openOutputStream() throws IOException {
        checkIfClosed();

        if (this.outputStream == null) {
            this.outputStream = this.serialPort.getOutputStream();
        }
        return this.outputStream;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.serialPort != null) {
            this.serialPort.notifyOnDataAvailable(false);
            this.serialPort.removeEventListener();
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }
            if (this.outputStream != null) {
                this.outputStream.close();
                this.outputStream = null;
            }

            this.serialPort.close();
            this.serialPort = null;
        }
    }

    private void checkIfClosed() throws IOException {
        if (this.serialPort == null) {
            throw new IOException("Connection is already closed");
        }
    }

    @Override
    public synchronized void sendMessage(byte[] message) throws KuraException, IOException {
        checkIfClosed();

        if (message == null) {
            throw new NullPointerException("Message must not be null");
        }

        logger.debug(SEND_MESSAGE, () -> getBytesAsString(message));

        if (this.outputStream == null) {
            openOutputStream();
        }

        this.outputStream.write(message, 0, message.length);
        this.outputStream.flush();
    }

    @Override
    public synchronized byte[] sendCommand(byte[] command, int timeout) throws KuraException, IOException {
        checkIfClosed();

        if (command == null) {
            throw new NullPointerException("Serial command must not be null");
        }

        logger.debug(SEND_MESSAGE, () -> getBytesAsString(command));

        if (this.outputStream == null) {
            openOutputStream();
        }
        if (this.inputStream == null) {
            openInputStream();
        }

        byte[] dataInBuffer = flushSerialBuffer();
        if (dataInBuffer != null && dataInBuffer.length > 0) {
            logger.warn("eating bytes in the serial buffer input stream before sending command: {}",
                    getBytesAsString(dataInBuffer));
        }
        this.outputStream.write(command, 0, command.length);
        this.outputStream.flush();

        ByteBuffer buffer = getResponse(timeout);
        if (buffer != null) {
            byte[] response = new byte[buffer.limit()];
            buffer.get(response, 0, response.length);
            return response;
        } else {
            return null;
        }
    }

    @Override
    public synchronized byte[] sendCommand(byte[] command, int timeout, int demark) throws KuraException, IOException {
        checkIfClosed();

        if (command == null) {
            throw new NullPointerException("Serial command must not be null");
        }

        logger.debug(SEND_MESSAGE, getBytesAsString(command));

        if (this.outputStream == null) {
            openOutputStream();
        }
        if (this.inputStream == null) {
            openInputStream();
        }

        byte[] dataInBuffer = flushSerialBuffer();
        if (dataInBuffer != null && dataInBuffer.length > 0) {
            logger.warn("eating bytes in the serial buffer input stream before sending command: {}",
                    getBytesAsString(dataInBuffer));
        }
        this.outputStream.write(command, 0, command.length);
        this.outputStream.flush();

        ByteBuffer buffer = getResponse(timeout, demark);
        if (buffer != null) {
            byte[] response = new byte[buffer.limit()];
            buffer.get(response, 0, response.length);
            return response;
        } else {
            return null;
        }
    }

    @Override
    public synchronized byte[] flushSerialBuffer() throws KuraException, IOException {
        checkIfClosed();

        ByteBuffer buffer = getResponse(50);
        if (buffer != null) {
            byte[] response = new byte[buffer.limit()];
            buffer.get(response, 0, response.length);
            return response;
        } else {
            return null;
        }
    }

    private synchronized ByteBuffer getResponse(int timeout) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        long start = System.currentTimeMillis();

        while (this.inputStream.available() < 1 && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        while (this.inputStream.available() >= 1) {
            int c = this.inputStream.read();
            buffer.put((byte) c);
        }

        buffer.flip();

        return buffer.limit() > 0 ? buffer : null;
    }

    private synchronized ByteBuffer getResponse(int timeout, int demark) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        long start = System.currentTimeMillis();

        while (this.inputStream.available() < 1 && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        start = System.currentTimeMillis();
        do {
            if (this.inputStream.available() > 0) {
                start = System.currentTimeMillis();
                int c = this.inputStream.read();
                buffer.put((byte) c);
            }
        } while (System.currentTimeMillis() - start < demark);

        buffer.flip();

        return buffer.limit() > 0 ? buffer : null;
    }

    /* default */ static String getBytesAsString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringJoiner sj = new StringJoiner(" ");

        for (byte b : bytes) {
            sj.add(String.format("%02X", b));
        }

        return sj.toString();
    }
}
