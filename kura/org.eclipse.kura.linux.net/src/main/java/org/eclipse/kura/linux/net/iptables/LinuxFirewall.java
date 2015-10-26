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
/*
* Copyright (c) 2013 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.linux.net.iptables;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation class for the ILinuxFirewallService
 *
 * @author eurotech
 */
public class LinuxFirewall {
	private static final Logger s_logger = LoggerFactory.getLogger(LinuxFirewall.class);

	private static final String[] HEADER = {"#!/bin/sh", "# IPTables Firewall script", ""};

	private static final String OS_VERSION = System.getProperty("kura.os.version");

	private static final String[] DEFAULT_POLICY = HEADER;

	private static final String[] CLEAR_ALL_CHAINS = {"",
						"#Clear all Built-in Chains",
						"iptables -F INPUT",
						"iptables -F OUTPUT",
						"iptables -F FORWARD",
						"iptables -t nat -F",
						""};

	private static final String[] BLOCK_POLICY = {"",
													"#Block all ports for input traffic",
													"iptables -P INPUT DROP",
													"#block Output Traffic",
													"iptables -P OUTPUT ACCEPT",
													"#block forward Traffic",
													"iptables -P FORWARD DROP",
													"",
													"#Allow all traffic to the loop back interface",
													"iptables -A INPUT -i lo -j ACCEPT",
													"",
													"#Allow Only incoming connection related to Outgoing connection",
													"iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT",
													""};

	/*
	private static final String UNBLOCK_POLICY = "\n" + "#unBlock all ports for input traffic\n" + "iptables -P INPUT ACCEPT\n" + "#Accept Output Traffic\n" + "iptables -P OUTPUT ACCEPT\n"
						+ "#unblock forward Traffic\n" + "iptables -P FORWARD ACCEPT\n" + "\n"
						+ "#Allow all traffic to loop back interface\n" + "iptables -A INPUT -i lo -j ACCEPT\n" + "\n"
						+ "#Allow Only incoming connection related to Outgoing connection\n" + "iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT\n" + "\n";
	 */

	private static final String[] ALLOW_ICMP = {"#allow inbound ICMP requests",
												"iptables -A INPUT -p icmp --icmp-type 8 -m state --state NEW,ESTABLISHED,RELATED -j ACCEPT",
												"iptables -A OUTPUT -p icmp --icmp-type 0 -m state --state ESTABLISHED,RELATED -j ACCEPT",
												""};

	private static final String[] DO_NOT_ALLOW_ICMP = {"#Do not allow inbound ICMP requests",
														"iptables -A INPUT -p icmp --icmp-type 8 -m state --state NEW,ESTABLISHED,RELATED -j DROP",
														"iptables -A OUTPUT -p icmp --icmp-type 0 -m state --state ESTABLISHED,RELATED -j DROP",
														""};

	private static final String[] ALLOW_FORWARDING = {"#allow fowarding if any masquerade is defined",
														"echo 1 > /proc/sys/net/ipv4/ip_forward"};

	private static final String[] DO_NOT_ALLOW_FORWARDING = {"#do not allow fowarding unless masquerade is defined",
																"echo 0 > /proc/sys/net/ipv4/ip_forward"};

	private static final String[] FOOTER = {"#source a custom firewall script",
											"source /etc/init.d/firewall_cust 2> /dev/null"};

	private static LinuxFirewall s_linuxFirewall;

	private static Object s_lock = new Object();

	private static final String FIREWALL_SCRIPT_NAME = "/etc/init.d/firewall";
	private static final String FIREWALL_TMP_SCRIPT_NAME = "/etc/init.d/firewall.tmp";

	private LinkedHashSet<LocalRule> m_localRules;
	private LinkedHashSet<PortForwardRule> m_portForwardRules;
	private LinkedHashSet<NATRule> m_autoNatRules;
	private LinkedHashSet<NATRule> m_natRules;
	private LinkedHashSet<String> m_customRules;
	private boolean m_allowIcmp;
	private boolean m_allowForwarding;

