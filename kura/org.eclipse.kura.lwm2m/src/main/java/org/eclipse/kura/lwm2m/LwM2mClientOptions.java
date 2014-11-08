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
package org.eclipse.kura.lwm2m;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.system.SystemService;

public class LwM2mClientOptions 
{
	private static final String CLIENT_IP_ADDRESS = "client.ip-address";
	private static final String CLIENT_IP_PORT    = "client.port";
	private static final String SERVER_IP_ADDRESS = "server.ip-address";
	private static final String SERVER_IP_PORT    = "server.port";	
	
	private Map<String,Object> m_properties;
	private SystemService	   m_systemService;
	private NetworkService     m_networkService;
	
	LwM2mClientOptions(Map<String,Object> properties, 
					   SystemService      systemService,
					   NetworkService     networkService)
	{
		m_properties = properties;
		m_systemService = systemService;
		m_networkService = networkService;
	}
		
	/**
	 * Returns the display name for the device.
	 * @return
	 * @throws KuraException 
	 * @throws UnknownHostException 
	 */
	public String getClientIpAddress() 
		throws KuraException, UnknownHostException 
	{
		if (m_properties != null) {			
			String clientIpAddressOption = (String) m_properties.get(CLIENT_IP_ADDRESS);
			if (clientIpAddressOption != null) {
				return clientIpAddressOption;
			}
		}			

		// Use the IP address of the device from the NetworkService.
		String netIfName = m_systemService.getPrimaryNetworkInterfaceName();
		for (NetInterface<? extends NetInterfaceAddress> ni : m_networkService.getActiveNetworkInterfaces()) {
			if (ni.getName().equals(netIfName)) {
			    for (NetInterfaceAddress nia : ni.getNetInterfaceAddresses()) {
			        if (nia.getAddress() instanceof IP4Address) {
			            return nia.getAddress().getHostAddress();
			        }
			    }
			}
		}
		
		return Inet4Address.getLocalHost().getHostAddress();
	}
		
	
	public int getClientIpPort() 
		throws KuraException 
	{
		if (m_properties != null) {			
			Integer clientIpPortOption = (Integer) m_properties.get(CLIENT_IP_PORT);
			if (clientIpPortOption != null) {
				return clientIpPortOption.intValue();
			}
		}
		throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_UNDEFINED, CLIENT_IP_PORT);
	}

	
	public String getServerIpAddress() 
		throws KuraException 
	{
		if (m_properties != null) {			
			String serverIpAddressOption = (String) m_properties.get(SERVER_IP_ADDRESS);
			if (serverIpAddressOption != null) {
				return serverIpAddressOption;
			}
		}
		throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_UNDEFINED, SERVER_IP_ADDRESS);
	}
	

	public int getServerIpPort() 
		throws KuraException 
	{
		if (m_properties != null) {			
			Integer serverIpPortOption = (Integer) m_properties.get(SERVER_IP_PORT);
			if (serverIpPortOption != null) {
				return serverIpPortOption.intValue();
			}
		}
		throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_UNDEFINED, SERVER_IP_PORT);
	}
}
