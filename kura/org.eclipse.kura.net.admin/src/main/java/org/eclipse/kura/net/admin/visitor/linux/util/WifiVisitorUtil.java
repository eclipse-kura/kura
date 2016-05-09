package org.eclipse.kura.net.admin.visitor.linux.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.net.admin.visitor.linux.HostapdConfigReader;
import org.eclipse.kura.net.wifi.WifiMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
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

public class WifiVisitorUtil {

	private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
	
	public static String getPassphrase(String ifaceName, WifiMode wifiMode) throws KuraException {
		String passphrase = null;
		ConfigurationService cs = getConfigurationService();
		if (cs != null) {
			ComponentConfiguration cc = cs.getConfigurableComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID);
			if (cc != null) {
				Map<String, Object> props = cc.getConfigurationProperties();
				passphrase = (String)props.get(formPassphraseConfigName(ifaceName, wifiMode));
			}
		}
		return passphrase;
	}
	
	public static void setPassphrase(String passphrase, String ifaceName, WifiMode wifiMode) throws KuraException {
		ConfigurationService cs = getConfigurationService();
		if (cs != null) {
			ComponentConfiguration cc = cs.getConfigurableComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID);
			if (cc != null) {
				Map<String, Object> props = cc.getConfigurationProperties();
				props.put(formPassphraseConfigName(ifaceName, wifiMode), passphrase); 
				cs.updateConfiguration(NETWORK_CONFIGURATION_SERVICE_PID, props);
			}
		}
	}
	
	private static String formPassphraseConfigName(String ifaceName, WifiMode wifiMode) throws KuraException {
		StringBuilder sb = new StringBuilder("net.interface.");
		sb.append(ifaceName);
		if (wifiMode == WifiMode.MASTER) {
			sb.append(".config.wifi.master.passphrase");
		} else if (wifiMode == WifiMode.INFRA) {
			sb.append(".config.wifi.infra.passphrase");
		} else {
			throw KuraException.internalError("invalid Wifi Mode: " + wifiMode);
		}
		return sb.toString();
	}
	
	private static ConfigurationService getConfigurationService() {
		ConfigurationService cs = null;
		BundleContext bundleContext = FrameworkUtil.getBundle(HostapdConfigReader.class).getBundleContext();
		if(bundleContext != null) {
			ServiceReference<ConfigurationService> sr = bundleContext.getServiceReference(ConfigurationService.class);
			if (sr != null) {
				cs = bundleContext.getService(sr);
			}
		}
		return cs;
	}
}
