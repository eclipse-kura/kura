/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.ibeacon;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconService;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconManager;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeIBeaconServiceImpl implements BluetoothLeIBeaconService {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeIBeaconServiceImpl.class);

    private BluetoothLeBeaconManager<BluetoothLeIBeacon> bluetoothLeBeaconManager;

    public void setBluetoothLeBeaconManager(BluetoothLeBeaconManager<BluetoothLeIBeacon> bluetoothLeBeaconManager) {
        this.bluetoothLeBeaconManager = bluetoothLeBeaconManager;
    }

    public void unsetBluetoothLeBeaconManager(BluetoothLeBeaconManager<BluetoothLeIBeacon> bluetoothLeBeaconManager) {
        this.bluetoothLeBeaconManager = null;
    }

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le IBeacon Service...");
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Le IBeacon Service...");
    }

    @Override
    public BluetoothLeBeaconScanner<BluetoothLeIBeacon> newBeaconScanner(BluetoothLeAdapter adapter) {
        return this.bluetoothLeBeaconManager.newBeaconScanner(adapter, new BluetoothLeIBeaconDecoderImpl());
    }

    @Override
    public BluetoothLeBeaconAdvertiser<BluetoothLeIBeacon> newBeaconAdvertiser(BluetoothLeAdapter adapter)
            throws KuraBluetoothBeaconAdvertiserNotAvailable {
        return this.bluetoothLeBeaconManager.newBeaconAdvertiser(adapter, new BluetoothLeIBeaconEncoderImpl());
    }

    @Override
    public void deleteBeaconScanner(BluetoothLeBeaconScanner<BluetoothLeIBeacon> scanner) {
        this.bluetoothLeBeaconManager.deleteBeaconScanner(scanner);
    }

    @Override
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeIBeacon> advertiser) {
        this.bluetoothLeBeaconManager.deleteBeaconAdvertiser(advertiser);
    }

}
