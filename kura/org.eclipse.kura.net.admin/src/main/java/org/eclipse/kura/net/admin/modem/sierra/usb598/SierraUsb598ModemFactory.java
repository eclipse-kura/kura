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
package org.eclipse.kura.net.admin.modem.sierra.usb598;

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

public class SierraUsb598ModemFactory implements CellularModemFactory {

	private static SierraUsb598ModemFactory s_factoryInstance = null;
	private static ModemTechnologyType s_type = ModemTechnologyType.EVDO;
	private BundleContext s_bundleContext = null;
	private Hashtable<String, SierraUsb598> m_modemServices = null;
	private ConnectionFactory m_connectionFactory = null;
	
	private SierraUsb598ModemFactory() {
		s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();
		
		ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(s_bundleContext, ConnectionFactory.class, null);
		serviceTracker.open(true);
		m_connectionFactory = serviceTracker.getService();
		
		m_modemServices = new Hashtable<String, SierraUsb598>();
	}
	
	public static SierraUsb598ModemFactory getInstance() {
	    if(s_factoryInstance == null) {
	        s_factoryInstance = new SierraUsb598ModemFactory();
	    }
	    return s_factoryInstance;
	}

	@Override
	public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {
		String key = modemDevice.getProductName();
		SierraUsb598 sierraUsb598 = m_modemServices.get(key);

		if (sierraUsb598 == null) {
			sierraUsb598 = new SierraUsb598(modemDevice, m_connectionFactory);
			sierraUsb598.bind();
			m_modemServices.put(key, sierraUsb598);
		} else {
			sierraUsb598.setModemDevice(modemDevice);
		}
		
		return sierraUsb598;
	}

	@Override
	public Hashtable<String, ? extends CellularModem> getModemServices() {
		return m_modemServices;
	}

	@Override
	public void releaseModemService(String usbPortAddress) {
		SierraUsb598 sierraUsb598 = m_modemServices.remove(usbPortAddress);
		sierraUsb598.unbind();
	}

	@Override
	@Deprecated
	public ModemTechnologyType getType() {
		return s_type;
	}
}
