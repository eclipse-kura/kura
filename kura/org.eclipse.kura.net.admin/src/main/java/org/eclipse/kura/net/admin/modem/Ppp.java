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
package org.eclipse.kura.net.admin.modem;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.ppp.PppLinux;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ppp implements IModemLinkService {

	private static final Logger s_logger = LoggerFactory.getLogger(Ppp.class);
	
	private final static long THREAD_TERMINATION_TOUT = 3000;
	
	private static Object s_lock = new Object();
	private String m_iface;
	private String m_port;
	private ScheduledFuture<?> m_pppScheduledFuture = null;
	private ScheduledThreadPoolExecutor m_executor;
	ConnectionInfo m_coninfo = null;
	
	private long m_tout = 0;
	private long m_startTime = 0;
		
	public Ppp(String iface, String port) {
		m_iface = iface;
		m_port = port;
		m_executor = new ScheduledThreadPoolExecutor(1);
		m_executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		m_executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
	}

	@Override
	public void connect() throws KuraException {
		connect(60);
	}
	
	@Override
	public void connect(int tout) throws KuraException {

		s_logger.info("connecting ...");
		m_tout = tout * 1000; // convert to milliseconds
		try {
			if (m_pppScheduledFuture != null) {
				m_pppScheduledFuture.cancel(true);
			}
			m_pppScheduledFuture = m_executor.scheduleAtFixedRate(
					new Runnable() {
						@Override
						public void run() {
							Thread.currentThread().setName("Ppp:Connect");
							if (!monitor()) {
								m_pppScheduledFuture.cancel(true);
							}
						}
					}, 0, 5, TimeUnit.SECONDS);
		} catch (Exception e) {
			m_startTime = 0;
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public void disconnect() throws KuraException {
		
		s_logger.info("disconnecting :: stopping PPP monitor ...");
		PppLinux.disconnect(m_iface, m_port);
		
		try {
			release();
			LinuxDns linuxDns = LinuxDns.getInstance();
			if (linuxDns.isPppDnsSet()) {
				linuxDns.unsetPppDns();
			}
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		m_coninfo = null;
	}
	
	@Override
	public void release() {
		if(m_pppScheduledFuture != null) {
			long timer = System.currentTimeMillis();
			while(!m_pppScheduledFuture.isDone()) {
				m_pppScheduledFuture.cancel(true);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
        		s_logger.debug("Ppp:Connect task done?- {}", m_pppScheduledFuture.isDone());
        		if ((System.currentTimeMillis()-timer) > THREAD_TERMINATION_TOUT) {
					s_logger.error("Failed to cancel Ppp:Connect task");
					break;
				}
			}
			m_pppScheduledFuture = null;
		}
		
		if (m_executor != null) {
			long timer = System.currentTimeMillis();
			while (!m_executor.isTerminated()) {
				s_logger.debug("Shutting down Ppp:Connect Thread ...");
				m_executor.shutdownNow();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
				s_logger.debug("Ppp:Connect Thread terminating? - {}", m_executor.isTerminating());
				s_logger.debug("Ppp:Connect Thread terminated? - {}", m_executor.isTerminated());
				if ((System.currentTimeMillis()-timer) > THREAD_TERMINATION_TOUT) {
					s_logger.error("Failed to terminate Ppp:Connect Thread");
					break;
				}
			}
			m_executor = null;
		}
	}
	
	@Override
	public String getIPaddress() throws KuraException {
		String ipAddress = null;
		if (LinuxNetworkUtil.isUp(m_iface)) {
			ipAddress = LinuxNetworkUtil.getCurrentIpAddress(m_iface);
		}
		return ipAddress;
	}

	@Override
	public String getGatewayIpAddress() {
		String ipAddress = null;
		
		if (m_coninfo != null) {
			ipAddress = m_coninfo.getGateway().getHostAddress();
		}
		
		return ipAddress;
	}

	@Override
	public String getIfaceName() {
		return m_iface;
	}
	
	@Override
	public PppState getPppState () throws KuraException {
		
		PppState pppState = PppState.NOT_CONNECTED;
		synchronized (s_lock) {
			boolean pppdRunning = PppLinux.isPppProcessRunning(m_iface, m_port);
			String ip = this.getIPaddress();
			
			//PppState pppState = PppState.NOT_CONNECTED;
			if (pppdRunning && (ip != null)) {
				pppState = PppState.CONNECTED;
			} else if (pppdRunning && (ip == null)) {
				pppState = PppState.IN_PROGRESS;
			} else {
				pppState = PppState.NOT_CONNECTED;
			}
		}
		return pppState;
	}
	
	@Override
	public PppState getPppState (int tout) throws KuraException {
		
		PppState pppState = PppState.NOT_CONNECTED;
		synchronized (s_lock) {
			boolean pppdRunning = PppLinux.isPppProcessRunning(m_iface, m_port, tout);
			String gatewayIP = this.getGatewayIpAddress();
			
			//PppState pppState = PppState.NOT_CONNECTED;
			if (pppdRunning && (gatewayIP != null)) {
				pppState = PppState.CONNECTED;
			} else if (pppdRunning && (gatewayIP == null)) {
				pppState = PppState.IN_PROGRESS;
			} else {
				pppState = PppState.NOT_CONNECTED;
			}
		}
		return pppState;
	}
	
	private boolean monitor() {
		try {
			PppState pppState = getPppState();
			
		    if(pppState == PppState.NOT_CONNECTED) {
		    	
		    	PppLinux.connect(m_iface, m_port);
		    	m_startTime = System.currentTimeMillis();
		    	
		    } else if (pppState == PppState.CONNECTED) {

		    	LinuxDns linuxDns = LinuxDns.getInstance();
		    	List<IPAddress> pppDnsServers = linuxDns.getPppDnServers();
		    	if (pppDnsServers != null) {
		    		s_logger.info("!!! PPP connection established -- " + m_iface + " inteface is up !!!");
					s_logger.info("IP ADDRESS: " + LinuxNetworkUtil.getCurrentIpAddress(m_iface));
					for (IPAddress dnsServer : pppDnsServers) {
						s_logger.info("DNS Server: " + dnsServer.getHostAddress());
					}
					m_startTime = 0;
					return false;
		    	}
		    	/*
				StringBuffer sbConInfoFilename = new StringBuffer();
				sbConInfoFilename.append("/tmp/.kura/coninfo-");
				sbConInfoFilename.append(m_iface);

				File fConInfoFile = new File(sbConInfoFilename.toString());
				if (fConInfoFile.exists()) {
					ConnectionInfo m_coninfo = new ConnectionInfoImpl(m_iface);
					if (m_coninfo != null) {	
						LinuxDns linuxDns = LinuxDns.getInstance();
						if (!linuxDns.isPppDnsSet()) {
							s_logger.info("Setting PPP DNS ...");
							linuxDns.setPppDns();
						}
						s_logger.info("!!! PPP connection established -- " + m_iface + " inteface is up !!!");
						s_logger.info("IFACE: " + m_coninfo.getIfaceName());
						s_logger.info("IP ADDRESS: " + m_coninfo.getIpAddress().getHostAddress());
						s_logger.info("GATEWAY: " + m_coninfo.getGateway().getHostAddress());
					}
			
					List<IP4Address> dnsServers = m_coninfo.getDnsServers();
					if ((dnsServers != null) && (dnsServers.size() > 0)) {
						for (IP4Address dnsServer : dnsServers) {
							s_logger.info("DNS Server: " + dnsServer.getHostAddress());
						}
						m_startTime = 0;
						return false;
					}
				}
				sbConInfoFilename = null;
				*/
			} else if (pppState == PppState.IN_PROGRESS) {
				if ((System.currentTimeMillis() - m_startTime) > m_tout) {
					s_logger.warn("ppp connection timed out on the " + m_iface + " will retry ...");
					PppLinux.disconnect(m_iface, m_port);
					m_startTime = 0;
					try {Thread.sleep(5000);} catch (InterruptedException e) {}
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}
}
