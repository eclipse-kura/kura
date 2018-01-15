/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.junit.Test;

public class BluetoothLeUtilTest {

    @Test(timeout = 1000)
    public void testKillCmd() throws IOException {
        assumeTrue("Only run this test on Linux", System.getProperty("os.name").matches("[Ll]inux"));

        String cmd = "sleep";

        ProcessBuilder pb = new ProcessBuilder(cmd, "10");
        Process proc = pb.start();

        assertTrue(proc.isAlive());

        BluetoothLeUtil.killCmd(cmd, "9");

        assertFalse(proc.isAlive());
    }

}
