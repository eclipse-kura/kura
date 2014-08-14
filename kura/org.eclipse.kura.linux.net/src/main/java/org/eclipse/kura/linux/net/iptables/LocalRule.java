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
	
	//required vars
	private int port;
	private String portRange;
	private String protocol;
	
	//optional vars
	private String permittedNetworkString;
	private String permittedInterfaceName;
	private String unpermittedInterfaceName;
	private String permittedMAC;
	private String sourcePortRange;
	
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
		this.port = port;
		this.portRange = null;
		this.protocol = protocol;
		this.sourcePortRange = sourcePortRange;
		
		if(permittedNetwork != null) {
			permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix();
		} else {
			permittedNetworkString = "0.0.0.0/0";
		}
		
		this.permittedInterfaceName = permittedInterfaceName;
		this.unpermittedInterfaceName = unpermittedInterfaceName;
		this.permittedMAC = permittedMAC;
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
		this.port = -1;
		this.portRange = portRange;
		this.protocol = protocol;
		this.sourcePortRange = sourcePortRange;	

		if(permittedNetwork != null) {
			permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix();
		} else {
			permittedNetworkString = "0.0.0.0/0";
		}

		this.permittedInterfaceName = permittedInterfaceName;
		this.unpermittedInterfaceName = unpermittedInterfaceName;
		this.permittedMAC = permittedMAC;
	}
	
	/**
	 * Constructor of <code>LocalRule</code> object.
	 */
	public LocalRule() {
		port = -1;
		portRange = null;
		protocol = null;
		permittedNetworkString = null;
		permittedInterfaceName = null;
		unpermittedInterfaceName = null;
		permittedMAC = null;
		sourcePortRange = null;
	}
	
	/**
	 * Returns true if the required <code>LocalRule</code> parameters have all been set.  Returns false otherwise.
	 * 
	 * @return		A boolean representing whether all parameters have been set.
	 */
	public boolean isComplete() {
		if(protocol != null && port != -1) {
			return true; 
		} else if(protocol != null && portRange != null) {
			if(isPortRangeValid(portRange)) {
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
		this.protocol = protocol;
	}
	
	/**
	 * Setter for the permittedNetwork.
	 * 
	 * @param permittedNetwork	A String representing the permittedNetwork.
	 */
	public void setPermittedNetwork(NetworkPair<IP4Address> permittedNetwork) {
		if(permittedNetwork != null) {
			permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix();
		} else {
			permittedNetworkString = "0.0.0.0/0";
		}
	}
	
	/**
	 * Setter for the permittedInterfaceName.
	 * 
	 * @param permittedInterfaceName	A String representing the only interface allowed on this open port
	 */
	public void setPermittedInterfaceName(String permittedInterfaceName) {
		this.permittedInterfaceName = permittedInterfaceName;
	}
	
	/**
	 * Setter for the unpermittedInterfaceName.
	 * 
	 * @param unpermittedInterfaceName	A String representing the only interface not allowed on this open port
	 */
	public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
		this.unpermittedInterfaceName = unpermittedInterfaceName;
	}
	
	/**
	 * Setter for the permittedMAC.
	 * 
	 * @param permittedMAC	A String representing the permittedMAC.
	 */
	public void setPermittedMAC(String permittedMAC) {
		this.permittedMAC = permittedMAC;
	}
	
	/**
	 * Setter for the sourcePortRange.
	 * 
	 * @param sourcePortRange	A String representing the sourcePortRange.
	 */
	public void setSourcePortRange(String sourcePortRange) {
		this.sourcePortRange = sourcePortRange;
	}
	
	/**
	 * Setter for the port.
	 * 
	 * @param port	An int representing the port.
	 */
	public void setPort(int port) {
		this.port = port;
		this.portRange = null;
	}
	
	/**
	 * Setter for the portRange
	 * 
	 * @param portRange	A string representing the port range of the form X:Y where X < Y and both are valid ports
	 */
	public void setPortRange(String portRange) {
		this.port = -1;
		this.portRange = portRange;
	}
	
	/**
	 * Getter for the sourcePortRange.
	 * 
	 * @return the sourcePortRange
	 */
	public String getSourcePortRange() {
		return this.sourcePortRange;
	}
	
	/**
	 * Getter for the permittedInterfaceName.
	 * 
	 * @param permittedInterfaceName	A String representing the only interface allowed on this open port
	 */
	public String getPermittedInterfaceName() {
		return permittedInterfaceName;
	}
	
	/**
	 * Getter for the unpermittedInterfaceName.
	 * 
	 * @param unpermittedInterfaceName	A String representing the only interface not allowed on this open port
	 */
	public String getUnpermittedInterfaceName() {
		return unpermittedInterfaceName;
	}

	/**
	 * Getter for port
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Getter for portRange
	 * 
	 * @return the portRange
	 */
	public String getPortRange() {
		return portRange;
	}

	/**
	 * Getter for protocol
	 * 
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Getter for permittedNetwork
	 * 
	 * @return the permittedNetwork
	 */
	public NetworkPair<IP4Address> getPermittedNetwork() throws KuraException {
		try {
			if(permittedNetworkString != null) {
				String[] split = permittedNetworkString.split("/");
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
		return permittedMAC;
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
		if(permittedInterfaceName != null) {
			interfaceString = new StringBuffer().append(" -i " ).append(permittedInterfaceName).toString();
		} else if(unpermittedInterfaceName != null) {
			interfaceString = new StringBuffer().append(" ! -i " ).append(unpermittedInterfaceName).toString();
		}
		
		if(port != -1) {
			if (this.permittedMAC == null && this.sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --dport " + this.port + " -j ACCEPT");
			} else if (this.permittedMAC == null && this.sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --sport " + this.sourcePortRange + " --dport " + this.port + " -j ACCEPT");
			} else if (this.permittedMAC != null && this.sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC + " --dport " + this.port + " -j ACCEPT");
			} else if (this.permittedMAC != null && this.sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC + " --sport " + this.sourcePortRange + " --dport " + this.port + " -j ACCEPT");
			} else {
				return null;
			}
		} else {
			if (this.permittedMAC == null && this.sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --dport " + this.portRange + " -j ACCEPT");
			} else if (this.permittedMAC == null && this.sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " --sport " + this.sourcePortRange + " --dport " + this.portRange + " -j ACCEPT");
			} else if (this.permittedMAC != null && this.sourcePortRange == null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC + " --dport " + this.portRange + " -j ACCEPT");
			} else if (this.permittedMAC != null && this.sourcePortRange != null) {
				return new String("iptables -I INPUT -p " + this.protocol + " -s " + permittedNetworkString + ((interfaceString != null) ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC + " --sport " + this.sourcePortRange + " --dport " + this.portRange + " -j ACCEPT");
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
        
        if(port != other.getPort()) {
            return false;
        } else if (!compareObjects(this.portRange, other.portRange)) {
            return false;
        } else if (!compareObjects(this.protocol, other.protocol)) {
            return false;
        } else if (!compareObjects(this.permittedMAC, other.permittedMAC)) {
            return false;
        } else if (!compareObjects(this.sourcePortRange, other.sourcePortRange)) {
            return false;
        } else if (!compareObjects(this.permittedInterfaceName, other.permittedInterfaceName)) {
            return false;
        } else if (!compareObjects(this.unpermittedInterfaceName, other.unpermittedInterfaceName)) {
            return false;
        } else if (!compareObjects(this.permittedNetworkString, other.permittedNetworkString)) {
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
        result = prime * result + port;
        result = prime * result
                + ((portRange == null) ? 0 : portRange.hashCode());
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result
                + ((sourcePortRange == null) ? 0 : sourcePortRange.hashCode());
        result = prime * result
                + ((permittedInterfaceName == null) ? 0 : permittedInterfaceName.hashCode());
        result = prime * result
                + ((unpermittedInterfaceName == null) ? 0 : unpermittedInterfaceName.hashCode());
        result = prime * result
                + ((permittedMAC == null) ? 0 : permittedMAC.hashCode());
        result = prime * result
                + ((permittedNetworkString == null) ? 0 : permittedNetworkString.hashCode());
        
        return result;
    }

}
