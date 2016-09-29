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
package org.eclipse.kura.linux.net.iptables;

import java.util.List;

/**
 * Creates an iptables command for a Port Forward Rule, allowing an incoming port to be forwarded to destinationIP/port.
 * 
 */
public class PortForwardRule {
	
	//required
	private String m_inboundIface;
	private String m_outboundIface;
	private String m_address;
	private String m_protocol;
	private int m_inPort;
	private int m_outPort;
	private boolean m_masquerade;
	
	//optional
	private String m_permittedNetwork;
	private int m_permittedNetworkMask;
	private String m_permittedMAC;
	private String m_sourcePortRange;

	/**
	 * Constructor of <code>PortForwardRule</code> object.
	 * 
	 * @param inboundIface	interface name on which inbound connection is allowed (such as ppp0) 
	 * @param outboundIface	interface name on which outbound connection is allowed (such as eth0)
	 * @param inPort	inbound port on which to listen for port forward
	 * @param protocol	protocol of port connection (tcp, udp)
	 * @param address	destination IP address to forward IP traffic
	 * @param outPort	destination port to forward IP traffic
	 * @param masquerade  use masquerading 
	 * @param permittedNetwork	source network or ip address from which connection is allowed (such as 192.168.1.0)
	 * @param permittedNetworkMask	source network mask from which connection is allowed (such as 255.255.255.0)
	 * @param permittedMAC	MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
	 * @param sourcePortRange	range of source ports allowed on IP connection (sourcePort1:sourcePort2) 
	 */
	public PortForwardRule(String inboundIface, String outboundIface,
			String address, String protocol, int inPort, int outPort,
			boolean masquerade, String permittedNetwork,
			int permittedNetworkMask, String permittedMAC,
			String sourcePortRange) {
		m_inboundIface = inboundIface;
		m_outboundIface = outboundIface;
		m_inPort = inPort;
		m_protocol = protocol;
		m_address = address;
		m_outPort = outPort;
		m_masquerade = masquerade;
		
		m_permittedNetwork = permittedNetwork;
		m_permittedNetworkMask = permittedNetworkMask;
		m_permittedMAC = permittedMAC;
		m_sourcePortRange = sourcePortRange;
	}
	
	/**
	 * Constructor of <code>PortForwardRule</code> object.
	 */
	public PortForwardRule() {
		m_inboundIface = null;
		m_outboundIface = null;
		m_inPort = 0;
		m_protocol = null;
		m_address = null;
		m_outPort = 0;
		m_masquerade = false;	
		m_permittedNetworkMask = 0;
		m_permittedNetwork = null;
		m_permittedMAC = null;
		m_sourcePortRange = null;
	}
	
	/**
	 * Returns true if the required <code>LocalRule</code> parameters have all been set.  Returns false otherwise.
	 * 
	 * @return		A boolean representing whether all parameters have been set.
	 */
	public boolean isComplete() {
		if ((m_protocol != null) && (m_inboundIface != null)
				&& (m_outboundIface != null) && (m_address != null)
				&& (m_inPort != 0) && (m_outPort != 0)) {
			return true;
		}
		return false;
	}
	
	public NatPreroutingChainRule getNatPreroutingChainRule() {
		int srcPortFirst = 0;
		int srcPortLast = 0;
		if (m_sourcePortRange != null) {	
			srcPortFirst = Integer.parseInt(m_sourcePortRange.split(":")[0]);
			srcPortLast = Integer.parseInt(m_sourcePortRange.split(":")[1]);
		}
		return new NatPreroutingChainRule(m_inboundIface, m_protocol, m_inPort,
				m_outPort, srcPortFirst, srcPortLast, m_address,
				m_permittedNetwork, m_permittedNetworkMask, m_permittedMAC);
	}
	
	public NatPostroutingChainRule getNatPostroutingChainRule() {
		return new NatPostroutingChainRule(m_address, (short)32,
				m_permittedNetwork, (short)m_permittedNetworkMask,
				m_outboundIface, m_protocol, m_masquerade);
	}
	
	public FilterForwardChainRule getFilterForwardChainRule() {
		int srcPortFirst = 0;
		int srcPortLast = 0;
		if (m_sourcePortRange != null) {
			srcPortFirst = Integer.parseInt(m_sourcePortRange.split(":")[0]);
			srcPortLast = Integer.parseInt(m_sourcePortRange.split(":")[1]);
		}
		return new FilterForwardChainRule(m_inboundIface, m_outboundIface,
				m_permittedNetwork, (short) m_permittedNetworkMask, m_address,
				(short) 32, m_protocol, m_permittedMAC, srcPortFirst,
				srcPortLast);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<String>forwardRules = getFilterForwardChainRule().toStrings();
		for (String forwardRule : forwardRules) {
			sb.append(forwardRule).append("; ");
		}
		sb.append(getNatPreroutingChainRule().toString()).append("; ");
		sb.append(getNatPostroutingChainRule().toString());
		return sb.toString();
	}
	
	/**
	 * Getter for inbound iface
	 * 
	 * @return the iface
	 */
	public String getInboundIface() {
		return m_inboundIface;
	}

	/**
	 * Setter for iface
	 * 
	 * @param iface the iface to set
	 */
	public void setInboundIface(String iface) {
		m_inboundIface = iface;
	}
	
	/**
	 * Getter for inbound iface
	 * 
	 * @return the iface
	 */
	public String getOutboundIface() {
		return m_outboundIface;
	}

	/**
	 * Setter for iface
	 * 
	 * @param iface the iface to set
	 */
	public void setOutboundIface(String iface) {
		m_outboundIface = iface;
	}

