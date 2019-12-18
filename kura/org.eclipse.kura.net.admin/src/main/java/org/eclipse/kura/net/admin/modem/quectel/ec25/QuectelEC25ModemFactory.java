/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.ec25;

import java.util.Hashtable;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Defines Quectel EC25 Modem Factory
 *
 * @author ilya.binshtok
 *
 */
public class QuectelEC25ModemFactory implements CellularModemFactory {

    // private static final Logger s_logger = LoggerFactory.getLogger(TelitHe910ModemFactory.class);

    private static QuectelEC25ModemFactory s_factoryInstance = null;

    private static ModemTechnologyType s_type = ModemTechnologyType.HSDPA;

    private BundleContext s_bundleContext = null;
    private Hashtable<String, QuectelEC25> m_modemServices = null;

    private ConnectionFactory m_connectionFactory = null;

    private QuectelEC25ModemFactory() {
        this.s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(
                this.s_bundleContext, ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.m_connectionFactory = serviceTracker.getService();

        this.m_modemServices = new Hashtable<String, QuectelEC25>();
    }

    public static QuectelEC25ModemFactory getInstance() {
        if (s_factoryInstance == null) {
            s_factoryInstance = new QuectelEC25ModemFactory();
        }
        return s_factoryInstance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {

        String key = modemDevice.getProductName();
        QuectelEC25 quectelEc25 = this.m_modemServices.get(key);

        if (quectelEc25 == null) {
            quectelEc25 = new QuectelEC25(modemDevice, platform, this.m_connectionFactory);
            this.m_modemServices.put(key, quectelEc25);
        } else {
            quectelEc25.setModemDevice(modemDevice);
        }

        return quectelEc25;
    }

    @Override
    public Hashtable<String, ? extends CellularModem> getModemServices() {
        return this.m_modemServices;
    }

    @Override
    public void releaseModemService(String usbPortAddress) {
        this.m_modemServices.remove(usbPortAddress);
    }

    @Override
    @Deprecated
    public ModemTechnologyType getType() {
        return s_type;
    }
}
