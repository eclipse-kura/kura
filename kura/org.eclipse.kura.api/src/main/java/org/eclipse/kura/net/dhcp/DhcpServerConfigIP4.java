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

/**
 * The configuration representing a DHCP server configuration for an IPv4 network.
 *
 * @author eurotech
 *
 */
public class DhcpServerConfigIP4 extends DhcpServerConfigIP<IP4Address>implements DhcpServerConfig4 {

    /**
     * The basic Constructor for a DhcpServerConfigIP4
     *
     * @param interfaceName
     *            the interface name associated with the DhcpServerConfig
     * @param enabled
     *            the status of the DhcpServer as a boolean
     * @param subnet
     *            the subnet of the DhcpServerConfig
     * @param routerAddress
     *            the router IPAddress
     * @param subnetMask
     *            the subnet mask of the DhcpServerConfig
     * @param defaultLeaseTime
     *            the default lease time to issue to DHCP clients
     * @param maximumLeaseTime
     *            the maximum lease time to issue to DHCP clients
     * @param prefix
     *            the network prefix associated with the DhcpServerConfig
     * @param rangeStart
     *            the network starting address to issue to DHCP clients
     * @param rangeEnd
     *            the network ending address to issue to DHCP clients
     * @param passDns
     *            whether or not to pass DNS to DHCP clients
     * @param dnsServers
     *            the DNS servers that will get passed to DHCP clients if passDns is true
     */
	@Deprecated
    public DhcpServerConfigIP4(String interfaceName, boolean enabled, IP4Address subnet, IP4Address routerAddress,
            IP4Address subnetMask, int defaultLeaseTime, int maximumLeaseTime, short prefix, IP4Address rangeStart,
            IP4Address rangeEnd, boolean passDns, List<IP4Address> dnsServers) {

    	super(interfaceName, enabled, subnet, routerAddress, subnetMask, defaultLeaseTime, maximumLeaseTime, prefix,
                rangeStart, rangeEnd, passDns, dnsServers);
    }
	
	/**
     * Private constructor for a DhcpServerConfigIP4 to be invoked by the newDhcpServerConfigIP4 method
     *
     * @param enabled
     *            the status of the DhcpServer as a boolean
     * @param interfaceName
     *            the interface name associated with the DhcpServerConfig
     * @param subnet
     *            the subnet of the DhcpServerConfig
     * @param routerAddress
     *            the router IPAddress
     * @param subnetMask
     *            the subnet mask of the DhcpServerConfig
     * @param defaultLeaseTime
     *            the default lease time to issue to DHCP clients
     * @param maximumLeaseTime
     *            the maximum lease time to issue to DHCP clients
     * @param prefix
     *            the network prefix associated with the DhcpServerConfig
     * @param rangeStart
     *            the network starting address to issue to DHCP clients
     * @param rangeEnd
     *            the network ending address to issue to DHCP clients
     * @param passDns
     *            whether or not to pass DNS to DHCP clients
     * @param dnsServers
     *            the DNS servers that will get passed to DHCP clients if passDns is true
     */
	private DhcpServerConfigIP4(boolean enabled, String interfaceName, IP4Address subnet, IP4Address routerAddress,
            IP4Address subnetMask, int defaultLeaseTime, int maximumLeaseTime, short prefix, IP4Address rangeStart,
            IP4Address rangeEnd, boolean passDns, List<IP4Address> dnsServers) {

    	super(interfaceName, enabled, subnet, routerAddress, subnetMask, defaultLeaseTime, maximumLeaseTime, prefix,
                rangeStart, rangeEnd, passDns, dnsServers);
    }
	
	/**
     * A method that invokes the Constructor after checking that DHCP pool IP addresses are in the subnet.
     *
     * @param interfaceName
     *            the interface name associated with the DhcpServerConfig
     * @param enabled
     *            the status of the DhcpServer as a boolean
     * @param subnet
     *            the subnet of the DhcpServerConfig
     * @param routerAddress
     *            the router IPAddress
     * @param subnetMask
     *            the subnet mask of the DhcpServerConfig
     * @param defaultLeaseTime
     *            the default lease time to issue to DHCP clients
     * @param maximumLeaseTime
     *            the maximum lease time to issue to DHCP clients
     * @param prefix
     *            the network prefix associated with the DhcpServerConfig
     * @param rangeStart
     *            the network starting address to issue to DHCP clients
     * @param rangeEnd
     *            the network ending address to issue to DHCP clients
     * @param passDns
     *            whether or not to pass DNS to DHCP clients
     * @param dnsServers
     *            the DNS servers that will get passed to DHCP clients if passDns is true
     */
    public static DhcpServerConfigIP4 newDhcpServerConfigIP4(String interfaceName, boolean enabled, IP4Address subnet, IP4Address routerAddress,
            IP4Address subnetMask, int defaultLeaseTime, int maximumLeaseTime, short prefix, IP4Address rangeStart,
            IP4Address rangeEnd, boolean passDns, List<IP4Address> dnsServers)  throws KuraException {
		
		DhcpServerConfigIP4 retVal = null;
		if (isIpAddressInSubnet(rangeStart.getHostAddress(), subnet.getHostAddress(), subnetMask.getHostAddress())
				&& isIpAddressInSubnet(rangeEnd.getHostAddress(), subnet.getHostAddress(),
						subnetMask.getHostAddress())) {

			retVal = new DhcpServerConfigIP4(enabled, interfaceName, subnet, routerAddress, subnetMask, defaultLeaseTime, maximumLeaseTime, prefix,
	                rangeStart, rangeEnd, passDns, dnsServers);
		} 

		return retVal;
	}
    
    private static int inet4address2int(Inet4Address inet4addr) {

		byte[] baInet4addr = inet4addr.getAddress();
		int ret = ((baInet4addr[0] & 0xFF) << 24) | ((baInet4addr[1] & 0xFF) << 16) | ((baInet4addr[2] & 0xFF) << 8)
				| ((baInet4addr[3] & 0xFF) << 0);
		return ret;
	}
	
	private static boolean isIpAddressInSubnet(String ip, String subnet, String netmask) throws KuraException {
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
