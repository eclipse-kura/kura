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
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothNotificationException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristicProperties;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattDescriptor;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattDescriptor;
import com.github.hypfvieh.bluetooth.DeviceManager;

public class BluetoothLeGattCharacteristicImpl implements BluetoothLeGattCharacteristic {

    private final BluetoothGattCharacteristic characteristic;

    public BluetoothLeGattCharacteristicImpl(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid) throws KuraBluetoothResourceNotFoundException {
        BluetoothGattDescriptor descriptor;
        descriptor = this.characteristic.getGattDescriptorByUuid(uuid.toString());
        if (descriptor != null) {
            return new BluetoothLeGattDescriptorImpl(descriptor);
        } else {
            throw new KuraBluetoothResourceNotFoundException("Descriptor not found");
        }
    }

    @Override
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid, long timeout)
            throws KuraBluetoothResourceNotFoundException {
        return findDescriptor(uuid);
    }

    @Override
    public List<BluetoothLeGattDescriptor> findDescriptors() throws KuraBluetoothResourceNotFoundException {
        List<BluetoothGattDescriptor> descriptorList = this.characteristic.getGattDescriptors();
        List<BluetoothLeGattDescriptor> descriptors = new ArrayList<>();
        if (descriptorList != null) {
            for (BluetoothGattDescriptor descriptor : descriptorList) {
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
            value = this.characteristic.readValue(null);
        } catch (DBusException e) {
            throw new KuraBluetoothIOException(e, "Read characteristic value failed");
        }
        return value;
    }

    @Override
    public void enableValueNotifications(Consumer<byte[]> callback) throws KuraBluetoothNotificationException {
        try {
            DeviceManager.getInstance().registerPropertyHandler(new AbstractPropertiesChangedHandler() {
                @Override
                public void handle(PropertiesChanged props) {
                    if (props != null) {
                        if (!props.getPath().contains(BluetoothLeGattCharacteristicImpl.this.characteristic.getDbusPath())) {
                            return;
                        }

                        for (Map.Entry<String, Variant<?>> entry : props.getPropertiesChanged().entrySet()) {
                            if (entry.getKey().equals("Value")) {
                                byte[] value = (byte[]) props.getPropertiesChanged().get("Value").getValue();
                                if (value != null) {
                                    callback.accept(value);
                                }
                            }
                        }
                    }
                }
            });
            this.characteristic.startNotify();
        } catch (DBusException e) {
            throw new KuraBluetoothNotificationException(e, "Notification can't be enabled");
        }
    }

    @Override
    public void disableValueNotifications() throws KuraBluetoothNotificationException {
        try {
            this.characteristic.stopNotify();
        } catch (DBusException e) {
            throw new KuraBluetoothNotificationException(e, "Notification can't be disabled");
        }
    }

    @Override
    public void writeValue(byte[] value) throws KuraBluetoothIOException {
        try {
            this.characteristic.writeValue(value, null);
        } catch (DBusException e) {
            throw new KuraBluetoothIOException(e, "Write characteristic value failed");
        }
    }

    @Override
    public UUID getUUID() {
        String uuid = this.characteristic.getUuid();
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
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
        Boolean notifying = this.characteristic.isNotifying();
        return notifying == null ? false : notifying;
    }

    @Override
    public List<BluetoothLeGattCharacteristicProperties> getProperties() {
        List<BluetoothLeGattCharacteristicProperties> properties = new ArrayList<>();
        for (String flag : this.characteristic.getFlags()) {
            properties.add(BluetoothLeGattCharacteristicProperties.valueOf(flag.toUpperCase()));
        }
        return properties;
    }

}
