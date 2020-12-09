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

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeBeaconManager allows the management of specific Bluetooth LE Beacon devices.
 * It provides beacon scanner and advertiser classes using the given adapter and codecs.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
@ProviderType
public interface BluetoothLeBeaconManager<T extends BluetoothLeBeacon> {

    /**
     * Instantiate a new scanner for beacons.
     *
     * @param adapter
     *            the bluetooth adapter used by the scanner
     * @param decoder
     *            the decoder used to parse the data acquired by the scanner
     * @return BluetoothLeBeaconScanner
     */
    public BluetoothLeBeaconScanner<T> newBeaconScanner(BluetoothLeAdapter adapter,
            BluetoothLeBeaconDecoder<T> decoder);

    /**
     * Instantiate a new advertiser for beacons.
     *
     * @param adapter
     *            the bluetooth adapter used by the advertiser
     * @param encoder
     *            the encoder used to encode the data to be broadcast
     * @return BluetoothLeBeaconAdvertiser
     * @throws KuraBluetoothBeaconAdvertiserNotAvailable
     */
    public BluetoothLeBeaconAdvertiser<T> newBeaconAdvertiser(BluetoothLeAdapter adapter,
            BluetoothLeBeaconEncoder<T> encoder) throws KuraBluetoothBeaconAdvertiserNotAvailable;

    /**
     * Delete the given scanner.
     *
     * @param scanner
     *            The scanner to be deleted
     */
    public void deleteBeaconScanner(BluetoothLeBeaconScanner<T> scanner);

    /**
     * Delete the given advertiser.
     *
     * @param advertiser
     *            The advertiser to be deleted
     */
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<T> advertiser);
}
