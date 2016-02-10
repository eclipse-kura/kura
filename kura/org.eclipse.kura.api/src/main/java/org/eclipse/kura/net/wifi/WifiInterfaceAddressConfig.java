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
package org.eclipse.kura.net.wifi;

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;

/**
 * Contains both the wifi interface status as well as all current configurations
 */
public interface WifiInterfaceAddressConfig extends WifiInterfaceAddress, NetInterfaceAddressConfig {

	/**
	 * Returns a List of NetConfig Objects associated with a given WifiInterfaceAddressConfig
	 * for a given WifiInterface
	 * @return the NetConfig Objects associated with the WifiInterfaceAddressConfig
	 */
	public List<NetConfig> getConfigs();
}
