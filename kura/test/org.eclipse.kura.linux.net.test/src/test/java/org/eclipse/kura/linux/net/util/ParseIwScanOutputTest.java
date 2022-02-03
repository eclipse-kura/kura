/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.stream.Stream;

import org.eclipse.kura.core.util.IOUtil;
import org.junit.Test;

public class ParseIwScanOutputTest {

    private static final String SSID_PLACEHOLDER = "@SSID@";

    private IWAPParser iwAPParser;
    private String iwOutput;

    @Test
    public void parseIwScanWithUTF8EscapedSSID() {

        givenIwApParserWithMACAddress("42:8c:8d:9b:e6:c6");

        whenIwScanOutputIs("iw-scan-output.txt");
        whenSSIDIs("kura\\xe2\\x82\\xac&");

        thenTheParsedAPSSIDIs("kura€&");
    }

    private void givenIwApParserWithMACAddress(String macAddress) {
        this.iwAPParser = new IWAPParser(macAddress);
    }

    private void whenSSIDIs(String ssid) {
        this.iwOutput = this.iwOutput.replace(SSID_PLACEHOLDER, ssid);
    }

    private void whenIwScanOutputIs(String filename) {
        try {
            this.iwOutput = loadFileAsString(filename);
        } catch (IOException e) {
            fail();
        }
    }

    private void thenTheParsedAPSSIDIs(String expectedSSID) {
        Stream.of(this.iwOutput.split("\n")).forEach(line -> {
            try {
                this.iwAPParser.parsePropLine(line.trim());
            } catch (Exception e) {
                fail();
            }
        });
        assertEquals(expectedSSID, this.iwAPParser.toWifiAccessPoint().getSSID());
    }

    private String loadFileAsString(String filename) throws IOException {
        return IOUtil.readResource(filename);
    }
}
