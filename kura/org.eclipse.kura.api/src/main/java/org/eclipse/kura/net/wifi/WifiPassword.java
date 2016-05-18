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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;

public class WifiPassword extends Password {
	
	private static final String HEXES = "0123456789ABCDEF";
	
	public WifiPassword(String password) {
		super(password);
	}
	
	public WifiPassword(char[] password) {
		super(password);
	}
		
	public void validate(WifiSecurity wifiSecurity) throws KuraException {
		if (m_password == null) {
			throw KuraException.internalError("the passwd can not be null");
		}
		String passKey = new String(m_password).trim();
		if (wifiSecurity == WifiSecurity.SECURITY_WEP) {
			if(passKey.length() == 10) {
				//check to make sure it is all hex
				try {
					Long.parseLong(passKey, 16);
				} catch(Exception e) {
					throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
				}
				
				//since we're here - save the password
				//fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
			} else if(passKey.length() == 26) {
				String part1 = passKey.substring(0, 13);
				String part2 = passKey.substring(13);
				
				try {
					Long.parseLong(part1, 16);
					Long.parseLong(part2, 16);
				} catch(Exception e) {
					throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
				}
				
				//since we're here - save the password
				//fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
			} else if(passKey.length() == 32) {
				String part1 = passKey.substring(0, 10);
				String part2 = passKey.substring(10, 20);
				String part3 = passKey.substring(20);
				try {
					Long.parseLong(part1, 16);
					Long.parseLong(part2, 16);
					Long.parseLong(part3, 16);
				} catch(Exception e) {
					throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
				}
				
				//since we're here - save the password
				//fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
			} else if ((passKey.length() == 5)
					|| (passKey.length() == 13)
					|| (passKey.length() == 16)) {
				
				// 5, 13, or 16 ASCII characters
				passKey = toHex(passKey);
				
				//since we're here - save the password
				//fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
			} else {
				throw KuraException.internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
			}
		} else if ((wifiSecurity == WifiSecurity.SECURITY_WPA)
				|| (wifiSecurity == WifiSecurity.SECURITY_WPA2)
				|| (wifiSecurity == WifiSecurity.SECURITY_WPA_WPA2)) {
			if((passKey.length() < 8) || (passKey.length() > 63)) {
				throw KuraException.internalError("the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: " + passKey);
			}
		}
	}
	
	/*
	 * This method converts supplied string to hex
	 */
	private String toHex(String s) {
		if (s == null) {
			return null;
		}
		byte[] raw = s.getBytes();

		StringBuffer hex = new StringBuffer(2 * raw.length);
		for (int i = 0; i < raw.length; i++) {
			hex.append(HEXES.charAt((raw[i] & 0xF0) >> 4)).append(HEXES.charAt((raw[i] & 0x0F)));
		}
		return hex.toString();
	}
}
