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
 * BluetoothLeNotificationListener must be implemented by any class
 * wishing to receive notifications on Bluetooth LE
 * notification events.
 *
 * @deprecated
 *
 */
@ConsumerType
@Deprecated
public interface BluetoothLeNotificationListener {

    /**
     * Fired when notification data is received from the
     * Bluetooth LE device.
     *
     * @param handle
     *            Handle of Characteristic
     * @param value
     *            Value received from the device
     */
    public void onDataReceived(String handle, String value);
}
