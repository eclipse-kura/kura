package org.eclipse.kura.net.firewall;

import org.eclipse.kura.net.NetConfig;

public class FirewallReverseNatConfig implements NetConfig {
	
	/** The source interface (WAN interface) for the Reverse NAT configuration **/
	private String m_sourceInterface;
	
	/** The destination interface (LAN interface) for the Reverse NAT configuration **/
	private String m_destinationInterface; 
	
	/** protocol (i.e. all, tcp, udp) */
	private String m_protocol;
	
	/** source network/host in CIDR notation */
	private String m_source;
	
	/** destination network/host in CIDR notation */
	private String m_destination;

	public FirewallReverseNatConfig(String srcIface, String dstIface, String protocol, String src, String dst) {
		m_sourceInterface = srcIface;
		m_destinationInterface = dstIface;
		m_protocol = protocol;
		m_source = src;
		m_destination = dst; 
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
		FirewallReverseNatConfig other = (FirewallReverseNatConfig) obj;
		
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
		builder.append("FirewallReverseNatConfig [m_sourceInterface=");
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
