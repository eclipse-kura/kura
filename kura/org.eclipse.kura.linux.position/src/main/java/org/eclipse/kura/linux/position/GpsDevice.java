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
package org.eclipse.kura.linux.position;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionException;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GPS Utility class, not intended to be used by the end user of Kura.<br>
 * Assuming the device talks NMEA over a serial port configured thru PositionService
 * 
 */
public class GpsDevice {
	private static final Logger s_logger = LoggerFactory.getLogger(GpsDevice.class);

	private static Object s_lock = new Object();
	static final String PROTOCOL_NAME = "position";
	
	//private String unitName = PROTOCOL_NAME;
	private SerialCommunicate comm;
	private boolean connConfigd = false;
	private boolean m_validPosition = false;
	private String m_lastSentence;
	
	private Measurement m_latitude = null;
	private Measurement m_longitude = null;
	private Measurement m_altitude = null;
	private Measurement m_speed = null;
	private Measurement m_track = null;
	private double m_latitudeNmea = 0;
	private double m_longitudeNmea = 0;
	private double m_altitudeNmea = 0;
	private double m_speedNmea = 0;
	private double m_trackNmea = 0;
	private int m_fixQuality = 0;
	private int m_nrSatellites = 0;
	private double m_DOP = 0;
	private double m_PDOP = 0;
	private double m_HDOP = 0;
	private double m_VDOP = 0;
	private int m_3Dfix = 0;
	private String m_dateNmea="";
	private String m_timeNmea="";
	
	public GpsDevice() {
		m_latitude = new Measurement(java.lang.Math.toRadians(0),Unit.rad);
		m_longitude = new Measurement(java.lang.Math.toRadians(0),Unit.rad);					
		m_altitude = new Measurement(0,Unit.m); 
		m_speed = new Measurement(0,Unit.m_s); 
		m_track = new Measurement(java.lang.Math.toRadians(0),Unit.rad); 
	}

	public String getProtocolName() {
		return "position";
	}

	public String getUnitAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	public void configureProtocol(Properties protocolConfig)
			throws PositionException {
		// TODO Auto-generated method stub

	}

	public void configureConnection(ConnectionFactory connFactory,
			Properties connectionConfig) throws PositionException {

		if (connConfigd) {
			comm.disconnect();
			comm = null;
			connConfigd = false;
		}

		try {
			comm = new SerialCommunicate(connFactory, connectionConfig);
		} catch (PositionException e) {
			throw(e);
		}
		connConfigd = true;
	}

	public int getConnectStatus() {
		if (!connConfigd)
			return KuraConnectionStatus.NEVERCONNECTED;
		return comm.getConnectStatus();
	}
	
	public Properties getConnectConfig() {
		if (!connConfigd) {
			return null;
		}
		return comm.getConnectConfig();
	}

	public Position getPosition() {
		return new Position(m_latitude, m_longitude, m_altitude, m_speed, m_track);
	}

	public NmeaPosition getNmeaPosition() {
		return new NmeaPosition(m_latitudeNmea, m_longitudeNmea, m_altitudeNmea, m_speedNmea, m_trackNmea, 
								m_fixQuality, 
								m_nrSatellites, 
								m_DOP, 
								m_PDOP, 
								m_HDOP, 
								m_VDOP, 
								m_3Dfix);
	}
	
	public boolean isValidPosition() {
		return m_validPosition;
	}

	public String getDateNmea() {
		return m_dateNmea;
	}

	public String getTimeNmea() {
		return m_timeNmea;
	}

	public void connect() throws PositionException {
		if (!connConfigd)
			throw new PositionException("Invalid serial port configuration");
		comm.connect();
	}

	public void disconnect() {
		if (connConfigd && comm != null)
			comm.disconnect();
	}

	public String getLastSentence() {
		return m_lastSentence;
	}

