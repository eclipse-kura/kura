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
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiMode;

public class WifiInterfaceState extends InterfaceState {

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
			if (HostapdManager.isRunning() && WifiMode.MASTER.equals(LinuxNetworkUtil.getWifiMode(interfaceName))) {
				m_link = true;
			}
		}
	}
}