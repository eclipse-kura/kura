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
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;

public class BluetoothLeBeaconAdvertiserImpl<T extends BluetoothLeBeacon> implements BluetoothLeBeaconAdvertiser<T> {

    private final BluetoothLeAdapter adapter;
    private final BluetoothLeBeaconEncoder<T> encoder;
    private final BluetoothLeBeaconManagerImpl beaconManager;

    public BluetoothLeBeaconAdvertiserImpl(BluetoothLeAdapter adapter, BluetoothLeBeaconEncoder<T> encoder,
            BluetoothLeBeaconManagerImpl beaconManager) {
        this.adapter = adapter;
        this.encoder = encoder;
        this.beaconManager = beaconManager;
    }

    @Override
    public BluetoothLeAdapter getAdapter() {
        return this.adapter;
    }

    @Override
    public void startBeaconAdvertising() throws KuraBluetoothCommandException {
        this.beaconManager.startBeaconAdvertising(this.adapter.getInterfaceName());
    }

    @Override
    public void stopBeaconAdvertising() throws KuraBluetoothCommandException {
        this.beaconManager.stopBeaconAdvertising(this.adapter.getInterfaceName());
    }

    @Override
    public void updateBeaconAdvertisingInterval(Integer min, Integer max) throws KuraBluetoothCommandException {
        this.beaconManager.updateBeaconAdvertisingInterval(min, max, this.adapter.getInterfaceName());
    }

    @Override
    public void updateBeaconAdvertisingData(T beacon) throws KuraBluetoothCommandException {
        this.beaconManager.updateBeaconAdvertisingData((BluetoothLeBeacon) beacon,
                (BluetoothLeBeaconEncoder<BluetoothLeBeacon>) this.encoder, this.adapter.getInterfaceName());
    }
}
