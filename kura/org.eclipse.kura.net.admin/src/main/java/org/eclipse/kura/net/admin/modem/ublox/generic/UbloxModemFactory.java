/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.ublox.generic;

import java.util.Hashtable;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Defines generic Ublox Modem Factory
 *
 * @author ilya.binshtok
 *
 */
public class UbloxModemFactory implements CellularModemFactory {

    private static UbloxModemFactory factoryInstance = null;

    private static ModemTechnologyType type = ModemTechnologyType.HSPA;

    private BundleContext bundleContext = null;
    private Hashtable<String, UbloxModem> modemServices = null;

    private ConnectionFactory connectionFactory = null;

    private UbloxModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(this.bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();

        this.modemServices = new Hashtable<>();
    }

    public static UbloxModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new UbloxModemFactory();
        }
        return factoryInstance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {

        String key = modemDevice.getProductName();
        UbloxModem ubloxModem = this.modemServices.get(key);

        if (ubloxModem == null) {
            ubloxModem = new UbloxModem(modemDevice, platform, this.connectionFactory);
            this.modemServices.put(key, ubloxModem);
        } else {
            ubloxModem.setModemDevice(modemDevice);
        }

        return ubloxModem;
    }

    @Override
    public Hashtable<String, ? extends CellularModem> getModemServices() {
        return this.modemServices;
    }

    @Override
    public void releaseModemService(String usbPortAddress) {
        this.modemServices.remove(usbPortAddress);
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return type;
    }
}
