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
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothNotificationException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristicProperties;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattDescriptor;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

import tinyb.BluetoothException;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattDescriptor;

public class BluetoothLeGattCharacteristicImpl implements BluetoothLeGattCharacteristic {

    private static final long TIMEOUT = 30;

    private final BluetoothGattCharacteristic characteristic;

    public BluetoothLeGattCharacteristicImpl(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid) throws KuraBluetoothResourceNotFoundException {
        return findDescriptor(uuid, BluetoothLeGattCharacteristicImpl.TIMEOUT);
    }

    @Override
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid, long timeout)
            throws KuraBluetoothResourceNotFoundException {
        BluetoothGattDescriptor descriptor;
        descriptor = this.characteristic.find(uuid.toString(), Duration.ofSeconds(timeout));
        if (descriptor != null) {
            return new BluetoothLeGattDescriptorImpl(descriptor);
        } else {
            throw new KuraBluetoothResourceNotFoundException("Descriptor not found");
        }
    }

    @Override
    public List<BluetoothLeGattDescriptor> findDescriptors() throws KuraBluetoothResourceNotFoundException {
        List<BluetoothGattDescriptor> tinybDescriptors = this.characteristic.getDescriptors();
        List<BluetoothLeGattDescriptor> descriptors = new ArrayList<>();
        if (tinybDescriptors != null) {
            for (BluetoothGattDescriptor descriptor : tinybDescriptors) {
                descriptors.add(new BluetoothLeGattDescriptorImpl(descriptor));
            }
        } else {
            throw new KuraBluetoothResourceNotFoundException("Descriptors not found");
        }
        return descriptors;
    }

    @Override
    public byte[] readValue() throws KuraBluetoothIOException {
        byte[] value;
        try {
            value = BluetoothLeGattCharacteristicImpl.this.characteristic.readValue();
        } catch (BluetoothException e) {
            throw new KuraBluetoothIOException(e, "Read characteristic value failed");
        }
        return value;
    }

    @Override
    public void enableValueNotifications(Consumer<byte[]> callback) throws KuraBluetoothNotificationException {
        BluetoothLeNotification<byte[]> notification = new BluetoothLeNotification<>(callback);
        try {
            this.characteristic.enableValueNotifications(notification);
        } catch (Exception e) {
            throw new KuraBluetoothNotificationException(e, "Notification can't be enabled");
        }
    }

    @Override
    public void disableValueNotifications() throws KuraBluetoothNotificationException {
        try {
            this.characteristic.disableValueNotifications();
        } catch (Exception e) {
            throw new KuraBluetoothNotificationException(e, "Notification can't be disabled");
        }
    }

    @Override
    public void writeValue(byte[] value) throws KuraBluetoothIOException {
        try {
            BluetoothLeGattCharacteristicImpl.this.characteristic.writeValue(value);
        } catch (BluetoothException e) {
            throw new KuraBluetoothIOException(e, "Write characteristic value failed");
        }
    }

    @Override
    public UUID getUUID() {
        return UUID.fromString(this.characteristic.getUUID());
    }

    @Override
    public BluetoothLeGattService getService() {
        return new BluetoothLeGattServiceImpl(this.characteristic.getService());
    }

    @Override
    public byte[] getValue() {
        return this.characteristic.getValue();
    }

    @Override
    public boolean isNotifying() {
        return this.characteristic.getNotifying();
    }

    @Override
    public List<BluetoothLeGattCharacteristicProperties> getProperties() {
        List<BluetoothLeGattCharacteristicProperties> properties = new ArrayList<>();
        for (String flag : this.characteristic.getFlags()) {
            properties.add(BluetoothLeGattCharacteristicProperties.valueOf(flag));
        }
        return properties;
    }

}
