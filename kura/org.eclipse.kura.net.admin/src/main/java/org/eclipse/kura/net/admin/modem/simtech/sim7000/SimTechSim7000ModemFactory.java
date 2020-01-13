/*******************************************************************************
 * Copyright (c) 2020 3 PORT d.o.o. and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     3 PORT d.o.o.
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.simtech.sim7000;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

public class SimTechSim7000ModemFactory extends AbstractCellularModemFactory<SimTechSim7000> {

    private static SimTechSim7000ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory;

    private BundleContext bundleContext = null;

    private SimTechSim7000ModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(
                this.bundleContext, ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    public static SimTechSim7000ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new SimTechSim7000ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.LTE;
    }

    @Override
    protected SimTechSim7000 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new SimTechSim7000(modemDevice, platform, this.connectionFactory);
    }
}
