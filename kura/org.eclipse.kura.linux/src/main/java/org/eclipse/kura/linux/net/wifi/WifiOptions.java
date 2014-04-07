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
/*
* Copyright (c) 2013 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.linux.net.wifi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;

public class WifiOptions {
	/**
	 * Reports the class name representing this interface.
	 */
	public static final String SERVICE_NAME = WifiOptions.class.getName();

	public static final String WIFI_MANAGED_DRIVER_WEXT = "wext";
	public static final String WIFI_MANAGED_DRIVER_HOSTAP = "hostap";
	public static final String WIFI_MANAGED_DRIVER_ATMEL = "atmel";
	public static final String WIFI_MANAGED_DRIVER_WIRED = "wired";
	public static final String WIFI_MANAGED_DRIVER_NL80211 = "nl80211";
	
	public static Collection<String> getSupportedOptions (String ifaceName) throws KuraException {
		
		Collection<String> options = new HashSet<String>();
		Process procIw = null;
		Process procIwConfig = null;
		Process procWhich = null;
		
		try {
			procWhich = ProcessUtil.exec("which iw");
			BufferedReader br = new BufferedReader(new InputStreamReader(procWhich.getInputStream()));
			String line = br.readLine();
			if (line != null) {
				procIw = ProcessUtil.exec("iw dev " + ifaceName + " info");
				int status = procIw.waitFor();
				if (status == 0) {
					options.add(WIFI_MANAGED_DRIVER_NL80211);
				}
			}

			procIwConfig = ProcessUtil.exec("iwconfig " + ifaceName);
			br = new BufferedReader(new InputStreamReader(procIwConfig.getInputStream()));
			line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains("IEEE 802.11")) {
					options.add(WIFI_MANAGED_DRIVER_WEXT);
					break;
				}
			}
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			ProcessUtil.destroy(procIw);
			ProcessUtil.destroy(procIwConfig);
			ProcessUtil.destroy(procWhich);
		}
		
		return options;
	}
}
