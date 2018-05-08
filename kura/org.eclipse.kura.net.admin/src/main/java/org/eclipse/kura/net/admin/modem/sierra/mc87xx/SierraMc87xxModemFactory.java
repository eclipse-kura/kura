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
package org.eclipse.kura.net.admin.modem.sierra.mc87xx;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Defines Sierra MC87xx Modem Factory
 *
 * @author ilya.binshtok
 *
 */
public class SierraMc87xxModemFactory extends AbstractCellularModemFactory<SierraMc87xx> {

    private static SierraMc87xxModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory = null;

    private SierraMc87xxModemFactory() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class)
                .getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    @Override
    protected SierraMc87xx createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new SierraMc87xx(modemDevice, this.connectionFactory);
    }

    public static SierraMc87xxModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new SierraMc87xxModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.HSDPA;
    }
}
