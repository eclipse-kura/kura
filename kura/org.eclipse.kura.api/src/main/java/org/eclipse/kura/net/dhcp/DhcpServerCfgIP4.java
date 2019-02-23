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
package org.eclipse.kura.net.dhcp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The configuration representing a 'networking' portion of DHCP server configuration for an IPv4 network.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.2
 */
@ProviderType
public class DhcpServerCfgIP4 extends DhcpServerCfgIP<IP4Address> {

	/**
     * The basic Constructor for a DhcpServerCfgIP4
     *
     * @param subnet
     *            the subnet of the DhcpServerConfig
     * @param subnetMask
     *            the subnet mask of the DhcpServerConfig
     * @param prefix
     *            the network prefix associated with the DhcpServerConfig
     * @param routerAddress
     *            the router IPAddress           
     * @param rangeStart
     *            the network starting address to issue to DHCP clients
     * @param rangeEnd
     *            the network ending address to issue to DHCP clients
     * @param dnsServers
     *            the DNS servers that will get passed to DHCP clients if passDns is true
     */
	public DhcpServerCfgIP4(IP4Address subnet, IP4Address subnetMask, short prefix, IP4Address routerAddress,
			IP4Address rangeStart, IP4Address rangeEnd, List<IP4Address> dnsServers) {
		super(subnet, subnetMask, prefix, routerAddress, rangeStart, rangeEnd, dnsServers);
	}	
	
	/**
	 * Validates DHCP pool
	 */
	public boolean isValid() throws KuraException {
		boolean ret = false;
		if (isIpAddressInSubnet(getRangeStart().getHostAddress(), getSubnet().getHostAddress(), getSubnetMask().getHostAddress())
				&& isIpAddressInSubnet(getRangeEnd().getHostAddress(), getSubnet().getHostAddress(),
						getSubnetMask().getHostAddress())) {
			ret = true;
		}
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.getClass().getName());
		sb.append(": [subnet=").append(this.getSubnet().getHostAddress()).append(", subnetMask=")
				.append(this.getSubnetMask().getHostAddress()).append(", prefix=").append(this.getPrefix())
				.append(", routerAddress=").append(this.getRouterAddress()).append(", rangeStart=")
				.append(this.getRangeStart()).append(", rangeEnd=").append(this.getRangeEnd());
		for (IP4Address dnsServer : this.getDnsServers()) {
			sb.append(", dnsServer=").append(dnsServer);
		}
		sb.append(']');
		return sb.toString();
	}
	
	private static int inet4address2int(Inet4Address inet4addr) {

		byte[] baInet4addr = inet4addr.getAddress();
		return ((baInet4addr[0] & 0xFF) << 24) | ((baInet4addr[1] & 0xFF) << 16) | ((baInet4addr[2] & 0xFF) << 8) | (baInet4addr[3] & 0xFF);
	}
	
	private boolean isIpAddressInSubnet(String ip, String subnet, String netmask) throws KuraException {
		boolean retVal = false;
		try {
			int iIp = inet4address2int((Inet4Address)InetAddress.getByName(ip));
			int iSubnet = inet4address2int((Inet4Address)InetAddress.getByName(subnet));
			int iNetmask = inet4address2int((Inet4Address)InetAddress.getByName(netmask));
			
			if ((iSubnet & iNetmask) == (iIp & iNetmask)) {
			    retVal = true;
			}
		} catch (UnknownHostException e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
		}
		
		return retVal;
	}
}
