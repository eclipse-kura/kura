/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.bluetooth.le;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothNotificationException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeGattCharacteristic represents a GATT characteristic.
 * If an application uses value notifications, it has to keep a reference to the corresponding GATT characteristic to
 * avoid that the garbage collector deletes it and removes the associated consumer.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
@ProviderType
public interface BluetoothLeGattCharacteristic {

    /**
     * Find a BluetoothLeGattDescriptor specifying the UUID of the descriptor.
     * 
     * @param uuid
     *            The UUID of the GATT descriptor
     * @return The BluetoothLeGattDescriptor
     * @throws KuraBluetoothResourceNotFoundException
     */
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid) throws KuraBluetoothResourceNotFoundException;

    /**
     * Find a BluetoothLeGattDescriptor specifying the UUID of the descriptor and the timeout in seconds.
     * 
     * @since 1.4
     * @param uuid
     *            The UUID of the GATT descriptor
     * @param timeout
     *            The timeout for retrieving the characteristic
     * @return The BluetoothLeGattDescriptor
     * @throws KuraBluetoothResourceNotFoundException
     */
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid, long timeout)
            throws KuraBluetoothResourceNotFoundException;

    /**
     * Returns a list of BluetoothLeGattDescriptors available on this characteristic.
     * 
     * @return A list of BluetoothLeGattDescriptor
     * @throws KuraBluetoothResourceNotFoundException
     */
    public List<BluetoothLeGattDescriptor> findDescriptors() throws KuraBluetoothResourceNotFoundException;

    /**
     * Reads the value of this characteristic.
     * 
     * @return A byte[] containing the value of this characteristic.
     * @throws KuraBluetoothIOException
     */
    public byte[] readValue() throws KuraBluetoothIOException;

    /**
     * Enables notifications for the value and calls accept function of the Consumer
     * object. It enables notifications for this characteristic at BLE level.
     * If an application uses value notifications, it has to keep a reference to the corresponding GATT characteristic
     * to avoid that the garbage collector deletes it and removes the associated consumer.
     * 
     * @param callback
     *            A Consumer<byte[]> object. Its accept function will be called
     *            when a notification is issued.
     * 
     * @throws KuraBluetoothNotificationException
     */
    public void enableValueNotifications(Consumer<byte[]> callback) throws KuraBluetoothNotificationException;

    /**
     * Disables notifications of the value and unregisters the consumer object
     * passed through the corresponding enable method. It disables notifications
     * at BLE level for this characteristic.
     * 
     * @throws KuraBluetoothNotificationException
     */
    public void disableValueNotifications() throws KuraBluetoothNotificationException;

    /**
     * Writes the value of this characteristic.
     * 
     * @param value
     *            The data as byte[] to be written
     * 
     * @throws KuraBluetoothIOException
     */
    public void writeValue(byte[] value) throws KuraBluetoothIOException;

    /**
     * Get the UUID of this characteristic.
     * 
     * @return The 128 byte UUID of this characteristic, NULL if an error occurred
     */
    public UUID getUUID();

    /**
     * Returns the service to which this characteristic belongs to.
     * 
     * @return The BluetoothLeGattService.
     */
    public BluetoothLeGattService getService();

    /**
     * Returns the cached value of this characteristic, if any.
     * 
     * @return The cached value of this characteristic.
     */
    public byte[] getValue();

    /**
     * Returns true if notification for changes of this characteristic are
     * activated.
     * 
     * @return True if notificatios are activated.
     */
    public boolean isNotifying();

    /**
     * Returns the list of BluetoothLeGattCharacteristicProperties this characteristic has.
     * 
     * @return A list of BluetoothLeGattCharacteristicProperties for this characteristic.
     */
    public List<BluetoothLeGattCharacteristicProperties> getProperties();

}