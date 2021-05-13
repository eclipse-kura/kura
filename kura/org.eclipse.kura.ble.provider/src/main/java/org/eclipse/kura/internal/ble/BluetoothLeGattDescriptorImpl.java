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

import java.util.UUID;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattDescriptor;

import tinyb.BluetoothException;
import tinyb.BluetoothGattDescriptor;

public class BluetoothLeGattDescriptorImpl implements BluetoothLeGattDescriptor {

    private final BluetoothGattDescriptor descriptor;

    public BluetoothLeGattDescriptorImpl(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public byte[] readValue() throws KuraBluetoothIOException {
        byte[] value;
        try {
            value = BluetoothLeGattDescriptorImpl.this.descriptor.readValue();
        } catch (BluetoothException e) {
            throw new KuraBluetoothIOException(e, "Read descriptor value failed");
        }
        return value;
    }

    @Override
    public void writeValue(byte[] value) throws KuraBluetoothIOException {
        try {
            BluetoothLeGattDescriptorImpl.this.descriptor.writeValue(value);
        } catch (BluetoothException e) {
            throw new KuraBluetoothIOException(e, "Write descriptor value failed");
        }
    }

    @Override
    public UUID getUUID() {
        return UUID.fromString(this.descriptor.getUUID());
    }

    @Override
    public BluetoothLeGattCharacteristic getCharacteristic() {
        return new BluetoothLeGattCharacteristicImpl(this.descriptor.getCharacteristic());
    }

    @Override
    public byte[] getValue() {
        return this.descriptor.getValue();
    }

}
