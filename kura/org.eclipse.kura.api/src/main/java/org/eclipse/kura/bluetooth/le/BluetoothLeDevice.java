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
 ******************************************************************************/
package org.eclipse.kura.bluetooth.le;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothPairException;
import org.eclipse.kura.KuraBluetoothRemoveException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeDevice represents a Bluetooth LE device to which connections
 * may be made.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
@ProviderType
public interface BluetoothLeDevice {

    /**
     * Find a BluetoothLeGattService specifying the UUID of the service.
     *
     * @param uuid
     *            The UUID of the GATT service
     * @return The BluetoothLeGattService
     * @throws KuraBluetoothResourceNotFoundException
     */
    public BluetoothLeGattService findService(UUID uuid) throws KuraBluetoothResourceNotFoundException;

    /**
     * Find a BluetoothLeGattService specifying the UUID of the service and the timeout in seconds.
     *
     * @since 1.4
     * @param uuid
     *            The UUID of the GATT service
     * @param timeout
     *            The timeout for retrieving the service
     * @return The BluetoothLeGattService
     * @throws KuraBluetoothResourceNotFoundException
     */
    public BluetoothLeGattService findService(UUID uuid, long timeout) throws KuraBluetoothResourceNotFoundException;

    /**
     * Returns a list of BluetoothGattServices available on this device.
     *
     * @return A list of BluetoothLeGattService
     * @throws KuraBluetoothResourceNotFoundException
     */
    public List<BluetoothLeGattService> findServices() throws KuraBluetoothResourceNotFoundException;

    /**
     * Disconnect from this device, removing all connected profiles.
     *
     * @throws KuraBluetoothConnectionException
     */
    public void disconnect() throws KuraBluetoothConnectionException;

    /**
     * A connection to this device is established, connecting each profile
     * flagged as auto-connectable.
     *
     * @throws KuraBluetoothConnectionException
     */
    public void connect() throws KuraBluetoothConnectionException;

    /**
     * Connects a specific profile available on the device, given by UUID
     *
     * @param uuid
     *            The UUID of the profile to be connected
     * @throws KuraBluetoothConnectionException
     */
    public void connectProfile(UUID uuid) throws KuraBluetoothConnectionException;

    /**
     * Disconnects a specific profile available on the device, given by UUID
     *
     * @param uuid
     *            The UUID of the profile to be disconnected
     * @throws KuraBluetoothConnectionException
     */
    public void disconnectProfile(UUID uuid) throws KuraBluetoothConnectionException;

    /**
     * A connection to this device is established, and the device is then
     * paired.
     *
     * @throws KuraBluetoothPairException
     */
    public void pair() throws KuraBluetoothPairException;

    /**
     * Cancel a pairing operation
     *
     * @throws KuraBluetoothPairException
     */
    public void cancelPairing() throws KuraBluetoothPairException;

    /**
     * Returns the hardware address of this device.
     *
     * @return The hardware address of this device.
     */
    public String getAddress();

    /**
     * Returns the remote friendly name of this device.
     *
     * @return The remote friendly name of this device, or NULL if not set.
     */
    public String getName();

    /**
     * Returns an alternative friendly name of this device.
     *
     * @return The alternative friendly name of this device, or NULL if not set.
     */
    public String getAlias();

    /**
     * Sets an alternative friendly name of this device.
     */
    public void setAlias(String value);

    /**
     * Returns the Bluetooth class of the device.
     *
     * @return The Bluetooth class of the device.
     */
    public int getBluetoothClass();

    /**
     * Returns the appearance of the device, as found by GAP service.
     *
     * @return The appearance of the device, as found by GAP service.
     */
    public short getAppearance();

    /**
     * Returns the proposed icon name of the device.
     *
     * @return The proposed icon name, or NULL if not set.
     */
    public String getIcon();

    /**
     * Returns the paired state the device.
     *
     * @return The paired state of the device.
     */
    public boolean isPaired();

    /**
     * Returns the trusted state the device.
     *
     * @return The trusted state of the device.
     */
    public boolean isTrusted();

    /**
     * Sets the trusted state the device.
     */
    public void setTrusted(boolean value);

    /**
     * Returns the blocked state the device.
     *
     * @return The blocked state of the device.
     */
    public boolean isBlocked();

    /**
     * Sets the blocked state the device.
     */
    public void setBlocked(boolean value);

    /**
     * Returns if device uses only pre-Bluetooth 2.1 pairing mechanism.
     *
     * @return True if device uses only pre-Bluetooth 2.1 pairing mechanism.
     */
    public boolean isLegacyPairing();

    /**
     * Returns the Received Signal Strength Indicator of the device.
     *
     * @return The Received Signal Strength Indicator of the device.
     */
    public short getRSSI();

    /**
     * Returns the connected state of the device.
     *
     * @return The connected state of the device.
     */
    public boolean isConnected();

    /**
     * Returns the UUIDs of the device.
     *
     * @return Array containing the UUIDs of the device, ends with NULL.
     */
    public UUID[] getUUIDs();

    /**
     * Returns the local ID of the adapter.
     *
     * @return The local ID of the adapter.
     */
    public String getModalias();

    /**
     * Returns the adapter on which this device was discovered or
     * connected.
     *
     * @return The adapter.
     */
    public BluetoothLeAdapter getAdapter();

    /**
     * Returns a map containing manufacturer specific advertisement data.
     * An entry has a short key and an array of bytes.
     *
     * @return manufacturer specific advertisement data.
     */
    public Map<Short, byte[]> getManufacturerData();

    /**
     * Returns a map containing service advertisement data.
     * An entry has a UUID key and an array of bytes.
     *
     * @return service advertisement data.
     */
    public Map<UUID, byte[]> getServiceData();

    /**
     * Returns the transmission power level (0 means unknown).
     *
     * @return the transmission power level (0 means unknown).
     */
    public short getTxPower();

    /**
     * Returns if the service discovery is ended.
     *
     * @since 2.0
     */
    public boolean isServicesResolved();

    /**
     * Remove this device from the system. Be aware that after the removing the object representing the device
     * will not be valid anymore and any operation on it will have no effect.
     *
     * @return TRUE if the device has been removed
     * @throws BluetKuraBluetoothRemoveExceptionoothException
     *
     * @since 2.0
     */
    public boolean remove() throws KuraBluetoothRemoveException;
}
