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

package org.eclipse.kura.linux.net.util;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetInterfaceType;

public class LinuxIfconfig {
	
	private String m_name;
	private NetInterfaceType m_type;
	private String m_macAddress;
	private String m_inetAddress;
	private String m_inetBcast;
	private String m_inetMask;
	private int m_mtu;
	private boolean m_multicast;
	private Map<String,String> m_driver;
	
	public LinuxIfconfig(String name) {
		m_name = name;
		m_type = NetInterfaceType.UNKNOWN;
	}
	public String getName() {
		return m_name;
	}
	public NetInterfaceType getType() {
		return m_type;
	}
	public void setType(NetInterfaceType type) {
		m_type = type;
	}
	public String getMacAddress() {
		return m_macAddress;
	}
	public void setMacAddress(String macAddress) {
		m_macAddress = macAddress;
	}
	public String getInetAddress() {
		return m_inetAddress;
	}
	public void setInetAddress(String inetAddress) {
		m_inetAddress = inetAddress;
	}
	public String getInetBcast() {
		return m_inetBcast;
	}
	public void setInetBcast(String inetBcast) {
		m_inetBcast = inetBcast;
	}
	public String getInetMask() {
		return m_inetMask;
	}
	public void setInetMask(String inetMask) {
		m_inetMask = inetMask;
	}
	public int getMtu() {
		return m_mtu;
	}
	public void setMtu(int mtu) {
		m_mtu = mtu;
	}
	public boolean isMulticast() {
		return m_multicast;
	}
	public void setMulticast(boolean multicast) {
		m_multicast = multicast;
	}
	
	public Map<String,String> getDriver() {
		return m_driver;
	}
	
	public void setDriver(Map<String,String> driver) {
		m_driver = driver;
	}
	
	public boolean isUp() {
		boolean ret = false;
		if ((m_inetAddress != null) && (m_inetMask != null)) {
			ret = true;
		}	
		return ret;
	}
	
	public byte[] getMacAddressBytes() throws KuraException {
		
		if(m_macAddress == null) {
			return new byte[]{0, 0, 0, 0, 0, 0};
		}

		String macAddress = new String(m_macAddress);
		macAddress = macAddress.replaceAll(":","");

		byte[] mac = new byte[6];
        for(int i=0; i<6; i++) {
        	mac[i] = (byte) ((Character.digit(macAddress.charAt(i*2), 16) << 4)
        					+ Character.digit(macAddress.charAt(i*2+1), 16));
        }
        
        return mac;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(m_name).append(":-> type: ").append(m_type).append(", MAC: ")
				.append(m_macAddress).append(", IP Address: ")
				.append(m_inetAddress).append(", Netmask: ").append(m_inetMask)
				.append(", Broadcast: ").append(m_inetBcast).append(", MTU: ")
				.append(m_mtu).append(", multicast?: ").append(m_multicast);
		return sb.toString();
	}
}
