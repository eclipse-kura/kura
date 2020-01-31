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
package org.eclipse.kura.linux.bluetooth.le;

import java.util.UUID;

import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;

public class BluetoothGattCharacteristicImpl implements BluetoothGattCharacteristic {

    private final UUID uuid;
    private String handle;
    private int properties;
    private String valueHandle;

    public BluetoothGattCharacteristicImpl(String uuid, String handle, String properties, String valueHandle) {
        this.uuid = UUID.fromString(uuid);
        setHandle(handle);
        setProperties(Integer.parseInt(properties.substring(2, properties.length()), 16));
        setValueHandle(valueHandle);
    }

    // --------------------------------------------------------------------
    //
    // BluetoothGattCharacteristic API
    //
    // --------------------------------------------------------------------
    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public int getPermissions() {
        return 0;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public void setProperties(int properties) {
        this.properties = properties;
    }

    public void setValueHandle(String valueHandle) {
        this.valueHandle = valueHandle;
    }

    @Override
    public String getHandle() {
        return this.handle;
    }

    @Override
    public int getProperties() {
        return this.properties;
    }

    @Override
    public String getValueHandle() {
        return this.valueHandle;
    }

}
