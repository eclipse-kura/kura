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
package org.eclipse.kura.linux.net;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;

public class ConnectionInfoImpl implements ConnectionInfo 
{	
	/**
	 * The interface name associated with the connection information
	 */
	private String m_ifaceName = null;
	
	/**
	 * The properties representing the connection information
	 */
	private Properties m_props = null;

	/**
	 * Creates a ConnectionInfo instance with the previously persisted connection properties if they existed
	 * 
	 * @param ifaceName			The interface name tied to the connection information
	 * @throws KuraException
	 */
	public ConnectionInfoImpl(String ifaceName) throws KuraException {

		m_ifaceName = ifaceName;
		m_props = new Properties();

		try {
			File coninfoFile = new File(formConinfoFileName(ifaceName));
			if (coninfoFile.exists()) {
				m_props.load(new FileInputStream(coninfoFile));
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	@Override
	public IP4Address getIpAddress() {
		IP4Address ipAddress = null;
		String sIpAddress = m_props.getProperty("IPADDR");
		if (sIpAddress != null) {
			try {
				ipAddress = (IP4Address) IPAddress.parseHostAddress(sIpAddress);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return ipAddress;
	}
	
	/**
	 * Gets the gateway address associated with this interface
	 * 
	 * @return		A IP4Address representing the gateway if it is not null
	 */
	public IP4Address getGateway() {
		
		IP4Address gateway = null;
		String sGateway = m_props.getProperty("GATEWAY");
		if (sGateway != null) {
			try {
				gateway = (IP4Address) IPAddress.parseHostAddress(sGateway);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return gateway;
	}
	
	/**
	 * Gets the DNS addresses associated with this interface
	 * 
	 * @return		A List of IP4Address objects representing the DNS of this interface.  If there are none it returns an empty list.
	 */
	public List<IP4Address> getDnsServers() {

		List<IP4Address> lDnsServers = new ArrayList<IP4Address>(2);

		for (int i = 1; i <= 2; i++) {
			String sDns = m_props.getProperty("DNS" + i);
			if (sDns != null) {
				try {
					IP4Address dns = (IP4Address) IPAddress.parseHostAddress(sDns);
					lDnsServers.add(dns);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return lDnsServers;
	}
	
	/**
	 * Gets the interface name associated with this connection information
	 * 
	 * @return		The interface name associated with this connection information
	 */
	public String getIfaceName() {
		return m_ifaceName;
	}

	private String formConinfoFileName (String ifaceName) {
		StringBuffer sb = new StringBuffer();
		sb.append("/tmp/.kura/coninfo-");
		sb.append(ifaceName);
		
		return sb.toString();
	}
}
