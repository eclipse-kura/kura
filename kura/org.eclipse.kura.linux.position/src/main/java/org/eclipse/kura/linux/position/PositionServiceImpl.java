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
package org.eclipse.kura.linux.position;

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
	private GpsDevice					 	m_gpsDevice;
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
		s_logger.debug("Activating...");

		m_configured = false;		
		m_configEnabled = false;
		m_isRunning = false;
		m_hasLock = false;
		m_useGpsd = false;
		initializeDefaultPosition(0, 0, 0);

		m_executor = Executors.newSingleThreadExecutor();
		m_properties = new HashMap<String, Object>();
		m_positionServiceProperties = new HashMap<String, Object>();

		// install event listener for serial ports
		Dictionary<String, String[]> props = new Hashtable<String, String[]>();
		String[] topic = { UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC,
				UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC,
				ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC,
				ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC};
		
		props.put(EventConstants.EVENT_TOPIC, topic);
		componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, props);

		updated(properties);
		s_logger.info("Activating... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		stop();
		if (m_executor != null) {
			s_logger.debug("Terminating PositionServiceImpl Thread ...");
			m_executor.shutdownNow();
			try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
			s_logger.info("PositionServiceImpl Thread terminated? - {}", m_executor.isTerminated());
			m_executor = null;
		}

		m_properties = null;
		m_positionServiceProperties = null;
		s_logger.info("Deactivating... Done.");
	}

	public void updated(Map<String,Object> properties) {
		
		s_logger.debug("Updating...");
		if (m_gpsDevice != null) {
			Properties currentConfigProps = m_gpsDevice.getConnectConfig();
			Properties serialProperties = getSerialConnectionProperties(properties);
			if ((currentConfigProps != null) && (serialProperties != null)) {
				if (currentConfigProps.getProperty("port").equals(serialProperties.getProperty("port"))
					&& currentConfigProps.getProperty("baudRate").equals(serialProperties.getProperty("baudRate"))	
					&& currentConfigProps.getProperty("stopBits").equals(serialProperties.getProperty("stopBits"))
					&& currentConfigProps.getProperty("bitsPerWord").equals(serialProperties.getProperty("bitsPerWord"))
					&& currentConfigProps.getProperty("parity").equals(serialProperties.getProperty("parity"))) {
					
					s_logger.debug("configureGpsDevice() :: same configuration, no need ot reconfigure GPS device");
					return;
				}
			}
		}
		
		if(m_isRunning) {
			stop();
		}

		if (!properties.containsKey("modem")) {
			m_positionServiceProperties.putAll(properties);
		}	
		m_properties.putAll(properties);
		
		m_configured = false;
		m_configEnabled = false;
		m_isRunning = false;
		m_hasLock = false;
		//m_useGpsd = (Boolean)m_properties.get("useGpsd");

		try {
			if ((Boolean)m_properties.get("enabled") && (Boolean) m_properties.get("static")) {
				initializeDefaultPosition((Double) m_properties.get("latitude"), (Double) m_properties.get("longitude"), (Double) m_properties.get("altitude"));
				m_eventAdmin.postEvent( new PositionLockedEvent( new HashMap<String,Object>()));
			}
			else {
				configureGpsDevice();
				start();
			}
		} catch (Exception e) {
			s_logger.error("Error starting PositionService background operations.", e);
		}
		s_logger.info("Updating... Done.");
	}


	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------

	@Override
	public Position getPosition() {
		if(m_useGpsd)
			return m_GpsdPosition;
		else if(m_gpsDevice!=null)
			return m_gpsDevice.getPosition();
		else return m_defaultPosition;
	}

	@Override
	public NmeaPosition getNmeaPosition() {
		if(m_useGpsd)
			return m_GpsdNmeaPosition;
		else if(m_gpsDevice!=null)
			return m_gpsDevice.getNmeaPosition();
		else return m_defaultNmeaPosition;
	}

	public boolean isLocked() {
		return m_hasLock;
	}

	@Override
	public String getNmeaTime() {
		if(m_useGpsd)
			return m_GpsdTimeNmea;
		else if(m_gpsDevice!=null)
			return m_gpsDevice.getTimeNmea();
		else return null;
	}

	@Override
	public String getNmeaDate() {
		if(m_useGpsd)
			return m_GpsdDateNmea;
		else if(m_gpsDevice!=null)
			return m_gpsDevice.getDateNmea();
		else return null;
	}

	public String getLastSentence() {
		if(m_useGpsd)
			return m_GpsdLastSentence;
		else if(m_gpsDevice!=null)
			return m_gpsDevice.getLastSentence();
		else return null;
	}

	public void handleEvent(Event event) {
		if(!m_useGpsd){
			if(UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC.contains(event.getTopic())){
				if(serialPortExists()){
					s_logger.debug("GPS connected");

					//we already have properties - just do it
					try {
						if(!m_isRunning) {
							configureGpsDevice();
							start();
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			else if(UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC.contains(event.getTopic())){
				if(!serialPortExists()) {
					s_logger.debug("GPS disconnected");
					stop();
				}
			} else if(ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC.contains(event.getTopic())) {
				
				s_logger.debug("ModemGpsEnabledEvent");
				
				m_properties.put("port", event.getProperty(ModemGpsEnabledEvent.Port));
				m_properties.put("baudRate", event.getProperty(ModemGpsEnabledEvent.BaudRate));
				m_properties.put("bitsPerWord", event.getProperty(ModemGpsEnabledEvent.DataBits));
				m_properties.put("stopBits", event.getProperty(ModemGpsEnabledEvent.StopBits));
				m_properties.put("parity", event.getProperty(ModemGpsEnabledEvent.Parity));
				m_properties.put("modem", "true");
				updated(m_properties);
			} else if (ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC.contains(event.getTopic())) {
				s_logger.debug("ModemGpsDisabledEvent");
				updated(m_positionServiceProperties);
			}
		}
	}

	private void start() {
		s_logger.debug("PositionService configured and starting");
		stopThread = false;
		if (monitorTask == null) {
			monitorTask = m_executor.submit(new Runnable() {
				public void run() {
					Thread.currentThread().setName("PositionServiceImpl");
					while(!stopThread) {
						performPoll();
						try {
							Thread.sleep(pollInterval);
						} catch (InterruptedException e) {
							//e.printStackTrace();
							//exit silently ...
						};
					}
				}
			});
		}

		m_isRunning = true;
	}

	private void stop() {
		s_logger.debug("PositionService stopping");
		if ((monitorTask != null) && (!monitorTask.isDone())) {
			stopThread = true;
			monitorTask.cancel(true);
			monitorTask = null;
		}		
		if(m_gpsDevice!=null) {
			m_gpsDevice.disconnect();
		}

		m_configured = false;		
		m_configEnabled = false;
		m_isRunning = false;
		m_hasLock = false;
	}

	private void initializeDefaultPosition(double lat, double lon, double alt){
		Measurement l_latitude = new Measurement(java.lang.Math.toRadians(lat),Unit.rad);
		Measurement l_longitude = new Measurement(java.lang.Math.toRadians(lon),Unit.rad);					
		Measurement l_altitude = new Measurement(alt,Unit.m); 
		Measurement l_speed = new Measurement(0,Unit.m_s); // conversion speed in knots to m/s : 1 m/s = 1.94384449 knots
		Measurement l_track = new Measurement(java.lang.Math.toRadians(0),Unit.rad); 
		double l_latitudeNmea = lat;
		double l_longitudeNmea = lon;					
		double l_altitudeNmea = alt; 
		double l_speedNmea = 0;
		double l_trackNmea = 0; 
		int l_fixQuality = 0;
		int l_nrSatellites = 0;
		double l_DOP = 0;
		double l_PDOP = 0;
		double l_HDOP = 0;
		double l_VDOP = 0;
		int l_3Dfix = 0;

		m_defaultPosition = new Position(l_latitude, l_longitude, l_altitude, l_speed, l_track);
		m_defaultNmeaPosition = new NmeaPosition(l_latitudeNmea, l_longitudeNmea, l_altitudeNmea, l_speedNmea, l_trackNmea, 
				l_fixQuality, l_nrSatellites, l_DOP, l_PDOP, l_HDOP, l_VDOP, l_3Dfix);

		m_GpsdPosition = new Position(l_latitude, l_longitude, l_altitude, l_speed, l_track);
		m_GpsdNmeaPosition = new NmeaPosition(l_latitudeNmea, l_longitudeNmea, l_altitudeNmea, l_speedNmea, l_trackNmea, 
				l_fixQuality, l_nrSatellites, l_DOP, l_PDOP, l_HDOP, l_VDOP, l_3Dfix);
	}

	private void performPoll() {		
		if(m_configEnabled && m_configured){
			boolean isValidPosition;
			if(m_useGpsd)isValidPosition=m_GpsdIsValidPosition;
			else isValidPosition=m_gpsDevice.isValidPosition();
			if(isValidPosition){
				if(!m_hasLock){
					m_hasLock=true;
					m_eventAdmin.postEvent( new PositionLockedEvent( new HashMap<String,Object>()));
					s_logger.info("The Position is valid");
					if(!m_useGpsd){
						s_logger.info(m_gpsDevice.getLastSentence());
						s_logger.info(m_gpsDevice.toString());
					}
				}
			}
			else{
				if(m_hasLock){
					m_hasLock=false;
					m_eventAdmin.postEvent( new PositionLostEvent( new HashMap<String,Object>()));
					s_logger.info("The Position is not valid");
					if(!m_useGpsd){
						s_logger.info(m_gpsDevice.getLastSentence());
					}
				}
			}
		}
	}

	private void configureGpsDevice() throws Exception {

		Properties serialProperties = getSerialConnectionProperties(m_properties);			
		if(serialProperties == null) 
			return;
		
		if (m_gpsDevice != null) {
			s_logger.info("configureGpsDevice() :: disconnecting GPS device ...");
			m_gpsDevice.disconnect();
			m_gpsDevice = null;
		}

		if(!serialPortExists()) {
			s_logger.warn("GPS device is not present - waiting for it to be ready");
			return;
		}
	
		try {			
			if(serialProperties != null) {
				s_logger.debug("Connecting to serial port: {}", serialProperties.getProperty("port"));
	
				// configure connection & protocol
				GpsDevice gpsDevice = new GpsDevice();
				gpsDevice.configureConnection(m_connectionFactory, serialProperties);
				gpsDevice.configureProtocol(getProtocolProperties());	
				m_gpsDevice = gpsDevice;
				m_configured = true;
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private boolean serialPortExists()
	{
		String portName;
		if(m_properties != null){				
			if(m_properties.get("port") != null){
				portName = (String) m_properties.get("port");

				if(portName != null) {
					if(portName.contains("/dev/")) {
						File f = new File(portName);
						if(f.exists()) {
							return true;
						}
					} else {
						List<UsbTtyDevice> utd = m_usbService.getUsbTtyDevices();	
						if(utd!=null){
							for (UsbTtyDevice u : utd) {
								if(portName.equals(u.getUsbPort())) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private Properties getSerialConnectionProperties(Map<String, Object> props) {
		Properties prop = new Properties();
		
		if(props != null){
			String portName = null;
			int baudRate = -1;
			int bitsPerWord = -1;
			int stopBits = -1;
			int parity = -1;

			if(props.get("enabled") != null) {
				m_configEnabled = (Boolean)props.get("enabled");
				if(!m_configEnabled) {
					return null;
				}
			} else {
				m_configEnabled = false;
				return null;
			}
			
			portName = (String) props.get("port");
			if(portName != null && !portName.contains("/dev/")) {
				List<UsbTtyDevice> utds = m_usbService.getUsbTtyDevices();
				for(UsbTtyDevice utd : utds) {
					if(utd.getUsbPort().equals(portName)) {
						portName = utd.getDeviceNode();
						break;
					}
				}
			}
			if(props.get("baudRate") != null) baudRate 		= (Integer) props.get("baudRate");
			if(props.get("bitsPerWord") != null) bitsPerWord = (Integer) props.get("bitsPerWord");
			if(props.get("stopBits") != null) stopBits 		= (Integer) props.get("stopBits");
			if(props.get("parity") != null) parity 			= (Integer) props.get("parity");

			if(portName==null)
				return null;			
			prop.setProperty("port", portName);
			prop.setProperty("baudRate", Integer.toString(baudRate));
			prop.setProperty("stopBits", Integer.toString(stopBits));
			prop.setProperty("parity", Integer.toString(parity));
			prop.setProperty("bitsPerWord", Integer.toString(bitsPerWord));
			
			s_logger.debug("port name: " + portName);
			s_logger.debug("baud rate " + baudRate);
			s_logger.debug("stop bits " + stopBits);
			s_logger.debug("parity " + parity);
			s_logger.debug("bits per word " + bitsPerWord);
			return prop;
		} else {
			return null;
		}
	}

	private Properties getProtocolProperties() {
		Properties prop = new Properties();

		prop.setProperty("unitName", "Gps");

		return prop;
	}
}