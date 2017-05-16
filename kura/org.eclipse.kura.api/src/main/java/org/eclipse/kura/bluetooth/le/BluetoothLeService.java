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

import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeService provides a mechanism for interfacing with Bluetooth LE devices.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
@ProviderType
public interface BluetoothLeService {

    /**
     * Return a list of available Bluetooth adapters.
     */
    public List<BluetoothLeAdapter> getAdapters();

    /**
     * Search for a Bluetooth adapter with the specified interface name (i.e. hci0).
     * If the adapter is not available, it returns null.
     * 
     * @param interfaceName
     *            the name of the adapter (i.e. hci0)
     */
    public BluetoothLeAdapter getAdapter(String interfaceName);

}
