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
package org.eclipse.kura.net.admin.modem.sierra.usb598;

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

public class SierraUsb598ModemFactory implements CellularModemFactory {

    private static SierraUsb598ModemFactory factoryInstance = null;
    private static ModemTechnologyType type = ModemTechnologyType.EVDO;
    private BundleContext bundleContext = null;
    private Hashtable<String, SierraUsb598> modemServices = null;
    private ConnectionFactory connectionFactory = null;

    private SierraUsb598ModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(this.bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();

        this.modemServices = new Hashtable<>();
    }

    public static SierraUsb598ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new SierraUsb598ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {
        String key = modemDevice.getProductName();
        SierraUsb598 sierraUsb598 = this.modemServices.get(key);

        if (sierraUsb598 == null) {
            sierraUsb598 = new SierraUsb598(modemDevice, this.connectionFactory);
            sierraUsb598.bind();
            this.modemServices.put(key, sierraUsb598);
        } else {
            sierraUsb598.setModemDevice(modemDevice);
        }

        return sierraUsb598;
    }

    @Override
    public Hashtable<String, ? extends CellularModem> getModemServices() {
        return this.modemServices;
    }

    @Override
    public void releaseModemService(String usbPortAddress) {
        SierraUsb598 sierraUsb598 = this.modemServices.remove(usbPortAddress);
        sierraUsb598.unbind();
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return type;
    }
}
