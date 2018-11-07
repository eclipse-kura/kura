/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.osgi.service.component.ComponentContext;

import tinyb.BluetoothManager;

public class BluetoothLeServiceImpl implements BluetoothLeService {

    private static final Logger logger = LogManager.getLogger(BluetoothLeServiceImpl.class);

    private BluetoothManager bluetoothManager;

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le Service...");
        if (!startBluetoothSystemd() && !startBluetoothInitd()) {
            startBluetoothDaemon();
        }
        try {
            this.bluetoothManager = BluetoothManager.getBluetoothManager();
        } catch (RuntimeException | UnsatisfiedLinkError e) {
            logger.error("Failed to start bluetooth service", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Service...");
        this.bluetoothManager = null;
    }

    @Override
    public List<BluetoothLeAdapter> getAdapters() {
        List<BluetoothLeAdapter> adapters = new ArrayList<>();
        if (this.bluetoothManager != null) {
            for (tinyb.BluetoothAdapter adapter : this.bluetoothManager.getAdapters()) {
                adapters.add(new BluetoothLeAdapterImpl(adapter));
            }
        }
        return adapters;
    }

    @Override
    public BluetoothLeAdapter getAdapter(String interfaceName) {
        BluetoothLeAdapterImpl adapter = null;
        if (this.bluetoothManager != null) {
            for (tinyb.BluetoothAdapter ba : this.bluetoothManager.getAdapters()) {
                if (ba.getInterfaceName().equals(interfaceName)) {
                    adapter = new BluetoothLeAdapterImpl(ba);
                    break;
                }
            }
        }
        return adapter;
    }

    private boolean startBluetoothSystemd() {
        String systemdCommand = "systemctl start bluetooth";
        boolean started = false;
        Process process;
        try {
            process = Runtime.getRuntime().exec(systemdCommand);
            started = process.waitFor() == 0 ? true : false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to start linux systemd bluetooth", e);
        } catch (IOException e) {
            logger.error("Failed to start linux systemd bluetooth", e);
        }
        return started;
    }

    private boolean startBluetoothInitd() {
        String initdCommand = "/etc/init.d/bluetooth start";
        boolean started = false;
        Process process;
        try {
            process = Runtime.getRuntime().exec(initdCommand);
            BufferedReader stdin = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s = stdin.readLine();
            if (s != null && s.toLowerCase().contains("starting bluetooth")) {
                s = stdin.readLine();
                started = s != null && s.toLowerCase().contains("bluetoothd");
            }
        } catch (IOException e) {
            logger.error("Failed to start linux init.d bluetooth", e);
        }
        return started;
    }

    private void startBluetoothDaemon() {
        String daemonCommand = "bluetoothd -E";
        try {
            Runtime.getRuntime().exec(daemonCommand);
        } catch (IOException e) {
            logger.error("Failed to start linux bluetooth service", e);
        }
    }
}
