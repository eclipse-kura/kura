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

import org.eclipse.kura.net.IP6Address;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The configuration representing a 'networking' portion of DHCP server configuration for an IPv6 network.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.2
 */
@ProviderType
public class DhcpServerCfgIP6 extends DhcpServerCfgIP<IP6Address> {

	/**
     * The basic Constructor for a DhcpServerCfgIP6
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
	public DhcpServerCfgIP6(IP6Address subnet, IP6Address subnetMask, short prefix, IP6Address routerAddress,
			IP6Address rangeStart, IP6Address rangeEnd, List<IP6Address> dnsServers) {
		super(subnet, subnetMask, prefix, routerAddress, rangeStart, rangeEnd, dnsServers);
	}
	
	/** class validator
	 * 
	 */
	public boolean isValid() {
		// TODO need to range implement validation
		return true;
	}
}
