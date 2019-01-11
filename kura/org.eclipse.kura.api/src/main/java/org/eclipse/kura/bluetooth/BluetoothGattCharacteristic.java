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

import java.util.UUID;

import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * 
 * @deprecated This class is deprecated in favor of {@link BluetoothLeGattCharacteristic}
 * 
 */
@ProviderType
@Deprecated
public interface BluetoothGattCharacteristic {

    /*
     * Get UUID of this characteristic
     */
    public UUID getUuid();

    /*
     * Get value of this characteristic
     */
    public Object getValue();

    /*
     * Set value of this characteristic
     */
    public void setValue(Object value);

    /*
     * Get permissions of this characteristic
     */
    public int getPermissions();

    public String getHandle();

    public int getProperties();

    public String getValueHandle();
}
