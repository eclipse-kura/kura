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

package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iwlistScanTool implements IScanTool {
	
	private static final Logger s_logger = LoggerFactory.getLogger(iwlistScanTool.class);
	
	private static final String SCAN_THREAD_NAME = "iwlistScanThread";
	
	private static final Object s_lock = new Object();
	private String m_ifaceName;
	private ExecutorService m_executor;
	private static Future<?>  s_task;

	private int m_timeout;
	
	// FIXME:MC Is this process always closed?
	private SafeProcess m_process;
	private boolean m_status;
	private String m_errmsg;
	
	protected iwlistScanTool() {
		m_timeout = 20;
	}
	
	protected iwlistScanTool(String ifaceName) {
		this();
		m_ifaceName = ifaceName;
		m_errmsg = "";
		m_status = false;
	}
	
	protected iwlistScanTool(String ifaceName, int tout) {
		this(ifaceName);
		m_timeout = tout;
	}

	@Override
	public List<WifiAccessPoint> scan() throws KuraException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("ifconfig ").append(m_ifaceName).append(" up");
		try {
			SafeProcess process = ProcessUtil.exec(sb.toString());
			process.waitFor();
		} catch (Exception e) {
			s_logger.error("failed to execute the {} command - {}", sb.toString(), e);
		}
		
		List<WifiAccessPoint> wifiAccessPoints = new ArrayList<WifiAccessPoint>();
		synchronized (s_lock) {
			long timerStart = System.currentTimeMillis();
			
			m_executor = Executors.newSingleThreadExecutor();
			s_task = m_executor.submit(new Runnable() {
				public void run() {
					Thread.currentThread().setName(SCAN_THREAD_NAME);
					int stat = -1;
					m_process = null;
					StringBuilder sb = new StringBuilder();
					sb.append("iwlist ").append(m_ifaceName).append(" scanning");
					s_logger.info("scan() :: executing: " + sb.toString());
					m_status = false;
					try {
						m_process = ProcessUtil.exec(sb.toString());
						stat = m_process.waitFor();
						s_logger.info("scan() :: " + sb.toString() + " command returns status=" + stat + " - process=" + m_process);
						if (stat == 0) {
							m_status = true;
						} else {
							s_logger.error("scan() :: failed to execute " + sb.toString() + " error code is " + stat);
						}	
					} catch (Exception e) {
						m_errmsg = "exception executing scan command";
						s_logger.error("failed to execute the {} command - {}", sb.toString(), e);
					}
				}
			});
			
			while (!s_task.isDone()) {
				if (System.currentTimeMillis() > timerStart+m_timeout*1000) {
					s_logger.warn("scan() :: scan timeout");
					sb = new StringBuilder();
					sb.append("iwlist ").append(m_ifaceName).append(" scanning");
					try {
						int pid = LinuxProcessUtil.getPid(sb.toString());
						if (pid >= 0) {
							s_logger.warn("scan() :: scan timeout :: killing pid {}", pid);
							LinuxProcessUtil.kill(pid);
						}
					} catch (Exception e) {
						s_logger.error("failed to get pid of the {} process - {}", sb.toString(), e);
					}	
					s_task.cancel(true);
					s_task = null;
					m_errmsg = "timeout executing scan command";
					break;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			
			if ((m_status == false) || (m_process == null)) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, m_errmsg);
			}
			
			s_logger.info("scan() :: the 'iw scan' command executed successfully, parsing output ...");
			try {
				wifiAccessPoints = parse();
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "error parsing scan results");
			} finally {
				s_logger.info("scan() :: destroing scan proccess ...");
				if (m_process != null) ProcessUtil.destroy(m_process);
				m_process = null;
				
				s_logger.info("scan() :: Terminating {} ...", SCAN_THREAD_NAME);
				m_executor.shutdownNow();
				try {
					m_executor.awaitTermination(2, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					s_logger.warn("Interrupted " + e);
				}
				s_logger.info("scan() :: 'iw scan' thread terminated? - {}", m_executor.isTerminated());
				m_executor = null;
			}
		}
		return wifiAccessPoints;
	}
	
	private List<WifiAccessPoint> parse() throws Exception {
		
		List<WifiAccessPoint> wifiAccessPoints = new ArrayList<WifiAccessPoint>();
		
		//get the output
		BufferedReader br = new BufferedReader(new InputStreamReader(m_process.getInputStream()));
		String line = null;
		
		String ssid = null;
		List<Long> bitrate = null;
		long frequency = -1;
		byte[] hardwareAddress = null;
		WifiMode mode = null;
		EnumSet<WifiSecurity> rsnSecurity = null;
		int strength = -1;
		EnumSet<WifiSecurity> wpaSecurity = null;
		
		while((line = br.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("Cell")) {
				//new AP
				if(ssid != null) {
					WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(ssid);
					wifiAccessPoint.setBitrate(bitrate);
					wifiAccessPoint.setFrequency(frequency);
					wifiAccessPoint.setHardwareAddress(hardwareAddress);
					wifiAccessPoint.setMode(mode);
					wifiAccessPoint.setRsnSecurity(rsnSecurity);
					wifiAccessPoint.setStrength(strength);
					wifiAccessPoint.setWpaSecurity(wpaSecurity);
					wifiAccessPoints.add(wifiAccessPoint);
				}
				
				//reset
				ssid = null;
				bitrate = null;
				frequency = -1;
				hardwareAddress = null;
				mode = null;
				rsnSecurity = null;
				strength = -1;
				wpaSecurity = null;
				
				//parse out the MAC
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); //eat Cell
				st.nextToken(); //eat Cell #
				st.nextToken(); // eat '-'
				st.nextToken(); // eat 'Address:'
				String macAddressString = st.nextToken();
				if(macAddressString != null) {
					hardwareAddress = NetworkUtil.macToBytes(macAddressString);				
				}
			} else if (line.startsWith("ESSID:")) {
				ssid = line.substring("ESSID:".length()+1, line.length()-1);
			} else if (line.startsWith("Quality=")) {
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); // eat 'Quality='
				st.nextToken(); // eat 'Signal'
				String signalLevel = st.nextToken();
				if (signalLevel != null) {
					signalLevel = signalLevel.substring(signalLevel.indexOf('=')+1);
					if (signalLevel.contains("/")) {
						// Could also be of format 39/100
						final String[] parts = signalLevel.split("/");
						strength = (int) Float.parseFloat(parts[0]);
						if(strength <= 0)
							strength = -100;
					    else if(strength >= 100)
					    	strength = -50;
					    else
					    	strength = (strength / 2) - 100;
					} else {
						strength = (int)Float.parseFloat(signalLevel);
					}
					strength = Math.abs(strength);
				}
				
			} else if (line.startsWith("Mode:")) {
				line = line.substring("Mode:".length());
				if (line.equals("Master")) {
					mode = WifiMode.MASTER;
				}
			} else if (line.startsWith("Frequency:")) {
				line = line.substring("Frequency:".length(), line.indexOf(' '));
				frequency = (long) (Float.parseFloat(line) * 1000);
			} else if (line.startsWith("Bit Rates:")) {
				if(bitrate == null) {
					bitrate = new ArrayList<Long>();
				}
				line = line.substring("Bit Rates:".length());
				String [] bitRates = line.split(";");
				for (String rate : bitRates) {
					if (rate != null) {
						rate = rate.trim();
						if (rate.length() > 0) {
							rate = rate.substring(0, rate.indexOf(' '));	
							bitrate.add((long) (Float.parseFloat(rate) * 1000000));
						}
					}
				}
			} else if (line.contains("IE: IEEE 802.11i/WPA2")) {
				rsnSecurity = EnumSet.noneOf(WifiSecurity.class);
				boolean foundGroup = false;
				boolean foundPairwise = false;
				boolean foundAuthSuites = false;
				while((line = br.readLine()) != null) {
					line = line.trim();
					if(line.contains("Group Cipher")) {
						foundGroup = true;
						if(line.contains("CCMP")) {
							rsnSecurity.add(WifiSecurity.GROUP_CCMP);
						}
						if(line.contains("TKIP")) {
							rsnSecurity.add(WifiSecurity.GROUP_TKIP);
						}
						if(line.contains("WEP104")) {
							rsnSecurity.add(WifiSecurity.GROUP_WEP104);
						}
						if(line.contains("WEP40")) {
							rsnSecurity.add(WifiSecurity.GROUP_WEP40);
						}
					} else if(line.contains("Pairwise Ciphers")) {
						foundPairwise = true;
						if(line.contains("CCMP")) {
							rsnSecurity.add(WifiSecurity.PAIR_CCMP);
						}
						if(line.contains("TKIP")) {
							rsnSecurity.add(WifiSecurity.PAIR_TKIP);
						}
						if(line.contains("WEP104")) {
							rsnSecurity.add(WifiSecurity.PAIR_WEP104);
						}
						if(line.contains("WEP40")) {
							rsnSecurity.add(WifiSecurity.PAIR_WEP40);
						}
					} else if(line.contains("Authentication Suites")) {
						foundAuthSuites = true;
						if(line.contains("802_1X")) {
							rsnSecurity.add(WifiSecurity.KEY_MGMT_802_1X);
						}
						if(line.contains("PSK")) {
							rsnSecurity.add(WifiSecurity.KEY_MGMT_PSK);
						}
					} else {
						s_logger.debug("Ignoring line in RSN: " + line);
					}
					
					if(foundGroup && foundPairwise && foundAuthSuites) {
						break;
					}
				}				
			} else if (line.contains("IE: WPA Version")) {
				wpaSecurity = EnumSet.noneOf(WifiSecurity.class);
				boolean foundGroup = false;
				boolean foundPairwise = false;
				boolean foundAuthSuites = false;
				while((line = br.readLine()) != null) {
					line = line.trim();
					if(line.contains("Group Cipher")) {
						foundGroup = true;
						if(line.contains("CCMP")) {
							wpaSecurity.add(WifiSecurity.GROUP_CCMP);
						}
						if(line.contains("TKIP")) {
							wpaSecurity.add(WifiSecurity.GROUP_TKIP);
						}
						if(line.contains("WEP104")) {
							wpaSecurity.add(WifiSecurity.GROUP_WEP104);
						}
						if(line.contains("WEP40")) {
							wpaSecurity.add(WifiSecurity.GROUP_WEP40);
						}
					} else if(line.contains("Pairwise Ciphers")) {
						foundPairwise = true;
						if(line.contains("CCMP")) {
							wpaSecurity.add(WifiSecurity.PAIR_CCMP);
						}
						if(line.contains("TKIP")) {
							wpaSecurity.add(WifiSecurity.PAIR_TKIP);
						}
						if(line.contains("WEP104")) {
							wpaSecurity.add(WifiSecurity.PAIR_WEP104);
						}
						if(line.contains("WEP40")) {
							wpaSecurity.add(WifiSecurity.PAIR_WEP40);
						}
					} else if(line.contains("Authentication Suites")) {
						foundAuthSuites = true;
						if(line.contains("802_1X")) {
							wpaSecurity.add(WifiSecurity.KEY_MGMT_802_1X);
						}
						if(line.contains("PSK")) {
							wpaSecurity.add(WifiSecurity.KEY_MGMT_PSK);
						}
					} else {
						s_logger.debug("Ignoring line in WPA: " + line);
					}
					
					if(foundGroup && foundPairwise && foundAuthSuites) {
						break;
					}
				}
			}
		}	
		
		//store the last one
		if (ssid != null) {
			WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(ssid);
			wifiAccessPoint.setBitrate(bitrate);
			wifiAccessPoint.setFrequency(frequency);
			wifiAccessPoint.setHardwareAddress(hardwareAddress);
			wifiAccessPoint.setMode(mode);
			wifiAccessPoint.setRsnSecurity(rsnSecurity);
			wifiAccessPoint.setStrength(strength);
			wifiAccessPoint.setWpaSecurity(wpaSecurity);
			wifiAccessPoints.add(wifiAccessPoint);
		}
		br.close();
		return wifiAccessPoints;
	}

}
