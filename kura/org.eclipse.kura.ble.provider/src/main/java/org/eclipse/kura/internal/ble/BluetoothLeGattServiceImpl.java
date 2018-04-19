/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

public class BluetoothLeGattServiceImpl implements BluetoothLeGattService {

    private static final long TIMEOUT = 30;

    private final BluetoothGattService service;

    public BluetoothLeGattServiceImpl(BluetoothGattService service) {
        this.service = service;
    }

    @Override
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid) throws KuraBluetoothResourceNotFoundException {
        return findCharacteristic(uuid, BluetoothLeGattServiceImpl.TIMEOUT);
    }

    @Override
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid, long timeout)
            throws KuraBluetoothResourceNotFoundException {
        BluetoothGattCharacteristic characteristic;
        characteristic = this.service.find(uuid.toString(), Duration.ofSeconds(timeout));
        if (characteristic != null) {
            return new BluetoothLeGattCharacteristicImpl(characteristic);
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt characteristic not found");
        }
    }

    @Override
    public List<BluetoothLeGattCharacteristic> findCharacteristics() throws KuraBluetoothResourceNotFoundException {
        List<BluetoothGattCharacteristic> tinybCharacteristics = this.service.getCharacteristics();
        List<BluetoothLeGattCharacteristic> characteristics = new ArrayList<>();
        if (tinybCharacteristics != null) {
            for (BluetoothGattCharacteristic characteristic : tinybCharacteristics) {
                characteristics.add(new BluetoothLeGattCharacteristicImpl(characteristic));
            }
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt characteristics not found");
        }
        return characteristics;
    }

    @Override
    public UUID getUUID() {
        return UUID.fromString(this.service.getUUID());
    }

    @Override
    public BluetoothLeDevice getDevice() {
        return new BluetoothLeDeviceImpl(this.service.getDevice());
    }

    @Override
    public boolean isPrimary() {
        return this.service.getPrimary();
    }

}
