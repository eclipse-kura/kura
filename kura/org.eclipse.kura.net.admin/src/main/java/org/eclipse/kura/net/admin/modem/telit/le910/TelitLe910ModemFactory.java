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

package org.eclipse.kura.net.admin.modem.telit.le910;

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

public class TelitLe910ModemFactory implements CellularModemFactory {

    private static TelitLe910ModemFactory s_factoryInstance = null;

    private static ModemTechnologyType s_type = ModemTechnologyType.LTE;

    private BundleContext s_bundleContext = null;
    private Hashtable<String, TelitLe910> m_modemServices = null;

    private ConnectionFactory m_connectionFactory = null;

    private TelitLe910ModemFactory() {
        this.s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();

        ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<>(
                this.s_bundleContext, ConnectionFactory.class, null);
        serviceTracker.open(true);
        this.m_connectionFactory = serviceTracker.getService();

        this.m_modemServices = new Hashtable<>();
    }

    public static TelitLe910ModemFactory getInstance() {
        if (s_factoryInstance == null) {
            s_factoryInstance = new TelitLe910ModemFactory();
        }
        return s_factoryInstance;
    }

    @Override
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {

        String key = modemDevice.getProductName();
        TelitLe910 telitLe910 = this.m_modemServices.get(key);

        if (telitLe910 == null) {
            telitLe910 = new TelitLe910(modemDevice, platform, this.m_connectionFactory);
            this.m_modemServices.put(key, telitLe910);
        } else {
            telitLe910.setModemDevice(modemDevice);
        }

        return telitLe910;
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
