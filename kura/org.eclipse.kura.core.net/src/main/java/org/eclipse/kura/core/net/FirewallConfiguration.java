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
	}
	
	public  FirewallConfiguration(Map<String, Object> properties) {
		this();
		String str = null;
		String [] astr = null;
		if (properties.containsKey(OPEN_PORTS_PROP_NAME)) {
			str = (String)properties.get(OPEN_PORTS_PROP_NAME);
			astr = str.split(";");
			for (String sop : astr) {
				try {
					String [] sa = sop.split(",");
					NetProtocol protocol = NetProtocol.valueOf(sa[1]);
					String permittedNetwork = sa[2].split("/")[0];
					short permittedNetworkMask = Short.parseShort(sa[2].split("/")[1]);
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
				} catch (Exception e) {
					s_logger.error("Failed to parse Open Port Entry - {}", e);
				}
			}
		}
		if (properties.containsKey(PORT_FORWARDING_PROP_NAME)) {
			str = (String)properties.get(PORT_FORWARDING_PROP_NAME);
			astr = str.split(";");
			for (String sop : astr) {
				try {
					String [] sa = sop.split(",");
					String inboundIface = sa[0];
					String outboundIface = sa[1];
					IP4Address address = (IP4Address)IPAddress.parseHostAddress(sa[2]);
					NetProtocol protocol = NetProtocol.valueOf(sa[3]);
					int inPort = Integer.parseInt(sa[4]);
					int outPort = Integer.parseInt(sa[5]);
					boolean masquerade = Boolean.parseBoolean(sa[6]);
					String permittedNetwork = sa[7].split("/")[0];
					short permittedNetworkMask = Short.parseShort(sa[7].split("/")[1]);
					String permittedMAC = sa[8]; 
					String sourcePortRange = sa[9];
					FirewallPortForwardConfigIP<? extends IPAddress> portForwardEntry = 
							new FirewallPortForwardConfigIP4(inboundIface, outboundIface, address, protocol, inPort, outPort, masquerade, 
									new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(permittedNetwork), permittedNetworkMask),
									permittedMAC, sourcePortRange);
					m_portForwardConfigs.add(portForwardEntry);
				} catch (Exception e) {
					s_logger.error("Failed to parse Port Forward Entry - {}", e);
				}
			}
		}		
		if (properties.containsKey(NAT_PROP_NAME)) {
			str = (String)properties.get(NAT_PROP_NAME);
			astr = str.split(";");
			for (String sop : astr) {
				String [] sa = sop.split(",");
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
		StringBuilder sb = new StringBuilder();
		for (FirewallOpenPortConfigIP<? extends IPAddress> openPortConfig : m_openPortConfigs) {
			String port = openPortConfig.getPortRange();
			if(port == null) {
				port = Integer.toString(openPortConfig.getPort());
			}
			sb.append(port).append(',');
			sb.append(openPortConfig.getProtocol()).append(',');
			sb.append(openPortConfig.getPermittedNetwork()).append(',');
			sb.append(openPortConfig.getPermittedInterfaceName()).append(',');
			sb.append(openPortConfig.getUnpermittedInterfaceName()).append(',');
			sb.append(openPortConfig.getPermittedMac()).append(',');
			sb.append(openPortConfig.getSourcePortRange()).append("#;");
		}
		sb.deleteCharAt(sb.lastIndexOf(";"));
		return sb.toString();
	}
	
	private String formPortForwardConfigPropValue() {
		StringBuilder sb = new StringBuilder();
		for (FirewallPortForwardConfigIP<? extends IPAddress> portForwardConfig : m_portForwardConfigs) {
			sb.append(portForwardConfig.getInboundInterface()).append(',');
			sb.append(portForwardConfig.getOutboundInterface()).append(',');
			sb.append(portForwardConfig.getAddress()).append(',');
			sb.append(portForwardConfig.getProtocol()).append(',');
			sb.append(portForwardConfig.getInPort()).append(',');
			sb.append(portForwardConfig.getOutPort()).append(',');
			sb.append(portForwardConfig.isMasquerade()).append(',');
			sb.append(portForwardConfig.getPermittedNetwork()).append(',');
			sb.append(portForwardConfig.getPermittedMac()).append(',');
			sb.append(portForwardConfig.getSourcePortRange()).append("#;");
			
		}
		sb.deleteCharAt(sb.lastIndexOf(";"));
		return sb.toString();
	}
	
	private String formNatConfigPropValue() {
		StringBuilder sb = new StringBuilder();
		for (FirewallNatConfig natConfig : m_natConfigs) {
			sb.append(natConfig.getSourceInterface()).append(',');
			sb.append(natConfig.getDestinationInterface()).append('.');
			sb.append(natConfig.getProtocol()).append(',');
			sb.append(natConfig.getSource()).append(',');
			sb.append(natConfig.getDestination()).append(',');
			sb.append(natConfig.isMasquerade()).append("#;");
		}
		sb.deleteCharAt(sb.lastIndexOf(";"));
		return sb.toString();
	}
}
