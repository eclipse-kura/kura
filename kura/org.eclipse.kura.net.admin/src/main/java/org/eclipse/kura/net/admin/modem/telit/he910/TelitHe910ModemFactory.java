/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.net.admin.modem.telit.he910;

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
 * Defines Telit HE910 Modem Factory
 * 
 * @author ilya.binshtok
 *
 */
public class TelitHe910ModemFactory implements CellularModemFactory {
	
    //private static final Logger s_logger = LoggerFactory.getLogger(TelitHe910ModemFactory.class);
    
    private static TelitHe910ModemFactory s_factoryInstance = null;
    
	private static ModemTechnologyType s_type = ModemTechnologyType.HSDPA;
	
	private BundleContext s_bundleContext = null;
	private Hashtable<String, TelitHe910> m_modemServices = null;
	
	private ConnectionFactory m_connectionFactory = null;
	
	private TelitHe910ModemFactory() {
		s_bundleContext = FrameworkUtil.getBundle(NetworkConfigurationService.class).getBundleContext();
		
		ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(s_bundleContext, ConnectionFactory.class, null);
		serviceTracker.open(true);
		m_connectionFactory = serviceTracker.getService();
		
		m_modemServices = new Hashtable<String, TelitHe910>();
	}
	
	public static TelitHe910ModemFactory getInstance() {
	    if(s_factoryInstance == null) {
	        s_factoryInstance = new TelitHe910ModemFactory();
	    }
	    return s_factoryInstance;
	}

	@Override
	public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception {
		
		String key = modemDevice.getProductName();
		TelitHe910 telitHe910 = m_modemServices.get(key);

		if (telitHe910 == null) {
			telitHe910 = new TelitHe910(modemDevice, platform, m_connectionFactory);
			m_modemServices.put(key, telitHe910);
		} else {
			telitHe910.setModemDevice(modemDevice);
		}
		
		return telitHe910;
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
