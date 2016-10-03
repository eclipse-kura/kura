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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothServiceImpl implements BluetoothService {

    private static final Logger s_logger = LoggerFactory.getLogger(BluetoothServiceImpl.class);

    private static ComponentContext s_context;

    // --------------------------------------------------------------------
    //
    // Activation APIs
    //
    // --------------------------------------------------------------------
    protected void activate(ComponentContext context) {
        s_logger.info("Activating Bluetooth Service...");
        s_context = context;
    }

    protected void deactivate(ComponentContext context) {
        s_logger.debug("Deactivating Bluetooth Service...");
    }

    // --------------------------------------------------------------------
    //
    // Service APIs
    //
    // --------------------------------------------------------------------
    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return getBluetoothAdapter("hci0");
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter(String name) {
        try {
            return new BluetoothAdapterImpl(name);
        } catch (KuraException e) {
            s_logger.error("Could not get bluetooth adapter", e);
            return null;
        }
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter(String name, BluetoothBeaconCommandListener bbcl) {
        try {
            BluetoothAdapterImpl bbs = new BluetoothAdapterImpl(name, bbcl);
            return bbs;
        } catch (KuraException e) {
            s_logger.error("Could not get bluetooth beacon service", e);
            return null;
        }
    }

    // --------------------------------------------------------------------
    //
    // Local methods
    //
    // --------------------------------------------------------------------
    static BundleContext getBundleContext() {
        return s_context.getBundleContext();
    }

}
