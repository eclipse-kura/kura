/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.kura.KuraException;
import org.junit.Test;


public class BluetoothProcessTest {

    @Test
    public void testExec() throws IOException, InterruptedException {
        boolean win = System.getProperty("os.name").matches("[Ww]indows.*");

        String[] cmdArray = { "target/test-classes/execTestCmd.sh" };
        if (win) {
            cmdArray[0] = "target/test-classes/execTestCmd.bat";
        } else {
            File f = new File(cmdArray[0]);
            assumeTrue(f.canExecute());
        }

        boolean[] visited = { false, false };
        BluetoothProcessListener listener = new BluetoothProcessListener() {

            @Override
            public void processInputStream(int ch) throws KuraException {
                // not needed
            }

            @Override
            public void processInputStream(String string) throws KuraException {
                assertTrue(string.matches("test\r?\n"));

                visited[0] = true;
            }

            @Override
            public void processErrorStream(String string) throws KuraException {
                assertTrue(string.matches("testerror\r?\n"));

                visited[1] = true;
            }
        };

        BluetoothProcess proc = new BluetoothProcess();
        proc.exec(cmdArray, listener);

        Thread.sleep(100);

        proc.destroy();

        assertTrue(visited[0]);
        assertTrue(visited[1]);
    }

}
