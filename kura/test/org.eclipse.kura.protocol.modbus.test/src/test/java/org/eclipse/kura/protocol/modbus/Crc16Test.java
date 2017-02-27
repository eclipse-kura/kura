/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

import static org.junit.Assert.*;

import org.junit.Test;

public class Crc16Test {

    @Test
    public void testGetCrc16() {
        int crcSeed = 0xFFFF;

        byte[] buff = { 0, 0, 0, 0, 0 };
        int expected = 0x0024;
        int actual = Crc16.getCrc16(buff, buff.length, crcSeed);
        assertEquals(expected, actual);

        buff = new byte[] { 0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xAB, (byte)0xCD, (byte)0xEF };
        expected = 0xF8E6;
        actual = Crc16.getCrc16(buff, buff.length, crcSeed);
        assertEquals(expected, actual);

        buff = new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
        expected = 0x8031;
        actual = Crc16.getCrc16(buff, buff.length, crcSeed);
        assertEquals(expected, actual);
    }

}
