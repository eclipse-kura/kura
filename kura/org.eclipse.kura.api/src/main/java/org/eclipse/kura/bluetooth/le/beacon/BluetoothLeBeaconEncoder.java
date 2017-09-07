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
 * BluetoothLeBeaconEncoder provides a way to encode beacons.
 *
 * @since 1.3
 */
@ConsumerType
public interface BluetoothLeBeaconEncoder<T extends BluetoothLeBeacon> {

    /**
     * Encodes a BluetoothLeBeacon into a byte array
     * 
     * @param beacon
     *            the BluetoothLeBeacon to be broadcast by an advertiser
     * @return byte[]
     */
    public byte[] encode(T beacon);

    /**
     * Get the type of beacon this encoder can manage
     * 
     * @return Class<T> the type of beacon (i.e. BlueoothLeIBeacon)
     * 
     */
    public Class<T> getBeaconType();
}
