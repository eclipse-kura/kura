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

package org.eclipse.kura.linux.net.dhcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServer;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerImpl implements DhcpServer {

	private static final Logger s_logger = LoggerFactory.getLogger(DhcpServerImpl.class);
	
//	public static final String CONFIGURATION_NAME = "org.eclipse.kura.linux.net.dhcp";
//	private static final String TMP_FILE_DIR = "/tmp/.kura/org.eclipse.kura.linux.net.dhcp/";
	private static final String FILE_DIR = "/etc/";
	private static final String PID_FILE_DIR = "/var/run/";
	
	private String m_interfaceName;
	private DhcpServerConfig4 m_dhcpServerConfig4;
	
	private String m_configFileName;
	private String m_pidFileName;
	private String persistentConfigFileName;
	private String persistentPidFilename;

	DhcpServerImpl(String interfaceName, boolean enabled, boolean passDns) throws KuraException {
		m_interfaceName = interfaceName;

		StringBuffer sb = new StringBuffer();
		sb.append("dhcpd-")
		.append(interfaceName)
		.append(".conf");
		m_configFileName = sb.toString();
		
		sb = new StringBuffer();
		sb.append("dhcpd-")
		.append(interfaceName)
		.append(".pid");
		m_pidFileName = sb.toString();
		
		persistentConfigFileName = FILE_DIR + m_configFileName;
		persistentPidFilename = PID_FILE_DIR + m_pidFileName;
		
		readConfig(enabled, passDns);
	}
	
	private void readConfig(boolean enabled, boolean passDns) throws KuraException {
		//TODO
		File configFile = new File(persistentConfigFileName);
		if(configFile.exists()) {
			
			s_logger.debug("initing DHCP Server configuration for {}", m_interfaceName);
			//parse the file
			/*
			# dhcpd.conf - DHCPD configuration file

			subnet 192.168.2.0 netmask 255.255.255.0 {
			    interface eth1;
			    default-lease-time 7200;
			    max-lease-time 7200;
			    option domain-name-servers 192.168.2.1;
			    option routers 192.168.2.1;
			    pool {
			        range 192.168.2.100 192.168.2.110;
			    }
			}*/
			
			try {
				IP4Address subnet = null;
				IP4Address netmask = null;
				IP4Address router = null;
				String interfaceName = null;
				int defaultLeaseTime = -1;
				int maxLeaseTime = -1;
				IP4Address rangeStart = null;
				IP4Address rangeEnd = null;
				ArrayList<IP4Address> dnsList = new ArrayList<IP4Address>();
				
				BufferedReader br = new BufferedReader(new FileReader(configFile));
				
				String line = null;
				while((line = br.readLine()) != null) {
					//TODO - really simple for now
					StringTokenizer st = new StringTokenizer(line);
					while(st.hasMoreTokens()) {
						String token = st.nextToken();
						if(token.equals("#")) {
							break;
						} else if(token.equals("subnet")) {
							subnet = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
							if(!st.nextToken().equals("netmask")) {
								br.close();
								br = null;
								throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "invalid dhcp config file: " + persistentConfigFileName);
							}
							netmask = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
						} else if(token.equals("interface")) {
							interfaceName = st.nextToken();
							interfaceName = interfaceName.substring(0, interfaceName.indexOf(';'));
						} else if(token.equals("default-lease-time")) {
							String leaseTime = st.nextToken();
							defaultLeaseTime = Integer.parseInt(leaseTime.substring(0, leaseTime.indexOf(';')));
						} else if(token.equals("max-lease-time")) {
							String leaseTime = st.nextToken();
							maxLeaseTime = Integer.parseInt(leaseTime.substring(0, leaseTime.indexOf(';')));
						} else if(token.equals("range")) {
							rangeStart = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
							String rangeEndString = st.nextToken();
							rangeEndString = rangeEndString.substring(0, rangeEndString.indexOf(';'));
							rangeEnd = (IP4Address) IPAddress.parseHostAddress(rangeEndString);
						} else if(token.equals("option")) {
							String option = st.nextToken();
							if(option.equals("routers")) {
								String routerString = st.nextToken();
								routerString = routerString.substring(0, routerString.indexOf(';'));
								router = (IP4Address) IPAddress.parseHostAddress(routerString);
							} else if(option.equals("domain-name-servers")) {
								String dnsString = st.nextToken();
								dnsString = dnsString.substring(0, dnsString.indexOf(';'));
								dnsList.add((IP4Address) IPAddress.parseHostAddress(dnsString));
							}
						}
					}
				}
				
                boolean running = this.isRunning();
                
                /*
                LinuxNamed linuxNamed = LinuxNamed.getInstance();
                DnsServerConfigIP4 dnsServerConfig = linuxNamed.getDnsServerConfig();
                List<IP4Address> forwarders = dnsServerConfig.getForwarders();
                List<NetworkPair<IP4Address>> allowedNetworks = dnsServerConfig.getAllowedNetworks();
                boolean passDns = (forwarders != null && forwarders.size() > 0
                        && allowedNetworks != null && allowedNetworks.size() > 0);
				*/
                
				//FIXME - prefix still hardcoded
				s_logger.debug("instantiating DHCP server configuration during init with " + 
						"\n\t\tinterfaceName: " + interfaceName +
						"\n\t\trunning: " + running +
						"\n\t\tsubnet: " + subnet.getHostAddress() +
						"\n\t\trouter: " + router.getHostAddress() +
						"\n\t\tnetmask: " + netmask.getHostAddress() +
						"\n\t\tdefaultLeaseTime: " + defaultLeaseTime +
						"\n\t\tmaxLeaseTime: " + maxLeaseTime +
						"\n\t\trangeStart: " + rangeStart.getHostAddress() +
						"\n\t\trangeEnd: " + rangeEnd.getHostAddress() +
						"\n\t\tpassDns: " + passDns +
						"\n\t\tdnsList: " + dnsList.toString());
				
				m_dhcpServerConfig4 = new DhcpServerConfigIP4(interfaceName, enabled, subnet, router, netmask, defaultLeaseTime, maxLeaseTime,
						(short) 24, rangeStart, rangeEnd, passDns, dnsList);
				
				br.close();
				br = null;
			} catch (FileNotFoundException e) {
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
			}	
		} else {
			s_logger.debug("There is no current DHCP server configuration for {}", m_interfaceName);
		}
	}
	
	public boolean isRunning() throws KuraException {
		try {
			// Check if dhcpd is running
			int pid = LinuxProcessUtil.getPid(formDhcpdCommand());
			return (pid > -1);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}	
	
	public boolean enable() throws KuraException {
	    s_logger.debug("enable()");
		// write to config file;
		try {
			writeConfig();
		} catch (Exception e1) {
			s_logger.error("Error writing configuration to filesystem");
			e1.printStackTrace();
			return false;
		}
		
		try {
			// Check if dhcpd is running
			if(this.isRunning()) {
				// If so, disable it
				s_logger.error("DHCP server is already running, bringing it down...");
				disable();
			}
			// Start dhcpd
            // FIXME:MC This leads to a process leak
			if (LinuxProcessUtil.startBackground(formDhcpdCommand(), false) == 0) {
				s_logger.debug("DHCP server started.");
				s_logger.trace(m_dhcpServerConfig4.toString());
				return true;
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return false;
	}

	public boolean disable() throws KuraException {
        s_logger.debug("disable()");

        // write to config file;
        try {
        	if(m_dhcpServerConfig4 != null) {
        		writeConfig();
        	}
        } catch (Exception e1) {
            s_logger.error("Error writing configuration to filesystem");
            e1.printStackTrace();
            return false;
        }

		try {
			// Check if dhcpd is running
			int pid = LinuxProcessUtil.getPid(formDhcpdCommand());
			if(pid > -1) {
				// If so, kill it.
				if (LinuxProcessUtil.stop(pid)) {
					removePidFile();
				} else {
					s_logger.debug("Failed to stop process...try to kill");
	                if(LinuxProcessUtil.kill(pid)) {
	                	removePidFile();
	                } else {
	                	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "error killing process, pid=" + pid);
	                }	
				}
			} else {
				s_logger.debug("tried to kill DHCP server for interface but it is not running");
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return true;
	}

	public boolean isConfigured() {
		return (m_dhcpServerConfig4 != null);
	}
	
	public void setConfig(DhcpServerConfigIP4 dhcpServerConfig4) throws KuraException {
	    s_logger.debug("setConfig()");

		try {
		    m_dhcpServerConfig4 = dhcpServerConfig4;
			if(m_dhcpServerConfig4 == null) {
				s_logger.warn("Set DHCP configuration to null");
			}
			
			if(dhcpServerConfig4.isEnabled()) {
			    this.enable();
			} else {
                writeConfig();
			    this.disable();
			}
		} catch (Exception e) {
			s_logger.error("Error setting subnet config for " + m_interfaceName);
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public DhcpServerConfig4 getDhcpServerConfig(boolean enabled, boolean passDns) {
        try {
            this.readConfig(enabled, passDns);
        } catch (Exception e) {
            s_logger.error("Error reading config", e);
        }
		return m_dhcpServerConfig4;
	}

	public String getConfigFilename() {
		return persistentConfigFileName;
	}
	
	private void writeConfig() throws KuraException {
		try {
			s_logger.trace("writing to " + FILE_DIR + m_configFileName + " with: " + m_dhcpServerConfig4.toString());
			FileOutputStream fos = new FileOutputStream(FILE_DIR + m_configFileName);
			PrintWriter pw = new PrintWriter(fos);
			pw.write(m_dhcpServerConfig4.toString());
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration files for dhcp servers: " + e.getMessage());
		}		
	}
	
	private void removePidFile() {
		
		File pidFile = new File(persistentPidFilename);
		if (pidFile.exists()) {
			pidFile.delete();
		}
	}
	
	private String formDhcpdCommand() {
		StringBuffer sb = new StringBuffer();
		sb.append("dhcpd -cf ").append(persistentConfigFileName)
				.append(" -pf ").append(persistentPidFilename);
		return sb.toString();
	}
}
