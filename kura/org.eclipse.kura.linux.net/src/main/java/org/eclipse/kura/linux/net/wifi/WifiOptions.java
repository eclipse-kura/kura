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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiOptions {
	private static final Logger s_logger = LoggerFactory.getLogger(WifiOptions.class);
	
	/**
	 * Reports the class name representing this interface.
	 */
	public static final String SERVICE_NAME = WifiOptions.class.getName();

	public static final String WIFI_MANAGED_DRIVER_WEXT = "wext";
	public static final String WIFI_MANAGED_DRIVER_HOSTAP = "hostap";
	public static final String WIFI_MANAGED_DRIVER_ATMEL = "atmel";
	public static final String WIFI_MANAGED_DRIVER_WIRED = "wired";
	public static final String WIFI_MANAGED_DRIVER_NL80211 = "nl80211";
	
	private static Map<String, Collection<String>> s_wifiOptions = new HashMap<String, Collection<String>>();
	
	public static Collection<String> getSupportedOptions (String ifaceName) throws KuraException 
	{		
		Collection<String> options = s_wifiOptions.get(ifaceName);
		if (options != null) {
			return options;
		}
		
		options = new HashSet<String>();
		SafeProcess procIw = null;
		SafeProcess procIwConfig = null;
		BufferedReader br = null;
		try {
			if (LinuxNetworkUtil.isToolExists("iw")) {
			    try {
			    	procIw = ProcessUtil.exec("iw dev " + ifaceName + " info");
			    } catch (Exception e) {
			    	s_logger.warn("Failed to execute 'iw dev {} info - {}", ifaceName, e);
			    }
			    
			    if (procIw != null) {
					int status = procIw.waitFor();
					if (status == 0) {
						options.add(WIFI_MANAGED_DRIVER_NL80211);
					}
			    }
			}

			if (LinuxNetworkUtil.isToolExists("iwconfig")) {
				procIwConfig = ProcessUtil.exec("iwconfig " + ifaceName);
				if (procIwConfig.waitFor() == 0) {
					br = new BufferedReader(new InputStreamReader(procIwConfig.getInputStream()));
					String line = null;
					while ((line = br.readLine()) != null) {
						if (line.contains("IEEE 802.11")) {
							options.add(WIFI_MANAGED_DRIVER_WEXT);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (procIw != null) ProcessUtil.destroy(procIw);
			if (procIwConfig != null) ProcessUtil.destroy(procIwConfig);
		}		
		
		s_wifiOptions.put(ifaceName, options);
		return options;
	}
}
