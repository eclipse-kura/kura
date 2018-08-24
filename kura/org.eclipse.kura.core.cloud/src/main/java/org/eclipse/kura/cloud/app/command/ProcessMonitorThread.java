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
package org.eclipse.kura.cloud.app.command;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Timer;

public class ProcessMonitorThread extends Thread {

    private final Process proc;
    private final String stdin;
    private final int procTout;
    private String stdout;
    private String stderr;
    private boolean timedOut;
    private Exception exception;
    private Integer exitValue;

    public ProcessMonitorThread(Process proc, String stdin, int procTout) {
        this.proc = proc;
        this.stdin = stdin;
        this.procTout = procTout;
        this.timedOut = false;
    }

    public String getStdout() {
        return this.stdout;
    }

    public String getStderr() {
        return this.stderr;
    }

    public Integer getExitValue() {
        return this.exitValue;
    }

    public boolean isTimedOut() {
        return this.timedOut;
    }

    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void run() {

        // Resources that should be cleaned up
        InputStream is = null;
        InputStream es = null;
        OutputStream os = null;
        BufferedWriter obw = null;

        StreamGobbler isg = null;
        StreamGobbler esg = null;
        Timer timer = null;

        try {
            if (this.procTout > 0) {
                timer = new Timer(true);
                InterruptTimerTask interrupter = new InterruptTimerTask(Thread.currentThread());
                timer.schedule(interrupter, this.procTout /* seconds */ * 1000L /* milliseconds per second */);
            }

            is = this.proc.getInputStream();
            es = this.proc.getErrorStream();

            os = this.proc.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            obw = new BufferedWriter(osw);

            // Spawn a couple of StreamGobbler to consume output streams
            isg = new StreamGobbler(is, "stdout");
            esg = new StreamGobbler(es, "stderr");
            isg.start();
            esg.start();

            if (this.stdin != null) {
                try {
                    obw.write(this.stdin);
                    obw.newLine();
                    obw.flush();
                } catch (IOException e) {
                    throw e;
                }
            }

            this.proc.waitFor();
            isg.join(1000);
            esg.join(1000);
            this.exitValue = this.proc.exitValue();
        } catch (IOException e) {
            this.exception = e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.timedOut = true;
        } finally {
            if (timer != null) {
                timer.cancel();
            }

            if (isg != null) {
                isg.interrupt();
                try {
                    isg.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                this.stdout = isg.getStreamAsString();
            }

            if (esg != null) {
                esg.interrupt();
                try {
                    esg.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                this.stderr = esg.getStreamAsString();
            }

            // See http://kylecartmell.com/?p=9
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (es != null) {
                try {
                    es.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (obw != null) {
                try {
                    obw.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (this.proc != null) {
                this.proc.destroy();
            }

            Thread.interrupted(); // See http://kylecartmell.com/?p=9
        }
    }
}