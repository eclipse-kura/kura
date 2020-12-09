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
package org.eclipse.kura.bluetooth.listener;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothAdvertisementScanListener must be implemented by any class
 * wishing to receive BLE advertisement data
 *
 * @deprecated
 *
 */
@ConsumerType
@Deprecated
public interface BluetoothAdvertisementScanListener {

    /**
     * Fired when bluetooth advertisement data is received
     *
     * @param btAdData
     */
    public void onAdvertisementDataReceived(BluetoothAdvertisementData btAdData);
}
