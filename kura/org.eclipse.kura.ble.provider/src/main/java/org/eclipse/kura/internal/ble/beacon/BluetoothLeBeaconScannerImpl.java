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
package org.eclipse.kura.internal.ble.beacon;

import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconDecoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;

public class BluetoothLeBeaconScannerImpl<T extends BluetoothLeBeacon> implements BluetoothLeBeaconScanner<T> {

    private final BluetoothLeAdapter adapter;
    private final BluetoothLeBeaconDecoder<T> decoder;
    private final BluetoothLeBeaconManagerImpl beaconManager;
    private boolean isScanning;

    public BluetoothLeBeaconScannerImpl(BluetoothLeAdapter adapter, BluetoothLeBeaconDecoder<T> decoder,
            BluetoothLeBeaconManagerImpl beaconManager) {
        this.adapter = adapter;
        this.decoder = decoder;
        this.beaconManager = beaconManager;
        this.isScanning = false;
    }

    @Override
    public BluetoothLeBeaconDecoder<T> getDecoder() {
        return this.decoder;
    }

    @Override
    public BluetoothLeAdapter getAdapter() {
        return this.adapter;
    }

    @Override
    public void startBeaconScan(long timeout) throws KuraBluetoothCommandException {
        try {
            this.beaconManager.startBeaconScan(this.adapter.getInterfaceName());
            this.isScanning = true;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < timeout * 1000L) {
                Thread.sleep(500);
            }
            stopBeaconScan();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stopBeaconScan() {
        this.beaconManager.stopBeaconScan(this.adapter.getInterfaceName());
        this.isScanning = false;
    }

    @Override
    public boolean isScanning() {
        return this.isScanning;
    }

    @Override
    public void addBeaconListener(BluetoothLeBeaconListener<T> listener) {
        this.beaconManager.addBeaconListener((BluetoothLeBeaconListener<BluetoothLeBeacon>) listener,
                this.decoder.getBeaconType());
    }

    @Override
    public void removeBeaconListener(BluetoothLeBeaconListener<T> listener) {
        this.beaconManager.removeBeaconListener((BluetoothLeBeaconListener<BluetoothLeBeacon>) listener);
    }
}
