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
 * BluetoothBeaconCommandListener must be implemented by any class
 * wishing to receive notifications on Bluetooth Beacon
 * command results.
 *
 * @deprecated
 * 
 */
@ConsumerType
@Deprecated
public interface BluetoothBeaconCommandListener {

    /**
     * Fired when an error in the command execution has occurred.
     *
     * @param errorCode
     */
    public void onCommandFailed(String errorCode);

    /**
     * Fired when the command succeeded.
     *
     */
    public void onCommandResults(String results);
}
