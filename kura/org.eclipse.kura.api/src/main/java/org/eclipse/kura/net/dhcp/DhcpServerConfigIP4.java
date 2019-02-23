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

import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The configuration representing a DHCP server configuration for an IPv4 network.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
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
     * The basic Constructor for a DhcpServerConfigIP4
     * 
     * @param dhcpServerCfg DHCP server configuration
     * @param dhcpServerCfgIP4 'network' configuration 
     * @throws KuraException
	 * @since 1.2
     */
	public DhcpServerConfigIP4(DhcpServerCfg dhcpServerCfg, DhcpServerCfgIP4 dhcpServerCfgIP4) throws KuraException {

		super (dhcpServerCfg, dhcpServerCfgIP4);
		if (!isValid() || !dhcpServerCfgIP4.isValid()) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
		}
    }
}
