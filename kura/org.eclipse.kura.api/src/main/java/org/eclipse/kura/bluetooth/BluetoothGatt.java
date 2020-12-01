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
 ******************************************************************************/
package org.eclipse.kura.bluetooth;

import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The BluetoothGatt service is the main communication interface with the Bluettoth LE device. The service
 * will provide information about available services and mechanisms for reading and writing to
 * available characteristics.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @deprecated
 *
 */
@ProviderType
@Deprecated
public interface BluetoothGatt {

    /**
     * Connect to devices GATT server.
     *
     * @return If connection was successful
     */
    public boolean connect() throws KuraException;

    /**
     * Connect to devices GATT server with a given adapter.
     *
     * @param adapterName
     *            the name of the bluetooth adapter
     * @return If connection was successful
     * @since 1.0.8
     */
    public boolean connect(String adapterName) throws KuraException;

    /**
     * Disconnect from devices GATT server.
     */
    public void disconnect();

    /**
     * Check if the device is connected.
     *
     * @return If connection was successful
     */
    public boolean checkConnection() throws KuraException;

    /**
     * Sets the listener by which asynchronous actions of the GATT
     * server will be communicated.
     *
     * @param listener
     *            BluetoothLeListener
     */
    public void setBluetoothLeNotificationListener(BluetoothLeNotificationListener listener);

    /**
     * Return a GATT service based on a UUID.
     *
     * @param uuid
     *            UUID of service
     * @return BluetoothGattService
     */
    public BluetoothGattService getService(UUID uuid);

    /**
     * Get a list of GATT services offered by the device.
     *
     * @return List of services
     */
    public List<BluetoothGattService> getServices();

    /**
     * Get a list of GATT characteristics based on start and end handles. Handle boundaries
     * can be obtained from the {@link #getServices() getServices} method.
     *
     * @param startHandle
     *            Start handle
     * @param endHandle
     *            End handle
     * @return List of GATT characteristics
     */
    public List<BluetoothGattCharacteristic> getCharacteristics(String startHandle, String endHandle);

    /**
     * Read characteristic value from handle.
     *
     * @param handle
     *            Characteristic handle
     * @return Characteristic value
     */
    public String readCharacteristicValue(String handle) throws KuraException;

    /**
     * Read value from characteristic by UUID.
     *
     * @param uuid
     *            UUID of Characteristic
     * @return Characteristic value
     */
    public String readCharacteristicValueByUuid(UUID uuid) throws KuraException;

    /**
     * Write value to characteristic.
     *
     * @param handle
     *            Handle of Characteristic
     * @param value
     *            Value to write to Characteristic
     */
    public void writeCharacteristicValue(String handle, String value);

    /**
     * Get security level.
     *
     * @throws KuraException
     * @since 1.2
     */
    public BluetoothGattSecurityLevel getSecurityLevel() throws KuraException;

    /**
     * Set security level.
     *
     * @param level
     *            Security Level
     * @since 1.2
     */
    public void setSecurityLevel(BluetoothGattSecurityLevel level);
}
