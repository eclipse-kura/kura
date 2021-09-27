/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.ble;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.freedesktop.dbus.exceptions.DBusException;
import org.osgi.service.component.ComponentContext;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;

public class BluetoothLeServiceImpl implements BluetoothLeService {

    private static final Logger logger = LogManager.getLogger(BluetoothLeServiceImpl.class);

    private DeviceManager deviceManager;
    private CommandExecutorService executorService;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        if (this.executorService == executorService) {
            this.executorService = null;
        }
    }

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le Service...");
        if (!startBluetoothSuppressed() && !startBluetoothUbuntuSnap() && !startBluetoothSystemd() && !startBluetoothInitd()) {
            startBluetoothDaemon();
        }
        try {
            this.deviceManager = getDeviceManager();
        } catch (DBusException e) {
            logger.error("Failed to start bluetooth service", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Service...");
        if (this.deviceManager != null) {
            this.deviceManager.closeConnection();
            this.deviceManager = null;
        }
    }

    @Override
    public List<BluetoothLeAdapter> getAdapters() {
        List<BluetoothLeAdapter> adapters = new ArrayList<>();
        if (this.deviceManager != null) {
            for (BluetoothAdapter adapter : this.deviceManager.getAdapters()) {
                adapters.add(new BluetoothLeAdapterImpl(adapter));
            }
        }
        return adapters;
    }

    @Override
    public BluetoothLeAdapter getAdapter(String interfaceName) {
        BluetoothLeAdapterImpl adapter = null;
        if (this.deviceManager != null) {
            BluetoothAdapter ba = this.deviceManager.getAdapter(interfaceName);
            if (ba != null) {
                adapter = new BluetoothLeAdapterImpl(ba);
            }
        }
        return adapter;
    }

    private boolean startBluetoothSystemd() {
        return execute("systemctl start bluetooth");
    }

    private boolean startBluetoothInitd() {
        return execute("/etc/init.d/bluetooth start");
    }

    private void startBluetoothDaemon() {
        execute("bluetoothd -E");
    }

    private boolean startBluetoothUbuntuSnap() {
        String snapName = System.getProperty("kura.os.snap.name");
        if (snapName != null && snapName.length() != 0) {
            // when running as snap, we assume bluez is installed as snap and running
            logger.info("We are running as snap, assume bluetooth is running");
            return true;
        } else {
            return false;
        }
    }

    private boolean startBluetoothSuppressed() {
        // Allow to disable the bluetooth service start, e.g. when running inside a container
        return Boolean.getBoolean("kura.ble.suppressBluetoothDaemonStart");
    }

    private boolean execute(String commandLine) {
        Command command = new Command(commandLine.split(" "));
        boolean started = this.executorService.execute(command).getExitStatus().isSuccessful();
        if (!started) {
            logger.error("Failed to start linux bluetooth service");
        }
        return started;
    }

    // For test only
    public DeviceManager getDeviceManager() throws DBusException {
        return DeviceManager.createInstance(false);
    }
}
