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
package org.eclipse.kura.core.comm;

import static org.junit.Assert.*;

import org.junit.Test;

public class CommConnectionImplTest {

    @Test
    public void testGetBytesAsStringNull() {
        assertNull(CommConnectionImpl.getBytesAsString(null));
    }

    @Test
    public void testGetBytesAsStringEmpty() {
        byte[] data = {};
        String stringData = CommConnectionImpl.getBytesAsString(data);

        assertEquals("", stringData);
    }

    @Test
    public void testGetBytesAsStringSingle() {
        byte[] data = { 0x42 };
        String stringData = CommConnectionImpl.getBytesAsString(data);

        assertEquals("42", stringData);
    }

    @Test
    public void testGetBytesAsStringMultiple() {
        byte[] data = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
        String stringData = CommConnectionImpl.getBytesAsString(data);

        assertEquals("01 23 45 67 89 AB CD EF", stringData);
    }
}
