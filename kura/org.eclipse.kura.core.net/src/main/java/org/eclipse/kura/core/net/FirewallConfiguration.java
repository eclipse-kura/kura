package org.eclipse.kura.core.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallConfiguration {
	
	private static final Logger s_logger = LoggerFactory.getLogger(FirewallConfiguration.class);
	
	public static final String OPEN_PORTS_PROP_NAME = "firewall.open.ports";
	public static final String PORT_FORWARDING_PROP_NAME = "firewall.port.forwarding";
	public static final String NAT_PROP_NAME = "firewall.nat";
	
	public static final String DFLT_OPEN_PORTS_VALUE = "22,tcp,,,,,#;80,tcp,,eth0,,,#;80,tcp,,eth1,,,#;80,tcp,,wlan0,,,#;80,tcp,10.234.0.0/16,,,,#;1450,tcp,,eth0,,,#;1450,tcp,,eth1,,,#;1450,tcp,,wlan0,,,#;502,tcp,127.0.0.1/32,,,,#;53,udp,,eth0,,,#;53,udp,,eth1,,,#;53,udp,,wlan0,,,#;67,udp,,eth0,,,#;67,udp,,eth1,,,#;67,udp,,wlan0,,,#;8000,tcp,,eth0,,,#;8000,tcp,,eth1,,,#;8000,tcp,,wlan0,,,#";
	public static final String DFLT_PORT_FORWARDING_VALUE = "";
	public static final String DFLT_NAT_VALUE ="";
	
	private List<FirewallOpenPortConfigIP<? extends IPAddress>> m_openPortConfigs;
	private List<FirewallPortForwardConfigIP<? extends IPAddress>> m_portForwardConfigs;
	private List<FirewallNatConfig> m_natConfigs;
	private List<FirewallAutoNatConfig> m_autoNatConfigs;
	
	public FirewallConfiguration() {
		m_openPortConfigs = new ArrayList<FirewallOpenPortConfigIP<? extends IPAddress>>();
		m_portForwardConfigs = new ArrayList<FirewallPortForwardConfigIP<? extends IPAddress>>();
		m_natConfigs = new ArrayList<FirewallNatConfig>();
		m_autoNatConfigs = new ArrayList<FirewallAutoNatConfig>();
	}
	
	public FirewallConfiguration(Map<String, Object> properties) {
		this();
		String str = null;
		String [] astr = null;
		if (properties.containsKey(OPEN_PORTS_PROP_NAME)) {
			str = (String)properties.get(OPEN_PORTS_PROP_NAME);
			astr = str.split(";");
			if (astr.length > 0) {
				for (String sop : astr) {
					try {
						String [] sa = sop.split(",");
						if (sa.length >= 7) {
							NetProtocol protocol = NetProtocol.valueOf(sa[1]);
							String permittedNetwork = sa[2];
							short permittedNetworkMask = 0;
							if ((permittedNetwork != null) && !permittedNetwork.isEmpty()) {
								permittedNetwork = sa[2].split("/")[0];
								permittedNetworkMask = Short.parseShort(sa[2].split("/")[1]);
							}
							String permittedIface = sa[3];
							String unpermittedIface = sa[4];
							String permittedMAC = sa[5];
							String sourcePortRange = sa[6];
							int port = 0;
							String portRange = null;
							FirewallOpenPortConfigIP<? extends IPAddress> openPortEntry = null;
							if(sa[0].indexOf(':') > 0) {
								portRange = sa[0];
								openPortEntry = new FirewallOpenPortConfigIP4(portRange, protocol,
										new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(permittedNetwork), permittedNetworkMask),
										permittedIface, unpermittedIface, permittedMAC, sourcePortRange);
							} else {
								port = Integer.parseInt(sa[0]);
								openPortEntry = new FirewallOpenPortConfigIP4(port, protocol,
										new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(permittedNetwork), permittedNetworkMask),
										permittedIface, unpermittedIface, permittedMAC, sourcePortRange);
							}
							m_openPortConfigs.add(openPortEntry);
						}
					} catch (Exception e) {
						s_logger.error("Failed to parse Open Port Entry - {}", e);
					}
				}
			}
		}
		if (properties.containsKey(PORT_FORWARDING_PROP_NAME)) {
			str = (String)properties.get(PORT_FORWARDING_PROP_NAME);
			astr = str.split(";");
			if (astr.length > 0) {
				for (String sop : astr) {
					try {
						String [] sa = sop.split(",");
						if (sa.length >= 10) {
							String inboundIface = sa[0];
							String outboundIface = sa[1];
							IP4Address address = (IP4Address)IPAddress.parseHostAddress(sa[2]);
							NetProtocol protocol = NetProtocol.valueOf(sa[3]);
							int inPort = Integer.parseInt(sa[4]);
							int outPort = Integer.parseInt(sa[5]);
							boolean masquerade = Boolean.parseBoolean(sa[6]);
							String permittedNetwork = sa[7];
							short permittedNetworkMask = 0;
							if ((permittedNetwork != null) && !permittedNetwork.isEmpty()) {	
								permittedNetwork = sa[7].split("/")[0];
								permittedNetworkMask = Short.parseShort(sa[7].split("/")[1]);
							}
							String permittedMAC = sa[8]; 
							String sourcePortRange = sa[9];
							FirewallPortForwardConfigIP<? extends IPAddress> portForwardEntry = 
									new FirewallPortForwardConfigIP4(inboundIface, outboundIface, address, protocol, inPort, outPort, masquerade, 
											new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(permittedNetwork), permittedNetworkMask),
											permittedMAC, sourcePortRange);
							m_portForwardConfigs.add(portForwardEntry);
						}
					} catch (Exception e) {
						s_logger.error("Failed to parse Port Forward Entry - {}", e);
					}
				}
			}
		}		
		if (properties.containsKey(NAT_PROP_NAME)) {
			str = (String)properties.get(NAT_PROP_NAME);
			astr = str.split(";");
			if (astr.length > 0) {
				for (String sop : astr) {
					String [] sa = sop.split(",");
					if (sa.length >= 7) {
						String srcIface = sa[0];
						String dstIface = sa[1];
						String protocol = sa[2];
						String src = sa[4];
						String dst = sa[5];
						boolean masquerade = Boolean.parseBoolean(sa[6]);
						FirewallNatConfig natEntry = new FirewallNatConfig(srcIface, dstIface, protocol, src, dst, masquerade);
						m_natConfigs.add(natEntry);
					}
				}
			}
		}
	}
	
	public void addConfig(NetConfig netConfig) {
		if (netConfig instanceof FirewallOpenPortConfigIP4) {
			m_openPortConfigs.add((FirewallOpenPortConfigIP4)netConfig);
		} else if(netConfig instanceof FirewallPortForwardConfigIP4) {
			m_portForwardConfigs.add((FirewallPortForwardConfigIP4)netConfig);
		} else if (netConfig instanceof FirewallNatConfig) {
			m_natConfigs.add((FirewallNatConfig)netConfig);
		} else if (netConfig instanceof FirewallAutoNatConfig) {
			m_autoNatConfigs.add((FirewallAutoNatConfig)netConfig);
		}
	}
	
	public List<NetConfig> getConfigs() {
		List<NetConfig> netConfigs = new ArrayList<NetConfig>();
		
		for (FirewallOpenPortConfigIP<? extends IPAddress> openPortConfig : m_openPortConfigs) {
			netConfigs.add(openPortConfig);
		}
		for (FirewallPortForwardConfigIP<? extends IPAddress> portForwardConfig : m_portForwardConfigs) {
			netConfigs.add(portForwardConfig);
		}
		for (FirewallNatConfig natConfig : m_natConfigs) {
			netConfigs.add(natConfig);
		}
		for (FirewallAutoNatConfig autoNatConfig : m_autoNatConfigs) {
			netConfigs.add(autoNatConfig);
		}
		
		return netConfigs;
	}
	
	public List<FirewallOpenPortConfigIP<? extends IPAddress>> getOpenPortConfigs() {
		return m_openPortConfigs;
	}

	public List<FirewallPortForwardConfigIP<? extends IPAddress>> getPortForwardConfigs() {
		return m_portForwardConfigs;
	}

	public List<FirewallNatConfig> getNatConfigs() { 
		return m_natConfigs;
	}

	public List<FirewallAutoNatConfig> getAutoNatConfigs() {
		return m_autoNatConfigs;
	}

	public Map<String,Object> getConfigurationProperties() {
		Map<String,Object> props = new HashMap<String,Object>();
		props.put(OPEN_PORTS_PROP_NAME, formOpenPortConfigPropValue());
		props.put(PORT_FORWARDING_PROP_NAME, formPortForwardConfigPropValue());
		props.put(NAT_PROP_NAME, formNatConfigPropValue());
		return props;
	}
	
	private String formOpenPortConfigPropValue() {
		s_logger.warn("<IAB> [+] formOpenPortConfigPropValue()");
		StringBuilder sb = new StringBuilder();
		for (FirewallOpenPortConfigIP<? extends IPAddress> openPortConfig : m_openPortConfigs) {
			String port = openPortConfig.getPortRange();
			if(port == null) {
				port = Integer.toString(openPortConfig.getPort());
			}
			sb.append(port).append(',');
			sb.append(openPortConfig.getProtocol()).append(',');
			if(openPortConfig.getPermittedNetwork() != null) {
				sb.append(openPortConfig.getPermittedNetwork());
			}
			sb.append(',');
			if (openPortConfig.getPermittedInterfaceName() != null) {
				sb.append(openPortConfig.getPermittedInterfaceName());
			}
			sb.append(',');
			if (openPortConfig.getUnpermittedInterfaceName() != null) {
				sb.append(openPortConfig.getUnpermittedInterfaceName());
			}
			sb.append(',');
			if (openPortConfig.getPermittedMac() != null) {
				sb.append(openPortConfig.getPermittedMac());
			}
			sb.append(',');
			if (openPortConfig.getSourcePortRange() != null) {
				sb.append(openPortConfig.getSourcePortRange());
			}
			sb.append(",#;");
		}
		int ind = sb.lastIndexOf(";");
		if (ind > 0) {
			sb.deleteCharAt(ind);
		}
		s_logger.warn("<IAB> [-] formOpenPortConfigPropValue() ::-> {}", sb.toString());
		return sb.toString();
	}
	
	private String formPortForwardConfigPropValue() {
		StringBuilder sb = new StringBuilder();
		for (FirewallPortForwardConfigIP<? extends IPAddress> portForwardConfig : m_portForwardConfigs) {
			if (portForwardConfig.getInboundInterface() != null) {
				sb.append(portForwardConfig.getInboundInterface());
			}
			sb.append(',');
			if (portForwardConfig.getOutboundInterface() != null) {
				sb.append(portForwardConfig.getOutboundInterface());
			}
			sb.append(',');
			if (portForwardConfig.getAddress() != null) {
				sb.append(portForwardConfig.getAddress());
			}
			sb.append(',');
			if (portForwardConfig.getProtocol() != null) {
				sb.append(portForwardConfig.getProtocol());
			}
			sb.append(',');
			sb.append(portForwardConfig.getInPort()).append(',');
			sb.append(portForwardConfig.getOutPort()).append(',');
			sb.append(portForwardConfig.isMasquerade()).append(',');
			if (portForwardConfig.getPermittedNetwork() != null) {
				sb.append(portForwardConfig.getPermittedNetwork());
			}
			sb.append(',');
			if (portForwardConfig.getPermittedMac() != null) {
				sb.append(portForwardConfig.getPermittedMac());
			}
			sb.append(',');
			if (portForwardConfig.getSourcePortRange() != null) { 
				sb.append(portForwardConfig.getSourcePortRange());
			}
			sb.append(",#;");
		}
		int ind = sb.lastIndexOf(";");
		if (ind > 0) {
			sb.deleteCharAt(ind);
		}
		return sb.toString();
	}
	
	private String formNatConfigPropValue() {
		StringBuilder sb = new StringBuilder();
		for (FirewallNatConfig natConfig : m_natConfigs) {
			if (natConfig.getSourceInterface() != null) {
				sb.append(natConfig.getSourceInterface());
			}
			sb.append(',');
			if (natConfig.getDestinationInterface() != null) {
				sb.append(natConfig.getDestinationInterface());
			}
			sb.append(',');
			if (natConfig.getProtocol() != null) {
				sb.append(natConfig.getProtocol());
			}
			sb.append(',');
			if (natConfig.getSource() != null) {
				sb.append(natConfig.getSource());
			}
			sb.append(',');
			if (natConfig.getDestination() != null) {
				sb.append(natConfig.getDestination());
			}
			sb.append(',');
			sb.append(natConfig.isMasquerade()).append(",#;");
		}
		int ind = sb.lastIndexOf(";");
		if (ind > 0) {
			sb.deleteCharAt(ind);
		}
		return sb.toString();
	}
}
