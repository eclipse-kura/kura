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
package org.eclipse.kura.bluetooth.le.beacon.listener;

import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothLeBeaconListener must be implemented by any class wishing to receive BLE beacon data
 *
 * @since 1.3
 */
@ConsumerType
@FunctionalInterface
public interface BluetoothLeBeaconListener<T extends BluetoothLeBeacon> {

    /**
     * Fired when Bluetooth LE beacons data is received
     *
     * @param beacon
     *            a received beacon
     */
    public void onBeaconsReceived(T beacon);
}
