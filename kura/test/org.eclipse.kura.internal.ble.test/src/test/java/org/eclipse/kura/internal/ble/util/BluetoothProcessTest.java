/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.junit.Test;

public class BluetoothProcessTest {

    @Test
    public void execTest() {
        CommandExecutorService executorMock = mock(CommandExecutorService.class);
        BluetoothProcess bluetoothProcess = new BluetoothProcess(executorMock);

        String[] commandLine = { "hcitool", "lescan" };
        Command command = new Command(commandLine);
        bluetoothProcess.exec(commandLine, null);

        verify(executorMock, times(1)).execute(eq(command), anyObject());
    }

    @Test
    public void execSnoopTest() {
        CommandExecutorService executorMock = mock(CommandExecutorService.class);
        BluetoothProcess bluetoothProcess = new BluetoothProcess(executorMock);

        String[] commandLine = { "hcitool", "lescan" };
        Command command = new Command("{ exec hcitool lescan >/dev/null; } 3>&1".split(" "));
        command.setExecuteInAShell(true);
        bluetoothProcess.execSnoop(commandLine, null);

        verify(executorMock, times(1)).execute(eq(command), anyObject());
    }

}
