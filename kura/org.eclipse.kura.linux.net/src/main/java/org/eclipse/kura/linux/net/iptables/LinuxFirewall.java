/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * Linux firewall implementation
 * 
 * @author eurotech
 */
public class LinuxFirewall {
	private static final Logger s_logger = LoggerFactory.getLogger(LinuxFirewall.class);

	private static final String RULE_DELIMETER = ";";
	private static final String OS_VERSION = System.getProperty("kura.os.version");

	private static final String ALLOW_ALL_TRAFFIC_TO_LOOPBACK = "iptables -A INPUT -i lo -j ACCEPT";
	private static final String ALLOW_ONLY_INCOMING_TO_OUTGOING = "iptables -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT";

	private static final String[] CLEAR_ALL_CHAINS = {
		"iptables -F INPUT",
		"iptables -F OUTPUT",
		"iptables -F FORWARD",
		"iptables -t nat -F",
	};

	private static final String[] BLOCK_POLICY = {
		"iptables -P INPUT DROP",			// block all ports for input traffic
		"iptables -P OUTPUT ACCEPT", 		// block Output Traffic
		"iptables -P FORWARD DROP", 		// block forward Traffic
		ALLOW_ALL_TRAFFIC_TO_LOOPBACK, 		// allow all traffic to the loop back interface
		ALLOW_ONLY_INCOMING_TO_OUTGOING,	// allow Only incoming connection related to Outgoing connection
	};

	private static final String[] ALLOW_ICMP = {
		"iptables -A INPUT -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT",
		"iptables -A OUTPUT -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j ACCEPT"
	};

	private static final String[] DO_NOT_ALLOW_ICMP = {
		"iptables -A INPUT -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j DROP",
		"iptables -A OUTPUT -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j DROP"
	};

	private static LinuxFirewall s_linuxFirewall;

	private static Object s_lock = new Object();

	private static final String IP_FORWARD_FILE_NAME = "/proc/sys/net/ipv4/ip_forward";
	private static final String FIREWALL_CONFIG_FILE_NAME = "/etc/sysconfig/iptables";
	private static final String CUSTOM_FIREWALL_SCRIPT_NAME = "/etc/init.d/firewall_cust";

	private LinkedHashSet<LocalRule> m_localRules;
	private LinkedHashSet<PortForwardRule> m_portForwardRules;
	private LinkedHashSet<NATRule> m_autoNatRules;
	private LinkedHashSet<NATRule> m_natRules;
	private LinkedHashSet<String> m_customRules;
	private boolean m_allowIcmp;
	private boolean m_allowForwarding;

	private LinuxFirewall() {
		try {
			File oscfile = new File(FIREWALL_CONFIG_FILE_NAME);
			if (!oscfile.exists()) {
				applyClearAllChainsRules();
				applyBlockAllRules();
				iptablesSave();
			} else {
				s_logger.debug("{} file already exists", oscfile);
			}
		} catch (Exception e) {
			s_logger.error("cannot create or read file", e);// File did not exist and was created
		}
		try {
			initialize();
		} catch (KuraException e) {
			s_logger.error("failed to initialize LinuxFirewall", e);
		}
	}

	public static LinuxFirewall getInstance() {
		if(s_linuxFirewall == null) {
			s_linuxFirewall = new LinuxFirewall();
		}

		return s_linuxFirewall;
	}

	public void initialize() throws KuraException {
		s_logger.debug("initialize() :: initializing firewall ...");
		m_localRules = new LinkedHashSet<LocalRule>();
		m_portForwardRules = new LinkedHashSet<PortForwardRule>();
		m_autoNatRules = new LinkedHashSet<NATRule>();
		m_natRules = new LinkedHashSet<NATRule>();
		m_customRules = new LinkedHashSet<String>();
		m_allowIcmp = true;
		m_allowForwarding = false;

		s_logger.debug("initialize() :: Parsing current firewall configuraion");
		parseFirewallConfigurationFile();
	}

