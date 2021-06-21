/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.ble.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Parses a btsnoop stream into btsnoop records
 */
public class BTSnoopParser {

    private InputStream is;
    private boolean gotHeader = false;

    public BTSnoopParser() {
        // Do nothing
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }

    public byte[] readRecord() throws IOException {
        if (!this.gotHeader) {
            // Read past the 16-byte header
            IOUtils.readFully(this.is, new byte[16]);
            this.gotHeader = true;
        }

        readInt(); // Original Length
        int includedLength = readInt(); // Included Length
        readInt(); // Packet Flags
        readInt(); // Cumulative Drops

        // Skip forward to packet data
        readInt();
        readInt();

        // Packet Data
        if (includedLength > 0) {
            byte[] packetData = new byte[includedLength];
            IOUtils.readFully(this.is, packetData);
            return packetData;
        }

        return new byte[0];
    }

    private int readInt() throws IOException {

        byte[] intBytes = new byte[4];

        IOUtils.readFully(this.is, intBytes);

        return intBytes[0] << 24 | intBytes[1] << 16 | intBytes[2] << 8 | intBytes[3];
    }
}
