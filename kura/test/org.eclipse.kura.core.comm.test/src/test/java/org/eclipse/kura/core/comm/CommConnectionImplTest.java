/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
