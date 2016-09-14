/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
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
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iwScanTool extends ScanTool implements IScanTool {
	
	private static final Logger s_logger = LoggerFactory.getLogger(iwScanTool.class);
	private static final String SCAN_THREAD_NAME = "iwScanThread";
	private static final Object s_lock = new Object();
	private String m_ifaceName;
	private ExecutorService m_executor;
	private static Future<?>  m_task;

	private int m_timeout;
	
	// FIXME:MC Is this process always closed?
	private SafeProcess m_process;
	private boolean m_status;
	private String m_errmsg;
	
	protected iwScanTool() {
		m_timeout = 20;
	}
	
	protected iwScanTool(String ifaceName) {
		this();
		m_ifaceName = ifaceName;
		m_errmsg = "";
		m_status = false;
	}
	
	protected iwScanTool(String ifaceName, int tout) {
		this(ifaceName);
		m_timeout = tout;
	}
	
	public List<WifiAccessPoint> scan() throws KuraException {
		
		List<WifiAccessPoint> wifiAccessPoints = new ArrayList<WifiAccessPoint>();
		synchronized (s_lock) {
			StringBuilder sb = new StringBuilder();
		    
			SafeProcess prIpLink = null;
			SafeProcess prIpAddr = null;
			try {
				if(!LinuxNetworkUtil.hasAddress(m_ifaceName)) {
				    // activate the interface
					sb.append("ip link set ").append(m_ifaceName).append(" up");
				    prIpLink = ProcessUtil.exec(sb.toString());
				    prIpLink.waitFor();
				 
				    // remove the previous ip address (needed on mgw)
				    sb = new StringBuilder();
					sb.append("ip addr flush dev ").append(m_ifaceName);
				    prIpAddr = ProcessUtil.exec(sb.toString());
				    prIpAddr.waitFor();
				}
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				if (prIpLink != null) ProcessUtil.destroy(prIpLink);
				if (prIpAddr != null) ProcessUtil.destroy(prIpAddr);
			}
	
			long timerStart = System.currentTimeMillis();
			
			m_executor = Executors.newSingleThreadExecutor();
			m_task = m_executor.submit(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName(SCAN_THREAD_NAME);
					int stat = -1;
					m_process = null;
					StringBuilder sb = new StringBuilder();
					sb.append("iw dev ").append(m_ifaceName).append(" scan");
					s_logger.info("scan() :: executing: {}", sb.toString());
					m_status = false;
					try {
						m_process = ProcessUtil.exec(sb.toString());
						stat = m_process.waitFor();
						s_logger.info("scan() :: {} command returns status={}", sb.toString(), stat);
						if (stat == 0) {
							m_status = true;
						} else {
							s_logger.error("scan() :: failed to execute {} error code is {}", sb.toString(), stat);
							s_logger.error("scan() :: STDERR: " + LinuxProcessUtil.getInputStreamAsString(m_process.getErrorStream()));
						}	
					} catch (Exception e) {
						m_errmsg = "exception executing scan command";
						e.printStackTrace();
					}
				}
			});
			
			while (!m_task.isDone()) {
				if (System.currentTimeMillis() > timerStart+m_timeout*1000) {
					s_logger.warn("scan() :: scan timeout");
					sb = new StringBuilder();
					sb.append("iw dev ").append(m_ifaceName).append(" scan");
					try {
						int pid = LinuxProcessUtil.getPid(sb.toString());
						if (pid >= 0) {
							s_logger.warn("scan() :: scan timeout :: killing pid {}", pid);
							LinuxProcessUtil.kill(pid);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					m_task.cancel(true);
					m_task = null;
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
					s_logger.warn("Interrupted", e);
				}
				s_logger.info("scan() :: 'iw scan' thread terminated? - {}", m_executor.isTerminated());
				m_executor = null;
			}
		}
	
		return wifiAccessPoints;
	}
	
	private List<WifiAccessPoint> parse() throws Exception {
		
		List<IWAPParser> apInfos = new ArrayList<IWAPParser>();
		IWAPParser currentAP = null;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(m_process.getInputStream()));
		try {
			String line = null;
			while((line = br.readLine()) != null) {
				
				if (line.startsWith("scan aborted!")) {
					br.close();
					s_logger.warn("parse() :: scan operation was aborted");
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "iw scan operation was aborted");
				}
				
				if (line.startsWith("BSS")) {
					//new AP - parse out the MAC
					StringTokenizer st = new StringTokenizer(line, " ");
					st.nextToken(); //eat BSS
					String macAddressString = st.nextToken().substring(0, 16);
					
					if(macAddressString != null) {
						// Set this AP parser as the current one
						currentAP = new IWAPParser(macAddressString);		
					}
					
					// Add it to the list
					apInfos.add(currentAP);
					
				} else {
					// Must be an AP property line
					String propLine = line.trim();
					
					if(currentAP != null) {
						// We're currently parsing an AP
						try {
							// Give this line to the AP parser
							currentAP.parsePropLine(propLine);
						} catch(Exception e) {
							currentAP = null;
							s_logger.error("Failed to parse line: {}; giving up on the current AP", propLine, e);
						}
					}
				}
			}
		} finally {
			br.close();
		}

		// Generate list of WifiAccessPoint objects
		List<WifiAccessPoint> wifiAccessPoints = new ArrayList<WifiAccessPoint>();
		for(IWAPParser info : apInfos) {
			wifiAccessPoints.add(info.toWifiAccessPoint());
		}
		return wifiAccessPoints;
	}
}

