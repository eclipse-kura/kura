/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.bluetooth;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothBeaconScanListener must be implemented by any class
 * wishing to receive BLE beacon data
 *
 * @deprecated This class is deprecated in favor of {@link org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener}
 *
 */
@ConsumerType
@Deprecated
public interface BluetoothBeaconScanListener {

    /**
     * Fired when bluetooth beacon data is received
     *
     * @param beaconData
     */
    public void onBeaconDataReceived(BluetoothBeaconData beaconData);

}
