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

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
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
public class UbloxModemFactory extends AbstractCellularModemFactory<UbloxModem> {

    private static UbloxModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory = null;

    private UbloxModemFactory() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class)
                .getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    @Override
    protected UbloxModem createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new UbloxModem(modemDevice, platform, this.connectionFactory);
    }

    public static UbloxModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new UbloxModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.HSPA;
    }
}