	/**
	 * Installation of a serial connection to communicate, using javax.comm.SerialPort 
	 * <li>port : the actual device port, such as "/dev/ttyUSB0" in linux</li>
	 * <li>baudRate : baud rate to be configured for the port</li>
	 * <li>stopBits : number of stop bits to be configured for the port</li>
	 * <li>parity : parity mode to be configured for the port</li>
	 * <li>bitsPerWord : only RTU mode supported, bitsPerWord must be 8</li>
	 * see {@link org.eclipse.kura.comm.CommConnection CommConnection} package for more
	 * detail.
	 */
	private final class SerialCommunicate {
		
		private final static long THREAD_TERMINATION_TOUT = 1; // in seconds
		
		private ScheduledExecutorService m_executor;
		private ScheduledFuture<?>  m_task;
		
		InputStream in;
		CommConnection conn=null;
		Properties connConfig = null; 

		public SerialCommunicate(ConnectionFactory connFactory, Properties connectionConfig)
				throws PositionException {
			s_logger.debug("Configure serial connection");
			
			connConfig = connectionConfig;
			
			String sPort;
			String sBaud;
			String sStop;
			String sParity;
			String sBits;				

			if (((sPort = connectionConfig.getProperty("port")) == null)
					|| ((sBaud = connectionConfig.getProperty("baudRate")) == null)
					|| ((sStop = connectionConfig.getProperty("stopBits")) == null)
					|| ((sParity = connectionConfig.getProperty("parity")) == null)
					|| ((sBits = connectionConfig.getProperty("bitsPerWord")) == null))
				throw new PositionException("Invalid serial port configuration");
			
			int baud = Integer.valueOf(sBaud).intValue();
			int stop = Integer.valueOf(sStop).intValue();
			int parity = Integer.valueOf(sParity).intValue();
			int bits = Integer.valueOf(sBits).intValue();

			String uri = new CommURI.Builder(sPort)
									.withBaudRate(baud)
									.withDataBits(bits)
									.withStopBits(stop)
									.withParity(parity)
									.withTimeout(2000)
									.build().toString();

			try {
				conn = (CommConnection) connFactory.createConnection(uri, 1, false);
			} catch (IOException e1) {
				throw new PositionException("Invalid GPS serial Port", e1);
			}

			// get the streams
			try {
				in = conn.openInputStream();
				conn.openOutputStream();
			} catch (Exception e) {
				throw new PositionException("input stream", e);
			}
			
			//clean up if this is not our first run
			if ((m_task != null) && (!m_task.isDone())) {
	    		s_logger.debug("SerialCommunicate() :: Cancelling GpsSerialCommunicate task ...");
	    		m_task.cancel(true);
	    		s_logger.info("SerialCommunicate() :: GpsSerialCommunicate task cancelled? = {}", m_task.isDone());
	    		m_task = null;
	    	}
			
			m_executor = Executors.newSingleThreadScheduledExecutor();
			
			m_task = m_executor.scheduleAtFixedRate(new Runnable() {
	    		@Override
	    		public void run() {
		    		Thread.currentThread().setName("GpsSerialCommunicate");
		    		if (!doPollWork()) {
		    			s_logger.info("The doPollWork() method returned 'false' - disconnecting ...");
		    			disconnect();
		    		}
	    	}}, 0, 20, TimeUnit.MILLISECONDS);			
		}

		public void connect() {
			/*
			 * always connected
			 */
		}

