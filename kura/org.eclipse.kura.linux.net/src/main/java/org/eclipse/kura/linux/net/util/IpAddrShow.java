package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.NetInterfaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpAddrShow {
	private static final Logger s_logger = LoggerFactory.getLogger(IpAddrShow.class);

	private String m_ifname;
	private ArrayList<LinuxIfconfig> m_configs = new ArrayList<LinuxIfconfig>();

	//    private enum ParseState {INIT, LINK, INET, INET6};

	public IpAddrShow() {
		super();
	}

	public IpAddrShow(String m_interfaceName) {
		super();
		this.m_ifname = m_interfaceName;
	}

	//	$ ip addr show
	//	
	//	1: lo: <LOOPBACK,UP,LOWER_UP> mtu 16436 qdisc noqueue state UNKNOWN 
	//	    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
	//	    inet 127.0.0.1/8 scope host lo
	//	    inet6 ::1/128 scope host 
	//	       valid_lft forever preferred_lft forever
	//	2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP qlen 1000
	//	    link/ether f0:de:f1:c4:53:db brd ff:ff:ff:ff:ff:ff
	//	    inet 10.42.0.1/24 brd 10.42.0.255 scope global eth0
	//	    inet6 fe80::f2de:f1ff:fec4:53db/64 scope link 
	//	       valid_lft forever preferred_lft forever
	//	3: wlan0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP qlen 1000
	//	    link/ether 24:77:03:52:63:9c brd ff:ff:ff:ff:ff:ff
	//	    inet 10.200.12.37/16 brd 10.200.255.255 scope global wlan0
	//	    inet6 fe80::2677:3ff:fe52:639c/64 scope link 
	//	       valid_lft forever preferred_lft forever
	//	4: vmnet1: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UNKNOWN qlen 1000
	//	    link/ether 00:50:56:c0:00:01 brd ff:ff:ff:ff:ff:ff
	//	    inet 172.16.35.1/24 brd 172.16.35.255 scope global vmnet1
	//	    inet6 fe80::250:56ff:fec0:1/64 scope link 
	//	       valid_lft forever preferred_lft forever
	//	5: vmnet8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UNKNOWN qlen 1000
	//	    link/ether 00:50:56:c0:00:08 brd ff:ff:ff:ff:ff:ff
	//	    inet 172.16.203.1/24 brd 172.16.203.255 scope global vmnet8
	//	    inet6 fe80::250:56ff:fec0:8/64 scope link 
	//	       valid_lft forever preferred_lft forever
	//	6: ppp0: <POINTOPOINT,MULTICAST,NOARP,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UNKNOWN qlen 3
	//	    link/ppp 
	//	    inet 37.180.63.28 peer 10.64.64.64/32 scope global ppp0	


	//	Alternatively one could use /sys/class/net and /proc/net.


	// ifconfig sucks. ip sucks too but at least the output is consistent.
	// Unfortunately 'ip' does not have the equivalent of 'ifconfig -a':
	// 'ip addr show' does not show interfaces that are DOWN (meaning 'ifdown')
	// 'ip link show' or 'ip -0 addr show' does not show inet addresses
	// this means that we need to call 'ip' twice to get all the information:
	// 'ip link show' followed by 'ip -4 addr show' and merge the two outputs.

	//	public LinuxIfconfig[] exec() throws KuraException {
	//		ArrayList<LinuxIfconfig> configs = new ArrayList<LinuxIfconfig>();
	//		
	//		StringBuilder sb = new StringBuilder("ip addr show");
	//		if (m_ifname != null) {
	//			sb.append(" ").append(m_ifname);
	//		}
	//        SafeProcess proc = null;
	//        BufferedReader br = null;
	//        String cmd = sb.toString();
	//        
	//        try {
	//        	proc = ProcessUtil.exec(cmd);
	//        	if (proc.waitFor() != 0) {
	//        		throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
	//        	}
	//
	//        	ParseState state = ParseState.INIT;
	//        	LinuxIfconfig config = new LinuxIfconfig("dummy"); // prevent compiler warning
	//        	br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	//        	String line;
	//
	//        	while((line = br.readLine()) != null) {
	//        		line = line.trim();
	//        		if (line.isEmpty()) {
	//        			continue;
	//        		}
	//        		
	//        		s_logger.debug(line);
	//
	//        		// DO NOT CHANGE THE ORDER OF STATES
	//        		if (state == ParseState.INET) {
	//        			// There might be 0 or more inet lines, one for every IP address assigned to the interface.
	//        			// We only support a single IP address so we always change the parsing state
	//        			// after this line.
	//        			state = ParseState.INET6;
	//
	//        			String inet = findValue(line, "inet", " ");
	//        			s_logger.debug("inet: '{}'", inet);
	//
	//        			if (inet != null) {
	//        				String[] parts = inet.split("/");
	//        				if (parts.length > 0) {
	//        					String inetaddr = parts[0];
	//        					config.setInetAddress(inetaddr);
	//        					String sprefix = null;
	//        					if (parts.length > 1) {
	//        						sprefix = parts[1];
	//        					}
	//        					if (sprefix != null) {
	//        						int prefix = Integer.valueOf(sprefix);
	//        						String inetmask = prefix2inetmask(prefix);
	//        						config.setInetMask(inetmask);
	//        					}
	//        				}
	//
	//        				String bcast = findValue(line, "brd", " ");
	//        				s_logger.debug("bcast: '{}'", bcast);
	//        				config.setInetBcast(bcast);
	//        				
	//        				String peer = findValue(line, "peer", " ");
	//        				s_logger.debug("peer: '{}'", peer);
	//        				config.setPeerInetAddr(peer);
	//
	//        				continue; // line processed
	//        			}
	//        		}
	//
	//        		if (state == ParseState.INET6) {
	//        			if (findIfindex(line) != -1) {
	//        				// this must be the link line of the following interface
	//        				configs.add(config);
	//        				state = ParseState.INIT;
	//        			} else {
	//        				// Eat other inet lines and inet6 altogether
	//        				continue;
	//        			}
	//        		}
	//
	//        		if (state == ParseState.INIT) {
	//        			String ifname = findIfname(line);
	//        			s_logger.debug("ifname: '{}'", ifname);
	//        			if (ifname == null) {
	//        				s_logger.warn("Interface name not found");
	//        				continue;
	//        			}
	//        			config = new LinuxIfconfig(ifname);
	//
	//        			String[] flags = findFlags(line);
	//        			boolean multicast = isMulticast(flags);
	//        			s_logger.debug("multicast: '{}'", multicast);        			
	//        			config.setMulticast(multicast);
	//        			
	//        			boolean up = isUp(flags);
	//        			s_logger.debug("up: '{}'", up);
	//        			config.setUp(up);
	//
	//        			String smtu = findValue(line, "mtu", " ");
	//        			s_logger.debug("mtu: '{}'", smtu);
	//        			if (smtu != null) {
	//        				int mtu = Integer.valueOf(smtu);
	//        				config.setMtu(mtu);
	//        			}
	//        			
	//        			String linkState = findValue(line, "state", " ");
	//        			s_logger.debug("link state: '{}'", linkState);
	//
	//        			// Some interfaces, like ppp0 report the link state as UNKNOWN.
	//        			// In this case we consider the link up.
	//        			if ("DOWN".equals(linkState)) {
	//        				config.setLinkUp(false);
	//        			} else {
	//        				config.setLinkUp(true);
	//        			}
	//
	//        			state = ParseState.LINK;
	//        			continue;
	//        		}
	//        		
	//        		if (state == ParseState.LINK) {
	//        			String link = findValue(line, "link", "/| ");
	//        			s_logger.debug("link: '{}'", link);
	//        			if ("loopback".equals(link)) {
	//        				config.setType(NetInterfaceType.LOOPBACK);
	//        			} else if ("ether".equals(link)) {
	//        				config.setType(NetInterfaceType.ETHERNET);
	//        			} else if ("ppp".equals(link)) {
	//        				config.setType(NetInterfaceType.MODEM);
	//        			} else {
	//        				config.setType(NetInterfaceType.UNKNOWN);
	//        			}
	//        			
	//        			String hwaddr = findValue(line, "link/"+link, " ");
	//        			s_logger.debug("hwaddr: '{}'", hwaddr);
	//        			config.setMacAddress(hwaddr); // can be null
	//
	//        			state = ParseState.INET;
	//        		}
	//        	}
	//        	if (!"dummy".equals(config.getName())) {
	//        		configs.add(config);
	//        	}
	//        } catch (KuraException e) {
	//        	throw e;
	//        } catch (Exception e) {
	//        	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
	//        } finally {
	//        	if (br != null) {
	//        		try {
	//					br.close();
	//				} catch (IOException e) {
	//					s_logger.warn("Cannot close reader", e);
	//				}
	//        	}
	//			if (proc != null) {
	//				ProcessUtil.destroy(proc);
	//			}
	//        }
	//        return configs.toArray(new LinuxIfconfig[0]);
	//	}

	public LinuxIfconfig[] exec() throws KuraException {
		execLink();
		execInet();
		return m_configs.toArray(new LinuxIfconfig[0]);
	}

	private void execLink() throws KuraException {		
		StringBuilder sb = new StringBuilder("ip -o link show");
		if (m_ifname != null) {
			sb.append(" dev ").append(m_ifname);
		}
		SafeProcess proc = null;
		BufferedReader br = null;
		String cmd = sb.toString();

		try {
			proc = ProcessUtil.exec(cmd);
			if (proc.waitFor() != 0) {
				throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
			}

			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;

			while((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				s_logger.debug(line);

				String ifname = findIfname(line);
				s_logger.debug("ifname: '{}'", ifname);
				if (ifname == null) {
					continue;
				}
				LinuxIfconfig config = new LinuxIfconfig(ifname);

				String[] flags = findFlags(line);
				boolean multicast = isMulticast(flags);
				s_logger.debug("multicast: '{}'", multicast);        			
				config.setMulticast(multicast);

//				boolean up = isUp(flags);
//				s_logger.debug("up: '{}'", up);
//				config.setUp(up);

				String smtu = findValue(line, "mtu", " ");
				s_logger.debug("mtu: '{}'", smtu);
				if (smtu != null) {
					int mtu = Integer.valueOf(smtu);
					config.setMtu(mtu);
				}

				String linkState = findValue(line, "state", " ");
				s_logger.debug("link state: '{}'", linkState);

				// Some interfaces, like ppp0 report the link state as UNKNOWN.
				// In this case we consider the link up.
				if ("DOWN".equals(linkState)) {
					config.setLinkUp(false);
				} else {
					config.setLinkUp(true);
				}

				String link = findValue(line, "link", "/| ");
				s_logger.debug("link: '{}'", link);
				if ("loopback".equals(link)) {
					config.setType(NetInterfaceType.LOOPBACK);
				} else if ("ether".equals(link)) {
					config.setType(NetInterfaceType.ETHERNET);
				} else if ("ppp".equals(link)) {
					config.setType(NetInterfaceType.MODEM);
				} else {
					config.setType(NetInterfaceType.UNKNOWN);
				}

				String hwaddr = findValue(line, "link/"+link, " ");
				s_logger.debug("hwaddr: '{}'", hwaddr);
				config.setMacAddress(hwaddr); // can be null

				m_configs.add(config);
			}
		} catch (KuraException e) {
			throw e;
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close reader", e);
				}
			}
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	private void execInet() throws KuraException {		
		StringBuilder sb = new StringBuilder("ip -o -4 addr show");
		if (m_ifname != null) {
			sb.append(" dev ").append(m_ifname);
		}
		SafeProcess proc = null;
		BufferedReader br = null;
		String cmd = sb.toString();

		try {
			proc = ProcessUtil.exec(cmd);
			if (proc.waitFor() != 0) {
				throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
			}

			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;

			// FIXME: Note that one interface might have multiple inet addresses
			// while this implementation assumes a single inet address.
			while((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				s_logger.debug(line);

				String ifname = findIfname(line);
				s_logger.debug("ifname: '{}'", ifname);
				if (ifname == null) {
					s_logger.warn("Interface name not found");
					continue;
				}

				LinuxIfconfig config = null;
				for (LinuxIfconfig conf : m_configs) {
					if (conf.getName().equals(ifname)) {
						config = conf;
						break;
					}
				}

				if (config == null) {
					s_logger.warn("Config for interface {} not found", ifname);
					continue;
				}

				String inet = findValue(line, "inet", " ");
				s_logger.debug("inet: '{}'", inet);

				if (inet != null) {
					String[] parts = inet.split("/");
					if (parts.length > 0) {
						String inetaddr = parts[0];
						config.setInetAddress(inetaddr);
						String sprefix = null;
						if (parts.length > 1) {
							sprefix = parts[1];
						}
						if (sprefix != null) {
							int prefix = Integer.valueOf(sprefix);
							String inetmask = prefix2inetmask(prefix);
							config.setInetMask(inetmask);
						}
					}
				}

				String bcast = findValue(line, "brd", " ");
				s_logger.debug("bcast: '{}'", bcast);
				config.setInetBcast(bcast);

				String peer = findValue(line, "peer", " ");
				s_logger.debug("peer: '{}'", peer);
				config.setPeerInetAddr(peer);
			}
		} catch (KuraException e) {
			throw e;
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close reader", e);
				}
			}
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	//	private int findIfindex(String input) {
	//		if (Character.isDigit(input.charAt(0))) {
	//			int end = input.indexOf(':');
	//			if (end < 0) {
	//				return -1; // not found
	//			}
	//			String sindex = input.substring(0, end);
	//			return Integer.valueOf(sindex);
	//		}
	//		return -1;
	//	}

	private String findIfname(String input) {
		int start = input.indexOf(':');
		if (start < 0) {
			return null;
		}
		int end = input.indexOf(' ', start + 2);
		if (end < 0) {
			return null;
		}
		if (input.charAt(end - 1) == ':') {
			end--;
		}		
		return input.substring(start + 2, end);
	}

	private String findValue(String input, String name, String regex) {
		int start = input.indexOf(name);
		if (start < 0) {
			return null;
		}
		String[] parts = input.substring(start).split(regex);
		if (parts.length > 1) {
			return parts[1];
		}
		return null;
	}

	private String[] findFlags(String input) {
		int start = input.indexOf('<');
		if (start < 0) {
			return null;
		}
		int end = input.indexOf('>', start + 1);
		if (end < 0) {
			return null;
		}
		String flags = input.substring(start + 1, end).trim();
		return flags.split(",");
	}

	private boolean isMulticast(String[] flags) {
		boolean multicast = false;
		for (String flag : flags) {
			if (flag.equals("MULTICAST")) {
				multicast = true;
				break;
			}
		}
		return multicast;
	}

//	private boolean isUp(String[] flags) {
//		boolean up = false;
//		for (String flag : flags) {
//			if (flag.equals("UP")) {
//				up = true;
//				break;
//			}
//		}
//		return up;
//	}

	private static String prefix2inetmask(int prefix) throws UnknownHostException {
		int mask = 0xffffffff << (32 - prefix);

		int value = mask;
		byte[] bytes = new byte[]{ 
				(byte)(value >>> 24), (byte)(value >> 16 & 0xff), (byte)(value >> 8 & 0xff), (byte)(value & 0xff) };

		InetAddress netAddr = InetAddress.getByAddress(bytes);
		return netAddr.toString().substring(1); // strip the leading '/'
	}

	public static void main(String args[]) {
		IpAddrShow ipAddrShow = new IpAddrShow();
		try {
			LinuxIfconfig[] ifconfigs = ipAddrShow.exec();
			System.out.println(Arrays.toString(ifconfigs));
		} catch (KuraException e) {
			s_logger.warn("Failed", e);
			System.out.println("Failed: " + e.getMessage());
		}
	}
}
