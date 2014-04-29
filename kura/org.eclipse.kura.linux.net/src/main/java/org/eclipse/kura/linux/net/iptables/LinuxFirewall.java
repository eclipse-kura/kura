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

	private static final String FIREWALL_SCRIPT_NAME = "/etc/init.d/firewall";
	private static final String FIREWALL_TMP_SCRIPT_NAME = "/etc/init.d/firewall.tmp";

	private LinkedHashSet<LocalRule> m_localRules;
	private LinkedHashSet<PortForwardRule> m_portForwardRules;
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
			
			m_localRules = new LinkedHashSet<LocalRule>();
			m_portForwardRules = new LinkedHashSet<PortForwardRule>();
			m_natRules = new LinkedHashSet<NATRule>();
			m_customRules = new LinkedHashSet<String>();
			m_allowIcmp = true;
			m_allowForwarding = false;
			
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
		try {
			// Open the file that is the first command line parameter
			FileInputStream fstream = new FileInputStream(sourceFile);

			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
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
			s_logger.error("the file: " + sourceFile + " does not exist");
		} catch(IOException ioe) {
			s_logger.error("IOException while trying to open: " + sourceFile);
			ioe.printStackTrace();
		}

		s_logger.trace("size of destination is" + destination.size());
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

	private void initialize() throws KuraException {
		
		s_logger.debug("Parsing current firewall configuraion");
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
					String interfaceName = null;
					String protocol = null;
					int inPort = -1;
					int outPort = -1;
					String sport = null;
					String permittedMac = null;
					String permittedNetwork = null;
					int permittedNetworkMask = -1;
					String address = null;
					
					StringTokenizer st = new StringTokenizer(line);
					while(st.hasMoreTokens()) {
						String token = st.nextToken();
						if(token.equals("iptables")) {
							st.nextToken();	//skip -t
							st.nextToken(); //skip nat
							st.nextToken();	//skip -A
							st.nextElement();	//skip PREROUTING
						} else if(token.equals("-i")) {
							interfaceName = st.nextToken();
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
					
					PortForwardRule portForwardRule = new PortForwardRule(interfaceName, address, protocol, inPort, outPort, permittedNetwork, permittedNetworkMask, permittedMac, sport);
					s_logger.debug("Adding port forward rule: " + portForwardRule.toString());
					m_portForwardRules.add(portForwardRule);
				} else if(line.startsWith("iptables -t nat -A POSTROUTING")) {
					s_logger.debug("Found NAT rule");
					
					//NAT Rule
					String destinationInterface = null;
					String sourceInterface = null;
					boolean masquerade = false;
					
					//just do this one by one
					StringTokenizer st = new StringTokenizer(line);
					st.nextToken();		//skip iptables
					String token = st.nextToken();		//get -t or -A (depending on whether or not masquerade is enabled)
					if(token.equals("-t")) {
						masquerade = true;
						st.nextToken();		//skip nat
						st.nextToken();		//skip -A
						st.nextToken();		//skip POSTROUTING
						st.nextToken();		//skip -o
						destinationInterface = st.nextToken();
						st.nextToken();		//skip -j
						st.nextToken();		//skip MASQUERADE
						st.nextToken();		//skip iptables
						st.nextToken();		//skip -A
					}

					//get the rest (or continue on if no MASQ)
					st.nextToken();		//skip FORWARD
					st.nextToken();		//skip -i
					destinationInterface = st.nextToken();
					st.nextToken();		//skip -o
					sourceInterface = st.nextToken();
					
					s_logger.debug("Parsed NAT rule with" +
							"   sourceInterface: " + sourceInterface +
							"   destinationInterface: " + destinationInterface +
							"   masquerade: " + masquerade );

					NATRule natRule = new NATRule(sourceInterface, destinationInterface, masquerade);
					s_logger.debug("Adding NAT rule " + natRule.toString());
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
		s_logger.trace("writing to file:  " + FIREWALL_TMP_SCRIPT_NAME);
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(FIREWALL_TMP_SCRIPT_NAME));
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
			pw.println("#custom nat service rules");
			Iterator<NATRule> itNATRules = m_natRules.iterator();
			while(itNATRules.hasNext()) {
			    pw.println(itNATRules.next());
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
			
			Process proc = null; 
			try {
				proc = ProcessUtil.exec("chmod 755 " + FIREWALL_TMP_SCRIPT_NAME);
				proc.waitFor();
			}
			finally {
				ProcessUtil.destroy(proc);
			}
			
			//move the file if we made it this far
			File tmpFirewallFile = new File(FIREWALL_TMP_SCRIPT_NAME);
			File firewallFile = new File(FIREWALL_SCRIPT_NAME);
			if(!FileUtils.contentEquals(tmpFirewallFile, firewallFile)) {
				if(tmpFirewallFile.renameTo(firewallFile)) {
					s_logger.trace("Successfully wrote firewall file");
					return true;
				} else {
					s_logger.error("Failed to write firewall file");
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for firewall");
				}
			} else {
				s_logger.info("Not rewriting firewall file because it is the same");
				return false;
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
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

	public void addPortForwardRule(String iface, String address, String protocol, int inPort, int outPort, String permittedNetwork, String permittedNetworkPrefix, String permittedMAC, String sourcePortRange)
						throws KuraException {
		try {
			PortForwardRule newPortForwardRule = null;
			if(permittedNetworkPrefix != null) {
				newPortForwardRule = new PortForwardRule(iface, address, protocol, inPort, outPort, permittedNetwork, Short.parseShort(permittedNetworkPrefix), permittedMAC, sourcePortRange);
			} else {
				newPortForwardRule = new PortForwardRule(iface, address, protocol, inPort, outPort, permittedNetwork, -1, permittedMAC, sourcePortRange);	
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
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void addNatRule(String sourceInterface, String destinationInterface, boolean masquerade) throws KuraException {
		
		try {
		    if(sourceInterface == null || sourceInterface.isEmpty()) {
		        s_logger.warn("Can't add NAT rule - source interface not specified");
		        return;
		    } else if(destinationInterface == null || destinationInterface.isEmpty()) {
                s_logger.warn("Can't add NAT rule - destination interface not specified");
                return;
		    }
		    
			NATRule newNatRule = new NATRule(sourceInterface, destinationInterface, masquerade);
			
			//make sure it is not already present
			for(NATRule natRule : m_natRules) {
				if(newNatRule.equals(natRule)) {
					s_logger.warn("Not adding nat rule that is already present: " + natRule);
					return;
				}
			}
			
			s_logger.info("adding NAT rule to firewall configuration: " + newNatRule.toString());
			this.m_natRules.add(newNatRule);
			this.m_allowForwarding = true;
			this.update();
		}
		catch (Exception e) {
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
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteNatRule(NATRule rule) throws KuraException {
		try {
			this.m_natRules.remove(rule);
			if (this.m_natRules.size() < 1) {
				this.m_allowForwarding = false;
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
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public void replaceAllNatRules(LinkedHashSet<NATRule> newNatRules) throws KuraException {
		try {
			this.m_natRules = newNatRules;
			this.m_allowForwarding = (this.m_natRules != null && this.m_natRules.size() > 0);
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAllNatRules() throws KuraException {
		try {
			this.m_natRules.clear();
			this.m_allowForwarding = false;
			this.update();
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void blockAllPorts() throws KuraException {
		deleteAllLocalRules();
		deleteAllPortForwardRules();
		deleteAllNatRules();
		this.update();
	}

	public void unblockAllPorts() throws KuraException {
		deleteAllLocalRules();
		deleteAllPortForwardRules();
		deleteAllNatRules();
		this.update();
	}

	/**
	 * This method is call when the EventListener receives a new configuration event.  The Configuration
	 * Object is then requested from the Configuration Manager Service, and the updateConfiguration 
	 * method is called.
	 */
	/*public void handleEvent(Event arg0) {
		// Get the new configuration object form the Configuration Manager Service
		Object configurationObject = configManager.getConfiguration(CONFIGURATION_NAME);
		// Make sure it is a Java Properties object and then update the configuration parameters
		if(configurationObject instanceof java.util.Properties) {
			s_logger.info("New configuration received");
			if(processNewConfig((Properties)configurationObject)) {
				configManager.storeConfiguration(CONFIGURATION_NAME);
			
			}
		} else {
			s_logger.error("Invalid Configuration Object of type:  " + configurationObject.getClass().toString());
		}
	}*/
	
	
//	public Object receiveConfig(Object config) throws KuraConfigurationException {
//		try {
//		// Get the new configuration object form the Configuration Manager Service
//		Object configurationObject = config;
//		// Make sure it is a Java Properties object and then update the configuration parameters
//		if(configurationObject instanceof java.util.Properties) {
//			s_logger.info("New configuration received");
//			if(processNewConfig((Properties)configurationObject)) {
//				s_logger.info("New configuration successfully submitted");
//				
//			}
//		} else {
//			s_logger.error("Invalid Configuration Object of type:  " + configurationObject.getClass().toString());
//		}
//		}catch (Exception e){
//			throw new KuraConfigurationException(LABEL + "error while trying to submit configuration for firewall");
//		}
//		//we don't want the config manager to archive this - since we already did in the filesystem.
//		return null;
//	}
//	
//	private boolean processNewConfig(Properties props) {
//		ArrayList natRules = new ArrayList();
//		ArrayList localRules = new ArrayList();
//		ArrayList portForwardRules = new ArrayList();
//		
//		NewRule newRule = null;
//		String ruleString, param;
//		Enumeration keys = props.keys();
//		Properties configs = new Properties();
//		
//		
//		try {
//			while(keys.hasMoreElements()) {
//				String key = (String)keys.nextElement();
//				ruleString = key.substring(0, key.lastIndexOf('_'));
//				param = key.substring(key.lastIndexOf('_')+1);
//				
//				newRule = getRule(ruleString);
//				s_logger.trace("New Rule");
//				s_logger.trace(" type = " + newRule.type); 
//				s_logger.trace(" index = " + newRule.index);
//				s_logger.trace(" param = " + param);
//				s_logger.trace(" value = " + (String)props.getProperty(key));
//				
//				// Check if the new rule is a NATRule
//				if(newRule.type.compareTo("NATRule")==0) {
//					s_logger.trace("NATRule found");
//					// Check if it is already contained in the natRules ArrayList
//					for(int i=0; i<natRules.size(); i++) {
//						NewRule tmpRule = (NewRule)natRules.get(i);
//						if(tmpRule.index == newRule.index) {
//							s_logger.trace("NATRule index recognized");
//							newRule.natRule = tmpRule.natRule;
//						}
//					}
//					// If not, create the NATRule and add it
//					if(newRule.natRule == null) {
//						s_logger.trace("NATRule index not recognized, creating new");
//						newRule.natRule = new NATRule();
//						natRules.add(newRule);
//					}
//					// Get the parameter and add it
//					if(param.compareTo("natSourceNetwork")==0) {
//						newRule.natRule.setNatSourceNetwork((String)props.get(key));
//					} else if(param.compareTo("sourceInterface")==0) {
//						newRule.natRule.setSourceInterface((String)props.get(key));
//					}  else if(param.compareTo("destinationInterface")==0) {
//						newRule.natRule.setDestinationInterface((String)props.get(key));
//					} else if(param.compareTo("masquerade")==0) {
//						newRule.natRule.setMasquerade(Boolean.valueOf((String)props.get(key)).booleanValue());
//					} else {
//						s_logger.error("New configuration contains malformatted parameter type:  " + param + ", rejecting configuration");
//						return false;
//					}
//					
//				// Check if the new rule is a LocalRule
//				} else if(newRule.type.compareTo("LocalRule")==0) {
//					s_logger.trace("LocalRule found");
//					// Check if it is already contained in the localRules ArrayList
//					for(int i=0; i<localRules.size(); i++) {
//						NewRule tmpRule = (NewRule)localRules.get(i);
//						if(tmpRule.index == newRule.index) {
//							s_logger.trace("LocalRule index recognized");
//							newRule.localRule = tmpRule.localRule;
//						}
//					}
//					// If not, create the LocalRule and add it
//					if(newRule.localRule == null) {
//						s_logger.trace("LocalRule index not recognized, creating new");
//						newRule.localRule = new LocalRule();
//						localRules.add(newRule);
//					}
//					// Get the parameter and add it
//					if(param.compareTo("port")==0) {
//						try {
//							int port = Integer.parseInt((String)props.get(key));
//							newRule.localRule.setPort(port);
//						} catch(NumberFormatException nfe) {
//							newRule.localRule.setPortRange((String)props.get(key));
//						}
//					} else if(param.compareTo("protocol")==0) {
//						newRule.localRule.setProtocol((String)props.get(key));
//					} else if(param.compareTo("permittedNetwork")==0) {
//						newRule.localRule.setPermittedNetwork((String)props.get(key));
//					}  else if(param.compareTo("permittedMAC")==0) {
//						newRule.localRule.setPermittedMAC((String)props.get(key));
//					} else if(param.compareTo("sourcePortRange")==0) {
//						newRule.localRule.setSourcePortRange((String)props.get(key));
//					} else {
//						s_logger.error("New configuration contains malformatted parameter type:  " + param + ", rejecting configuration");
//						return false;
//					}
//				// Check if the new rule is a PortForwardRule
//				} else if(newRule.type.compareTo("PortForwardRule")==0) {
//					s_logger.trace("PortForwardRule found");
//					// Check if it is already contained in the portForwardRules ArrayList
//					for(int i=0; i<portForwardRules.size(); i++) {
//						NewRule tmpRule = (NewRule)portForwardRules.get(i);
//						if(tmpRule.index == newRule.index) {
//							s_logger.trace("PortForwardRule index recognized");
//							newRule.portForwardRule = tmpRule.portForwardRule;
//						}
//					}
//					// If not, create the PortForwardRule and add it
//					if(newRule.portForwardRule == null) {
//						s_logger.trace("PortForwardRule index not recognized, creating new");
//						newRule.portForwardRule = new PortForwardRule();
//						portForwardRules.add(newRule);
//					}
//					// Get the parameter and add it
//					if(param.compareTo("address")==0) {
//						newRule.portForwardRule.setAddress((String)props.get(key));
//					} else if(param.compareTo("iface")==0) {
//						newRule.portForwardRule.setIface((String)props.get(key));
//					} else if(param.compareTo("outPort")==0) {
//						newRule.portForwardRule.setOutPort(Integer.parseInt((String)props.get(key)));
//					} else if(param.compareTo("inPort")==0) {
//						newRule.portForwardRule.setInPort(Integer.parseInt((String)props.get(key)));
//					} else if(param.compareTo("protocol")==0) {
//						newRule.portForwardRule.setProtocol((String)props.get(key));
//					} else if(param.compareTo("permittedNetwork")==0) {
//						newRule.portForwardRule.setPermittedNetwork((String)props.get(key));
//					} else if(param.compareTo("permittedNetworkMask")==0) {
//						newRule.portForwardRule.setPermittedNetworkMask(networkUtilityService.getNetmaskIntForm((String)props.get(key)));
//					}  else if(param.compareTo("permittedMAC")==0) {
//						newRule.portForwardRule.setPermittedMAC((String)props.get(key));
//					} else if(param.compareTo("sourcePortRange")==0) {
//						newRule.portForwardRule.setSourcePortRange((String)props.get(key));
//					} else {
//						s_logger.error("New configuration contains malformatted parameter type:  " + param + ", rejecting configuration");
//						return false;
//					}
//				} else {
//					s_logger.error("New configuration contains malformatted rule type:  " + newRule.type + ", rejecting configuration");
//					return false;
//				}
//			}
//		
//			// Now that the new rule ArrayLists are all populated, check to make sure all required parameters are present
//			for(int i=0; i<natRules.size(); i++) {
//				s_logger.trace("Checking " + natRules.size() + " NATRules for completion");
//				if(!((NewRule)natRules.get(i)).natRule.isComplete()) {
//					s_logger.error("New configuration NATRule: " + ((NewRule)natRules.get(i)).toString()+ " does not contain all required parameters, rejecting configuration");
//					return false;
//				}
//			}
//			for(int i=0; i<localRules.size(); i++) {
//				s_logger.trace("Checking " + localRules.size() + " LocalRules for completion");
//				if(!((NewRule)localRules.get(i)).localRule.isComplete()) {
//					s_logger.error("New configuration LocalRule: " + ((NewRule)localRules.get(i)).toString()+ " does not contain all required parameters, rejecting configuration");
//					return false;
//				}
//			}
//			for(int i=0; i<portForwardRules.size(); i++) {
//				s_logger.trace("Checking " + portForwardRules.size() + " PortForwardRules for completion");
//				if(!((NewRule)portForwardRules.get(i)).portForwardRule.isComplete()) {
//					s_logger.error("New configuration PortForwardRule: " + ((NewRule)portForwardRules.get(i)).toString()+ " does not contain all required parameters, rejecting configuration");
//					return false;
//				}
//			}
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false;
//		}
//
//		// Delete all the current rules.
//		try {
//			deleteAllNatRules();
//			deleteAllLocalRules();
//			deleteAllPortForwardRules();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false;
//		}
//		
//		// Add all new rules
//		for(int i=0; i<natRules.size(); i++) {
//			NATRule tmpNatRule = ((NewRule)natRules.get(i)).natRule;
//			try {
//				addNatRule(tmpNatRule.getNatSourceNetwork(), 
//						tmpNatRule.getDestinationInterface(), 
//						tmpNatRule.getDestinationInterface(), 
//						tmpNatRule.getMasquerade());
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		for(int i=0; i<localRules.size(); i++) {
//			s_logger.trace("Adding " + localRules.size() + " LocalRules");
//			LocalRule tmpLocalRule = ((NewRule)localRules.get(i)).localRule;
//			s_logger.trace("LocalRule: " + tmpLocalRule.toString());
//			try {
//				if(tmpLocalRule.getPort() != 0) {
//					addLocalRule(tmpLocalRule.getPort(),
//							tmpLocalRule.getProtocol(), 
//							tmpLocalRule.getPermittedNetwork(), 
//							tmpLocalRule.getPermittedNetworkMask(), 
//							tmpLocalRule.getPermittedMAC(), 
//							tmpLocalRule.getSourcePortRange());
//				} else {
//					addLocalRule(tmpLocalRule.getPortRange(),
//							tmpLocalRule.getProtocol(), 
//							tmpLocalRule.getPermittedNetwork(), 
//							tmpLocalRule.getPermittedNetworkMask(), 
//							tmpLocalRule.getPermittedMAC(), 
//							tmpLocalRule.getSourcePortRange());
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		for(int i=0; i<portForwardRules.size(); i++) {
//			PortForwardRule tmpPortForwardRule = ((NewRule)portForwardRules.get(i)).portForwardRule;
//			try {
//				addPortForwardRule(tmpPortForwardRule.getIface(), 
//						tmpPortForwardRule.getAddress(), 
//						tmpPortForwardRule.getProtocol(), 
//						tmpPortForwardRule.getInPort(), 
//						tmpPortForwardRule.getOutPort(), 
//						tmpPortForwardRule.getPermittedNetwork(),
//						tmpPortForwardRule.getPermittedNetworkMask(),
//						tmpPortForwardRule.getPermittedMAC(), 
//						tmpPortForwardRule.getSourcePortRange());
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		
//		try {
//			writeFile();
//			writeFile();
//			
//		} catch (Exception e) {
//			s_logger.error("Error writing new  configuration to file");
//			e.printStackTrace();
//		}
//		
//		return true;
//		
//	}

	private void runScript() throws KuraException {
		Process proc = null;
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
			ProcessUtil.destroy(proc);
		}
	}
	
	/*
	 * Saves the current iptables config into /etc/sysconfig/iptables
	 */
	private void iptablesSave() throws KuraException {
		Process proc = null;
		try {
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
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
			ProcessUtil.destroy(proc);
		}
	}
	
	public void enable() throws KuraException {
		this.update();
		this.iptablesSave();
	}
	
	public void disable() throws KuraException {
		this.iptablesSave();

		s_logger.trace("writing to file:  " + FIREWALL_TMP_SCRIPT_NAME);
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
					s_logger.trace("Successfully wrote firewall file");
					runScript();
				}else{
					s_logger.error("Failed to write firewall file");
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for firewall");
				}
			} else {
				s_logger.info("Not rewriting firewall file because it is the same");
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
	    if(writeFile()) {
	    	runScript();
	    }
	}
}
