/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.eg25;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

public class QuectelEG25ModemFactory extends AbstractCellularModemFactory<QuectelEG25> {

    private static QuectelEG25ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory;

    private BundleContext bundleContext = null;

    private QuectelEG25ModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(this.bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    public static QuectelEG25ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new QuectelEG25ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.LTE;
    }

    @Override
    protected QuectelEG25 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new QuectelEG25(modemDevice, platform, this.connectionFactory);
    }
}
