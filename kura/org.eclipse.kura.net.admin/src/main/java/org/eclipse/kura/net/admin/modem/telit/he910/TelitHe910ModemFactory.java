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
package org.eclipse.kura.net.admin.modem.telit.he910;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Defines Telit HE910 Modem Factory
 *
 * @author ilya.binshtok
 *
 */
public class TelitHe910ModemFactory extends AbstractCellularModemFactory<TelitHe910> {

    private static TelitHe910ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory = null;

    private TelitHe910ModemFactory() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class)
                .getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    public static TelitHe910ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new TelitHe910ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.HSDPA;
    }

    @Override
    protected TelitHe910 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new TelitHe910(modemDevice, platform, this.connectionFactory);
    }
}
