/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

}
