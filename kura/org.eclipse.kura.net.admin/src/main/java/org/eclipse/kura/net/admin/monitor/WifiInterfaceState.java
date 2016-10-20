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
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiInterfaceState extends InterfaceState {
	
	private static final Logger s_logger = LoggerFactory.getLogger(WifiInterfaceState.class);
	/**
	 * WifiInterfaceState
	 * 
	 * @param interfaceName - interface name as {@link String}
	 * @param wifiMode configured wifi mode as {@link WifiMode}
	 * @throws KuraException
	 */
	public WifiInterfaceState(String interfaceName, WifiMode wifiMode) throws KuraException {
		super(NetInterfaceType.WIFI, interfaceName);
		if (WifiMode.MASTER.equals(wifiMode)) {
			if (m_link) {
				boolean isHostapdRunning = HostapdManager.isRunning(interfaceName);
				boolean isIfaceInApMode = WifiMode.MASTER.equals(LinuxNetworkUtil.getWifiMode(interfaceName));
				if (!isHostapdRunning || !isIfaceInApMode) {
					s_logger.warn("WifiInterfaceState() :: !! Link is down for the " + interfaceName
							+ " interface. isHostapdRunning? " + isHostapdRunning + " isIfaceInApMode? "
							+ isIfaceInApMode);
					m_link = false;
				} 
			}
		}
	}
}