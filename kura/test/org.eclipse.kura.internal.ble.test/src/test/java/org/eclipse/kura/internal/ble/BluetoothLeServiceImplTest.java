/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.ble;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.system.SystemService;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;

public class BluetoothLeServiceImplTest {

    private static BluetoothLeServiceImpl bluetoothLeService;
    private static DeviceManager deviceManagerMock;

    @BeforeClass
    public static void setup() {
        BluetoothAdapter adapter = mock(BluetoothAdapter.class);
        when(adapter.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        deviceManagerMock = mock(DeviceManager.class);
        when(deviceManagerMock.getAdapter("hci0")).thenReturn(adapter);
        List<BluetoothAdapter> adapters = new ArrayList<>();
        adapters.add(adapter);
        when(deviceManagerMock.getAdapters()).thenReturn(adapters);
        bluetoothLeService = new BluetoothLeServiceImpl() {

            @Override
            protected DeviceManager getDeviceManagerInternal() {
                return deviceManagerMock;
            }
        };
    }

    protected SystemService mockSystemService() {
        SystemService systemMock = mock(SystemService.class);
        when(systemMock.getProperties()).thenReturn(new Properties());
        return systemMock;
    }

    @Test
    public void activateTest() {
        CommandExecutorService executorMock = mock(CommandExecutorService.class);
        Command commandSystemd = new Command("systemctl start bluetooth".split(" "));
        CommandStatus statusSystemd = new CommandStatus(commandSystemd, new LinuxExitStatus(1));
        when(executorMock.execute(commandSystemd)).thenReturn(statusSystemd);
        Command commandSysV = new Command("/etc/init.d/bluetooth start".split(" "));
        CommandStatus statusSysV = new CommandStatus(commandSysV, new LinuxExitStatus(1));
        when(executorMock.execute(commandSysV)).thenReturn(statusSysV);
        Command commandBTService = new Command("bluetoothd -E".split(" "));
        CommandStatus statusBTService = new CommandStatus(commandBTService, new LinuxExitStatus(0));
        when(executorMock.execute(commandBTService)).thenReturn(statusBTService);
        bluetoothLeService.setExecutorService(executorMock);
        bluetoothLeService.setSystemService(mockSystemService());
        bluetoothLeService.activate(null);

        verify(executorMock, times(0)).execute(commandSystemd);
        verify(executorMock, times(0)).execute(commandSysV);
        verify(executorMock, times(0)).execute(commandBTService);
    }

    @Test
    public void getAdapterTest() {
        CommandExecutorService executorMock = mock(CommandExecutorService.class);
        Command command = new Command("systemctl start bluetooth".split(" "));
        CommandStatus status = new CommandStatus(command, new LinuxExitStatus(0));
        when(executorMock.execute(command)).thenReturn(status);
        bluetoothLeService.setExecutorService(executorMock);
        bluetoothLeService.setSystemService(mockSystemService());
        bluetoothLeService.activate(null);
        BluetoothLeAdapter adapter = bluetoothLeService.getAdapter("hci0");

        verify(executorMock, times(1)).execute(command);
        assertEquals("AA:BB:CC:DD:EE:FF", adapter.getAddress());
    }

    @Test
    public void getAdaptersTest() {
        CommandExecutorService executorMock = mock(CommandExecutorService.class);
        Command command = new Command("systemctl start bluetooth".split(" "));
        CommandStatus status = new CommandStatus(command, new LinuxExitStatus(0));
        when(executorMock.execute(command)).thenReturn(status);
        bluetoothLeService.setExecutorService(executorMock);
        bluetoothLeService.setSystemService(mockSystemService());
        bluetoothLeService.activate(null);
        BluetoothLeAdapter adapter = bluetoothLeService.getAdapters().get(0);

        verify(executorMock, times(1)).execute(command);
        assertEquals("AA:BB:CC:DD:EE:FF", adapter.getAddress());
    }
}
