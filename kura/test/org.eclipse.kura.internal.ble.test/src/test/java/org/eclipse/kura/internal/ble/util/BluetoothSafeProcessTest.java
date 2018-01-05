/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;


public class BluetoothSafeProcessTest {

    @Test
    public void testExec() throws IOException, InterruptedException {
        boolean win = System.getProperty("os.name").matches("[Ww]indows.*");

        String cmd = "target/test-classes/execSafeTestCmd.sh 123";
        if (win) {
            cmd = "target/test-classes/execSafeTestCmd.bat 123";
        } else {
            File f = new File(cmd.split(" ")[0]);
            assumeTrue(f.canExecute());
        }

        BluetoothSafeProcess proc = BluetoothProcessUtil.exec(cmd);

        Thread.sleep(100);

        InputStream in = proc.getInputStream();
        InputStream err = proc.getErrorStream();

        assertNull(proc.getOutputStream());
        assertNotNull(in);
        assertNotNull(err);

        byte[] buf = new byte[40];
        int len = in.read(buf);
        String string = new String(buf, 0, len);
        assertTrue(string.matches("test 123\r?\n"));

        len = err.read(buf);
        string = new String(buf, 0, len);
        assertTrue(string.matches("testerror\r?\n"));

        in.close();
        err.close();

        BluetoothProcessUtil.destroy(proc);
    }

}
