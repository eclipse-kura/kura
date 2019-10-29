/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
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
import org.eclipse.kura.executor.CommandExecutorService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothServiceImpl implements BluetoothService {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothServiceImpl.class);

    private static ComponentContext componentContext;
    private CommandExecutorService executorService;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    // --------------------------------------------------------------------
    //
    // Activation APIs
    //
    // --------------------------------------------------------------------
    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Service...");
        componentContext = context;
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Service...");
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
            return new BluetoothAdapterImpl(name, this.executorService);
        } catch (KuraException e) {
            logger.error("Could not get bluetooth adapter", e);
            return null;
        }
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter(String name, BluetoothBeaconCommandListener bbcl) {
        try {
            return new BluetoothAdapterImpl(name, bbcl, this.executorService);
        } catch (KuraException e) {
            logger.error("Could not get bluetooth beacon service", e);
            return null;
        }
    }

    public static BundleContext getBundleContext() {
        return componentContext.getBundleContext();
    }

}
