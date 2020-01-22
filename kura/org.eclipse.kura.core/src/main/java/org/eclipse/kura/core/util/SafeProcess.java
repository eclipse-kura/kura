/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeProcess {

    private static final Logger logger = LoggerFactory.getLogger(SafeProcess.class);

    private static final ExecutorService s_streamGobblers = Executors.newFixedThreadPool(2);

    private Process process;
    private Future<byte[]> futureInputGobbler;
    private Future<byte[]> futureErrorGobbler;
    private byte[] inBytes;
    private byte[] errBytes;
    private boolean waited;
    private int exitValue;

    SafeProcess() {
        super();
    }

    public OutputStream getOutputStream() {
        logger.warn("getOutputStream() is unsupported");
        return null;
    }

    public InputStream getInputStream() {
        if (!this.waited) {
            logger.warn("getInputStream() must be called after waitFor()");
            // Thread.dumpStack();
        }
        return new ByteArrayInputStream(this.inBytes);
    }

    public InputStream getErrorStream() {
        if (!this.waited) {
            logger.warn("getErrorStream() must be called after waitFor()");
            // Thread.dumpStack();
        }
        return new ByteArrayInputStream(this.errBytes);
    }

    void exec(String[] cmdarray) throws IOException {
        logger.debug("Executing: {}", Arrays.toString(cmdarray));
        ProcessBuilder pb = new ProcessBuilder(cmdarray);
        this.process = pb.start();

        // process the input stream
        this.futureInputGobbler = s_streamGobblers.submit(() -> {
            Thread.currentThread().setName("SafeProcess InputStream Gobbler");
            return readStreamFully(SafeProcess.this.process.getInputStream());
        });

        // process the error stream
        this.futureErrorGobbler = s_streamGobblers.submit(() -> {
            Thread.currentThread().setName("SafeProcess ErrorStream Gobbler");
            return readStreamFully(SafeProcess.this.process.getErrorStream());
        });

        // wait for the process execution
        try {
            this.inBytes = this.futureInputGobbler.get();
            this.errBytes = this.futureErrorGobbler.get();
            this.exitValue = this.process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } finally {
            closeQuietly(this.process.getInputStream());
            closeQuietly(this.process.getErrorStream());
            closeQuietly(this.process.getOutputStream());
            this.process.destroy();
            this.process = null;
            this.waited = true;
        }
    }

    public int waitFor() throws InterruptedException {
        return this.exitValue;
    }

    public int exitValue() {
        return this.exitValue;
    }

    public void destroy() {
        if (!this.waited) {
            logger.warn("Calling destroy() before waitFor() might lead to resource leaks");
            Thread.dumpStack();
            if (this.process != null) {
                this.process.destroy();
            }
        }
        this.inBytes = null; // just in case...
        this.errBytes = null;
        this.process = null;
    }

    private byte[] readStreamFully(InputStream is) throws IOException {
        int len;
        byte[] buf = new byte[1024];
        ByteArrayOutputStream inBaos = new ByteArrayOutputStream(1024);
        while ((len = is.read(buf)) != -1) {
            inBaos.write(buf, 0, len);
        }
        return inBaos.toByteArray();
    }

    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
                is = null;
            } catch (IOException e) {
                logger.warn("Failed to close process input stream", e);
            }
        }
    }

    private void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.close();
                os = null;
            } catch (IOException e) {
                logger.warn("Failed to close process output stream", e);
            }
        }
    }
}
