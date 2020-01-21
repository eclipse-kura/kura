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

import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothBeaconScanListener must be implemented by any class
 * wishing to receive BLE beacon data
 *
 * @deprecated This class is deprecated in favor of {@link BluetoothLeBeaconListener}
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
