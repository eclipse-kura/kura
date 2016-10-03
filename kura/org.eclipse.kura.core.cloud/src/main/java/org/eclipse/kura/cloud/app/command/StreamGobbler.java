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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class StreamGobbler extends Thread {

    InputStream is;
    String type;
    StringBuilder sb;

    public StreamGobbler(InputStream is, String type) {
        this.is = is;
        this.type = type;
        this.sb = new StringBuilder();
    }

    public String getStreamAsString() {
        return this.sb.toString();
    }

    @Override
    public void run() {
        InputStreamReader isr = new InputStreamReader(this.is);
        BufferedReader br = new BufferedReader(isr);

        try {
            final int bufLen = 1024;
            final int maxBytes = 100 * bufLen;
            int count = 0;
            char[] cbuf = new char[bufLen];
            int read = -1;

            while ((read = br.read(cbuf)) != -1) {
                // System.out.println(type + ">number of bytes read: " + read);

                if (count < maxBytes) {
                    count += read; // might slightly exceed MAX_BYTES
                    this.sb.append(cbuf, 0, read);
                }
            }
        } catch (IOException ioe) {
            // ioe.printStackTrace();
        } finally {
            try {
                br.close();
                this.is.close(); // Just in case...
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}