	/**
	 * Getter for address
	 * 
	 * @return the address
	 */
	public String getAddress() {
		return m_address;
	}

	/**
	 * Setter for address
	 * 
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		m_address = address;
	}

	/**
	 * Getter for protocol
	 * 
	 * @return the protocol
	 */
	public String getProtocol() {
		return m_protocol;
	}

	/**
	 * Setter for protocol
	 * 
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol) {
		m_protocol = protocol;
	}

	/**
	 * Getter for inPort
	 * 
	 * @return the inPort
	 */
	public int getInPort() {
		return m_inPort;
	}

	/**
	 * Setter for inPort
	 * 
	 * @param inPort the inPort to set
	 */
	public void setInPort(int inPort) {
		m_inPort = inPort;
	}

	/**
	 * Getter for outPort
	 * 
	 * @return the outPort
	 */
	public int getOutPort() {
		return m_outPort;
	}

	/**
	 * Setter for outPort
	 * 
	 * @param outPort the outPort to set
	 */
	public void setOutPort(int outPort) {
		m_outPort = outPort;
	}
	
	/**
	 * Getter for masquerade
	 * 
	 * @return the 'masquerade' flag 
	 */
	public boolean isMasquerade() {
		return m_masquerade;
	}

	/**
	 * Setter for masquerade
	 * 
	 * @param masquerade - 'masquerade' flag
	 */
	public void setMasquerade(boolean masquerade) {
		this.m_masquerade = masquerade;
	}

	/**
	 * Getter for permittedNetwork
	 * 
	 * @return the permittedNetwork
	 */
	public String getPermittedNetwork() {
		return m_permittedNetwork;
	}

	/**
	 * Setter for permittedNetwork
	 * 
	 * @param permittedNetwork the permittedNetwork to set
	 */
	public void setPermittedNetwork(String permittedNetwork) {
		m_permittedNetwork = permittedNetwork;
	}
	
	/**
	 * Getter for permittedNetworkMask
	 * 
	 * @return the permittedNetworkMask
	 */
	public int getPermittedNetworkMask() {
		return m_permittedNetworkMask;
	}

	/**
	 * Setter for permittedNetworkMask
	 * 
	 * @param permittedNetworkMask  of the permittedNetwork to set
	 */
	public void setPermittedNetworkMask(int permittedNetworkMask) {
		m_permittedNetworkMask = permittedNetworkMask;
	}

	/**
	 * Getter for permittedMAC
	 * 
	 * @return the permittedMAC
	 */
	public String getPermittedMAC() {
		return m_permittedMAC;
	}

	/**
	 * Setter for permittedMAC
	 * 
	 * @param permittedMAC the permittedMAC to set
	 */
	public void setPermittedMAC(String permittedMAC) {
		m_permittedMAC = permittedMAC;
	}

	/**
	 * Getter for sourcePortRange
	 * 
	 * @return the sourcePortRange
	 */
	public String getSourcePortRange() {
		return m_sourcePortRange;
	}

	/**
	 * Setter for sourcePortRange
	 * 
	 * @param sourcePortRange the sourcePortRange to set
	 */
	public void setSourcePortRange(String sourcePortRange) {
		m_sourcePortRange = sourcePortRange;
	}
	
	@Override
    public boolean equals(Object o) {
        if(!(o instanceof PortForwardRule)) {
            return false;
        }
        
        PortForwardRule other = (PortForwardRule) o;
        
        if (!compareObjects(m_inboundIface, other.m_inboundIface)) {
            return false;
        } else if (!compareObjects(m_outboundIface, other.m_outboundIface)) {
            return false;
        } else if (!compareObjects(m_address, other.m_address)) {
            return false;
        } else if (!compareObjects(m_protocol, other.m_protocol)) {
            return false;
        } else if (m_inPort != other.m_inPort) {
            return false;
        } else if (m_outPort != other.m_outPort) {
            return false;
        } else if (m_masquerade != other.m_masquerade) {
        	return false;
        } else if (!compareObjects(m_permittedNetwork, other.m_permittedNetwork)) {
            return false;
        } else if (m_permittedNetworkMask != other.m_permittedNetworkMask) {
            return false;
        } else if (!compareObjects(m_permittedMAC, other.m_permittedMAC)) {
            return false;
        } else if (!compareObjects(m_sourcePortRange, other.m_sourcePortRange)) {
            return false;
        }
        
        return true;
    }
    
    private boolean compareObjects(Object obj1, Object obj2) {
        if(obj1 != null) {
            return obj1.equals(obj2);
        } else if(obj2 != null) {
            return false;
        }
        
        return true;
    }	

    @Override
    public int hashCode() {
        final int prime = 79;
        int result = 1;
        result = prime * result + m_inPort;
        result = prime * result + m_outPort;
        result = prime * result
                + ((m_inboundIface == null) ? 0 : m_inboundIface.hashCode());
        result = prime * result
                + ((m_outboundIface == null) ? 0 : m_outboundIface.hashCode());
        result = prime * result + (m_masquerade ? 1231 : 1237);
        result = prime * result
                + ((m_address == null) ? 0 : m_address.hashCode());
        result = prime * result
                + ((m_protocol == null) ? 0 : m_protocol.hashCode());
        result = prime * result
                + ((m_permittedNetwork == null) ? 0 : m_permittedNetwork.hashCode());
        result = prime * result + m_permittedNetworkMask;
        result = prime * result
                + ((m_permittedMAC == null) ? 0 : m_permittedMAC.hashCode());
        result = prime * result
                + ((m_sourcePortRange == null) ? 0 : m_sourcePortRange.hashCode());
        
        return result;
    }
}

