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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

public class BluetoothLeGattServiceImpl implements BluetoothLeGattService {

    private final BluetoothGattService service;

    public BluetoothLeGattServiceImpl(BluetoothGattService service) {
        this.service = service;
    }

    @Override
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid) throws KuraBluetoothResourceNotFoundException {
        BluetoothGattCharacteristic characteristic;
        characteristic = this.service.getGattCharacteristicByUuid(uuid.toString());
        if (characteristic != null) {
            return new BluetoothLeGattCharacteristicImpl(characteristic);
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt characteristic not found");
        }
    }

    @Override
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid, long timeout)
            throws KuraBluetoothResourceNotFoundException {
        return findCharacteristic(uuid);
    }

    @Override
    public List<BluetoothLeGattCharacteristic> findCharacteristics() throws KuraBluetoothResourceNotFoundException {
        List<BluetoothGattCharacteristic> characteristicList = this.service.getGattCharacteristics();
        List<BluetoothLeGattCharacteristic> characteristics = new ArrayList<>();
        if (characteristicList != null) {
            for (BluetoothGattCharacteristic characteristic : characteristicList) {
                characteristics.add(new BluetoothLeGattCharacteristicImpl(characteristic));
            }
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt characteristics not found");
        }
        return characteristics;
    }

    @Override
    public UUID getUUID() {
        String uuid = this.service.getUuid();
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }

    @Override
    public BluetoothLeDevice getDevice() {
        return new BluetoothLeDeviceImpl(this.service.getDevice());
    }

    @Override
    public boolean isPrimary() {
        Boolean primary = this.service.isPrimary();
        return primary == null ? false : primary;
    }

}
