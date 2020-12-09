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

import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeBeaconScanner allows to manage the scanner mechanism for Bluetooth LE beacons.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.3
 */
@ProviderType
public interface BluetoothLeBeaconScanner<T extends BluetoothLeBeacon> {

    /**
     * Start a scan for beacons of given duration in seconds.
     *
     * @param duration
     *            The scan duration in seconds
     * @throws KuraBluetoothCommandException
     */
    public void startBeaconScan(long duration) throws KuraBluetoothCommandException;

    /**
     * Stop the scan for beacons.
     */
    public void stopBeaconScan();

    /**
     * Indicates if a scan is running.
     *
     * @return
     */
    public boolean isScanning();

    /**
     * Add a listener for detected beacons.
     *
     * @param listener
     *            The beacon listener
     */
    public void addBeaconListener(BluetoothLeBeaconListener<T> listener);

    /**
     * Remove the given beacon listener
     *
     * @param listener
     *            The beacon listener
     */
    public void removeBeaconListener(BluetoothLeBeaconListener<T> listener);

    /**
     * Get the bluetooth adapter this advertiser is associated to.
     *
     * @return BluetoothLeAdapter
     */
    public BluetoothLeAdapter getAdapter();

    /**
     * Get the decoder used by this scanner.
     *
     * @return BluetoothLeBeaconDecoder
     */
    public BluetoothLeBeaconDecoder<T> getDecoder();
}
