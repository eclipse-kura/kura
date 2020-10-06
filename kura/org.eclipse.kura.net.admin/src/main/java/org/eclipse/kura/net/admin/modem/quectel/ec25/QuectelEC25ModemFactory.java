/*******************************************************************************
 * Copyright (c) 2019 Sterwen Technology
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.ec25;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Defines Quectel EC25 Modem Factory
 *
 *
 */
public class QuectelEC25ModemFactory extends AbstractCellularModemFactory<QuectelEC25> {

    private static QuectelEC25ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory;

    private BundleContext bundleContext = null;

    private QuectelEC25ModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(this.bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    public static QuectelEC25ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new QuectelEC25ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.LTE;
    }

    @Override
    protected QuectelEC25 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new QuectelEC25(modemDevice, platform, this.connectionFactory);
    }
}
