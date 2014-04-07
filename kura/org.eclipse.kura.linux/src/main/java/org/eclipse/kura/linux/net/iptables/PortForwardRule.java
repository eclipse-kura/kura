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

/* 
 * Copyright (c) 2013 Eurotech Inc. All rights reserved.
 */

/**
 * Creates an iptables command for a Port Forward Rule, allowing an incoming port to be forwarded to destinationIP/port.
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
 *   PARAM (required) -> "address" | "iface" | "protocol" | "inPort" | "outPort"
 *   PARAM (optional) -> "permittedNetwork" | "permittedMAC" | "sourcePortRange"
 *   VALUE	          -> (value of the specified parameter)
 * 
 *   EXAMPLE:
 *   
 *   PortForwardRule0_address=192.168.1.1
 *   PortForwardRule0_iface=eth0
 *   PortForwardRule0_protocol=tcp
 *   PortForwardRule0_inPort=1234
 *   PortForwardRule0_outPort=1234
 *   PortForwardRule0_permittedNetwork=192.168.1.1
 *   PortForwardRule0_permittedMAC=AA:BB:CC:DD:EE:FF
 *   PortForwardRule0_sourcePortRange=3333:4444
 */
public class PortForwardRule {

	/*
	port forwarding to specific ip address on 'internal' network
    must define:
		inbound interface
		protocol (i.e. tcp and/or udp)
		inbound port
		destination port (for mapping)
    optional:
		specific IPs to allow for this forwarded port
		specific MAC addresses to allow for this forwarded port
		specific source port range to allow for this connection
	*/
	
	//required
	private String iface;
	private String address;
	private String protocol;
	private int inPort;
	private int outPort;
	
	//optional
	private String permittedNetwork;
	private int permittedNetworkMask;
	private String permittedMAC;
	private String sourcePortRange;

	/**
	 * Constructor of <code>PortForwardRule</code> object.
	 * 
	 * @param iface	interface name on which inbound connection is allowed (such as ppp0) 
	 * @param inPort	inbound port on which to listen for port forward
	 * @param protocol	protocol of port connection (tcp, udp)
	 * @param address	destination IP address to forward IP traffic
	 * @param outPort	destination port to forward IP traffic
	 * @param permittedNetwork	source network or ip address from which connection is allowed (such as 192.168.1.0)
	 * @param permittedNetworkMask	source network mask from which connection is allowed (such as 255.255.255.0)
	 * @param permittedMAC	MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
	 * @param sourcePortRange	range of source ports allowed on IP connection (sourcePort1:sourcePort2) 
	 */
	public PortForwardRule(String iface, String address, String protocol, int inPort, int outPort, String permittedNetwork, int permittedNetworkMask, String permittedMAC, String sourcePortRange) {
		this.iface = iface;
		this.inPort = inPort;
		this.protocol = protocol;
		this.address = address;
		this.outPort = outPort;
		
		this.permittedNetwork = permittedNetwork;
		this.permittedNetworkMask = permittedNetworkMask;
		this.permittedMAC = permittedMAC;
		this.sourcePortRange = sourcePortRange;
	}
	
	/**
	 * Constructor of <code>PortForwardRule</code> object.
	 */
	public PortForwardRule() {
		this.iface = null;
		this.inPort = 0;
		this.protocol = null;
		this.address = null;
		this.outPort = 0;
		
		this.permittedNetworkMask = 0;
		this.permittedNetwork = null;
		this.permittedMAC = null;
		this.sourcePortRange = null;
	}
	
	/**
	 * Returns true if the required <code>LocalRule</code> parameters have all been set.  Returns false otherwise.
	 * 
	 * @return		A boolean representing whether all parameters have been set.
	 */
	public boolean isComplete() {
		if(protocol != null && iface != null && address != null && inPort != 0 && outPort != 0)
			return true;
		return false;
	}
	
