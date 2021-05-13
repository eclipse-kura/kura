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
import org.freedesktop.dbus.exceptions.DBusException;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattDescriptor;

public class BluetoothLeGattDescriptorImpl implements BluetoothLeGattDescriptor {

    private final BluetoothGattDescriptor descriptor;

    public BluetoothLeGattDescriptorImpl(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public byte[] readValue() throws KuraBluetoothIOException {
        byte[] value;
        try {
            value = this.descriptor.readValue(null);
        } catch (DBusException e) {
            throw new KuraBluetoothIOException(e, "Read descriptor value failed");
        }
        return value;
    }

    @Override
    public void writeValue(byte[] value) throws KuraBluetoothIOException {
        try {
            this.descriptor.writeValue(value, null);
        } catch (DBusException e) {
            throw new KuraBluetoothIOException(e, "Write descriptor value failed");
        }
    }

    @Override
    public UUID getUUID() {
        String uuid = this.descriptor.getUuid();
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
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
