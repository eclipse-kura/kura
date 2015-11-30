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
package org.eclipse.kura.linux.net.iptables;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * Copyright (c) 2013 Eurotech Inc. All rights reserved.
 */

/**
 * Creates an iptables command for a Local Rule, allowing an incoming port connection.
 * 
 * CONFIGURATION
 * 
 * Configuration will be accepted in the form of key/value pairs.  The key/value pairs are
 * strictly defined here:
 * 
 *   CONFIG_ENTRY     -> KEY + "=" + VALUE
 *   KEY              -> TYPE + INDEX + "_" + PARAM
 *   TYPE             -> "LocalRule"
 *   INDEX            -> "0" | "1" | "2" | ... | "N"
 *   PARAM (required) -> "port" | "protocol"
 *   PARAM (optional) -> "permittedNetwork" | "permittedMAC" | "sourcePortRange"
 *   VALUE	          -> (value of the specified parameter)
 * 
 *   EXAMPLE:
 *   
 *   LocalRule0_port=1234
 *   LocalRule0_protocol=tcp
 *   LocalRule0_permittedNetwork=192.168.1.1
 *   LocalRule0_permittedMAC=AA:BB:CC:DD:EE:FF
 *   LocalRule0_sourcePortRange=3333:4444
 */
public class LocalRule {
	
	private static final Logger s_logger = LoggerFactory.getLogger(LocalRule.class);
	
	//required vars
	private int m_port;
	private String m_portRange;
	private String m_protocol;
	
	//optional vars
	private String m_permittedNetworkString;
	private String m_permittedInterfaceName;
	private String m_unpermittedInterfaceName;
	private String m_permittedMAC;
	private String m_sourcePortRange;
	
	/**
	 * Constructor of <code>LocalRule</code> object.
	 * 
	 * @param port	destination local IP port number to allow
	 * @param protocol	protocol of port (tcp, udp)
	 * @param sourcePortRange	range of source ports allowed on IP connection (sourcePort1:sourcePort2) 
	 * @param permittedNetwork	source network or ip address from which connection is allowed (such as 192.168.1.0/24)
	 * @param permittedInterfaceName  only allow open port for this interface
	 * @param unpermittedInterfaceName  allow open port for all interfaces except this one
	 * @param permittedMAC	MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
	 */
	public LocalRule(int port, String protocol, NetworkPair<IP4Address> permittedNetwork, String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC, String sourcePortRange) {
		m_port = port;
		m_portRange = null;
		m_protocol = protocol;
		m_sourcePortRange = sourcePortRange;
		
		if(permittedNetwork != null) {
			m_permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix();
		} else {
			m_permittedNetworkString = "0.0.0.0/0";
		}
		
		m_permittedInterfaceName = permittedInterfaceName;
		m_unpermittedInterfaceName = unpermittedInterfaceName;
		m_permittedMAC = permittedMAC;
	}
	
	/**
	 * Constructor of <code>LocalRule</code> object.
	 * 
	 * @param portRange	destination local IP port range to allow of the form X:Y where X<Y and both are valid ports
	 * @param protocol	protocol of port (tcp, udp)
	 * @param sourcePortRange	range of source ports allowed on IP connection (sourcePort1:sourcePort2) 
	 * @param permittedNetwork	source network or ip address from which connection is allowed (such as 192.168.1.0/24)
	 * @param permittedInterfaceName  only allow open port for this interface
	 * @param unpermittedInterfaceName  allow open port for all interfaces except this one
	 * @param permittedMAC	MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
	 */
	public LocalRule(String portRange, String protocol, NetworkPair<IP4Address> permittedNetwork, String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC, String sourcePortRange) {
		m_port = -1;
		m_portRange = portRange;
		m_protocol = protocol;
		m_sourcePortRange = sourcePortRange;	

		if(permittedNetwork != null) {
			m_permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix();
		} else {
			m_permittedNetworkString = "0.0.0.0/0";
		}

		m_permittedInterfaceName = permittedInterfaceName;
		m_unpermittedInterfaceName = unpermittedInterfaceName;
		m_permittedMAC = permittedMAC;
	}
	
	/**
	 * Constructor of <code>LocalRule</code> object.
	 */
	public LocalRule() {
		m_port = -1;
		m_portRange = null;
		m_protocol = null;
		m_permittedNetworkString = null;
		m_permittedInterfaceName = null;
		m_unpermittedInterfaceName = null;
		m_permittedMAC = null;
		m_sourcePortRange = null;
	}
	
