/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothService provides a mechanism for interfacing with Standard
 * Bluetooth and Bluetooth LE devices.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @deprecated This class is deprecated in favor of {@link BluetoothLeService}
 *
 */
@ProviderType
@Deprecated
public interface BluetoothService {

    /**
     * Get the default Bluetooth adapter for the host machine.
     *
     * @return Default Bluetooth adapter
     */
    public BluetoothAdapter getBluetoothAdapter();

    /**
     * Get the Bluetooth adapter specified by name.
     *
     * @param name
     *            Name of the Bluetooth Adapter
     * @return Bluetooth Adapter
     */
    public BluetoothAdapter getBluetoothAdapter(String name);

    /**
     * Get the Bluetooth adapter specified by name.
     *
     * @param name
     *            Name of the Bluetooth Adapter
     * @param bbcl
     *            Bluetooth Beacon Listener for commands
     * @return Bluetooth Adapter
     */
    public BluetoothAdapter getBluetoothAdapter(String name, BluetoothBeaconCommandListener bbcl);
}
