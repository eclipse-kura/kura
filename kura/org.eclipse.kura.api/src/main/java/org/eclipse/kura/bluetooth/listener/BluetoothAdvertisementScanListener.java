/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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
