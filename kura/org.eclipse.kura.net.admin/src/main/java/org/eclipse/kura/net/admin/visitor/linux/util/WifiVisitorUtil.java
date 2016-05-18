package org.eclipse.kura.net.admin.visitor.linux.util;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.net.admin.visitor.linux.HostapdConfigReader;
import org.eclipse.kura.net.wifi.WifiMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger s_logger = LoggerFactory.getLogger(WifiVisitorUtil.class);
	private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
	
	public static String getPassphrase(String ifaceName, WifiMode wifiMode) throws KuraException {
		String passphrase = null;
		String key = formPassphraseConfigName(ifaceName, wifiMode);
		ConfigurationService configurationService = getConfigurationService();
		CryptoService cryptoService = getCryptoService();
		if ((configurationService != null) && (cryptoService != null)) {
			Map<String, Object> props = configurationService.getConfigurableComponentConfigurationProperties(NETWORK_CONFIGURATION_SERVICE_PID);	
			if (props.containsKey(key)) {
				passphrase = (String)props.get(key);
			
				try {
					Password decryptedPassword = new Password(cryptoService.decryptAes(passphrase.toCharArray()));
					passphrase = decryptedPassword.toString();
				} catch (KuraException e) {
				}
			}
		}
		return passphrase;
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
	
	public static CryptoService getCryptoService() {
		CryptoService cs = null;
		BundleContext bundleContext = FrameworkUtil.getBundle(HostapdConfigReader.class).getBundleContext();
		if(bundleContext != null) {
			ServiceReference<CryptoService> sr = bundleContext.getServiceReference(CryptoService.class);
			if (sr != null) {
				cs = bundleContext.getService(sr);
			}
		}
		return cs;
	}
}
