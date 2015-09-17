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
package org.eclipse.kura.windows.position;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.net.modem.ModemGpsDisabledEvent;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionLostEvent;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionServiceImpl implements PositionService, ConfigurableComponent, EventHandler {

	private static final Logger s_logger = LoggerFactory.getLogger(PositionServiceImpl.class);

	private final static long THREAD_TERMINATION_TOUT = 1; // in seconds

	private static Future<?>				monitorTask;
	private static boolean 					stopThread;

	private Map<String,Object>				m_properties;
	private Map<String,Object>				m_positionServiceProperties;
	private ConnectionFactory 	            m_connectionFactory;
//	private GpsDevice					 	m_gpsDevice;
	private ExecutorService                 m_executor;
	private EventAdmin            			m_eventAdmin;
	private UsbService						m_usbService;

	private int pollInterval = 500;	//milliseconds
	private boolean m_configured;
	private boolean m_useGpsd = false;
	private boolean m_configEnabled;
	private boolean m_isRunning;
	private boolean m_hasLock;

	// to avoid NPE don't return a null pointer
	private Position m_defaultPosition=null;
	private NmeaPosition m_defaultNmeaPosition=null;

	// add gpsd variables
	private Position m_GpsdPosition=null;
	private NmeaPosition m_GpsdNmeaPosition=null;
	private String m_GpsdTimeNmea="";
	private String m_GpsdDateNmea="";
	private String m_GpsdLastSentence="";
	private boolean m_GpsdIsValidPosition=false;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = connectionFactory;
	}

	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = null;
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = eventAdmin;
	}

	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = null;
	}

	public void setUsbService(UsbService usbService) {
		this.m_usbService = usbService;
	}

	public void unsetUsbService(UsbService usbService) {
		this.m_usbService = null;
	}


	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		Measurement l_latitude = new Measurement(0.0, Unit.rad);
		Measurement l_longitude = new Measurement(0.0, Unit.rad);
		Measurement l_altitude = new Measurement(0.0, Unit.m);
		Measurement l_speed = new Measurement(0.0, Unit.m_s);
		Measurement l_track = new Measurement(0.0, Unit.rad);
	
		m_defaultPosition = new Position(l_latitude, l_longitude, l_altitude, l_speed, l_track);
		s_logger.debug("WINPOS: Activating...");
	}

	protected void deactivate(ComponentContext componentContext)
	{
		s_logger.info("WINPOS: Deactivating...");
	}

	public void updated(Map<String,Object> properties) 
	{
		s_logger.debug("WINPOS: Updating...");
	}


	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------

	@Override
	public Position getPosition() {
		s_logger.info("WINPOS: getPosition");
		return m_defaultPosition;
	}

	@Override
	public NmeaPosition getNmeaPosition() {
		s_logger.info("WINPOS: getNmeaPosition");
		return null;
	}

	public boolean isLocked() {
		s_logger.info("WINPOS: isLocked");
		return true;
	}

	@Override
	public String getNmeaTime() {
		s_logger.info("WINPOS: getNmeaTime");
		return null;
	}

	@Override
	public String getNmeaDate() {
		s_logger.info("WINPOS: getNmeaDate");
		return null;
	}

	public String getLastSentence() {
		s_logger.info("WINPOS: getLastSentence");
		return null;
	}

	public void handleEvent(Event event) {
	}


	private void initializeDefaultPosition(double lat, double lon, double alt){
	}

	private void performPoll() {		
	}

	private void configureGpsDevice() throws Exception {
	}

	private boolean serialPortExists()
	{
		return false;
	}

	private Properties getSerialConnectionProperties(Map<String, Object> props) {
		return null;
	}

	private Properties getProtocolProperties() {
		return null;
	}
}