/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.ublox.generic;

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
 * Defines generic Ublox Modem Factory
 * 
 * @author ilya.binshtok
 *
 */
public class UbloxModemFactory implements CellularModemFactory {
	    
    private static UbloxModemFactory s_factoryInstance = null;
    
	private static ModemTechnologyType s_type = ModemTechnologyType.HSPA;
	
	private BundleContext s_bundleContext = null;
	private Hashtable<String, UbloxModem> m_modemServices = null;
	
	private ConnectionFactory m_connectionFactory = null;
	
	private UbloxModemFactory() {
		s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();
		
		ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(s_bundleContext, ConnectionFactory.class, null);
		serviceTracker.open(true);
		m_connectionFactory = serviceTracker.getService();
		
		m_modemServices = new Hashtable<String, UbloxModem>();
	}
	
	public static UbloxModemFactory getInstance() {
	    if(s_factoryInstance == null) {
	        s_factoryInstance = new UbloxModemFactory();
	    }
	    return s_factoryInstance;
	}

	@Override
	public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {
		
		String key = modemDevice.getProductName();
		UbloxModem ubloxModem = m_modemServices.get(key);

		if (ubloxModem == null) {
			ubloxModem = new UbloxModem(modemDevice, platform, m_connectionFactory);
			m_modemServices.put(key, ubloxModem);
		} else {
			ubloxModem.setModemDevice(modemDevice);
		}
		
		return ubloxModem;
	}

	@Override
	public Hashtable<String, ? extends CellularModem> getModemServices() {
		return m_modemServices;
	}

	@Override
	public void releaseModemService(String usbPortAddress) {
	    m_modemServices.remove(usbPortAddress);
	}

	@Override
	@Deprecated
	public ModemTechnologyType getType() {
		return s_type;
	}
}
