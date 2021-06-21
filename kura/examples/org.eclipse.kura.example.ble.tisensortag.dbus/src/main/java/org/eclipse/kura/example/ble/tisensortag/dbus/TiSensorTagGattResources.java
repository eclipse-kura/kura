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
 *******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag.dbus;

import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

public class TiSensorTagGattResources {

    private String name;
    private BluetoothLeGattService gattService;
    private BluetoothLeGattCharacteristic gattValueCharacteristic;

    public TiSensorTagGattResources(String name, BluetoothLeGattService gattService,
            BluetoothLeGattCharacteristic gattValueChar) {
        this.name = name;
        this.gattService = gattService;
        this.gattValueCharacteristic = gattValueChar;
    }

    public String getName() {
        return name;
    }

    public BluetoothLeGattService getGattService() {
        return gattService;
    }

    public BluetoothLeGattCharacteristic getGattValueCharacteristic() {
        return gattValueCharacteristic;
    }
}
