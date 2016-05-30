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

package org.eclipse.kura.net.admin.visitor.linux.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.net.admin.visitor.linux.HostapdConfigReader;
import org.eclipse.kura.net.wifi.WifiMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class WifiVisitorUtil {

	private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
	//private static final Logger s_logger = LoggerFactory.getLogger(WifiVisitorUtil.class);
	/**
	 * Obtains password from configuration snapshot and dercypts it.
	 * 
	 * @param ifaceName - interface name as {@link String}
	 * @param wifiMode - WiFi mode as {@link WifiMode}
	 * @return passphrase as {@link String}
	 * @throws KuraException
	 */
	public static String getPassphrase(String ifaceName, WifiMode wifiMode) throws KuraException {
		String passphrase = null;
		String key = formPassphraseConfigName(ifaceName, wifiMode);
		ConfigurationService configurationService = getConfigurationService();
		CryptoService cryptoService = getCryptoService();
		if ((configurationService != null) && (cryptoService != null)) {
			long snapshotId = 0L;
			Set<Long> snapshotIds = configurationService.getSnapshots();
			for (Long id : snapshotIds) {
				if (id.longValue() > snapshotId) {
					snapshotId = id.longValue();
				}
			}
			ComponentConfiguration netConfigServiceComponentConfig = null;
			List<ComponentConfiguration> componentConfigurations = configurationService.getSnapshot(snapshotId);
			for (ComponentConfiguration cc : componentConfigurations) {
				if (NETWORK_CONFIGURATION_SERVICE_PID.equals(cc.getPid())) {
					netConfigServiceComponentConfig = cc;
					break;
				}
			}
			if (netConfigServiceComponentConfig != null) {
				Map<String, Object> props = netConfigServiceComponentConfig.getConfigurationProperties();
				if (props.containsKey(key)) {
					passphrase = ((Password)props.get(key)).toString();
				}
			}
		}
		return passphrase;
	}
	
	/**
	 * Obtains CryptoService
	 * 
	 * @return CryptoService
	 */
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
	
	/*
	 * Obtains ConfigurationService
	 */
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
	
	/*
	 * Forms password configuration parameter name 
	 */
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
}
