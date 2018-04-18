/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.ble.ibeacon;

import java.util.UUID;

import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;

public class BluetoothLeIBeacon extends BluetoothLeBeacon {

    private UUID uuid;
    private short major;
    private short minor;
    private short txPower;

    public BluetoothLeIBeacon() {
        super();
    }

    public BluetoothLeIBeacon(UUID uuid, short major, short minor, short txPower) {
        super();
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.txPower = txPower;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public short getMajor() {
        return this.major;
    }

    public void setMajor(short major) {
        this.major = major;
    }

    public short getMinor() {
        return this.minor;
    }

    public void setMinor(short minor) {
        this.minor = minor;
    }

    public short getTxPower() {
        return this.txPower;
    }

    public void setTxPower(short txPower) {
        this.txPower = txPower;
    }

}