		public void disconnect() {
			synchronized (s_lock) {
				if ((m_task != null) && (!m_task.isDone())) {
		    		s_logger.debug("disconnect() :: Cancelling GpsSerialCommunicate task ...");
		    		m_task.cancel(true);
		    		s_logger.info("disconnect() :: GpsSerialCommunicate task cancelled? = {}", m_task.isDone());
		    		m_task = null;
		    	}
		    	
		    	if (m_executor != null) {
		    		s_logger.debug("disconnect() :: Terminating GpsSerialCommunicate Thread ...");
		    		m_executor.shutdownNow();
		    		try {
						m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						s_logger.warn("Interrupted - {}", e);
					}
		    		s_logger.info("disconnect() :: GpsSerialCommunicate Thread terminated? - {}", m_executor.isTerminated());
					m_executor = null;
		    	}
				
				if (conn!=null) {
					try {
						if(in!=null){
							in.close();
							in=null;
						}
						conn.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					conn = null;
				}
			}
		}

		public int getConnectStatus() {
			return KuraConnectionStatus.CONNECTED;
		}
		
		public Properties getConnectConfig() {
			return connConfig;
		}

		public boolean doPollWork() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				int c=-1;
				if (in != null) {
					while (c != 10) {
						try {
							c = in.read();
						} catch (Exception e) {
							s_logger.error("Exception in gps read - {}", e);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {
								s_logger.warn("Interrupted - {}", e1);
							}
							return false;
						}
						if (c != 13 && c != -1) {
							readBuffer.append((char) c);
						}
					}
					try {
						if (readBuffer.length() > 0) {
							s_logger.debug("GPS RAW: " + readBuffer.toString());
							parseNmeaSentence(readBuffer.toString());
						}
					} catch (Exception e) {
						s_logger.error("Exception in parseNmeaSentence ");
					}
				} else {
					s_logger.debug("GPS InputStream is null");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
			} catch (Exception e) {
				s_logger.error("Exception in Gps doPollWork");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {}
			}
			return true;
		}

