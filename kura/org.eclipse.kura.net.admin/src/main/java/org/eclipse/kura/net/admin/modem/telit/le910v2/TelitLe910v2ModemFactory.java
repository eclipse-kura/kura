/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.telit.le910v2;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

public class TelitLe910v2ModemFactory extends AbstractCellularModemFactory<TelitLe910v2> {

    private static TelitLe910v2ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory = null;

    private TelitLe910v2ModemFactory() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class)
                .getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    @Override
    protected TelitLe910v2 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new TelitLe910v2(modemDevice, platform, this.connectionFactory);
    }

    public static TelitLe910v2ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new TelitLe910v2ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.LTE;
    }
}
