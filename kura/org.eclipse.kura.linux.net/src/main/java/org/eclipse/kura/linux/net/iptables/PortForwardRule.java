/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.iptables;

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
	
	/**
	 * Converts the <code>PortForwardRule</code> to a <code>String</code>.  
	 * Returns a PREROUTING/FORWARD pair of the following iptables strings depending on the <code>PortForwardRule</code> format:
	 * <code>
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT

	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED-j ACCEPT
	 * 
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -m mac --mac-source {permittedMAC} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -m mac --mac-source {m_permittedMAC} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
	 *
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -m mac --mac-source {permittedMAC} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} --j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -m mac --mac-source {m_permittedMAC} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
	 *  
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
	 *
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT 
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
	 *
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {m_permittedMAC} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
	 *
	 * <p>  iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
	 * <p>  iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
	 * <p>  iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {m_permittedMAC} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
	 * <p>  iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
	 * </code>
	 * 
	 * @return the String representation of <code>PortForwardRule</code>
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if((m_permittedNetwork == null) && (m_permittedMAC == null) && (m_sourcePortRange == null)) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} --dport {inPort} -j DNAT --to {address}:{outPort} 
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol).append(" --dport ")
					.append(m_inPort).append(" -j DNAT --to ")
					.append(m_address).append(':').append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" --dport ").append(m_outPort)
					.append(" -d ").append(m_address).append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
			
			return sb.toString();
		} else if(m_permittedNetwork == null && m_permittedMAC == null && m_sourcePortRange != null) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol).append(" --sport ")
					.append(m_sourcePortRange).append(" --dport ")
					.append(m_inPort).append(" -j DNAT --to ")
					.append(m_address).append(':').append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" --sport ")
					.append(m_sourcePortRange).append(" --dport ")
					.append(m_outPort).append(" -d ").append(m_address)
					.append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
	
			return sb.toString();
		} else if(m_permittedNetwork == null && m_permittedMAC != null && m_sourcePortRange == null) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -m mac --mac-source {m_permittedMAC} --dport {inPort} -j DNAT --to {address}:{outPort}
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -m mac --mac-source {m_permittedMAC} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol)
					.append(" -m mac --mac-source ").append(m_permittedMAC)
					.append(" --dport ").append(m_inPort)
					.append(" -j DNAT --to ").append(m_address).append(':')
					.append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" -m mac --mac-source ")
					.append(m_permittedMAC).append(" --dport ")
					.append(m_outPort).append(" -d ").append(m_address)
					.append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
	
			return sb.toString();
		} else if(m_permittedNetwork == null && m_permittedMAC != null && m_sourcePortRange != null) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -m mac --mac-source {m_permittedMAC} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort} 
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -m mac --mac-source {m_permittedMAC} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol)
					.append(" -m mac --mac-source ").append(m_permittedMAC)
					.append(" --sport ").append(m_sourcePortRange)
					.append(" --dport ").append(m_inPort)
					.append(" -j DNAT --to ").append(m_address).append(':')
					.append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" -m mac --mac-source ")
					.append(m_permittedMAC).append(" --sport ")
					.append(m_sourcePortRange).append(" --dport ")
					.append(m_outPort).append(" -d ").append(m_address)
					.append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
	
			return sb.toString();
		} else if((m_permittedNetwork != null) && (m_permittedMAC == null) && (m_sourcePortRange == null)) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} --dport {inPort} -j DNAT --to {address}:{outPort}
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask).append(" --dport ")
					.append(m_inPort).append(" -j DNAT --to ")
					.append(m_address).append(':').append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask).append(" --dport ")
					.append(m_outPort).append(" -d ").append(m_address)
					.append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
			
			return sb.toString();
		} else if(m_permittedNetwork != null && m_permittedMAC == null && m_sourcePortRange != null) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort}
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask).append(" --sport ")
					.append(m_sourcePortRange).append(" --dport ")
					.append(m_inPort).append(" -j DNAT --to ")
					.append(m_address).append(':').append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask).append(" --sport ")
					.append(m_sourcePortRange).append(" --dport ")
					.append(m_outPort).append(" -d ").append(m_address)
					.append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
			
			return sb.toString();
		} else if((m_permittedNetwork != null) && (m_permittedMAC != null) && (m_sourcePortRange == null)) {
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {m_permittedMAC} --dport {inPort} -j DNAT --to {address}:{outPort}
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {m_permittedMAC} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask)
					.append(" -m mac --mac-source ").append(m_permittedMAC)
					.append(" --dport ").append(m_inPort)
					.append(" -j DNAT --to ").append(m_address).append(':')
					.append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask)
					.append(" -m mac --mac-source ").append(m_permittedMAC)
					.append(" --dport ").append(m_outPort).append(" -d ")
					.append(m_address).append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
			
			return sb.toString();
		} else if((m_permittedNetwork != null) && (m_permittedMAC != null) && (m_sourcePortRange != null)) {	
			/** 
			 * <code>
			 * <p> iptables -t nat -A PREROUTING -i {inboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {m_permittedMAC} --sport {sourcePortRange} --dport {inPort} -j DNAT --to {address}:{outPort} 
			 * <p> iptables -t nat -A POSTROUTING -o {outboundIface} -p {protocol} -d {address} -j MASQUERADE
			 * <p> iptables -A FORWARD -i {inboundIface} -o {outboundIface} -p {protocol} -s {permittedNetwork} -m mac --mac-source {m_permittedMAC} --sport {sourcePortRange} --dport {outPort} -d {address} -j ACCEPT
			 * <p> iptables -A FORWARD -i {outboundIface} -o {inboundIface} -p {protocol} -s {address} -m state --state RELATED,ESTABLISHED -j ACCEPT
			 * </code>
			 */
			sb.append("iptables -t nat -A PREROUTING -i ").append(m_inboundIface)
					.append(" -p ").append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask)
					.append(" -m mac --mac-source ").append(m_permittedMAC)
					.append(" --sport ").append(m_sourcePortRange)
					.append(" --dport ").append(m_inPort)
					.append(" -j DNAT --to ").append(m_address).append(':')
					.append(m_outPort);
			sb.append("; ");
			if (m_masquerade) {
				sb.append("iptables -t nat -A POSTROUTING -o ")
						.append(m_outboundIface).append(" -p ")
						.append(m_protocol).append(" -d ").append(m_address)
						.append(" -j MASQUERADE");
				sb.append("; ");
			}
			sb.append("iptables -A FORWARD -i ").append(m_inboundIface)
					.append(" -o ").append(m_outboundIface).append(" -p ")
					.append(m_protocol).append(" -s ")
					.append(m_permittedNetwork).append("/")
					.append(m_permittedNetworkMask)
					.append(" -m mac --mac-source ").append(m_permittedMAC)
					.append(" --sport ").append(m_sourcePortRange)
					.append(" --dport ").append(m_outPort).append(" -d ")
					.append(m_address).append(" -j ACCEPT");
			sb.append("; ");
			sb.append("iptables -A FORWARD -i ").append(m_outboundIface)
					.append(" -o ").append(m_inboundIface).append(" -p ")
					.append(m_protocol).append(" -s ").append(m_address)
					.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
			
			return sb.toString();
		} else {
			return null;
		}
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

