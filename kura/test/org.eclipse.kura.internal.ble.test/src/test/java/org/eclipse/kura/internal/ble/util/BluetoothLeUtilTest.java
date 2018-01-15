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

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class BluetoothLeUtilTest {

    @Test(timeout = 1000)
    public void testKillCmd() throws IOException, InterruptedException {
        assumeTrue("Only run this test on Linux", System.getProperty("os.name").matches("[Ll]inux"));
        assumeTrue("Sleep exists", new File("/bin/sleep").exists());
        assumeTrue("Pidof exists", new File("/bin/pidof").exists());
        assumeTrue("Kill exists", new File("/bin/kill").exists());

        String cmd = "sleep";

        ProcessBuilder pb = new ProcessBuilder(cmd, "10");
        Process proc = pb.start();

        Thread.sleep(100);

        assertTrue("Process should be alive", proc.isAlive());

        BluetoothLeUtil.killCmd(cmd, "9");

        assertFalse("Process should have been terminated", proc.isAlive());
    }

}
