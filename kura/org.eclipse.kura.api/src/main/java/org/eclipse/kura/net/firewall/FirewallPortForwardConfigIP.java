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
package org.eclipse.kura.net.firewall;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;

/**
 * The base class for firewall port forward configurations
 * 
 * @author eurotech
 *
 * @param <T>
 */
public abstract class FirewallPortForwardConfigIP<T extends IPAddress> implements FirewallPortForwardConfig {
	
	/** The interface name on which this configuration will listen for inbound connections **/
	private String inboundIface;
	
	/** The interface name on which packet will be forwarded */
	private String outboundIface;
	
	/** The LAN address to forward to **/
	private IP4Address address;
	
	/** The protocol (TCP or UDP) to listen for and forward **/
	private NetProtocol protocol;
	
	/** The inbound (WAN) port to listen on **/
	private int inPort;
	
	/** The outbound (LAN) port to listen on **/
	private int outPort;
	
	/** use masquerading */
	private boolean masquerade;
	
	/** The (optional) permitted network for inbound connections **/
	private NetworkPair<T> permittedNetwork;
	
	/** The (optional) permitted MAC address for inbound connections **/
	private String permittedMac;
	
	/** The (options) permitted source port range for inbound connections **/
	private String sourcePortRange;
	
	/**
	 * Creates and empty port forward configuration
	 */
	public FirewallPortForwardConfigIP() {
		super();
	}
	
	/**
	 * Creates a complete port forward configuration
	 * 
	 * @param inboundIface			The interface name on which this configuration will listen for inbound connections
	 * @param outboundIface			The inetrface name on which packet will be forwarded
	 * @param address				The LAN address to forward to
	 * @param protocol				The protocol (TCP or UDP) to listen for and forward
	 * @param inPort				The inbound (WAN) port to listen on
	 * @param outPort				The outbound (LAN) port to listen on
	 * @param masquerade			Use masquerade 
	 * @param permittedNetwork		The (optional) permitted network for inbound connections
	 * @param permittedMac			The (optional) permitted MAC address for inbound connections
	 * @param sourcePortRange		The (options) permitted source port range for inbound connections
	 */
	public FirewallPortForwardConfigIP(String inboundIface, String outboundIface, IP4Address address,
			NetProtocol protocol, int inPort, int outPort, boolean masquerade,
			NetworkPair<T> permittedNetwork,
			String permittedMac, String sourcePortRange) {
		super();
		this.inboundIface = inboundIface;
		this.outboundIface = outboundIface;
		this.address = address;
		this.protocol = protocol;
		this.inPort = inPort;
		this.outPort = outPort;
		this.masquerade = masquerade;
		this.permittedNetwork = permittedNetwork;
		this.permittedMac = permittedMac;
		this.sourcePortRange = sourcePortRange;
	}
	
	public String getInboundInterface() {
		return inboundIface;
	}
	
	public void setInboundInterface(String interfaceName) {
		this.inboundIface = interfaceName;
	}
	
	public String getOutboundInterface() {
		return outboundIface;
	}
	
	public void setOutboundInterface(String interfaceName) {
		this.outboundIface = interfaceName;
	}
	
	public IP4Address getAddress() {
		return address;
	}
	
	public void setAddress(IP4Address address) {
		this.address = address;
	}
	
	public NetProtocol getProtocol() {
		return protocol;
	}
	
	public void setProtocol(NetProtocol protocol) {
		this.protocol = protocol;
	}
	
	public int getInPort() {
		return inPort;
	}
	
	public void setInPort(int inPort) {
		this.inPort = inPort;
	}
	
	public int getOutPort() {
		return outPort;
	}
	
	public void setOutPort(int outPort) {
		this.outPort = outPort;
	}
	
	public boolean isMasquerade() {
		return masquerade;
	}

	public void setMasquerade(boolean masquerade) {
		this.masquerade = masquerade;
	}
	
	public NetworkPair<T> getPermittedNetwork() {
		return permittedNetwork;
	}
	
	public void setPermittedNetwork(
			NetworkPair<T> permittedNetwork) {
		this.permittedNetwork = permittedNetwork;
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
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + inPort;
		result = prime * result
				+ ((inboundIface == null) ? 0 : inboundIface.hashCode());
		result = prime * result
				+ ((outboundIface == null) ? 0 : outboundIface.hashCode());
		result = prime * result + (masquerade ? 1231 : 1237);
		result = prime * result + outPort;
		result = prime * result
				+ ((permittedMac == null) ? 0 : permittedMac.hashCode());
		result = prime
				* result
				+ ((permittedNetwork == null) ? 0 : permittedNetwork.hashCode());
		result = prime * result
				+ ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result
				+ ((sourcePortRange == null) ? 0 : sourcePortRange.hashCode());
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
		@SuppressWarnings("rawtypes")
		FirewallPortForwardConfigIP other = (FirewallPortForwardConfigIP) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (inPort != other.inPort)
			return false;
		if (inboundIface == null) {
			if (other.inboundIface != null)
				return false;
		} else if (!inboundIface.equals(other.inboundIface))
			return false;
		if (outboundIface == null) {
			if (other.outboundIface != null)
				return false;
		} else if (!outboundIface.equals(other.outboundIface))
			return false;
		if (outPort != other.outPort)
			return false;
		if (masquerade != other.masquerade)
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
		if (protocol != other.protocol)
			return false;
		if (sourcePortRange == null) {
			if (other.sourcePortRange != null)
				return false;
		} else if (!sourcePortRange.equals(other.sourcePortRange))
			return false;
		return true;
	}
	
	@Override
	public boolean isValid() {
		if(inboundIface == null || inboundIface.trim().isEmpty()) {
			return false;
		}
		
		if(outboundIface == null || outboundIface.trim().isEmpty()) {
			return false;
		}
		
		if(address == null) {
			return false;
		}
		
		if(inPort < 0 || inPort > 65535 || outPort < 0 || outPort > 65535) {
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
		builder.append("FirewallPortForwardConfigIP [inboundIface=");
		builder.append(inboundIface);
		builder.append(", outboundIface=");
		builder.append(outboundIface);
		builder.append(", address=");
		builder.append(address);
		builder.append(", protocol=");
		builder.append(protocol);
		builder.append(", inPort=");
		builder.append(inPort);
		builder.append(", outPort=");
		builder.append(outPort);
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
