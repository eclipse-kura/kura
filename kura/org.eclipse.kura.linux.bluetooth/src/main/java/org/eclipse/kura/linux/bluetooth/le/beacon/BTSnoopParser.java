package org.eclipse.kura.linux.bluetooth.le.beacon;

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

        readInt();
        int includedLength = readInt();
        readInt();
        readInt();
        readInt();
        readInt();
        byte[] packetData = new byte[includedLength];

        // bluetooth record
        IOUtils.readFully(this.is, packetData);

        return packetData;
    }

    private int readInt() throws IOException {

        byte[] intBytes = new byte[4];

        IOUtils.readFully(this.is, intBytes);

        return intBytes[0] << 24 | intBytes[1] << 16 | intBytes[2] << 8 | intBytes[3];
    }
}