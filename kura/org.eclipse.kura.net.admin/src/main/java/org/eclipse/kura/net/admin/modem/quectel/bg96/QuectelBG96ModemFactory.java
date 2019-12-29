/*******************************************************************************
 * Copyright (c) 2019 Sterwen-Technology
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.bg96;

import java.util.Hashtable;

import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.util.AbstractCellularModemFactory;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Defines Quectel BG96 Modem Factory
 *
 *
 */
public class QuectelBG96ModemFactory extends AbstractCellularModemFactory<QuectelBG96> {

    // private static final Logger s_logger = LoggerFactory.getLogger(TelitHe910ModemFactory.class);

    private static QuectelBG96ModemFactory s_factoryInstance = null;
    private ConnectionFactory connectionFactory;
    private static ModemTechnologyType s_type = ModemTechnologyType.HSDPA;

    private BundleContext s_bundleContext = null;
    private Hashtable<String, QuectelBG96> m_modemServices = null;

    private QuectelBG96ModemFactory() {
        this.s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(
                this.s_bundleContext, ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.connectionFactory = serviceTracker.getService();

        this.m_modemServices = new Hashtable<String, QuectelBG96>();
    }

    public static QuectelBG96ModemFactory getInstance() {
        if (s_factoryInstance == null) {
            s_factoryInstance = new QuectelBG96ModemFactory();
        }
        return s_factoryInstance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {

        String key = modemDevice.getProductName();
        QuectelBG96 quectelBg96 = this.m_modemServices.get(key);

        if (quectelBg96 == null) {
            quectelBg96 = new QuectelBG96(modemDevice, platform, this.connectionFactory);
            this.m_modemServices.put(key, quectelBg96);
        } else {
            quectelBg96.setModemDevice(modemDevice);
        }

        return quectelBg96;
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

    @Override
    protected QuectelBG96 createCellularModem(ModemDevice modemDevice, String platform) throws Exception {
        return new QuectelBG96(modemDevice, platform, this.connectionFactory);
    }
}
