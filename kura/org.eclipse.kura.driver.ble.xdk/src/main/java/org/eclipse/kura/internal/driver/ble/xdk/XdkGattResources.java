/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.driver.ble.xdk;

import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

public class XdkGattResources {

    private String name;
    private BluetoothLeGattService gattService;
    private BluetoothLeGattCharacteristic gattValueCharacteristic;

    public XdkGattResources(String name, BluetoothLeGattService gattService,
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
