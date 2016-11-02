/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.core.comm;

import static org.eclipse.kura.core.comm.CommConnectionImpl.getBytesAsString;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CommConnectionImplTest {

    @Test
    public void testHex1() {

        assertEquals(null, getBytesAsString(null));
        assertEquals("", getBytesAsString(new byte[] {}));
        assertEquals("00 ", getBytesAsString(new byte[] { 0x00 }));
        assertEquals("FF ", getBytesAsString(new byte[] { (byte) 0xFF }));
        assertEquals("00 FF ", getBytesAsString(new byte[] { 0x00, (byte) 0xFF }));
    }
}
