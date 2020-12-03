/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.bluetooth.le;

import java.util.List;
import java.util.UUID;

import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattService;

public class BluetoothGattServiceImpl implements BluetoothGattService {

    private final UUID uuid;
    private final String startHandle;
    private final String endHandle;

    public BluetoothGattServiceImpl(String uuid, String startHandle, String endHandle) {
        this.uuid = UUID.fromString(uuid);
        this.startHandle = startHandle;
        this.endHandle = endHandle;
    }

    // --------------------------------------------------------------------
    //
    // BluetoothGattService API
    //
    // --------------------------------------------------------------------

    @Override
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        return null;
    }

    @Override
    public List<BluetoothGattCharacteristic> getCharacterisitcs() {
        return null;
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String getStartHandle() {
        return this.startHandle;
    }

    @Override
    public String getEndHandle() {
        return this.endHandle;
    }
}
