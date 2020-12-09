/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.driver.ble.xdk;

import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

public class XdkGattResources {

    private final String name;
    private final BluetoothLeGattService gattService;
    private final BluetoothLeGattCharacteristic gattValueCharacteristic;

    public XdkGattResources(String name, BluetoothLeGattService gattService,
            BluetoothLeGattCharacteristic gattValueChar) {
        this.name = name;
        this.gattService = gattService;
        this.gattValueCharacteristic = gattValueChar;
    }

    public String getName() {
        return this.name;
    }

    public BluetoothLeGattService getGattService() {
        return this.gattService;
    }

    public BluetoothLeGattCharacteristic getGattValueCharacteristic() {
        return this.gattValueCharacteristic;
    }
}
