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
package org.eclipse.kura.linux.bluetooth;

import org.eclipse.kura.bluetooth.BluetoothConnector;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.linux.bluetooth.le.BluetoothGattImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BluetoothDeviceImpl implements BluetoothDevice {

    public static final int DEVICE_TYPE_DUAL = 0x003;
    public static final int DEVICE_TYPE_LE = 0x002;
    public static final int DEVICE_TYPE_UNKNOWN = 0x000;

    private final String m_name;
    private final String m_address;

    public BluetoothDeviceImpl(String address, String name) {
        this.m_address = address;
        this.m_name = name;
    }

    // --------------------------------------------------------------------
    //
    // BluetoothDevice API
    //
    // --------------------------------------------------------------------
    @Override
    public String getName() {
        return this.m_name;
    }

    @Override
    public String getAdress() {
        return this.m_address;
    }

    @Override
    public int getType() {
        return DEVICE_TYPE_UNKNOWN;
    }

    @Override
    public BluetoothConnector getBluetoothConnector() {
        BluetoothConnector bluetoothConnector = null;
        BundleContext bundleContext = BluetoothServiceImpl.getBundleContext();
        if (bundleContext != null) {
            ServiceReference<BluetoothConnector> sr = bundleContext.getServiceReference(BluetoothConnector.class);
            if (sr != null) {
                bluetoothConnector = bundleContext.getService(sr);
            }
        }
        return bluetoothConnector;
    }

    @Override
    public BluetoothGatt getBluetoothGatt() {
        return new BluetoothGattImpl(this.m_address);
    }

}
