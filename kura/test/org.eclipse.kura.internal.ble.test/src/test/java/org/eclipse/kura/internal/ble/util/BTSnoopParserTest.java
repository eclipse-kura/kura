/*******************************************************************************
 * Copyright (c) 2021, 2026 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

public class BTSnoopParserTest {

    @Test
    public void readRecordTest() throws IOException {
        byte[] inputArray = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03,
                0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05, 0x0A, 0x0B };
        InputStream inputStream = new ByteArrayInputStream(inputArray);

        BTSnoopParser btSnoopParser = new BTSnoopParser();
        btSnoopParser.setInputStream(inputStream);
        byte[] result = btSnoopParser.readRecord();
        assertEquals(2, result.length);
        assertEquals(0x0A, result[0]);
        assertEquals(0x0B, result[1]);
    }

    // The 4 length-field bytes are reconstructed into an int via shifts without
    // masking. A byte
    // with the high bit set (0x81) is legal here since Included Length can exceed
    // 127, but was
    // previously sign-extended, corrupting the reconstructed value (e.g. 129 became
    // -127) and
    // causing new byte[includedLength] to throw NegativeArraySizeException. 0x81
    // must decode to
    // 129, not -127.
    @Test
    public void readRecordWithHighIncludedLengthByteTest() throws IOException {
        byte[] header = new byte[16];
        byte[] originalLength = { 0x00, 0x00, 0x00, 0x00 };
        byte[] includedLength = { 0x00, 0x00, 0x00, (byte) 0x81 }; // 129, but 0x81 has the high bit set
        byte[] packetFlags = { 0x00, 0x00, 0x00, 0x00 };
        byte[] cumulativeDrops = { 0x00, 0x00, 0x00, 0x00 };
        byte[] skip1 = { 0x00, 0x00, 0x00, 0x00 };
        byte[] skip2 = { 0x00, 0x00, 0x00, 0x00 };
        byte[] packetData = new byte[129];
        Arrays.fill(packetData, (byte) 0x11);

        ByteArrayOutputStream recordBytes = new ByteArrayOutputStream();
        recordBytes.write(header);
        recordBytes.write(originalLength);
        recordBytes.write(includedLength);
        recordBytes.write(packetFlags);
        recordBytes.write(cumulativeDrops);
        recordBytes.write(skip1);
        recordBytes.write(skip2);
        recordBytes.write(packetData);

        InputStream inputStream = new ByteArrayInputStream(recordBytes.toByteArray());

        BTSnoopParser btSnoopParser = new BTSnoopParser();
        btSnoopParser.setInputStream(inputStream);
        byte[] result = btSnoopParser.readRecord();

        assertEquals(129, result.length);
        assertArrayEquals(packetData, result);
    }

    @Test
    public void readRecordWithMaxIncludedLengthTest() throws IOException {
        int maxLength = 1 + 4 + 0xFFFF;
        byte[] packetData = new byte[maxLength];
        Arrays.fill(packetData, (byte) 0x22);

        BTSnoopParser btSnoopParser = new BTSnoopParser();
        btSnoopParser.setInputStream(new ByteArrayInputStream(buildRecord(maxLength, packetData)));
        byte[] result = btSnoopParser.readRecord();

        assertArrayEquals(packetData, result);
    }

    @Test(expected = IOException.class)
    public void readRecordWithTooLargeIncludedLengthTest() throws IOException {
        BTSnoopParser btSnoopParser = new BTSnoopParser();
        btSnoopParser.setInputStream(new ByteArrayInputStream(buildRecord(0x7FFFFFFF, new byte[0])));
        btSnoopParser.readRecord();
    }

    @Test(expected = IOException.class)
    public void readRecordWithNegativeIncludedLengthTest() throws IOException {
        BTSnoopParser btSnoopParser = new BTSnoopParser();
        btSnoopParser.setInputStream(new ByteArrayInputStream(buildRecord(-1, new byte[0])));
        btSnoopParser.readRecord();
    }

    private static byte[] buildRecord(int includedLength, byte[] packetData) throws IOException {
        ByteArrayOutputStream recordBytes = new ByteArrayOutputStream();
        recordBytes.write(new byte[16]); // header
        recordBytes.write(new byte[4]); // original length
        recordBytes.write(new byte[] { (byte) (includedLength >>> 24), (byte) (includedLength >>> 16),
                (byte) (includedLength >>> 8), (byte) includedLength });
        recordBytes.write(new byte[16]); // packet flags, cumulative drops, timestamp
        recordBytes.write(packetData);
        return recordBytes.toByteArray();
    }

}
