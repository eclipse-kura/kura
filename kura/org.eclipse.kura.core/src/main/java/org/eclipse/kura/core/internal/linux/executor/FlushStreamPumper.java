/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.internal.linux.executor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.exec.StreamPumper;
import org.apache.commons.exec.util.DebugUtils;

public class FlushStreamPumper extends StreamPumper {

    private static final int DEFAULT_SIZE = 1024;
    private final InputStream is;
    private final OutputStream os;
    private final int size;
    private boolean finished;
    private final boolean closeWhenExhausted;

    public FlushStreamPumper(InputStream is, OutputStream os, boolean closeWhenExhausted) {
        super(is, os, closeWhenExhausted);
        this.is = is;
        this.os = os;
        this.size = DEFAULT_SIZE;
        this.closeWhenExhausted = closeWhenExhausted;
    }

    @Override
    public void run() {
        synchronized (this) {
            // Just in case this object is reused in the future
            this.finished = false;
        }

        final byte[] buf = new byte[this.size];

        int length;
        try {
            while ((length = this.is.read(buf)) > 0) {
                this.os.write(buf, 0, length);
                this.os.flush();
            }
        } catch (final Exception e) {
            // nothing to do
        } finally {
            if (this.closeWhenExhausted) {
                try {
                    this.os.close();
                } catch (final IOException e) {
                    final String msg = "Got exception while closing exhausted output stream";
                    DebugUtils.handleException(msg, e);
                }
            }
            synchronized (this) {
                this.finished = true;
                notifyAll();
            }
        }
    }

    @Override
    public synchronized boolean isFinished() {
        return this.finished;
    }

}