	public LocalRule(String rule) throws KuraException {
		try {
			String [] aRuleTokens = rule.split(" ");
			for (int i = 0; i < aRuleTokens.length; i++) {
				if("-i".equals(aRuleTokens[i])) {
					if("!".equals(aRuleTokens[i-1])) {
						m_unpermittedInterfaceName = aRuleTokens[++i];
					} else {
						m_permittedInterfaceName = aRuleTokens[++i];
					}
				}
				else if("-s".equals(aRuleTokens[i])) {
					m_permittedNetworkString = aRuleTokens[++i];
				} else if ("-p".equals(aRuleTokens[i])) {
					m_protocol = aRuleTokens[++i];
				} else if ("--dport".equals(aRuleTokens[i])) {
					if (aRuleTokens[i+1].indexOf(':') > 0) {
						m_portRange = aRuleTokens[++i];
						m_port = -1;
					} else {
						m_port = Integer.parseInt(aRuleTokens[++i]);
						m_portRange = null;
					}
				} else if ("--sport".equals(aRuleTokens[i])) {
					m_sourcePortRange = aRuleTokens[++i];
				} else if ("--mac-source".equals(aRuleTokens[i])) {
					m_permittedMAC = aRuleTokens[++i];
				}
			}
			if (m_permittedNetworkString == null) {
				m_permittedNetworkString = "0.0.0.0/0";
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	/**
	 * Returns true if the required <code>LocalRule</code> parameters have all been set.  Returns false otherwise.
	 * 
	 * @return		A boolean representing whether all parameters have been set.
	 */
	public boolean isComplete() {
		if(m_protocol != null && m_port != -1) {
			return true; 
		} else if(m_protocol != null && m_portRange != null) {
			if(isPortRangeValid(m_portRange)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Setter for the protocol.
	 * 
	 * @param protocol	A String representing the protocol.
	 */
	public void setProtocol(String protocol) {
		m_protocol = protocol;
	}
	
	/**
	 * Setter for the permittedNetwork.
	 * 
	 * @param permittedNetwork	A String representing the permittedNetwork.
	 */
	public void setPermittedNetwork(NetworkPair<IP4Address> permittedNetwork) {
		if(permittedNetwork != null) {
			m_permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix();
		} else {
			m_permittedNetworkString = "0.0.0.0/0";
		}
	}
	
	/**
	 * Setter for the permittedInterfaceName.
	 * 
	 * @param permittedInterfaceName	A String representing the only interface allowed on this open port
	 */
	public void setPermittedInterfaceName(String permittedInterfaceName) {
		m_permittedInterfaceName = permittedInterfaceName;
	}
	
	/**
	 * Setter for the unpermittedInterfaceName.
	 * 
	 * @param unpermittedInterfaceName	A String representing the only interface not allowed on this open port
	 */
	public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
		m_unpermittedInterfaceName = unpermittedInterfaceName;
	}
	
	/**
	 * Setter for the permittedMAC.
	 * 
	 * @param permittedMAC	A String representing the permittedMAC.
	 */
	public void setPermittedMAC(String permittedMAC) {
		m_permittedMAC = permittedMAC;
	}
	
	/**
	 * Setter for the sourcePortRange.
	 * 
	 * @param sourcePortRange	A String representing the sourcePortRange.
	 */
	public void setSourcePortRange(String sourcePortRange) {
		m_sourcePortRange = sourcePortRange;
	}
	
	/**
	 * Setter for the port.
	 * 
	 * @param port	An int representing the port.
	 */
	public void setPort(int port) {
		m_port = port;
		m_portRange = null;
	}
	
	/**
	 * Setter for the portRange
	 * 
	 * @param portRange	A string representing the port range of the form X:Y where X < Y and both are valid ports
	 */
	public void setPortRange(String portRange) {
		m_port = -1;
		m_portRange = portRange;
	}
	
	/**
	 * Getter for the sourcePortRange.
	 * 
	 * @return the sourcePortRange
	 */
	public String getSourcePortRange() {
		return m_sourcePortRange;
	}
	
	/**
	 * Getter for the permittedInterfaceName.
	 * 
	 * @param permittedInterfaceName	A String representing the only interface allowed on this open port
	 */
	public String getPermittedInterfaceName() {
		return m_permittedInterfaceName;
	}
	
	/**
	 * Getter for the unpermittedInterfaceName.
	 * 
	 * @param unpermittedInterfaceName	A String representing the only interface not allowed on this open port
	 */
	public String getUnpermittedInterfaceName() {
		return m_unpermittedInterfaceName;
	}

	/**
	 * Getter for port
	 * 
	 * @return the port
	 */
	public int getPort() {
		return m_port;
	}
	
	/**
	 * Getter for portRange
	 * 
	 * @return the portRange
	 */
	public String getPortRange() {
		return m_portRange;
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
	 * Getter for permittedNetwork
	 * 
	 * @return the permittedNetwork
	 */
	public NetworkPair<IP4Address> getPermittedNetwork() throws KuraException {
		try {
			if(m_permittedNetworkString != null) {
				String[] split = m_permittedNetworkString.split("/");
				return new NetworkPair(IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
			} else {
				return new NetworkPair(IPAddress.parseHostAddress("0.0.0.0"), (short)0);
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
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
	 * Converts the <code>LocalRule</code> to a <code>String</code>.  
	 * Returns one of the following iptables strings depending on the <code>LocalRule</code> format:
	 * <code>
	 * <p>  iptables -I INPUT -p {protocol} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} -m mac --mac-source {permittedMAC} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} -m mac --mac-source {permittedMAC} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} -s {permittedNetwork} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} -s {permittedNetwork} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --dport {port} -j ACCEPT
	 * <p>  iptables -I INPUT -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
	 * </code>
	 */
	public String toString() {
		
		String interfaceString = null;
		if(m_permittedInterfaceName != null) {
			interfaceString = new StringBuffer().append(" -i " ).append(m_permittedInterfaceName).toString();
		} else if(m_unpermittedInterfaceName != null) {
			interfaceString = new StringBuffer().append(" ! -i " ).append(m_unpermittedInterfaceName).toString();
		}
		
		if(m_port != -1) {
			if (m_permittedMAC == null && m_sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --dport " + m_port + " -j ACCEPT");
			} else if (m_permittedMAC == null && m_sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --sport " + m_sourcePortRange + " --dport " + m_port + " -j ACCEPT");
			} else if (m_permittedMAC != null && m_sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + m_permittedMAC + " --dport " + m_port + " -j ACCEPT");
			} else if (m_permittedMAC != null && m_sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + m_permittedMAC + " --sport " + m_sourcePortRange + " --dport " + m_port + " -j ACCEPT");
			} else {
				return null;
			}
		} else {
			if (m_permittedMAC == null && m_sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --dport " + m_portRange + " -j ACCEPT");
			} else if (m_permittedMAC == null && m_sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --sport " + m_sourcePortRange + " --dport " + m_portRange + " -j ACCEPT");
			} else if (m_permittedMAC != null && m_sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + m_permittedMAC + " --dport " + m_portRange + " -j ACCEPT");
			} else if (m_permittedMAC != null && m_sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + m_protocol + " -s " + m_permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + m_permittedMAC + " --sport " + m_sourcePortRange + " --dport " + m_portRange + " -j ACCEPT");
			} else {
				return null;
			}
		}
	}
	
	private boolean isPortRangeValid(String range) {
		try {
			String[] rangeParts = range.split(":");
			if(rangeParts.length == 2) {
				int portStart = Integer.parseInt(rangeParts[0]);
				int portEnd = Integer.parseInt(rangeParts[1]);
						
				if(portStart > 0 && portStart < 65535 &&
						portEnd > 0 && portEnd < 65535 &&
						portStart < portEnd) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch(Exception e) {
			return false;
		}
	}
	
	@Override
	public boolean equals(Object o) {
	    if(!(o instanceof LocalRule)) {
	        return false;
	    }
	    
        LocalRule other = (LocalRule) o;
        
        if(m_port != other.getPort()) {
            return false;
        } else if (!compareObjects(m_portRange, other.m_portRange)) {
            return false;
        } else if (!compareObjects(m_protocol, other.m_protocol)) {
            return false;
        } else if (!compareObjects(m_permittedMAC, other.m_permittedMAC)) {
            return false;
        } else if (!compareObjects(m_sourcePortRange, other.m_sourcePortRange)) {
            return false;
        } else if (!compareObjects(m_permittedInterfaceName, other.m_permittedInterfaceName)) {
            return false;
        } else if (!compareObjects(m_unpermittedInterfaceName, other.m_unpermittedInterfaceName)) {
            return false;
        } else if (!compareObjects(m_permittedNetworkString, other.m_permittedNetworkString)) {
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
        final int prime = 73;
        int result = 1;
        result = prime * result + m_port;
        result = prime * result
                + ((m_portRange == null) ? 0 : m_portRange.hashCode());
        result = prime * result
                + ((m_protocol == null) ? 0 : m_protocol.hashCode());
        result = prime * result
                + ((m_sourcePortRange == null) ? 0 : m_sourcePortRange.hashCode());
        result = prime * result
                + ((m_permittedInterfaceName == null) ? 0 : m_permittedInterfaceName.hashCode());
        result = prime * result
                + ((m_unpermittedInterfaceName == null) ? 0 : m_unpermittedInterfaceName.hashCode());
        result = prime * result
                + ((m_permittedMAC == null) ? 0 : m_permittedMAC.hashCode());
        result = prime * result
                + ((m_permittedNetworkString == null) ? 0 : m_permittedNetworkString.hashCode());
        
        return result;
    }

}
