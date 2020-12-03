/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra.usb598;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

public class SierraUsb598ModemFactory extends AbstractCellularModemFactory<SierraUsb598> {

    private static SierraUsb598ModemFactory factoryInstance = null;
    private ConnectionFactory connectionFactory = null;

    private SierraUsb598ModemFactory() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class)
                .getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(bundleContext,
                ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();

    }

    @Override
    protected SierraUsb598 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new SierraUsb598(modemDevice, this.connectionFactory);
    }

    @Override
    protected void shutdownCellularModem(SierraUsb598 modem) {
        modem.unbind();
    }

    public static SierraUsb598ModemFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new SierraUsb598ModemFactory();
        }
        return factoryInstance;
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return ModemTechnologyType.EVDO;
    }
}
