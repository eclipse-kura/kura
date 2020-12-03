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
 *******************************************************************************/
package org.eclipse.kura.linux.bluetooth;

import org.eclipse.kura.bluetooth.BluetoothConnector;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.bluetooth.le.BluetoothGattImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BluetoothDeviceImpl implements BluetoothDevice {

    public static final int DEVICE_TYPE_DUAL = 0x003;
    public static final int DEVICE_TYPE_LE = 0x002;
    public static final int DEVICE_TYPE_UNKNOWN = 0x000;

    private final String name;
    private final String address;
    private final CommandExecutorService executorService;

    public BluetoothDeviceImpl(String address, String name, CommandExecutorService executorService) {
        this.address = address;
        this.name = name;
        this.executorService = executorService;
    }

    // --------------------------------------------------------------------
    //
    // BluetoothDevice API
    //
    // --------------------------------------------------------------------
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getAdress() {
        return this.address;
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
        return new BluetoothGattImpl(this.address, this.executorService);
    }

}
