/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.bluetooth;

import java.util.List;
import java.util.UUID;

import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * 
 * @deprecated This class is deprecated in favor of {@link BluetoothLeGattService}
 * 
 */
@ProviderType
@Deprecated
public interface BluetoothGattService {

    /*
     * Get characteristic based on UUID
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid);

    /*
     * Get list of characteristics of the service
     */
    public List<BluetoothGattCharacteristic> getCharacterisitcs();

    /*
     * Return the UUID of this service
     */
    public UUID getUuid();

    public String getStartHandle();

    public String getEndHandle();

}
