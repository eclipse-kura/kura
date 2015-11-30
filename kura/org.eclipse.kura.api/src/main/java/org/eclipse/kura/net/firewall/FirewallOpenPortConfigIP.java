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
package org.eclipse.kura.net.firewall;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;

/**
 * The base class for firewall open port configurations
 * 
 * @author eurotech
 *
 * @param <T>
 */
public abstract class FirewallOpenPortConfigIP<T extends IPAddress> implements FirewallOpenPortConfig {

	/** The port to open for inbound connections **/
	private int port;
	
	/** Range of ports to open for inbound connections **/
	private String portRange;
	
	/** The type of protocol to allow for inbound connections **/
	private NetProtocol protocol;
	
	/** The (optional) permitted network for inbound connections **/
	private NetworkPair<T> permittedNetwork;
	
	/** The (optional) permitted interface name for inbound connections **/
	private String permittedInterfaceName;
	
	/** The (optional) not permitted interface name for inbound connections **/
	private String unpermittedInterfaceName;
	
	/** The (optional) permitted MAC address for inbound connections **/
	private String permittedMac;
	
	/** The (options) permitted source port range for inbound connections **/
	private String sourcePortRange;
	
	/**
	 * Creates and empty open port configuration
	 */
	public FirewallOpenPortConfigIP() {
		super();
	}

	/**
	 * Creates a complete Open Port configuration
	 * 
	 * @param port					   The port to open for inbound connections
	 * @param protocol				   The type of protocol to allow for inbound connections
	 * @param permittedNetwork		   The (optional) permitted network for inbound connections
	 * @param permittedInterfaceName   The (optional) permitted interface name for inbound connections
	 * @param unpermittedInterfaceName The (optional) not permitted interface name for inbound connections
	 * @param permittedMac			   The (optional) permitted MAC address for inbound connections
	 * @param sourcePortRange		   The (options) permitted source port range for inbound connections
	 */
	public FirewallOpenPortConfigIP(int port, NetProtocol protocol, NetworkPair<T> permittedNetwork, String permittedInterfaceName, String unpermittedInterfaceName, String permittedMac, String sourcePortRange) { 	
		super();
		this.port = port;
		this.portRange = null;
		this.protocol = protocol;
		this.permittedNetwork = permittedNetwork;
		this.permittedInterfaceName = permittedInterfaceName;
		this.unpermittedInterfaceName = unpermittedInterfaceName;
		this.permittedMac = permittedMac;
		this.sourcePortRange = sourcePortRange;
	}
	
	public FirewallOpenPortConfigIP(String portRange, NetProtocol protocol, NetworkPair<T> permittedNetwork, String permittedInterfaceName, String unpermittedInterfaceName, String permittedMac, String sourcePortRange) { 	
		super();
		this.portRange = portRange;
		this.port = -1;
		this.protocol = protocol;
		this.permittedNetwork = permittedNetwork;
		this.permittedInterfaceName = permittedInterfaceName;
		this.unpermittedInterfaceName = unpermittedInterfaceName;
		this.permittedMac = permittedMac;
		this.sourcePortRange = sourcePortRange;
	}

	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getPortRange() {
		return portRange;
	}
	
	public void setPortRange(String portRange) {
		this.portRange = portRange;
	}

	public NetProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(NetProtocol protocol) {
		this.protocol = protocol;
	}

	public NetworkPair<T> getPermittedNetwork() {
		return permittedNetwork;
	}

	public void setPermittedNetwork(NetworkPair<T> permittedNetwork) {
		this.permittedNetwork = permittedNetwork;
	}

	public String getPermittedInterfaceName() {
		return permittedInterfaceName;
	}

	public void setPermittedInterfaceName(String permittedInterfaceName) {
		this.permittedInterfaceName = permittedInterfaceName;
	}

	public String getUnpermittedInterfaceName() {
		return unpermittedInterfaceName;
	}

	public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
		this.unpermittedInterfaceName = unpermittedInterfaceName;
	}
	
	public String getPermittedMac() {
		return permittedMac;
	}

	public void setPermittedMac(String permittedMac) {
		this.permittedMac = permittedMac;
	}

	public String getSourcePortRange() {
		return sourcePortRange;
	}

	public void setSourcePortRange(String sourcePortRange) {
		this.sourcePortRange = sourcePortRange;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((permittedInterfaceName == null) ? 0
						: permittedInterfaceName.hashCode());
		result = prime * result
				+ ((permittedMac == null) ? 0 : permittedMac.hashCode());
		result = prime
				* result
				+ ((permittedNetwork == null) ? 0 : permittedNetwork.hashCode());
		result = prime * result + port;
		result = prime * result
				+ ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result
				+ ((sourcePortRange == null) ? 0 : sourcePortRange.hashCode());
		result = prime
				* result
				+ ((unpermittedInterfaceName == null) ? 0
						: unpermittedInterfaceName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FirewallOpenPortConfigIP other = (FirewallOpenPortConfigIP) obj;
		if (permittedInterfaceName == null) {
			if (other.permittedInterfaceName != null)
				return false;
		} else if (!permittedInterfaceName.equals(other.permittedInterfaceName))
			return false;
		if (permittedMac == null) {
			if (other.permittedMac != null)
				return false;
		} else if (!permittedMac.equals(other.permittedMac))
			return false;
		if (permittedNetwork == null) {
			if (other.permittedNetwork != null)
				return false;
		} else if (!permittedNetwork.equals(other.permittedNetwork))
			return false;
		if (port != other.port)
			return false;
		if (protocol != other.protocol)
			return false;
		if (sourcePortRange == null) {
			if (other.sourcePortRange != null)
				return false;
		} else if (!sourcePortRange.equals(other.sourcePortRange))
			return false;
		if (unpermittedInterfaceName == null) {
			if (other.unpermittedInterfaceName != null)
				return false;
		} else if (!unpermittedInterfaceName
				.equals(other.unpermittedInterfaceName))
			return false;
		return true;
	}

	@Override
	public boolean isValid() {
		if(port < 0 || port > 65535) {
			return false;
		}
		
		if(protocol == null || !protocol.equals(NetProtocol.tcp) || !protocol.equals(NetProtocol.udp)) {
			return false;
		}
		
		//TODO - add checks for optional parameters to make sure if they are not null they are valid
		
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FirewallOpenPortConfigIP [port=");
		builder.append(port);
		builder.append(", protocol=");
		builder.append(protocol);
		builder.append(", permittedNetwork=");
		builder.append(permittedNetwork);
		builder.append(", permittedMac=");
		builder.append(permittedMac);
		builder.append(", sourcePortRange=");
		builder.append(sourcePortRange);
		builder.append("]");
		return builder.toString();
	}
}