		private void parseNmeaSentence(String scannedInput) {

			double lon, lat, speed, alt, track;
			
			// got a message... do a cksum
			if (!NmeaCksum(scannedInput)){
				s_logger.error("NMEA checksum not valid");
				return;
			}
			//s_logger.info(scannedInput);
			m_lastSentence=scannedInput;
			NMEAParser gpsParser = new NMEAParser();

			gpsParser.parseSentence(scannedInput);
			m_validPosition=gpsParser.is_validPosition();
			//s_logger.debug("Parse : "+scannedInput+" position valid = "+m_validPosition);
			
			if(!m_validPosition)
				return;
			
			if(!scannedInput.startsWith("$G")){
				//Invalid NMEA String. Return.
				s_logger.warn("Invalid NMEA sentence: " + scannedInput);
				return;
			}
			//Remove the first 3 characters from the input string in order to normalize the commands
			scannedInput = scannedInput.substring(3);			
			
			if(scannedInput.startsWith("TXT")) {
				s_logger.debug("U-Blox init message: " + scannedInput);
			} else if (scannedInput.startsWith("GGA")) {
				try {
					lon = gpsParser.get_longNmea();
					lat = gpsParser.get_latNmea();
					alt = gpsParser.get_altNmea();
					m_fixQuality = gpsParser.get_fixQuality();
					m_latitude = new Measurement(java.lang.Math.toRadians(lat),Unit.rad);
					m_longitude = new Measurement(java.lang.Math.toRadians(lon),Unit.rad);					
					m_altitude = new Measurement(alt,Unit.m); 
					m_latitudeNmea = lat;
					m_longitudeNmea = lon;					
					m_altitudeNmea = alt; 
					m_DOP = gpsParser.get_DOPNmea();
					m_nrSatellites = gpsParser.get_nrSatellites();
					m_timeNmea = gpsParser.get_timeNmea();
				} catch (Exception e) {
					m_latitude = null;
					m_longitude = null;					
					m_altitude = null;
					m_latitudeNmea = 0;
					m_longitudeNmea = 0;					
					m_altitudeNmea = 0; 
				}
			} else if (scannedInput.startsWith("GLL")) {
				try {
					lon = gpsParser.get_longNmea();
					lat = gpsParser.get_latNmea();
					m_latitude = new Measurement(java.lang.Math.toRadians(lat),Unit.rad);
					m_longitude = new Measurement(java.lang.Math.toRadians(lon),Unit.rad);	
					m_latitudeNmea = lat;
					m_longitudeNmea = lon;					
				} catch (Exception e) {
					m_latitude = null;
					m_longitude = null;					
					m_latitudeNmea = 0;
					m_longitudeNmea = 0;					
				}
			} else if (scannedInput.startsWith("GSA")) {
				try {
					m_PDOP = gpsParser.get_PDOPNmea();
					m_HDOP = gpsParser.get_HDOPNmea();
					m_VDOP = gpsParser.get_VDOPNmea();
					m_3Dfix = gpsParser.get_3DfixNmea();
					//System.out.println("m_PDOP = "+m_PDOP+"  m_HDOP = "+m_HDOP+"  m_VDOP = "+m_VDOP+"  m_3Dfix = "+m_3Dfix);
				} catch (Exception e) {
					m_PDOP = 0;
					m_HDOP = 0;
					m_VDOP = 0;
					m_3Dfix = 0;
				}
			} else if (scannedInput.startsWith("GSV")) {
			} else if (scannedInput.startsWith("RMC")) {
				try {
					lon = gpsParser.get_longNmea();
					lat = gpsParser.get_latNmea();
					speed = gpsParser.get_speedNmea();
					track = gpsParser.get_trackNmea();
					m_latitude = new Measurement(java.lang.Math.toRadians(lat),Unit.rad);
					m_longitude = new Measurement(java.lang.Math.toRadians(lon),Unit.rad);		
					m_speed = new Measurement(speed,Unit.m_s);
					m_track = new Measurement(java.lang.Math.toRadians(track),Unit.rad); 
					m_latitudeNmea = lat;
					m_longitudeNmea = lon;
					m_speedNmea = speed;
					m_trackNmea = track;
					m_dateNmea = gpsParser.get_dateNmea();
				} catch (Exception e) {
					m_latitude = null;
					m_longitude = null;
					m_speed = null;
					m_latitudeNmea = 0;
					m_longitudeNmea = 0;
					m_speedNmea = 0;
					m_trackNmea = 0;
				}
			} else if (scannedInput.startsWith("VTG")) {
				try {
					speed = gpsParser.get_speedNmea();
					m_speed = new Measurement(speed,Unit.m_s);
					m_speedNmea = speed;
				} catch (Exception e) {
					m_speed = null;
					m_speedNmea = 0;
				}
			} else if (scannedInput.indexOf("FOM") != -1) {
				//FOM = scannedInput;
			} else if (scannedInput.indexOf("PPS") != -1) {
				//PPS = scannedInput;
			} else {
				s_logger.warn("Unrecognized NMEA sentence: " + scannedInput);
			}
		}

		private boolean NmeaCksum(String nmeaMessageIn){
			int starpos = nmeaMessageIn.indexOf('*');
			String s_Cksum = nmeaMessageIn.substring(starpos+1,nmeaMessageIn.length()-1);
			int i_Cksum = Integer.parseInt(s_Cksum,16); // Check sum is coded in hex string

			int i_newCksum = 0;
			for (int i = 1; i < starpos; i++) {
				i_newCksum ^= nmeaMessageIn.charAt(i);
			}
			
			return(i_newCksum==i_Cksum);
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(" longitude=");
		sb.append(m_longitudeNmea);
		sb.append("\n latitude=");
		sb.append(m_latitudeNmea);
		sb.append("\n altitude=");
		sb.append(m_altitudeNmea);
		sb.append("\n speed=");
		sb.append(m_speedNmea);
		sb.append("\n date=");
		sb.append(m_dateNmea);
		sb.append("   time=");
		sb.append(m_timeNmea);
		sb.append("\n DOP=");
		sb.append(m_DOP);
		sb.append("\n 3Dfix=");
		sb.append(m_3Dfix);
		sb.append("\n fixQuality=");
		sb.append(m_fixQuality);
		return sb.toString();
	}
}
