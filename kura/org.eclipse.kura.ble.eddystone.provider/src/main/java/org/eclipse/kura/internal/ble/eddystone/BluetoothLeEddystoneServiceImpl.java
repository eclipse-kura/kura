/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.eddystone;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneService;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconManager;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeEddystoneServiceImpl implements BluetoothLeEddystoneService {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeEddystoneServiceImpl.class);

    private BluetoothLeBeaconManager<BluetoothLeEddystone> bluetoothLeBeaconManager = null;

    public void setBluetoothLeBeaconManager(BluetoothLeBeaconManager<BluetoothLeEddystone> bluetoothLeBeaconManager) {
        this.bluetoothLeBeaconManager = bluetoothLeBeaconManager;
    }

    public void unsetBluetoothLeBeaconManager(BluetoothLeBeaconManager<BluetoothLeEddystone> bluetoothLeBeaconManager) {
        this.bluetoothLeBeaconManager = null;
    }

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le Eddystone Service...");
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Le Eddystone Service...");
    }

    @Override
    public BluetoothLeBeaconScanner<BluetoothLeEddystone> newBeaconScanner(BluetoothLeAdapter adapter) {
        return this.bluetoothLeBeaconManager.newBeaconScanner(adapter, new BluetoothLeEddystoneDecoderImpl());
    }

    @Override
    public BluetoothLeBeaconAdvertiser<BluetoothLeEddystone> newBeaconAdvertiser(BluetoothLeAdapter adapter)
            throws KuraBluetoothBeaconAdvertiserNotAvailable {
        return this.bluetoothLeBeaconManager.newBeaconAdvertiser(adapter, new BluetoothLeEddystoneEncoderImpl());
    }

    @Override
    public void deleteBeaconScanner(BluetoothLeBeaconScanner<BluetoothLeEddystone> scanner) {
        this.bluetoothLeBeaconManager.deleteBeaconScanner(scanner);
    }

    @Override
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeEddystone> advertiser) {
        this.bluetoothLeBeaconManager.deleteBeaconAdvertiser(advertiser);
    }

}