/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.bluetooth.le.beacon;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothLeBeaconDecoder provides a way to decode beacons.
 *
 * @since 1.3
 */
@ConsumerType
public interface BluetoothLeBeaconDecoder<T extends BluetoothLeBeacon> {

    /**
     * Decodes a byte array into a BluetoothLeBeacon object
     * 
     * @param data
     *            the byte array acquired by a scanner
     * @return BluetoothLeBeacon
     */
    public T decode(byte[] data);

    /**
     * Get the type of beacon this decoder can manage
     * 
     * @return Class<T> the type of beacon (i.e. BlueoothLeIBeacon)
     * 
     */
    public Class<T> getBeaconType();
}