	private void parseFirewallConfigurationFile() throws KuraException {
		BufferedReader br = null;
		try {
			List<NatPreroutingChainRule> natPreroutingChain = new ArrayList<NatPreroutingChainRule>();
			List<NatPostroutingChainRule> natPostroutingChain = new ArrayList<NatPostroutingChainRule>();
			List<FilterForwardChainRule> filterForwardChain = new ArrayList<FilterForwardChainRule>();

			br = new BufferedReader(new FileReader(FIREWALL_CONFIG_FILE_NAME));
			String line = null;
			boolean readingNatTable = false;
			boolean readingFilterTable = false;
			lineloop:
				while((line = br.readLine()) != null) {
					line = line.trim();
					//skip any predefined lines or comment lines
					if(line.equals("")) {
						continue;
					}
					if(line.startsWith("#") || line.startsWith(":")) {
						continue;
					}
					if (line.equals("*nat")) {
						readingNatTable = true;
					} else if (line.equals("*filter")) {
						readingFilterTable = true;
					} else if (line.equals("COMMIT")) {
						if (readingNatTable) {
							readingNatTable = false;
						}
						if (readingFilterTable) {
							readingFilterTable = false;
						}
					} else if (readingNatTable && line.startsWith("-A PREROUTING")) {
						natPreroutingChain.add(new NatPreroutingChainRule(line));
					} else if (readingNatTable && line.startsWith("-A POSTROUTING")) {
						natPostroutingChain.add(new NatPostroutingChainRule(line));
					} else if (readingFilterTable && line.startsWith("-A FORWARD")) {
						filterForwardChain.add(new FilterForwardChainRule(line));
					} else if (readingFilterTable && line.startsWith("-A INPUT")) {
						if (ALLOW_ALL_TRAFFIC_TO_LOOPBACK.contains(line)) {
							continue;
						}
						if (ALLOW_ONLY_INCOMING_TO_OUTGOING.contains(line)) {
							continue;
						}
						for(String allowIcmp : ALLOW_ICMP) {
							if(allowIcmp.contains(line)) {
								m_allowIcmp = true;
								continue lineloop;
							}
						}
						for(String allowIcmp : DO_NOT_ALLOW_ICMP) {
							if(allowIcmp.contains(line)) {
								m_allowIcmp = false;
								continue lineloop;
							}
						}
						try {
							LocalRule localRule = new LocalRule(line);
							s_logger.debug("parseFirewallConfigurationFile() :: Adding local rule: {}", localRule);
							m_localRules.add(localRule);
						} catch (KuraException e) {
							s_logger.error("Failed to parse Local Rule: {} - {}", line, e);
						}
					}
				}

			// ! done parsing !
			for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
				// found port forwarding rule ...
				String inboundIfaceName = natPreroutingChainRule.getInputInterface();
				String outboundIfaceName = null;
				String protocol = natPreroutingChainRule.getProtocol();
				int inPort = natPreroutingChainRule.getExternalPort();
				int outPort = natPreroutingChainRule.getInternalPort();
				boolean masquerade = false;
				StringBuilder sbSport = new StringBuilder().append(natPreroutingChainRule.getSrcPortFirst()).append(':').append(natPreroutingChainRule.getSrcPortLast());
				String sport = sbSport.toString();
				String permittedMac = natPreroutingChainRule.getPermittedMacAddress();
				String permittedNetwork = natPreroutingChainRule.getPermittedNetwork();
				int permittedNetworkMask = natPreroutingChainRule.getPermittedNetworkMask();
				String address = natPreroutingChainRule.getDstIpAddress();

				for (NatPostroutingChainRule natPostroutingChainRule : natPostroutingChain) {
					if (natPreroutingChainRule.getDstIpAddress().equals(natPostroutingChainRule.getDstNetwork())) {
						outboundIfaceName = natPostroutingChainRule.getDstInterface();
						if (natPostroutingChainRule.isMasquerade()) {
							masquerade = true;
						}	
					}
				}
				PortForwardRule portForwardRule = new PortForwardRule(
						inboundIfaceName, outboundIfaceName, address, protocol, inPort, outPort,
						masquerade, permittedNetwork, permittedNetworkMask,
						permittedMac, sport);
				s_logger.debug("Adding port forward rule: {}", portForwardRule);
				m_portForwardRules.add(portForwardRule);
			}

			for (NatPostroutingChainRule natPostroutingChainRule : natPostroutingChain) {
				String destinationInterface = natPostroutingChainRule.getDstInterface();
				boolean masquerade = natPostroutingChainRule.isMasquerade();
				String protocol = natPostroutingChainRule.getProtocol();
				if (protocol != null) {
					// found NAT rule, ... maybe
					boolean isNATrule = false;
					String source = natPostroutingChainRule.getSrcNetwork();
					String destination = natPostroutingChainRule.getDstNetwork();
					if (destination != null) {
						StringBuilder sbDestination = new StringBuilder().append(destination).append(':').append(natPostroutingChainRule.getDstMask());
						destination = sbDestination.toString();
					} else {
						isNATrule = true;
					}
					if (source != null) {
						isNATrule = true;
						StringBuilder sbSource = new StringBuilder().append(source).append(':').append(natPostroutingChainRule.getSrcMask());
						source = sbSource.toString();
					} else {
						if (!isNATrule) {
							boolean matchFound = false;
							for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
								if (natPreroutingChainRule.getDstIpAddress().equals(natPostroutingChainRule.getDstNetwork())) {
									matchFound = true;
									break;
								}
							}
							if (!matchFound) {
								isNATrule = true;
							}
						}
					}
					if (isNATrule) {
						// match FORWARD rule to find out source interface ...
						for (FilterForwardChainRule filterForwardChainRule : filterForwardChain) {
							if (natPostroutingChainRule.isMatchingForwardChainRule(filterForwardChainRule)) {
								String sourceInterface = filterForwardChainRule.getInputInterface();
								s_logger.debug("parseFirewallConfigurationFile() :: Parsed NAT rule with" +
										"   sourceInterface: " + sourceInterface +
										"   destinationInterface: " + destinationInterface +
										"   masquerade: " + masquerade + 
										"	protocol: " + protocol + 
										"	source network/host: " + source + 
										"	destination network/host " + destination);
								NATRule natRule = new NATRule(sourceInterface, destinationInterface, protocol, source, destination, masquerade);
								s_logger.debug("parseFirewallConfigurationFile() :: Adding NAT rule {}", natRule);
								m_natRules.add(natRule);
							}
						}
					}
				} else {
					// found Auto NAT rule ...
					// match FORWARD rule to find out source interface ...
					for (FilterForwardChainRule filterForwardChainRule : filterForwardChain) {
						if (natPostroutingChainRule.isMatchingForwardChainRule(filterForwardChainRule)) {
							String sourceInterface = filterForwardChainRule.getInputInterface();
							s_logger.debug("parseFirewallConfigurationFile() :: Parsed auto NAT rule with" +
									"   sourceInterface: " + sourceInterface +
									"   destinationInterface: " + destinationInterface +
									"   masquerade: " + masquerade );

							NATRule natRule = new NATRule(sourceInterface, destinationInterface, masquerade);
							s_logger.debug("parseFirewallConfigurationFile() :: Adding auto NAT rule {}", natRule);
							m_autoNatRules.add(natRule);
						}
					}
				}
			}
		} catch (Exception e) {
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
				s_logger.debug("permittedNetwork: {}", permittedNetwork);
				s_logger.debug("permittedNetworkPrefix: {}", permittedNetworkPrefix);

				newLocalRule = new LocalRule(port, protocol, new NetworkPair(IPAddress.parseHostAddress(permittedNetwork), Short.parseShort(permittedNetworkPrefix)), permittedInterfaceName, unpermittedInterfaceName, permittedMAC, sourcePortRange);
			} else {
				newLocalRule = new LocalRule(port, protocol, new NetworkPair(IPAddress.parseHostAddress("0.0.0.0"), (short)0), permittedInterfaceName, permittedInterfaceName, permittedMAC, sourcePortRange);
			}

			ArrayList<LocalRule> localRules = new ArrayList<LocalRule>();
			localRules.add(newLocalRule);
			addLocalRules(localRules);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void addLocalRules(List<LocalRule> newLocalRules) throws KuraException {
		try {
			boolean doUpdate = false;
			for (LocalRule newLocalRule : newLocalRules) {
				//make sure it is not already present
				boolean addRule = true;
				for(LocalRule localRule : m_localRules) {
					if(newLocalRule.equals(localRule)) {
						addRule = false;
						break;
					}
				}
				if (addRule) {
					s_logger.info("Adding local rule to firewall configuration: {}", newLocalRule.toString());
					m_localRules.add(newLocalRule);
					doUpdate = true;
				} else {
					s_logger.warn("Not adding local rule that is already present: {}", newLocalRule.toString());
				}
			}
			if (doUpdate) {
				update();
			}
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

			ArrayList<PortForwardRule> portForwardRules = new ArrayList<PortForwardRule>();
			portForwardRules.add(newPortForwardRule);
			addPortForwardRules(portForwardRules);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void addPortForwardRules(List<PortForwardRule> newPortForwardRules) throws KuraException {
		try {
			boolean doUpdate = false;
			for (PortForwardRule newPortForwardRule : newPortForwardRules) {
				//make sure it is not already present
				boolean addRule = true;
				for(PortForwardRule portForwardRule : m_portForwardRules) {
					if(newPortForwardRule.equals(portForwardRule)) {
						addRule = false;
						break;
					}
				}
				if (addRule) {
					s_logger.info("Adding port forward rule to firewall configuration: {}", newPortForwardRule.toString());
					m_portForwardRules.add(newPortForwardRule);
					doUpdate = true;
				} else {
					s_logger.warn("Not adding port forward rule that is already present: {}", newPortForwardRule.toString());
				}
			}
			if (doUpdate) {
				m_allowForwarding = true;
				update();
			}
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
			ArrayList<NATRule> natRules = new ArrayList<NATRule>();
			natRules.add(newNatRule);
			addAutoNatRules(natRules);
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

			ArrayList<NATRule> natRules = new ArrayList<NATRule>();
			natRules.add(newNatRule);
			addNatRules(natRules);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void addAutoNatRules(List<NATRule> newNatRules) throws KuraException {
		addNatRules(newNatRules, m_autoNatRules);
	}

	public void addNatRules(List<NATRule> newNatRules) throws KuraException {
		addNatRules(newNatRules, m_natRules);
	}

	private void addNatRules(List<NATRule> newNatRules, LinkedHashSet<NATRule> rules) throws KuraException {
		try {
			boolean doUpdate = false;
			for (NATRule newNatRule : newNatRules) {
				//make sure it is not already present
				boolean addRule = true;
				for(NATRule natRule : rules) {
					if(newNatRule.equals(natRule)) {
						addRule = false;
						break;
					}
				}
				if (addRule) {
					s_logger.info("Adding auto NAT rule to firewall configuration: {}", newNatRule.toString());
					rules.add(newNatRule);
					doUpdate = true;
				} else {
					s_logger.warn("Not adding auto nat rule that is already present: {}", newNatRule.toString());
				}
			}
			if (doUpdate) {
				m_allowForwarding = true;
				update();
			}
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
		if (m_portForwardRules == null){
			return;
		}
		try {
			m_portForwardRules.remove(rule);
			if (((m_autoNatRules != null) && (m_autoNatRules.size() < 1))
					&& ((m_natRules != null) && (m_natRules.size() < 1))
					&& ((m_portForwardRules != null) && (m_portForwardRules.size() < 1))) {

				m_allowForwarding = false;
			}
			this.update();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void deleteAutoNatRule(NATRule rule) throws KuraException {
		if (m_autoNatRules == null){
			return;
		}
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

	private void applyRules() throws KuraException {
		SafeProcess proc = null;
		try {
			applyClearAllChainsRules();
			applyBlockAllRules();

			s_logger.debug("Applying local rules...");	
			if(m_localRules != null){
				for(LocalRule lr: m_localRules){	
					boolean status = applyRule(lr.toString());
					s_logger.trace("applyRules() :: Local rule: {} has been applied with status={}", lr, status);
				}
			}

			s_logger.debug("Applying port forward rules...");	
			if(m_portForwardRules != null){
				for(PortForwardRule pfr: m_portForwardRules) {
					boolean status = applyRule(pfr.toString());
					s_logger.trace("applyRules() :: Port Forward rule: {} has been applied with status={}", pfr, status);
				}
			}

			s_logger.debug("Applying auto NAT rules...");	
			if(m_autoNatRules != null){
				List<NatPostroutingChainRule> appliedNatPostroutingChainRules = new ArrayList<NatPostroutingChainRule>();
				for(NATRule autoNatRule: m_autoNatRules) {
					boolean found = false;
					NatPostroutingChainRule natPostroutingChainRule = autoNatRule.getNatPostroutingChainRule();;
					for (NatPostroutingChainRule appliedNatPostroutingChainRule : appliedNatPostroutingChainRules) {

						if(appliedNatPostroutingChainRule.equals(natPostroutingChainRule)) {
							found = true;
							break;
						}
					}
					if (found) {
						autoNatRule.setMasquerade(false);
					} 

					boolean status = applyRule(autoNatRule.toString());
					s_logger.trace("applyRules() :: Auto NAT rule: {} has been applied with status={}", autoNatRule, status);	
					if (status) {
						appliedNatPostroutingChainRules.add(natPostroutingChainRule);
					}
				}
			}

			s_logger.debug("Applying NAT rules...");	
			if(m_natRules != null){
				for(NATRule natRule: m_natRules){
					applyRule(natRule.toString());
				}
			}

			s_logger.debug("Applying custom rules...");	
			if(m_customRules != null){
				for(String customRule: m_customRules){
					applyRule(customRule.toString());
				}
			}

			s_logger.debug("Managing ICMP...");	
			if(m_allowIcmp){
				proc = ProcessUtil.exec(ALLOW_ICMP[0]);
				proc.waitFor();
				proc = ProcessUtil.exec(ALLOW_ICMP[1]);
				proc.waitFor();
			} else {
				proc = ProcessUtil.exec(DO_NOT_ALLOW_ICMP[0]);
				proc.waitFor();
				proc = ProcessUtil.exec(DO_NOT_ALLOW_ICMP[1]);
				proc.waitFor();
			}

			s_logger.debug("Managing port forwarding...");
			enableForwarding(m_allowForwarding);
			runCustomFirewallScript();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	private static void enableForwarding(boolean allow) throws KuraException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(IP_FORWARD_FILE_NAME);
			if (allow) {
				fw.write('1');
			} else {
				fw.write('0');
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
			}
		}
	}

	private static void applyClearAllChainsRules() throws KuraException {
		s_logger.debug("Cleaning chains...");
		SafeProcess proc = null;
		try {
			for(String clearRule: CLEAR_ALL_CHAINS){
				if(clearRule.startsWith("iptables")){
					proc = ProcessUtil.exec(clearRule);
					proc.waitFor();
				}
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	private static void applyBlockAllRules() throws KuraException {
		s_logger.debug("Setting block policy...");
		SafeProcess proc = null;
		try {
			for(String blockRule: BLOCK_POLICY){
				if(blockRule.startsWith("iptables")){
					proc = ProcessUtil.exec(blockRule);
					proc.waitFor();
				}
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}


	private static boolean applyRule(String ruleToApply) throws KuraException {
		boolean ret = false;
		SafeProcess proc = null;
		try {
			String [] aRules = ruleToApply.split(RULE_DELIMETER);
			for (String rule : aRules) {	
				proc = ProcessUtil.exec(rule.trim());
				int status = proc.waitFor();
				ret = (status == 0)? true : false;	
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		return ret;
	}

	/*
	 * Runs custom firewall script
	 */
	private static void runCustomFirewallScript() throws KuraException {
		SafeProcess proc = null;
		try {
			File file = new File(CUSTOM_FIREWALL_SCRIPT_NAME);
			if(file.exists()) {
				s_logger.info("Running custom firewall script - {}", CUSTOM_FIREWALL_SCRIPT_NAME);			
				proc = ProcessUtil.exec("sh " + CUSTOM_FIREWALL_SCRIPT_NAME);
				proc.waitFor();
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	/*
	 * Saves the current iptables config into /etc/sysconfig/iptables
	 */
	private void iptablesSave() throws KuraException {
		SafeProcess proc = null;
		BufferedReader br = null;
		PrintWriter out = null;
		try {
			int status = -1;
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
					OS_VERSION.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getImageName() + "_" + KuraConstants.ReliaGATE_50_21_Ubuntu.getImageVersion()) ||
					OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) || 
					OS_VERSION.equals(KuraConstants.BeagleBone.getImageName()) ||
					OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
				proc = ProcessUtil.exec("iptables-save");
				status = proc.waitFor();
				if (status != 0) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Failed to execute the iptable-save command");
				}
				String line = null;
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				out = new PrintWriter(FIREWALL_CONFIG_FILE_NAME);
				while ((line = br.readLine()) != null) {
					out.println(line);
				}
			} else {
				proc= ProcessUtil.exec("service iptables save");
				status = proc.waitFor();
			}
			s_logger.debug("iptablesSave() :: completed!, status={}", status);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					s_logger.error("iptablesSave() :: failed to close BufferedReader - {}", e); 
				}
			}
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	public void enable() throws KuraException {
		update();
	}

	public void disable() throws KuraException {
		applyClearAllChainsRules();
		iptablesSave();
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
			applyRules();
			iptablesSave();
		}
	}
}