	/**
	 * Converts the <code>PortForwardRule</code> to a <code>String</code>.  
	 * Returns one of the following iptables strings depending on the <code>PortForwardRule</code> format:
	 * <code>
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} -m mac --mac-source {permittedMAC} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} -m mac --mac-source {permittedMAC} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} -s {permittedNetwork} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} -s {permittedNetwork} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A PREROUTING -i {iface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * </code>
	 * 
	 * @return the String representation of <code>PortForwardRule</code>
	 */
	public String toString() {
		if(this.permittedNetwork == null && this.permittedMAC == null && this.sourcePortRange == null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork == null && this.permittedMAC == null && this.sourcePortRange != null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " --sport " + this.sourcePortRange + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork == null && this.permittedMAC != null && this.sourcePortRange == null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " -m mac --mac-source " + this.permittedMAC + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork == null && this.permittedMAC != null && this.sourcePortRange != null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " -m mac --mac-source " + this.permittedMAC + " --sport " + this.sourcePortRange + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork != null && this.permittedMAC == null && this.sourcePortRange == null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " -s " + this.permittedNetwork + "/" + this.permittedNetworkMask + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork != null && this.permittedMAC == null && this.sourcePortRange != null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " -s " + this.permittedNetwork + "/" + this.permittedNetworkMask + " --sport " + this.sourcePortRange + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork != null && this.permittedMAC != null && this.sourcePortRange == null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " -s " + this.permittedNetwork + "/" + this.permittedNetworkMask + " -m mac --mac-source " + this.permittedMAC + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else if(this.permittedNetwork != null && this.permittedMAC != null && this.sourcePortRange != null) {
			return new String("iptables -t nat -A PREROUTING -i " + this.iface + " -p " + this.protocol + " -s " + this.permittedNetwork + "/" + this.permittedNetworkMask + " -m mac --mac-source " + this.permittedMAC + " --sport " + this.sourcePortRange + " --dport " + this.inPort + " -j DNAT --to " + this.address + ":" + this.outPort);
		} else {
			return null;
		}
	}

	/**
	 * Getter for iface
	 * 
	 * @return the iface
	 */
	public String getIface() {
		return iface;
	}

	/**
	 * Setter for iface
	 * 
	 * @param iface the iface to set
	 */
	public void setIface(String iface) {
		this.iface = iface;
	}

	/**
	 * Getter for address
	 * 
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Setter for address
	 * 
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
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
	 * Setter for protocol
	 * 
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Getter for inPort
	 * 
	 * @return the inPort
	 */
	public int getInPort() {
		return inPort;
	}

	/**
	 * Setter for inPort
	 * 
	 * @param inPort the inPort to set
	 */
	public void setInPort(int inPort) {
		this.inPort = inPort;
	}

	/**
	 * Getter for outPort
	 * 
	 * @return the outPort
	 */
	public int getOutPort() {
		return outPort;
	}

	/**
	 * Setter for outPort
	 * 
	 * @param outPort the outPort to set
	 */
	public void setOutPort(int outPort) {
		this.outPort = outPort;
	}

	/**
	 * Getter for permittedNetwork
	 * 
	 * @return the permittedNetwork
	 */
	public String getPermittedNetwork() {
		return permittedNetwork;
	}

	/**
	 * Setter for permittedNetwork
	 * 
	 * @param permittedNetwork the permittedNetwork to set
	 */
	public void setPermittedNetwork(String permittedNetwork) {
		this.permittedNetwork = permittedNetwork;
	}
	
	/**
	 * Getter for permittedNetworkMask
	 * 
	 * @return the permittedNetworkMask
	 */
	public int getPermittedNetworkMask() {
		return permittedNetworkMask;
	}

	/**
	 * Setter for permittedNetworkMask
	 * 
	 * @param permittedNetworkMask  of the permittedNetwork to set
	 */
	public void setPermittedNetworkMask(int permittedNetworkMask) {
		this.permittedNetworkMask = permittedNetworkMask;
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
	 * Setter for permittedMAC
	 * 
	 * @param permittedMAC the permittedMAC to set
	 */
	public void setPermittedMAC(String permittedMAC) {
		this.permittedMAC = permittedMAC;
	}

	/**
	 * Getter for sourcePortRange
	 * 
	 * @return the sourcePortRange
	 */
	public String getSourcePortRange() {
		return sourcePortRange;
	}

	/**
	 * Setter for sourcePortRange
	 * 
	 * @param sourcePortRange the sourcePortRange to set
	 */
	public void setSourcePortRange(String sourcePortRange) {
		this.sourcePortRange = sourcePortRange;
	}
	
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof PortForwardRule)) {
            return false;
        }
        
        PortForwardRule other = (PortForwardRule) o;
        
        if (!compareObjects(this.iface, other.iface)) {
            return false;
        } else if (!compareObjects(this.address, other.address)) {
            return false;
        } else if (!compareObjects(this.protocol, other.protocol)) {
            return false;
        } else if (this.inPort != other.inPort) {
            return false;
        } else if (this.outPort != other.outPort) {
            return false;
        } else if (!compareObjects(this.permittedNetwork, other.permittedNetwork)) {
            return false;
        } else if (this.permittedNetworkMask != other.permittedNetworkMask) {
            return false;
        } else if (!compareObjects(this.permittedMAC, other.permittedMAC)) {
            return false;
        } else if (!compareObjects(this.sourcePortRange, other.sourcePortRange)) {
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
        result = prime * result + inPort;
        result = prime * result + outPort;
        result = prime * result
                + ((iface == null) ? 0 : iface.hashCode());
        result = prime * result
                + ((address == null) ? 0 : address.hashCode());
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result
                + ((permittedNetwork == null) ? 0 : permittedNetwork.hashCode());
        result = prime * result + permittedNetworkMask;
        result = prime * result
                + ((permittedMAC == null) ? 0 : permittedMAC.hashCode());
        result = prime * result
                + ((sourcePortRange == null) ? 0 : sourcePortRange.hashCode());
        
        return result;
    }
}

