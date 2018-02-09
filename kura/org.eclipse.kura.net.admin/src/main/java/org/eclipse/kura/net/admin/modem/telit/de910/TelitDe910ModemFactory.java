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
package org.eclipse.kura.net.admin.modem.telit.de910;

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

public class TelitDe910ModemFactory implements CellularModemFactory {

    private static TelitDe910ModemFactory s_factoryInstance = null;
    private static ModemTechnologyType s_type = ModemTechnologyType.EVDO;
    private BundleContext s_bundleContext = null;
    private Hashtable<String, TelitDe910> m_modemServices = null;
    private ConnectionFactory m_connectionFactory = null;

    private TelitDe910ModemFactory() {
        this.s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(
                this.s_bundleContext, ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.m_connectionFactory = serviceTracker.getService();

        this.m_modemServices = new Hashtable<>();
    }

    public static TelitDe910ModemFactory getInstance() {
        if (s_factoryInstance == null) {
            s_factoryInstance = new TelitDe910ModemFactory();
        }
        return s_factoryInstance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {
        String key = modemDevice.getProductName();
        TelitDe910 telitDe910 = this.m_modemServices.get(key);

        if (telitDe910 == null) {
            telitDe910 = new TelitDe910(modemDevice, platform, this.m_connectionFactory);
            this.m_modemServices.put(key, telitDe910);
        } else {
            telitDe910.setModemDevice(modemDevice);
        }

        return telitDe910;
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
