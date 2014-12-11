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
package org.eclipse.kura.emulator.position;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionServiceImpl implements PositionService {
	
	private static final Logger s_logger = LoggerFactory.getLogger(PositionServiceImpl.class);

	private static final String LOCATION = "boston";

	private ComponentContext m_ctx;
	private EventAdmin m_eventAdmin;
	
    private ScheduledExecutorService m_worker;
    private ScheduledFuture<?> m_handle;
    
	private GpsPoint[] gpsPoints;
	private Position currentPosition;
	private NmeaPosition currentNmeaPosition;
	private Date currentTime;
	private int index = 0;
	private boolean m_useGpsd;
	private boolean isEnabled=false;

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
		//
		// save the bundle context
		m_ctx = componentContext;
		m_useGpsd = false;
		if(properties!=null){
			if(properties.get("enabled")!=null)
				isEnabled = (Boolean)properties.get("enabled");
				
			if(properties.get("useGpsd")!=null)
				m_useGpsd = (Boolean)properties.get("useGpsd");
			if(m_useGpsd)
				s_logger.info("USE GPSD");
		}

		start();
	}
	
	public void updated(Map<String,Object> properties) 
	{
		if(properties!=null){
			if(properties.get("useGpsd")!=null)
				m_useGpsd = (Boolean)properties.get("useGpsd");
			if(m_useGpsd)
				s_logger.info("USE GPSD");
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		System.out.println("stopping");
		stop();
	}

	@Override
	public Position getPosition() {
		return currentPosition;
	}

	@Override
	public NmeaPosition getNmeaPosition() {
		return currentNmeaPosition;
	}

	@Override
	public String getNmeaTime() {
		return currentTime.toString();
	}

	@Override
	public String getNmeaDate() {
		return currentTime.toString();
	}

	@Override
	public boolean isLocked() {
		// Always return true
		return true;
	}

	@Override
	public String getLastSentence() {
		// Not supported in emulator mode since this is not NMEA
		return null;
	}

	public void start() {
		
		index = 0;
		
		String fileName = null;
		if (LOCATION.equals("boston")) {
			fileName = "boston.gpx";
		} else if (LOCATION.equals("denver")) {
			fileName = "denver.gpx";
		} else if (LOCATION.equals("paris")) {
			fileName = "paris.gpx";
		} else if (LOCATION.equals("test")) {
			fileName = "test.gpx";
		}

		GpsXmlHandler handler = new GpsXmlHandler();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		try {
			// Create the builder and parse the file
			SAXParser parser = factory.newSAXParser();
			s_logger.debug("Parsing: " + fileName);

			BundleContext bundleContext = m_ctx.getBundleContext();
			URL url = bundleContext.getBundle().getResource(fileName);
			InputStream is = url.openStream();
			
			/*
			BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/src/main/resources/" + fileName)));
			StringBuffer buffer = new StringBuffer();
			String temp = null;

			while((temp = br.readLine()) != null) {
				buffer.append(temp);
			}
			br.close();
			br = null;

			String string = new String(buffer);
			byte[] data = string.getBytes();
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
*/
			parser.parse(is, handler);
			gpsPoints = handler.getGpsPoints();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		// schedule a new worker based on the properties of the service
		m_worker = Executors.newSingleThreadScheduledExecutor();
        m_handle = m_worker.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                	if(isEnabled)
                        updateGps();
                }
        }, 0, 5, TimeUnit.SECONDS);
        
        s_logger.debug("posting event");
        m_eventAdmin.postEvent( new PositionLockedEvent( new HashMap<String,Object>()));
	}

	public void stop() {
		if (m_handle != null) {
			m_handle.cancel(true);
			m_handle = null;
		}
		
		m_worker = null;
	}
	
	private void updateGps() {
		s_logger.debug("GPS Emulator index: " + index);
		if ((index + 1) == gpsPoints.length) {
			s_logger.debug("GPS Emulator - wrapping index");
			index = 0;
		}
		
		Measurement latitude = new Measurement(java.lang.Math.toRadians(gpsPoints[index].getLatitude()),Unit.rad);
		Measurement longitude = new Measurement(java.lang.Math.toRadians(gpsPoints[index].getLongitude()),Unit.rad);
		Measurement altitude = new Measurement(gpsPoints[index].getAltitude(),Unit.m);
		
		s_logger.debug("Updating lat/long/altitude: " + latitude + "/" + longitude + "/" + altitude);
		
		//Measurement lat, Measurement lon, Measurement alt, Measurement speed, Measurement track
		currentTime = new Date();
		currentPosition = new Position(latitude, longitude, altitude, null, null);
		currentNmeaPosition = new NmeaPosition(gpsPoints[index].getLatitude(),gpsPoints[index].getLongitude(),gpsPoints[index].getAltitude(),0,0);
		
		index++;
		
		return;
	}
}
