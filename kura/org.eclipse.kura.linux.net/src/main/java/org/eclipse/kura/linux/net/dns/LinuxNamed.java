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
package org.eclipse.kura.linux.net.dns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxNamed {
	
	private static final Logger s_logger = LoggerFactory.getLogger(LinuxNamed.class);

	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
	
	private static LinuxNamed s_linuxNamed = null;
	private static String s_persistentConfigFileName = null;
	private static String s_logFileName = null;
	private static String s_rfc1912ZonesFilename = null;
	private static String s_procString = null;
	
	private DnsServerConfigIP4 m_dnsServerConfigIP4;
	
	private LinuxNamed() throws KuraException {
		if(OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
				OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())) {
			s_persistentConfigFileName = "/etc/bind/named.conf";
			s_procString = "/usr/sbin/named";
			if (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())) {
				s_logFileName = "/var/named.log";
				s_rfc1912ZonesFilename = "/etc/bind/named.rfc1912.zones";
			} else {
				s_logFileName = "/var/log/named.log";
				s_rfc1912ZonesFilename = "/etc/named.rfc1912.zones";
			}
		} else {
			s_persistentConfigFileName = "/etc/named.conf";
			s_procString = "named -u named -t";
			s_logFileName = "/var/log/named.log";
			s_rfc1912ZonesFilename = "/etc/named.rfc1912.zones";
		}
		
		//initialize the configuration
		init();
		
		if(m_dnsServerConfigIP4 == null) {
			Set<IP4Address> forwarders = new HashSet<IP4Address>();
			HashSet<NetworkPair<IP4Address>> allowedNetworks = new HashSet<NetworkPair<IP4Address>>();
			m_dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
		}
	}
	
	public static synchronized LinuxNamed getInstance() throws KuraException {
		if(s_linuxNamed == null) {
			s_linuxNamed = new LinuxNamed();
		}		
		
		return s_linuxNamed;
	}
	
	private void init() throws KuraException {
		//TODO
		File configFile = new File(s_persistentConfigFileName);
		if(configFile.exists()) {
			
			s_logger.debug("initing DNS Server configuration");
			
			try {
				Set<IP4Address> forwarders = new HashSet<IP4Address>();
				Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<NetworkPair<IP4Address>>();
				
				BufferedReader br = new BufferedReader(new FileReader(configFile));
				boolean forwardingConfig = true;
				
				String line = null;
				while((line = br.readLine()) != null) {
					if(line.trim().equals("forward only;")) {
						forwardingConfig = true;
						break;
					}
				}
				
				br.close();
				br = null;
				
				if(forwardingConfig) {
					br = new BufferedReader(new FileReader(configFile));
					while((line = br.readLine()) != null) {
						//TODO - really simple for now
						StringTokenizer st = new StringTokenizer(line);
						while(st.hasMoreTokens()) {
							String token = st.nextToken();
							if(token.equals("forwarders")) {
								//get the forwarders 'forwarders {192.168.1.1;192.168.2.1;};'
								StringTokenizer st2 = new StringTokenizer(st.nextToken(), "{} ;");
								while(st2.hasMoreTokens()) {
									String forwarder = st2.nextToken();
									if(forwarder != null && !forwarder.trim().equals("")) {
										s_logger.debug("found forwarder: " + forwarder);
										forwarders.add((IP4Address) IPAddress.parseHostAddress(forwarder));
									}
								}
							} else if(token.equals("allow-query")) {
								//get the networks 'allow-query {192.168.2.0/24;192.168.3.0/24};'
								StringTokenizer st2 = new StringTokenizer(st.nextToken(), "{} ;");
								while(st2.hasMoreTokens()) {
									String allowedNetwork = st2.nextToken();
									if(allowedNetwork != null && !allowedNetwork.trim().equals("")) {
										String[] splitNetwork = allowedNetwork.split("/");
										allowedNetworks.add(new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(splitNetwork[0]), Short.parseShort(splitNetwork[1])));
									}
								}
							}
						}
					}
					
					br.close();
					br = null;
		
					//set the configuration and return
					m_dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
					return;
				}
			} catch (FileNotFoundException e) {
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
			}	
		} else {
			s_logger.debug("There is no current DNS server configuration that allows forwarding");
		}
	}
	
	public boolean isEnabled() throws KuraException {
		try {
			// Check if named is running
			int pid = LinuxProcessUtil.getPid(s_procString);
			return (pid > -1);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}	
	
	public boolean enable() throws KuraException {
		//write config happened during 'set config' step
		
		try {
			// Check if named is running
			int pid = LinuxProcessUtil.getPid(s_procString);
			if(pid > -1) {
				// If so, disable it
				s_logger.error("DNS server is already running, bringing it down...");
				disable();
			}
			// Start named
			int result = -1;
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
				result = LinuxProcessUtil.start("/etc/init.d/bind start");
			} 
			else if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())) {
				result = LinuxProcessUtil.start("/etc/init.d/bind9 start");
			}
			else if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
				result = LinuxProcessUtil.start("/etc/init.d/bind start");
			}
			else {
				result = LinuxProcessUtil.start("/etc/init.d/named start");
			}
			if(result == 0) {
				s_logger.debug("DNS server started.");
				s_logger.trace(m_dnsServerConfigIP4.toString());
				return true;
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return false;
	}

	public boolean disable() throws KuraException {
		try {
			int result = -1;
			// If so, stop it.
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
				result = LinuxProcessUtil.start("/etc/init.d/bind stop");
			} 
			else if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())) {
				result = LinuxProcessUtil.start("/etc/init.d/bind9 stop");
			}
			else if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
				result = LinuxProcessUtil.start("/etc/init.d/bind stop");
			}
			else {
				result = LinuxProcessUtil.start("/etc/init.d/named stop");
			}
			
			if(result == 0) {
				s_logger.debug("DNS server stopped.");
				s_logger.trace(m_dnsServerConfigIP4.toString());
				return true;
			} else {
				s_logger.debug("tried to kill DNS server for interface but it is not running");
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return true;
	}
	
	public boolean restart() throws KuraException {
		try {
			if(LinuxProcessUtil.start("/etc/init.d/named restart") == 0) {
		        s_logger.debug("DNS server restarted.");
		    } else {
		    	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "error restarting");
		    }
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return true;
	}

	public boolean isConfigured() {
		if(m_dnsServerConfigIP4 != null && m_dnsServerConfigIP4.getForwarders() != null && m_dnsServerConfigIP4.getForwarders().size() > 0 &&
				m_dnsServerConfigIP4.getAllowedNetworks() != null && m_dnsServerConfigIP4.getAllowedNetworks().size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setConfig(DnsServerConfigIP4 dnsServerConfigIP4) throws KuraException {
		try {
			
			m_dnsServerConfigIP4 = dnsServerConfigIP4;
			if(m_dnsServerConfigIP4 == null) {
				s_logger.warn("Set DNS server configuration to null");
			}
			
			writeConfig();
		} catch (Exception e) {
			s_logger.error("Error setting DNS server config");
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public DnsServerConfigIP4 getDnsServerConfig() {
		return m_dnsServerConfigIP4;
	}

	public String getConfigFilename() {
		return s_persistentConfigFileName;
	}
	
	private void writeConfig() throws KuraException {
		try {
			FileOutputStream fos = new FileOutputStream(s_persistentConfigFileName);
			PrintWriter pw = new PrintWriter(fos);
			
			//build up the file
			if(m_dnsServerConfigIP4 == null || m_dnsServerConfigIP4.getForwarders() == null || m_dnsServerConfigIP4.getAllowedNetworks() == null ||
					m_dnsServerConfigIP4.getForwarders().size() == 0 || m_dnsServerConfigIP4.getAllowedNetworks().size() == 0) {
				s_logger.debug("writing default named.conf to " + s_persistentConfigFileName + " with: " + m_dnsServerConfigIP4.toString());
				pw.print(getDefaultNamedFile());
			} else {
				s_logger.debug("writing custom named.conf to " + s_persistentConfigFileName + " with: " + m_dnsServerConfigIP4.toString());
				pw.print(getForwardingNamedFile());
			}
			
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration files for dns servers: " + e.getMessage());
		}
	}
	
	private String getForwardingNamedFile() {
		StringBuilder sb = new StringBuilder()
		.append("// Forwarding and Caching Name Server Configuration\n")
		.append("options {\n")
		.append("\tdirectory \"/var/named\";\n")
		.append("\tversion \"not currently available\";\n")
		
		.append("\tforwarders {");
		Set<IP4Address> forwarders = m_dnsServerConfigIP4.getForwarders();
		for(IP4Address forwarder : forwarders) {
			sb.append(forwarder.getHostAddress())
			.append(";");
		}
		sb.append("};\n");
		
		sb.append("\tforward only;\n")
		.append("\tallow-transfer{\"none\";};\n")
		
		.append("\tallow-query {");
		Set<NetworkPair<IP4Address>> allowedNetworks = m_dnsServerConfigIP4.getAllowedNetworks();
		for(NetworkPair<IP4Address> pair : allowedNetworks) {
			sb.append(pair.getIpAddress().getHostAddress())
			.append("/")
			.append(pair.getPrefix())
			.append(";");
		}
		sb.append("};\n");
		sb.append("\tmax-cache-ttl 30;\n");
		sb.append("\tmax-ncache-ttl 30;\n");
		sb.append("};\n")
		.append("logging{\n")
		.append("\tchannel named_log {\n")
		.append("\t\tfile \"")
		.append(s_logFileName)
		.append("\" versions 3;\n")
		.append("\t\tseverity info;\n")
		.append("\t\tprint-severity yes;\n")
		.append("\t\tprint-time yes;\n")
		.append("\t\tprint-category yes;\n")
		.append("\t};\n")
		.append("\tcategory default{\n")
		.append("\t\tnamed_log;\n")
		.append("\t};\n")
		.append("};\n")
		.append("zone \".\" IN {\n")
		.append("\ttype hint;\n")
		.append("\tfile \"named.ca\";\n")
		.append("};\n")
		.append("include \"")
		.append(s_rfc1912ZonesFilename)
		.append("\";\n");

		return sb.toString();
	}
	
	private static final String getDefaultNamedFile() {
		StringBuilder sb = new StringBuilder()
		.append("//\n")
		.append("// named.conf\n")
		.append("//\n")
		.append("// Provided by Red Hat bind package to configure the ISC BIND named(8) DNS\n")
		.append("// server as a caching only nameserver (as a localhost DNS resolver only).\n")
		.append("//\n")
		.append("// See /usr/share/doc/bind*/sample/ for example named configuration files.\n")
		.append("//\n")
		.append("\n")
		.append("options {\n")
		.append("\tlisten-on port 53 { 127.0.0.1; };\n")
		.append("\tlisten-on-v6 port 53 { ::1; };\n")
		.append("\tdirectory	\"/var/named\";\n")
		.append("\tdump-file	\"/var/named/data/cache_dump.db\";\n")
		.append("\tstatistics-file \"/var/named/data/named_stats.txt\";\n")
		.append("\tmemstatistics-file \"/var/named/data/named_mem_stats.txt\";\n")
		.append("\tallow-query     { localhost; };\n")
		.append("\trecursion yes;\n")
		.append("\n")
		.append("\tmax-cache-ttl 30;\n")
		.append("\tmax-ncache-ttl 30;\n")
		.append("\tdnssec-enable yes;\n")
		.append("\tdnssec-validation yes;\n")
		.append("\tdnssec-lookaside auto;\n")
		.append("\n")
		.append("\t/* Path to ISC DLV key */\n")
		.append("\nbindkeys-file \"/etc/named.iscdlv.key\";\n")
		.append("};\n")
		.append("\n")
		.append("logging {\n")
		.append("\tchannel default_debug {\n")
		.append("\t\tfile \"data/named.run\";\n")
		.append("\t\tseverity dynamic;\n")
		.append("\t};\n")
		.append("};\n")
		.append("\n")
		.append("zone \".\" IN {\n")
		.append("\ttype hint;\n")
		.append("\tfile \"named.ca\";\n")
		.append("};\n")
		.append("\n")
		.append("include \"")
		.append(s_rfc1912ZonesFilename)
		.append("\";\n");
		//.append("include \"/etc/named.rfc1912.zones\";\n");
	
		return sb.toString();
	}
}
