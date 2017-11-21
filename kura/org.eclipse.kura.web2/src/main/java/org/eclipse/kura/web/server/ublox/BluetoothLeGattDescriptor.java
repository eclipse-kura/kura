/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server.ublox;

import java.util.UUID;


/**
 * BluetoothLeGattDescriptor represents a GATT descriptor.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
//@ProviderType
public interface BluetoothLeGattDescriptor {

    /**
     * Reads the value of this descriptor
     * 
     * @return A byte[] containing data from this descriptor
     * @throws KuraBluetoothIOException
     */
    public byte[] readValue() throws Exception;

    /**
     * Writes the value of this descriptor.
     * 
     * @param value
     *            The data as byte[] to be written
     * @throws KuraBluetoothIOException
     */
    public void writeValue(byte[] value) throws Exception;

    /**
     * Get the UUID of this descriptor.
     * 
     * @return The 128 byte UUID of this descriptor, NULL if an error occurred
     */
    public UUID getUUID();

    /**
     * Returns the characteristic to which this descriptor belongs to.
     * 
     * @return The characteristic.
     */
    public BluetoothLeGattCharacteristic getCharacteristic();

    /**
     * Returns the cached value of this descriptor, if any.
     * 
     * @return The cached value of this descriptor.
     */
    public byte[] getValue();

}
