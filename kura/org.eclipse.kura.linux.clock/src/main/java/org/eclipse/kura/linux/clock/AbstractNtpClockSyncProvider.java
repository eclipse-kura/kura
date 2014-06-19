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
package org.eclipse.kura.linux.clock;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNtpClockSyncProvider implements ClockSyncProvider
{
	private static final Logger s_logger = LoggerFactory.getLogger(AbstractNtpClockSyncProvider.class);
	
	protected Map<String, Object> m_properties;
	protected ClockSyncListener   m_listener;

	protected String                      m_ntpHost;
	protected int                         m_ntpPort;
	protected int                         m_ntpTimeout;
	protected int                         m_refreshInterval;
	protected Date                        m_lastSync;
	protected ScheduledThreadPoolExecutor m_scheduler;
	protected int                         m_maxRetry;
	protected int                         m_numRetry;
	protected boolean					  m_isSynced;
	protected int						  m_syncCount;	

	
	@Override
	public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException
	{
		m_properties = properties;
		m_listener   = listener;
		
		readProperties();
	}

	@Override
	public void start() throws KuraException 
	{		
		if (m_refreshInterval < 0) {			
			// Never do any update. So Nothing to do.
			s_logger.info("No clock update required");
			if (m_scheduler != null) {
				m_scheduler.shutdown();
				m_scheduler = null;
			}
		}
		else if (m_refreshInterval == 0) {
			// Perform one clock update - but in a thread.
			s_logger.info("Perform clock update just once");
			if (m_scheduler != null) {
				m_scheduler.shutdown();
				m_scheduler = null;
			}
			m_scheduler = new ScheduledThreadPoolExecutor(1);
			
			//call recursive retry method for setting the clock
			scheduleOnce();	
		}
		else {
			// Perform periodic clock updates.
			s_logger.info("Perform periodic clock updates every {} sec", m_refreshInterval);
			if (m_scheduler != null) {
				m_scheduler.shutdown();
				m_scheduler = null;
			}
			m_scheduler = new ScheduledThreadPoolExecutor(1);
			m_scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					Thread.currentThread().setName("AbstractNtpClockSyncProvider:schedule");
					if(!m_isSynced){
						m_syncCount=0;
						try { 
							s_logger.info("Try to sync clock ("+m_numRetry+")");
							syncClock(); 
							s_logger.info("Clock synced");
							m_isSynced=true;
							m_numRetry=0;
						}
						catch(KuraException e) {
							m_numRetry++;
							if(m_numRetry>=m_maxRetry) m_isSynced=true; // give up retry
							s_logger.error("Error Synchronizing Clock", e);
						}
					}
					else{
						m_syncCount++;
						if((m_syncCount*60)>=m_refreshInterval-1){
							m_isSynced=false;
							m_numRetry=0;
						}
					}
				}
			}, 0, 60, TimeUnit.SECONDS);
		}		
	}
	
	private void scheduleOnce() {
		if(m_scheduler != null) {
			m_scheduler.schedule(new Runnable() {
				public void run() {
					Thread.currentThread().setName("AbstractNtpClockSyncProvider:scheduleOnce");
					try {
						syncClock();
					} catch(KuraException e) {
						s_logger.error("Error Synchronizing Clock - retrying", e);
						scheduleOnce();
					}
				}
			}, 1, TimeUnit.SECONDS);
		}
	}


	@Override
	public void stop() throws KuraException {
		if (m_scheduler != null) {
			m_scheduler.shutdown();
			m_scheduler = null;
		}
	}

	@Override
	public Date getLastSync() {
		return m_lastSync;
	}

	
	// ----------------------------------------------------------------
	//
	//   Private/Protected Methods
	//
	// ----------------------------------------------------------------

	private void readProperties() throws KuraException
	{
		m_ntpHost = (String) m_properties.get("clock.ntp.host");
		if (m_ntpHost == null) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "clock.ntp.host");
		}
		
		m_ntpPort = 123;
		if (m_properties.containsKey("clock.ntp.port")) {
			m_ntpPort = (Integer) m_properties.get("clock.ntp.port");
		}

		m_ntpTimeout = 10000;
		if (m_properties.containsKey("clock.ntp.timeout")) {
			m_ntpTimeout = (Integer) m_properties.get("clock.ntp.timeout");
		}

		m_refreshInterval = 0;
		if (m_properties.containsKey("clock.ntp.refresh-interval")) {
			m_refreshInterval = (Integer) m_properties.get("clock.ntp.refresh-interval");
		}		
		
		m_maxRetry = 0;
		if (m_properties.containsKey("clock.ntp.max-retry")) {
			m_maxRetry = (Integer) m_properties.get("clock.ntp.max-retry");
		}
	}
	
	
	protected abstract void syncClock() throws KuraException;
}
