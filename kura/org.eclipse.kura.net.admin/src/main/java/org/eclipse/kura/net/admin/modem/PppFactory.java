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
package org.eclipse.kura.net.admin.modem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PppFactory {
	
    private static final Logger s_logger = LoggerFactory.getLogger(PppFactory.class);
    
	private static Map<String, IModemLinkService> s_pppServices = new HashMap<String, IModemLinkService>();
	
	public static IModemLinkService obtainPppService(int pppNo, String port) {
		
		IModemLinkService modemLinkService = null;
		if (pppNo >= 0) {
			modemLinkService = obtainPppService("ppp"+pppNo, port);
		}
		return modemLinkService;
	}
	
	public static IModemLinkService obtainPppService(String iface, String port) {
		
		IModemLinkService modemLinkService = null;
		
		if (s_pppServices.containsKey(iface)) {
			modemLinkService = s_pppServices.get(iface);
		} else {
		    s_logger.debug("Creating new modemLinkService for " + iface);
			modemLinkService = new Ppp(iface, port);
			s_pppServices.put(iface, modemLinkService);
		}
		return modemLinkService;
	}
	
	public static IModemLinkService releasePppService (String iface) {
		
	    IModemLinkService modemLinkService = null;
	    
		if (s_pppServices.containsKey(iface)) {
		    s_logger.debug("Removing modemLinkService for " + iface);
		    modemLinkService = s_pppServices.remove(iface);
		}
		
		return modemLinkService;
	}
	
	public static void releaseAllPppServices() {
		
		Set<String> set = s_pppServices.keySet();
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String iface = it.next();
			s_logger.debug("releasing modm link for {} interface", iface);
			s_pppServices.remove(iface);
		}
	}
}
