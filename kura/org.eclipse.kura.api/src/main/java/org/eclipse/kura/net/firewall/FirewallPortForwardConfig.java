/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.firewall;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface for firewall port forward configurations
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface FirewallPortForwardConfig extends NetConfig {

    /**
     * The external (WAN) interface to listen for inbound connections on
     *
     * @return The interface name used for this port forward configuration
     */
    public String getInboundInterface();

    /**
     * The internal (LAN) interface packets will be forwarded to
     *
     * @return The interface name used for this port forward configuration
     */
    public String getOutboundInterface();

    /**
     * The LAN IP address to forward connections to
     *
     * @return The LAN IPAddress to forward connections to
     */
    public IP4Address getAddress();

    /**
     * Gets the type of network protocol (TCP or UDP) that is used for this configuration
     *
     * @return The NetProtocol type associated with this interface
     */
    public NetProtocol getProtocol();

    /**
     * The inbound (WAN) port to use for this configuration
     *
     * @return The WAN port number
     */
    public int getInPort();

    /**
     * The outbound (LAN) port to use for this configuration
     *
     * @return The LAN port number
     */
    public int getOutPort();

    /**
     * Use masquerading
     *
     * @return boolean
     */
    public boolean isMasquerade();

    /**
     * Gets the (optional) permitted remote network that can make inbound connections
     *
     * @return The NetworkPair representing the permitted network
     */
    public NetworkPair<? extends IPAddress> getPermittedNetwork();

    /**
     * Gets the (optional) permitted MAC address that is allowed to make inbound connections
     *
     * @return The MAC address that is allowed to make inbound connections
     */
    public String getPermittedMac();

    /**
     * Gets the (optional) permitted source port range that is allowed to make inbound connections
     *
     * @return The source port range that is allowed to make inbound connections
     */
    public String getSourcePortRange();
}
