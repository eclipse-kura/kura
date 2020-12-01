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
