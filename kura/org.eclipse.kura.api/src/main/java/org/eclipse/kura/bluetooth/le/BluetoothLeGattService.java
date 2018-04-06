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

import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeGattService represents a GATT service.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
@ProviderType
public interface BluetoothLeGattService {

    /**
     * Find a BluetoothLeGattCharacteristic specifying the UUID of the characteristic.
     * 
     * @param uuid
     *            The UUID of the GATT characteristic
     * @return The BluetoothLeGattCharacteristic
     * @throws KuraBluetoothResourceNotFoundException
     */
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid) throws KuraBluetoothResourceNotFoundException;

    /**
     * Find a BluetoothLeGattCharacteristic specifying the UUID of the characteristic and the timeout in seconds.
     * 
     * @since 1.4
     * @param uuid
     *            The UUID of the GATT characteristic
     * @param timeout
     *            The timeout for retrieving the service
     * @return The BluetoothLeGattCharacteristic
     * @throws KuraBluetoothResourceNotFoundException
     */
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid, long timeout)
            throws KuraBluetoothResourceNotFoundException;

    /**
     * Returns a list of BluetoothLeGattCharacteristic available on this service.
     * 
     * @return A list of BluetoothLeGattCharacteristic
     * @throws KuraBluetoothResourceNotFoundException
     */
    public List<BluetoothLeGattCharacteristic> findCharacteristics() throws KuraBluetoothResourceNotFoundException;

    /**
     * Get the UUID of this service
     * 
     * @return The 128 byte UUID of this service, NULL if an error occurred
     */
    public UUID getUUID();

    /**
     * Returns the device to which this service belongs to.
     * 
     * @return The device.
     */
    public BluetoothLeDevice getDevice();

    /**
     * Returns true if this service is a primary service, false if secondary.
     * 
     * @return true if this service is a primary service, false if secondary.
     */
    public boolean isPrimary();

}
