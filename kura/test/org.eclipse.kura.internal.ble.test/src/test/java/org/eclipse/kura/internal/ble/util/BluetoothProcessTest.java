/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream(3);
        baos.write("test\\r?\\n".getBytes(UTF_8));
        ByteArrayOutputStream baes = new ByteArrayOutputStream(3);
        baes.write("testerror\\r?\\n".getBytes(UTF_8));
        CommandStatus status = new CommandStatus(new LinuxExitStatus(0));
        status.setErrorStream(baes);
        status.setOutputStream(baos);
        status.setTimedout(false);

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        when(esMock.execute(anyObject())).thenReturn(status);
        BluetoothProcess proc = new BluetoothProcess(esMock);
        proc.exec(cmdArray, listener);

        Thread.sleep(100);

        proc.destroy();

        assertTrue(visited[0]);
        assertTrue(visited[1]);
    }

}
