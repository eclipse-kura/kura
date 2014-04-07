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
package org.eclipse.kura.core.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetUtil 
{
	private static final Logger s_logger = LoggerFactory.getLogger(NetUtil.class);
	
	
	public static String hardwareAddressToString(byte[] macAddress)
	{
		if (macAddress == null) {
			return "N/A";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i<macAddress.length; i++) {
			sb.append(String.format("%02X%s", macAddress[i], (i<macAddress.length-1) ? ":" : ""));		
		}
		return sb.toString();
	}
	
	public static byte[] hardwareAddressToBytes(String macAddress) {
		if(macAddress == null || macAddress.isEmpty()) {
			return new byte[]{0, 0, 0, 0, 0, 0};
		}

		macAddress = macAddress.replaceAll(":","");

		byte[] mac = new byte[6];
        for(int i=0; i<6; i++) {
        	mac[i] = (byte) ((Character.digit(macAddress.charAt(i*2), 16) << 4)
        					+ Character.digit(macAddress.charAt(i*2+1), 16));
        }
        
        return mac;
	}
	
	
	public static String getPrimaryMacAddress()    
	{
		NetworkInterface firstInterface = null;
		Enumeration<NetworkInterface> nifs = null;
		try {
			
			// look for eth0 or en0 first
			nifs = NetworkInterface.getNetworkInterfaces();
			if (nifs != null) {					
				while (nifs.hasMoreElements()) {					
					NetworkInterface nif = nifs.nextElement();
					if ("eth0".equals(nif.getName()) || "en0".equals(nif.getName())) {
						return hardwareAddressToString(nif.getHardwareAddress());
					}
				}
			}
			
			// if not found yet, look for the first active ethernet interface
			nifs = NetworkInterface.getNetworkInterfaces();
			if (nifs != null) {					
				while (nifs.hasMoreElements()) {					
					NetworkInterface nif = nifs.nextElement();
					if (!nif.isVirtual() && nif.getHardwareAddress() != null) {
						firstInterface = nif;
						if (nif.getName().startsWith("eth") || nif.getName().startsWith("en")) {
							return hardwareAddressToString(nif.getHardwareAddress());
						}
					}
				}
			}
			
			if (firstInterface != null) {
				return hardwareAddressToString(firstInterface.getHardwareAddress());
			}
		}
		catch (Exception e) {
			s_logger.warn("Exception while getting current IP", e);
		}

		return null;
	}
	
	
	public static InetAddress getCurrentInetAddress()    
	{
		try {
			
			Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
			if (nifs != null) {
				
				while (nifs.hasMoreElements()) {
					
					NetworkInterface nif = nifs.nextElement();
					if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual() && nif.getHardwareAddress() != null) {
						
						Enumeration<InetAddress> nadrs = nif.getInetAddresses();
	                    while (nadrs.hasMoreElements()) {
	                    	
	                    	InetAddress adr = nadrs.nextElement();
	                        if (adr != null && !adr.isLoopbackAddress() && (nif.isPointToPoint() || !adr.isLinkLocalAddress())) {
	                        	return adr;
	                        }
	                    }
					}
				}
			}
		}
		catch (Exception e) {
			s_logger.warn("Exception while getting current IP", e);
		}
		return null;
	}
}
