/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
        if (!startBluetoothUbuntuSnap() && !startBluetoothSystemd() && !startBluetoothInitd()) {
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
}
