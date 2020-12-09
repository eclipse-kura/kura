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

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @deprecated This class is deprecated in favor of {@link BluetoothLeIBeacon}
 *
 */
@ProviderType
@Deprecated
@SuppressWarnings("checkstyle:visibilityModifier")
public class BluetoothBeaconData {

    public String uuid;
    public String address;
    public int major;
    public int minor;
    public int rssi;
    public int txpower;

    @Override
    public String toString() {
        return "BluetoothBeaconData [uuid=" + this.uuid + ", address=" + this.address + ", major=" + this.major
                + ", minor=" + this.minor + ", rssi=" + this.rssi + ", txpower=" + this.txpower + "]";
    }
}
