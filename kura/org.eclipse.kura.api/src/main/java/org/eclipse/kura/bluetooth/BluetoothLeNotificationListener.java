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
