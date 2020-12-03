/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.generic;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

public class QuectelGenericModemFactory extends AbstractCellularModemFactory<QuectelGeneric> {

    private static QuectelGenericModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory;

    private BundleContext bundleContext = null;

    private QuectelGenericModemFactory() {
        this.bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(this.bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();
    }

    public static QuectelGenericModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new QuectelGenericModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.LTE;
    }

    @Override
    protected QuectelGeneric createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new QuectelGeneric(modemDevice, platform, this.connectionFactory);
    }
}
