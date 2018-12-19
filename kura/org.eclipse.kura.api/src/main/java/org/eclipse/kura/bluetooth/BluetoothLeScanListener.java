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
package org.eclipse.kura.bluetooth;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothLeScanListener must be implemented by any class
 * wishing to receive notifications on Bluetooth LE
 * scan events.
 *
 * @deprecated
 * 
 */
@ConsumerType
@Deprecated
public interface BluetoothLeScanListener {

    /**
     * Fired when an error in the scan has occurred.
     *
     * @param errorCode
     */
    public void onScanFailed(int errorCode);

    /**
     * Fired when the Bluetooth LE scan is complete.
     *
     * @param devices
     *            A list of found devices
     */
    public void onScanResults(List<BluetoothDevice> devices);

}