	private LinuxFirewall() {
		try {
			try {
				File oscfile = new File(FIREWALL_SCRIPT_NAME);
				if (oscfile.exists() == false) {
					FileOutputStream fos = new FileOutputStream(oscfile);
					s_logger.debug(oscfile + " new file created");
					PrintWriter pw = new PrintWriter(fos);
					for(String line : DEFAULT_POLICY) {
						pw.println(line);
					}
					pw.println();
					pw.close();
				} else {
					s_logger.debug(oscfile + " file already exists");
				}
			} catch (IOException e) {
				s_logger.error("cannot create or read file");// File did not exist and was created
			}
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
			s_logger.error("failed to initialize LinuxFirewall");
		}
	}

	public static LinuxFirewall getInstance() {
		if(s_linuxFirewall == null) {
			s_linuxFirewall = new LinuxFirewall();
		}

		return s_linuxFirewall;
	}

	public ArrayList<String> readFileLinebyLine(String sourceFile) {
		ArrayList<String> destination = new ArrayList<String>();
		DataInputStream in = null;
		BufferedReader br = null;

		try {
			// Open the file that is the first command line parameter
			FileInputStream fstream = new FileInputStream(sourceFile);

			// Get the object of DataInputStream
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int i = 0;

			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				destination.add(i, strLine);
				i = i + 1;
			}

			// Close the input stream
			in.close();
		} catch(FileNotFoundException e) {// Catch exception if any
			s_logger.error("the file: " + sourceFile + " does not exist", e);
		} catch(IOException ioe) {
			s_logger.error("IOException while trying to open: " + sourceFile, ioe);
		}
		finally {
			if(in != null){
				try{
					in.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing DataInputStream!", ex);
				}
			}
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!", ex);
				}
			}
		}

		s_logger.trace("size of destination is {}", destination.size());
		return destination;
	}

	public void createFile(String file1) throws KuraException {
		File file = new File(file1);
		if (file.exists() == true) {
			s_logger.debug(file + " already exists in getdefaultroute method");

		} else {
			s_logger.debug(file + " does not exist in getdefaultroute method");
			try {
				file.createNewFile();
			} catch (IOException e) {
				s_logger.error(file + ", ERROR creating new file");
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			s_logger.debug("new File: " + file + " created");
		}
	}

	public void initialize() throws KuraException {

		s_logger.debug("initializing firewall ...");
		m_localRules = new LinkedHashSet<LocalRule>();
		m_portForwardRules = new LinkedHashSet<PortForwardRule>();
		m_autoNatRules = new LinkedHashSet<NATRule>();
		m_natRules = new LinkedHashSet<NATRule>();
		m_customRules = new LinkedHashSet<String>();
		m_allowIcmp = true;
		m_allowForwarding = false;

		s_logger.debug("initialize() :: Parsing current firewall configuraion");

		File tmpFirewallFile = new File(FIREWALL_TMP_SCRIPT_NAME);
		if(tmpFirewallFile.exists()) {
			tmpFirewallFile.delete();
		}

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(FIREWALL_SCRIPT_NAME));
			String line = null;

			lineloop:
			while((line = br.readLine()) != null) {
				//skip any predefined lines or comment lines
				if(line.trim().equals("")) {
					continue;
				}
				if(line.trim().startsWith("#")) {
					continue;
				}
				for(String headerLine : HEADER) {
					if(line.equals(headerLine)) {
						continue lineloop;
					}
				}
				for(String clearAllChains : CLEAR_ALL_CHAINS) {
					if(line.equals(clearAllChains)) {
						continue lineloop;
					}
				}
				for(String blockPolicy : BLOCK_POLICY) {
					if(line.equals(blockPolicy)) {
						continue lineloop;
					}
				}
				for(String allowIcmp : ALLOW_ICMP) {
					if(line.equals(allowIcmp)) {
						m_allowIcmp = true;
						continue lineloop;
					}
				}
				for(String doNotAllowIcmp : DO_NOT_ALLOW_ICMP) {
					if(line.equals(doNotAllowIcmp)) {
						m_allowIcmp = false;
						continue lineloop;
					}
				}
				for(String allowForwarding : ALLOW_FORWARDING) {
					if(line.equals(allowForwarding)) {
						m_allowForwarding = true;
						continue lineloop;
					}
				}
				for(String doNotAllowForwarding : DO_NOT_ALLOW_FORWARDING) {
					if(line.equals(doNotAllowForwarding)) {
						m_allowForwarding = false;
						continue lineloop;
					}
				}
				for(String footer : FOOTER) {
					if(line.equals(footer)) {
						continue lineloop;
					}
				}

				if(line.startsWith("iptables -I INPUT -p")) {
					s_logger.debug("Found local rule");

					//Local Rule
					int dport = -1;
					String sport = null;
					String permittedMac = null;
					String permittedNetwork = null;
					String permittedInterfaceName = null;
					String unpermittedInterfaceName = null;
					int permittedNetworkMask = -1;
					String protocol = null;

					StringTokenizer st = new StringTokenizer(line);
					while(st.hasMoreTokens()) {
						String token = st.nextToken();
						if(token.equals("iptables")) {
							st.nextToken();	//skip -I
							st.nextToken(); //skip INPUT
						} else if(token.equals("-p")) {
							protocol = st.nextToken();
						} else if(token.equals("--dport")) {
							dport = Integer.parseInt(st.nextToken());
						} else if(token.equals("--sport")) {
							sport = st.nextToken();
						} else if(token.equals("--mac-source")) {
							permittedMac = st.nextToken();
						} else if(token.equals("-m")) {
							st.nextToken();	//skip mac
						} else if(token.equals("-s")) {
							String[] permitted = st.nextToken().split("/");
							permittedNetwork = permitted[0];
							permittedNetworkMask = Integer.parseInt(permitted[1]);
						} else if(token.equals("!")) {
							st.nextToken(); //skip -i
							unpermittedInterfaceName = st.nextToken();
						} else if(token.equals("-i")) {
							permittedInterfaceName = st.nextToken();
						} else if(token.equals("-j")) {
							//got to the end of the line...
							break;
						} else {
							throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Error parsing LocalRule: " + line);
						}
					}

					LocalRule localRule = new LocalRule(dport, protocol, new NetworkPair(IPAddress.parseHostAddress(permittedNetwork), (short) permittedNetworkMask), permittedInterfaceName, unpermittedInterfaceName, permittedMac, sport);
					s_logger.debug("Adding local rule: " + localRule.toString());
					m_localRules.add(localRule);
				} else if(line.startsWith("iptables -t nat -A PREROUTING")) {
					s_logger.debug("Found port forward rule");

					//Port Forward Rule
					String inboundIfaceName = null;
					String outboundIfaceName = null;
					String protocol = null;
					int inPort = -1;
					int outPort = -1;
					boolean masquerade = false;
					String sport = null;
					String permittedMac = null;
					String permittedNetwork = null;
					int permittedNetworkMask = -1;
					String address = null;

					StringTokenizer st = new StringTokenizer(line, "; \t\n\r\f");
					while(st.hasMoreTokens()) {
						String token = st.nextToken();
						if(token.equals("iptables")) {
							String tok1 = st.nextToken();	//skip -t
							String tok2 = st.nextToken(); //skip nat
							if (tok1.equals("-t") && tok2.equals("nat")) {
								st.nextToken();	//skip -A
								String tok3 = st.nextToken();	//skip PREROUTING/POSTROUTING
								if (tok3.equals("POSTROUTING")) {
									// this is masquerading rule, set out-bound interface, masquerade flag and skip the rest
									st.nextToken();	//skip -o
									outboundIfaceName = st.nextToken();
									masquerade = true;
									break;
								}
							} else if (tok1.equals("-A") && tok2.equals("FORWARD")) {
								// this is a forwarding rule, skip it
								break;
							} else {
								throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Error parsing LocalRule: " + line);
							}
						} else if(token.equals("-i")) {
							inboundIfaceName = st.nextToken();
						} else if(token.equals("-p")) {
							protocol = st.nextToken();
						} else if(token.equals("--dport")) {
							inPort = Integer.parseInt(st.nextToken());
						} else if(token.equals("--sport")) {
							sport = st.nextToken();
						} else if(token.equals("--mac-source")) {
							permittedMac = st.nextToken();
						} else if(token.equals("-m")) {
							st.nextToken();	//skip mac
						} else if(token.equals("-j")) {
							st.nextToken(); //skip DNAT
						} else if(token.equals("--to")) {
							String[] to = st.nextToken().split(":");
							address = to[0];
							outPort = Integer.parseInt(to[1]);
						} else if(token.equals("-s")) {
							String[] permitted = st.nextToken().split("/");
							permittedNetwork = permitted[0];
							permittedNetworkMask = Integer.parseInt(permitted[1]);
						} else {
							throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Error parsing LocalRule: " + line);
						}
					}

					PortForwardRule portForwardRule = new PortForwardRule(
							inboundIfaceName, outboundIfaceName, address, protocol, inPort, outPort,
							masquerade, permittedNetwork, permittedNetworkMask,
							permittedMac, sport);

					s_logger.debug("Adding port forward rule: " + portForwardRule.toString());
					m_portForwardRules.add(portForwardRule);
				} else if(line.startsWith("iptables -t nat -A POSTROUTING")) {
					s_logger.debug("Found NAT rule");

					//NAT Rule
					String destinationInterface = null;
					String sourceInterface = null;
					boolean masquerade = false;

					String protocol = null;
					String source = null;
					String destination = null;

					//just do this one by one
					StringTokenizer st = new StringTokenizer(line);
					st.nextToken();		//skip iptables
					String token = st.nextToken();		//get -t or -A (depending on whether or not masquerade is enabled)
					if(token.equals("-t")) {
						masquerade = true;
						st.nextToken();		//skip nat
						st.nextToken();		//skip -A
						st.nextToken();		//skip POSTROUTING
						String tok = st.nextToken();		//skip -o or -p
						if (tok.equals("-p")) {
							protocol = st.nextToken();
							st.nextToken(); 	// skip -s
							source = st.nextToken();
							st.nextToken(); 	// skip -d
							destination = st.nextToken();
							st.nextToken(); 	// skip -o
							destinationInterface = st.nextToken();
						} else {
							destinationInterface = st.nextToken();
						}
						st.nextToken();		//skip -j
						st.nextToken();		//skip MASQUERADE
						st.nextToken();		//skip iptables
						st.nextToken();		//skip -A
					}

					//get the rest (or continue on if no MASQ)
					st.nextToken();		//skip FORWARD
					String tok = st.nextToken();		//skip -i or -p
					if (tok.equals("-p")) {
						st.nextToken(); // skip protocol
						st.nextToken(); // skip -s
						st.nextToken(); // skip source
						st.nextToken(); // skip -d
						st.nextToken(); // skip destination
						st.nextToken(); // skip -i
						st.nextToken(); // skip destination interface
						st.nextToken();		//skip -o
						sourceInterface = st.nextToken();
					} else {
						destinationInterface = st.nextToken();
						st.nextToken();		//skip -o
						sourceInterface = st.nextToken();
					}

					if (protocol == null) {
						// used to be s_logger.debug
						s_logger.debug("Parsed auto NAT rule with" +
								"   sourceInterface: " + sourceInterface +
								"   destinationInterface: " + destinationInterface +
								"   masquerade: " + masquerade );

						NATRule natRule = new NATRule(sourceInterface, destinationInterface, masquerade);
						s_logger.debug("Adding auto NAT rule " + natRule.toString());
						m_autoNatRules.add(natRule);
					} else {
						s_logger.debug("Parsed NAT rule with" +
								"   sourceInterface: " + sourceInterface +
								"   destinationInterface: " + destinationInterface +
								"   masquerade: " + masquerade +
								"	protocol: " + protocol +
								"	source network/host: " + source +
								"	destination network/host + destination");
						NATRule natRule = new NATRule(sourceInterface, destinationInterface, protocol, source, destination, masquerade);
						s_logger.warn("Adding NAT rule " + natRule.toString());
						m_natRules.add(natRule);
					}
				} else if (line.startsWith("iptables -A FORWARD")) {
					s_logger.debug("Found FORWARD rule");

					//just do this one by one
					StringTokenizer st = new StringTokenizer(line);
					st.nextToken();		//skip iptables
					st.nextToken();		//skip -A
					st.nextToken();		//skip FORWARD
					st.nextToken();		//skip -p
					String protocol = st.nextToken();
					st.nextToken(); 	// skip -s
					String source = st.nextToken();
					st.nextToken(); 	// skip -d
					String destination = st.nextToken();
					st.nextToken(); 	// skip -i
					String destinationInterface = st.nextToken();
					st.nextToken(); 	// skip -o
					String sourceInterface = st.nextToken();

					NATRule natRule = new NATRule(sourceInterface, destinationInterface, protocol, source, destination, false);
					s_logger.warn("Adding NAT rule (no MASQUERADING)" + natRule.toString());
					m_natRules.add(natRule);
				} else {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "invalid line in /etc/init.d/firewall: " + line);
				}
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			//close
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				br = null;
			}
		}
	}

	private boolean writeFile() throws KuraException {
		s_logger.trace("writing to file: {}", FIREWALL_TMP_SCRIPT_NAME);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(FIREWALL_TMP_SCRIPT_NAME));
			for(String line : HEADER) {
				pw.println(line);
			}
			for(String line : CLEAR_ALL_CHAINS) {
				pw.println(line);
			}
			for(String line : BLOCK_POLICY) {
				pw.println(line);
			}

			if(m_allowIcmp) {
				for(String line : ALLOW_ICMP) {
					pw.println(line);
				}
			} else {
				for(String line : DO_NOT_ALLOW_ICMP) {
					pw.println(line);
				}
			}

			pw.println("#custom local service rules");
			Iterator<LocalRule> itLocalRules = m_localRules.iterator();
			while(itLocalRules.hasNext()) {
			    pw.println(itLocalRules.next());
			}
			pw.println();
			pw.println("#custom port forward service rules");
			Iterator<PortForwardRule> itPortForwardRules = m_portForwardRules.iterator();
			while(itPortForwardRules.hasNext()) {
			    pw.println(itPortForwardRules.next());
			}
			pw.println();
			pw.println("#custom automatic NAT service rules (if NAT option is enabled for LAN interface)");
			Iterator<NATRule> itAutoNatRules = m_autoNatRules.iterator();
			while(itAutoNatRules.hasNext()) {
			    pw.println(itAutoNatRules.next());
			}
			pw.println();
			pw.println("#custom NAT service rules");
			Iterator<NATRule> itNatRules = m_natRules.iterator();
			while(itNatRules.hasNext()) {
			    pw.println(itNatRules.next());
			}
			pw.println();
			pw.println("#custom rules");
			Iterator<String> itCustomRules = m_customRules.iterator();
			while(itCustomRules.hasNext()) {
			    pw.println(itCustomRules.next());
			}
			pw.println();
			if(m_allowForwarding) {
				for(String line : ALLOW_FORWARDING) {
					pw.println(line);
				}
			} else {
				for(String line : DO_NOT_ALLOW_FORWARDING) {
					pw.println(line);
				}
			}

			pw.println();
			for(String line : FOOTER) {
				pw.println(line);
			}
			pw.close();

			SafeProcess proc = null;
			try {
				proc = ProcessUtil.exec("chmod 755 " + FIREWALL_TMP_SCRIPT_NAME);
				proc.waitFor();
			}
			finally {
				if (proc != null) ProcessUtil.destroy(proc);
			}

			//move the file if we made it this far
			File tmpFirewallFile = new File(FIREWALL_TMP_SCRIPT_NAME);
			File firewallFile = new File(FIREWALL_SCRIPT_NAME);
			if(!FileUtils.contentEquals(tmpFirewallFile, firewallFile)) {
				if(tmpFirewallFile.renameTo(firewallFile)) {
					s_logger.info("writeFile() :: Successfully wrote firewall file");
					return true;
				} else {
					s_logger.error("writeFile() :: Failed to write firewall file");
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for firewall");
				}
			} else {
				s_logger.info("writeFile() :: Not rewriting firewall file because it is the same");
				return false;
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally{
			if(pw != null){
				pw.close();
			}
		}
	}

	public void addCustomRule(String rule) throws KuraException {
		try {
			s_logger.info("adding custom local rule to  firewall configuration");
			m_customRules.add(rule);
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void addLocalRule(int port, String protocol, String permittedNetwork, String permittedNetworkPrefix, String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC, String sourcePortRange) throws KuraException {
		try {
			LocalRule newLocalRule = null;
			if(permittedNetwork != null && permittedNetworkPrefix != null) {
				s_logger.debug("permittedNetwork: " + permittedNetwork);
				s_logger.debug("permittedNetworkPrefix: " + permittedNetworkPrefix);

				newLocalRule = new LocalRule(port, protocol, new NetworkPair(IPAddress.parseHostAddress(permittedNetwork), Short.parseShort(permittedNetworkPrefix)), permittedInterfaceName, unpermittedInterfaceName, permittedMAC, sourcePortRange);
			} else {
				newLocalRule = new LocalRule(port, protocol, new NetworkPair(IPAddress.parseHostAddress("0.0.0.0"), (short)0), permittedInterfaceName, permittedInterfaceName, permittedMAC, sourcePortRange);
			}

			//make sure it is not already present
			for(LocalRule localRule : m_localRules) {
				if(newLocalRule.equals(localRule)) {
					s_logger.warn("Not adding local rule that is already present: " + localRule);
					return;
				}
			}

			s_logger.info("adding local rule to firewall configuration: " + newLocalRule.toString());
			m_localRules.add(newLocalRule);
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void addPortForwardRule(String inboundIface, String outboundIface,
			String address, String protocol, int inPort, int outPort,
			boolean masquerade, String permittedNetwork,
			String permittedNetworkPrefix, String permittedMAC,
			String sourcePortRange) throws KuraException {
		try {
			PortForwardRule newPortForwardRule = null;
			if(permittedNetworkPrefix != null) {
				newPortForwardRule = new PortForwardRule(inboundIface,
						outboundIface, address, protocol, inPort, outPort,
						masquerade, permittedNetwork,
						Short.parseShort(permittedNetworkPrefix), permittedMAC,
						sourcePortRange);
			} else {
				newPortForwardRule = new PortForwardRule(inboundIface,
						outboundIface, address, protocol, inPort, outPort,
						masquerade, permittedNetwork, -1, permittedMAC,
						sourcePortRange);
			}

			//make sure it is not already present
			for(PortForwardRule portForwardRule : m_portForwardRules) {
				if(newPortForwardRule.equals(portForwardRule)) {
					s_logger.warn("Not adding port forward rule that is already present: " + portForwardRule);
					return;
				}
			}

			s_logger.info("adding port forward rule to firewall configuration: " + newPortForwardRule.toString());
			m_portForwardRules.add(newPortForwardRule);

			m_allowForwarding = true;
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	/**
	 * Adds automatic NAT rule
	 *
	 * @param sourceInterface
	 * @param destinationInterface
	 * @param masquerade
	 * @throws EsfException
	 */
	public void addNatRule(String sourceInterface, String destinationInterface, boolean masquerade) throws KuraException {

		try {
		    if(sourceInterface == null || sourceInterface.isEmpty()) {
		        s_logger.warn("Can't add auto NAT rule - source interface not specified");
		        return;
		    } else if(destinationInterface == null || destinationInterface.isEmpty()) {
                s_logger.warn("Can't add auto NAT rule - destination interface not specified");
                return;
		    }

			NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, masquerade);

			//make sure it is not already present
			for(NATRule natRule : m_autoNatRules) {
				if(newNatRule.equals(natRule)) {
					s_logger.warn("Not adding auto nat rule that is already present: " + natRule);
					return;
				}
			}

			s_logger.info("adding auto NAT rule to firewall configuration: " + newNatRule.toString());
			m_autoNatRules.add(newNatRule);
			m_allowForwarding = true;
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	/**
	 * Adds NAT Rule
	 *
	 * @param sourceInterface
	 * @param destinationInterface
	 * @param protocol
	 * @param source
	 * @param destination
	 * @param masquerade
	 * @throws EsfException
	 */
	public void addNatRule(String sourceInterface, String destinationInterface,
			String protocol, String source, String destination,
			boolean masquerade) throws KuraException {

		try {
			if(sourceInterface == null || sourceInterface.isEmpty()) {
		        s_logger.warn("Can't add NAT rule - source interface not specified");
		        return;
		    } else if(destinationInterface == null || destinationInterface.isEmpty()) {
	            s_logger.warn("Can't add NAT rule - destination interface not specified");
	            return;
		    }

			NATRule newNatRule = new NATRule(sourceInterface,
					destinationInterface, protocol, source, destination,
					masquerade);
			// TODO need to add comparison
			s_logger.info("adding NAT rule to firewall configuration: {}", newNatRule.toString());
			m_natRules.add(newNatRule);
			m_allowForwarding = true;
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public Set<LocalRule> getLocalRules() throws KuraException {
		try {
			return m_localRules;
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public Set<PortForwardRule> getPortForwardRules() throws KuraException {
		try {
			return m_portForwardRules;
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public Set<NATRule> getAutoNatRules() throws KuraException {
		try {
			return m_autoNatRules;
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public Set<NATRule> getNatRules() throws KuraException {
		try {
			return m_natRules;
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteLocalRule(LocalRule rule) throws KuraException {
		try {
			m_localRules.remove(rule);
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deletePortForwardRule(PortForwardRule rule) throws KuraException {
		try {
			m_portForwardRules.remove(rule);
			if (((m_autoNatRules != null) && (m_autoNatRules.size() < 1))
					&& ((m_natRules != null) && (m_natRules.size() < 1))
					&& ((m_portForwardRules != null) && (m_portForwardRules.size() < 1))) {

				m_allowForwarding = false;
			}
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAutoNatRule(NATRule rule) throws KuraException {
		try {
			m_autoNatRules.remove(rule);
			if (((m_autoNatRules != null) && (m_autoNatRules.size() < 1))
					&& ((m_natRules != null) && (m_natRules.size() < 1))
					&& ((m_portForwardRules != null) && (m_portForwardRules.size() < 1))) {

				m_allowForwarding = false;
			}
			update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAllLocalRules() throws KuraException {
		try {
			this.m_localRules.clear();
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAllPortForwardRules() throws KuraException {
		try {
			m_portForwardRules.clear();
			if (((m_autoNatRules != null) && (m_autoNatRules.size() < 1))
					&& ((m_natRules != null) && (m_natRules.size() < 1))) {

				m_allowForwarding = false;
			}
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void replaceAllNatRules(LinkedHashSet<NATRule> newNatRules) throws KuraException {
		try {
			m_autoNatRules = newNatRules;
			if (((m_autoNatRules != null) && (m_autoNatRules.size() > 0))
					|| ((m_natRules != null) && (m_natRules.size() > 0))
					|| ((m_portForwardRules != null) && (m_portForwardRules.size() > 0))) {

				m_allowForwarding = true;
			} else {
				m_allowForwarding = false;
			}
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAllAutoNatRules() throws KuraException {
		try {
			m_autoNatRules.clear();
			if ((m_natRules != null) && (m_natRules.size() < 1)
					&& ((m_portForwardRules != null) && (m_portForwardRules.size() < 1))) {

				m_allowForwarding = false;
			}
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAllNatRules() throws KuraException {
		try {
			m_natRules.clear();
			if (((m_autoNatRules != null) && (m_autoNatRules.size() < 1))
					&& ((m_portForwardRules != null) && (m_portForwardRules.size() < 1))) {
				m_allowForwarding = false;
			}
			this.update();
		} catch (KuraException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void blockAllPorts() throws KuraException {
		deleteAllLocalRules();
		deleteAllPortForwardRules();
		deleteAllAutoNatRules();
		this.update();
	}

	public void unblockAllPorts() throws KuraException {
		deleteAllLocalRules();
		deleteAllPortForwardRules();
		deleteAllAutoNatRules();
		this.update();
	}

	private void runScript() throws KuraException {
		SafeProcess proc = null;
		try {
			File file = new File(FIREWALL_SCRIPT_NAME);
			if(!file.exists()) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Firewall configuration file: " + FIREWALL_SCRIPT_NAME + " does not exist.");
			}
			s_logger.info("Running the firewall script");
			proc = ProcessUtil.exec("sh " + FIREWALL_SCRIPT_NAME);
			proc.waitFor();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}

	/*
	 * Saves the current iptables config into /etc/sysconfig/iptables
	 */
	private void iptablesSave() throws KuraException {
		SafeProcess proc = null;
		try {
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
				(OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName()))) {
				proc = ProcessUtil.exec("iptables-save > /opt/eurotech/firewall_rules.fw");
				proc.waitFor();
			} else {
				proc= ProcessUtil.exec("service iptables save");
				proc.waitFor();
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}

	public void enable() throws KuraException {
		this.update();
		this.iptablesSave();
	}

	public void disable() throws KuraException {
		this.iptablesSave();

		s_logger.trace("writing to file: {}", FIREWALL_TMP_SCRIPT_NAME);
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(FIREWALL_TMP_SCRIPT_NAME));
			for(String line : HEADER) {
				pw.println(line);
			}
			for(String line : CLEAR_ALL_CHAINS) {
				pw.println(line);
			}
			pw.flush();
			pw.close();

			//move the file if we made it this far
			File tmpFirewallFile = new File(FIREWALL_TMP_SCRIPT_NAME);
			File firewallFile = new File(FIREWALL_SCRIPT_NAME);
			if(!FileUtils.contentEquals(tmpFirewallFile, firewallFile)) {
				if(tmpFirewallFile.renameTo(firewallFile)){
					s_logger.info("disable() :: Successfully wrote firewall file");
					runScript();
				}else{
					s_logger.error("disable() :: Failed to write firewall file");
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for firewall");
				}
			} else {
				s_logger.info("disable() :: Not rewriting firewall file because it is the same");
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void allowIcmp() {
		m_allowIcmp = true;
	}

	public void disableIcmp() {
		m_allowIcmp = false;
	}

	public void enableForwarding() {
		m_allowForwarding = true;
	}

	public void disableForwarding() {
		m_allowForwarding = false;
	}

	private void update() throws KuraException {
		synchronized(s_lock) {
		    if(writeFile()) {
		    	runScript();
		    }
		}
	}
}
