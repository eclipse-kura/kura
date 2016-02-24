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

import org.eclipse.kura.net.NetConfig;

/**
 * Represents a NAT configuration
 * 
 * @author eurotech
 *
 */
public class FirewallNatConfig implements NetConfig {
	
	/** The source interface (WAN interface) **/
	private String m_sourceInterface;
	
	/** The destination interface (LAN interface) **/
	private String m_destinationInterface; 
	
	/** protocol (i.e. all, tcp, udp) */
	private String m_protocol;
	
	/** source network/host in CIDR notation */
	private String m_source;
	
	/** destination network/host in CIDR notation */
	private String m_destination;
	
	/** Whether or not MASQUERADE should be enabled **/
	private boolean m_masquerade;

	public FirewallNatConfig(String srcIface, String dstIface, String protocol,
			String src, String dst, boolean masquerade) {
		m_sourceInterface = srcIface;
		m_destinationInterface = dstIface;
		m_protocol = protocol;
		m_source = src;
		m_destination = dst;
		m_masquerade = masquerade;
	}
	
	public String getSourceInterface() {
		return m_sourceInterface;
	}

	public String getDestinationInterface() {
		return m_destinationInterface;
	}
	
	public String getProtocol() {
		return m_protocol;
	}
	
	public String getSource() {
		return m_source;
	}

	public String getDestination() {
		return m_destination;
	}
	
	public boolean isMasquerade() {
		return m_masquerade;
	}

	@Override
	public boolean isValid() {
		if ((m_destinationInterface != null)
				&& !m_destinationInterface.trim().isEmpty()
				&& (m_sourceInterface != null)
				&& !m_sourceInterface.trim().isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((m_destinationInterface == null) ? 0 : m_destinationInterface.hashCode());
	
		result = prime
				* result
				+ ((m_sourceInterface == null) ? 0 : m_sourceInterface.hashCode());
		
		result = prime
				* result
				+ ((m_protocol == null) ? 0 : m_protocol.hashCode());
		
		result = prime
				* result
				+ ((m_source == null) ? 0 : m_source.hashCode());
		
		result = prime
				* result
				+ ((m_destination == null) ? 0 : m_destination.hashCode());
		
		result = prime * result + (m_masquerade ? 1231 : 1237);
				
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
		FirewallNatConfig other = (FirewallNatConfig) obj;
		
		if (m_masquerade != other.m_masquerade)
			return false;
		
		if (m_sourceInterface == null) {
			if (other.m_sourceInterface != null) {
				return false;
			}
		} else if (!m_sourceInterface.equals(other.m_sourceInterface)) {
			return false;
		} else if (!m_protocol.equals(other.m_protocol)) {
			return false;
		}
		
		if (m_destinationInterface == null) {
			if (other.m_destinationInterface != null) {
				return false;
			}
		} else if (!m_destinationInterface.equals(other.m_destinationInterface)) {
			return false;
		}
		
		if (m_source == null) {
			if (other.m_source != null) {
				return false;
			}
		} else if (!m_source.equals(other.m_source)) {
			return false;
		}
		
		if (m_destination == null) {
			if (other.m_destination != null) {
				return false;
			}
		} else if (!m_destination.equals(other.m_destination)) {
			return false;
		}

		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FirewallNatConfig [m_sourceInterface=");
		builder.append(m_sourceInterface);
		builder.append(", m_destinationInterface=");
		builder.append(m_destinationInterface);
		builder.append(", m_source=");
		builder.append(m_source);
		builder.append(", m_destination=");
		builder.append(m_destination);
		builder.append("]");
		return builder.toString();
	}
}
