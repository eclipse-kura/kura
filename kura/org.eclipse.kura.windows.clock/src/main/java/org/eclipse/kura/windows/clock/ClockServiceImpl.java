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
package org.eclipse.kura.windows.clock;

import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockEvent;
import org.eclipse.kura.clock.ClockService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.util.ProcessUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClockServiceImpl implements ConfigurableComponent, ClockService, ClockSyncListener
{
	private static final Logger s_logger = LoggerFactory.getLogger(ClockServiceImpl.class);

	@SuppressWarnings("unused")
	private ComponentContext      m_ctx;
	private EventAdmin            m_eventAdmin;
	private Map<String,Object>    m_properties;
	private ClockSyncProvider     m_provider;
	private boolean 			  m_configEnabled;
		
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = eventAdmin;
	}

	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = null;
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		// save the properties
		m_properties = properties;
		
		s_logger.info("Activate. Current Time: {}", new Date());

		// save the bundle context
		m_ctx = componentContext;		
		
		try {		
			if(m_properties.get("enabled") != null) {
				m_configEnabled = (Boolean) m_properties.get("enabled");
			} else {
				m_configEnabled = false;
			}
			
			if(m_configEnabled) {
				// start the provider
				startClockSyncProvider();
			}
		}
		catch (Throwable t) {
			s_logger.error("Error updating ClockService Configuration", t);
		}
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("Deactivate...");
		try {
			stopClockSyncProvider();
		}
		catch (Throwable t) {
			s_logger.error("Error deactivate ClockService", t);
		}
	}
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated...");		
		try {

			// save the properties
			m_properties = properties;
			
			if(m_properties.get("enabled") != null) {
				m_configEnabled = (Boolean) m_properties.get("enabled");
			} else {
				m_configEnabled = false;
				return;
			}
			
			if(m_configEnabled) {
				// start the provider
				startClockSyncProvider();
			}
		}
		catch (Throwable t) {
			s_logger.error("Error updating ClockService Configuration", t);
		}
	}

		
	// ----------------------------------------------------------------
	//
	//   Master Client Management APIs
	//
	// ----------------------------------------------------------------
	
	@Override
	public Date getLastSync() throws KuraException {
		if (m_provider != null) {
			return m_provider.getLastSync();
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Clock service not configured yet");
		}
	}

	
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	private void startClockSyncProvider() throws KuraException
	{
		stopClockSyncProvider();
		String provider = (String) m_properties.get("clock.provider");
		if ("java-ntp".equals(provider)) {
			m_provider = new JavaNtpClockSyncProvider();
		}
		else if ("ntpd".equals(provider)) {
			m_provider = new NtpdClockSyncProvider();			
		}
		else if ("gps".equals(provider)) {
			m_provider = new GpsClockSyncProvider();			
		}
		if (m_provider != null) {
			m_provider.init(m_properties, this);
			m_provider.start();
		}
	}

	private void stopClockSyncProvider() throws KuraException 
	{
		if (m_provider != null) {
			m_provider.stop();
			m_provider = null;
		}
	}

	/**
	 * Called by the current ClockSyncProvider after each Clock synchronization
	 */
	public void onClockUpdate(long offset) {

		s_logger.info("Clock update. Offset: {}", offset);

		// set system clock if necessary
		boolean bClockUpToDate = false;
		if (offset != 0) {
			long time = System.currentTimeMillis() + offset;
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis( time );
			try {
				WindowsSetSystemTime winTime = new WindowsSetSystemTime();
				winTime.SetLocalTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
				bClockUpToDate = true;
				s_logger.info("System Clock Updated to {}", cal.getTime());
			}
			catch (Exception e) {
				s_logger.error("Error updating System Clock", e);
			}
		}
		else {
			bClockUpToDate = true;
		}

		// set hardware clock - this should be done automatically above
/*
		boolean updateHwClock = false;
		if (m_properties.containsKey("clock.set.hwclock")) {
			updateHwClock = (Boolean) m_properties.get("clock.set.hwclock");
		}
		if (updateHwClock) {
		}
*/

		// Raise the event
		if (bClockUpToDate) {
			m_eventAdmin.postEvent( new ClockEvent( new HashMap<String,Object>()));
		}
	}
}

