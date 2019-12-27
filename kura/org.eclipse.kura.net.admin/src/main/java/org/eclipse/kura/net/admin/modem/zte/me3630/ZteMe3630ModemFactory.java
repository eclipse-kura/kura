/*******************************************************************************
 * Copyright (c) 2019 3 PORT d.o.o. and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     3 PORT d.o.o.
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.zte.me3630;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

public class ZteMe3630ModemFactory extends AbstractCellularModemFactory<ZteMe3630> {

    private static ZteMe3630ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory;

    private BundleContext bundleContext = null;

    private ZteMe3630ModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(this.bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    public static ZteMe3630ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new ZteMe3630ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.LTE;
    }

    @Override
    protected ZteMe3630 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new ZteMe3630(modemDevice, platform, this.connectionFactory);
    }
}