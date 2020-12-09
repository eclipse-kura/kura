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
