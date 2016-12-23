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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
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
import org.eclipse.kura.position.PositionListener;
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

    // private String unitName = PROTOCOL_NAME;
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
    private String m_dateNmea = "";
    private String m_timeNmea = "";
    private Collection<PositionListener> m_listeners;

    public GpsDevice() {
        this.m_latitude = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
        this.m_longitude = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
        this.m_altitude = new Measurement(0, Unit.m);
        this.m_speed = new Measurement(0, Unit.m_s);
        this.m_track = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
    }

    public String getProtocolName() {
        return "position";
    }

    public String getUnitAddress() {
        return null;
    }

    public void configureProtocol(Properties protocolConfig) throws PositionException {
        // TODO Auto-generated method stub

    }

    public void configureConnection(ConnectionFactory connFactory, Properties connectionConfig)
            throws PositionException {

        if (this.connConfigd) {
            this.comm.disconnect();
            this.comm = null;
            this.connConfigd = false;
        }

        try {
            this.comm = new SerialCommunicate(connFactory, connectionConfig);
        } catch (PositionException e) {
            throw e;
        }
        this.connConfigd = true;
    }

    public int getConnectStatus() {
        if (!this.connConfigd) {
            return KuraConnectionStatus.NEVERCONNECTED;
        }
        return this.comm.getConnectStatus();
    }

    public Properties getConnectConfig() {
        if (!this.connConfigd) {
            return null;
        }
        return this.comm.getConnectConfig();
    }

    public Position getPosition() {
        return new Position(this.m_latitude, this.m_longitude, this.m_altitude, this.m_speed, this.m_track);
    }

    public NmeaPosition getNmeaPosition() {
        return new NmeaPosition(this.m_latitudeNmea, this.m_longitudeNmea, this.m_altitudeNmea, this.m_speedNmea,
                this.m_trackNmea, this.m_fixQuality, this.m_nrSatellites, this.m_DOP, this.m_PDOP, this.m_HDOP,
                this.m_VDOP, this.m_3Dfix);
    }

    public boolean isValidPosition() {
        return this.m_validPosition;
    }

    public String getDateNmea() {
        return this.m_dateNmea;
    }

    public String getTimeNmea() {
        return this.m_timeNmea;
    }

    public void connect() throws PositionException {
        if (!this.connConfigd) {
            throw new PositionException("Invalid serial port configuration");
        }
        this.comm.connect();
    }

    public void disconnect() {
        if (this.connConfigd && this.comm != null) {
            this.comm.disconnect();
        }
    }

    public String getLastSentence() {
        return this.m_lastSentence;
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
        private ScheduledFuture<?> m_task;

        InputStream in;
        CommConnection conn = null;
        Properties connConfig = null;

        public SerialCommunicate(ConnectionFactory connFactory, Properties connectionConfig) throws PositionException {
            s_logger.debug("Configure serial connection");

            this.connConfig = connectionConfig;

            String sPort;
            String sBaud;
            String sStop;
            String sParity;
            String sBits;

            if ((sPort = connectionConfig.getProperty("port")) == null
                    || (sBaud = connectionConfig.getProperty("baudRate")) == null
                    || (sStop = connectionConfig.getProperty("stopBits")) == null
                    || (sParity = connectionConfig.getProperty("parity")) == null
                    || (sBits = connectionConfig.getProperty("bitsPerWord")) == null) {
                throw new PositionException("Invalid serial port configuration");
            }

            int baud = Integer.valueOf(sBaud).intValue();
            int stop = Integer.valueOf(sStop).intValue();
            int parity = Integer.valueOf(sParity).intValue();
            int bits = Integer.valueOf(sBits).intValue();

            String uri = new CommURI.Builder(sPort).withBaudRate(baud).withDataBits(bits).withStopBits(stop)
                    .withParity(parity).withTimeout(2000).build().toString();

            try {
                this.conn = (CommConnection) connFactory.createConnection(uri, 1, false);
            } catch (IOException e1) {
                throw new PositionException("Invalid GPS serial Port", e1);
            }

            // get the streams
            try {
                this.in = this.conn.openInputStream();
                this.conn.openOutputStream();
            } catch (Exception e) {
                throw new PositionException("input stream", e);
            }

            // clean up if this is not our first run
            if (this.m_task != null && !this.m_task.isDone()) {
                s_logger.debug("SerialCommunicate() :: Cancelling GpsSerialCommunicate task ...");
                this.m_task.cancel(true);
                s_logger.info("SerialCommunicate() :: GpsSerialCommunicate task cancelled? = {}", this.m_task.isDone());
                this.m_task = null;
            }

            this.m_executor = Executors.newSingleThreadScheduledExecutor();

            this.m_task = this.m_executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("GpsSerialCommunicate");
                    if (!doPollWork()) {
                        s_logger.info("The doPollWork() method returned 'false' - disconnecting ...");
                        disconnect();
                    }
                }
            }, 0, 20, TimeUnit.MILLISECONDS);
        }

        public void connect() {
            /*
             * always connected
             */
        }

        public void disconnect() {
            synchronized (s_lock) {
                if (this.m_task != null && !this.m_task.isDone()) {
                    s_logger.debug("disconnect() :: Cancelling GpsSerialCommunicate task ...");
                    this.m_task.cancel(true);
                    s_logger.info("disconnect() :: GpsSerialCommunicate task cancelled? = {}", this.m_task.isDone());
                    this.m_task = null;
                }

                if (this.m_executor != null) {
                    s_logger.debug("disconnect() :: Terminating GpsSerialCommunicate Thread ...");
                    this.m_executor.shutdownNow();
                    try {
                        this.m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        s_logger.warn("Interrupted - {}", e);
                    }
                    s_logger.info("disconnect() :: GpsSerialCommunicate Thread terminated? - {}",
                            this.m_executor.isTerminated());
                    this.m_executor = null;
                }

                if (this.conn != null) {
                    try {
                        if (this.in != null) {
                            this.in.close();
                            this.in = null;
                        }
                        this.conn.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    this.conn = null;
                }
            }
        }

        public int getConnectStatus() {
            return KuraConnectionStatus.CONNECTED;
        }

        public Properties getConnectConfig() {
            return this.connConfig;
        }

        public boolean doPollWork() {
            try {
                StringBuffer readBuffer = new StringBuffer();
                int c = -1;
                if (this.in != null) {
                    while (c != 10) {
                        try {
                            c = this.in.read();
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
                            s_logger.debug("GPS RAW: {}", readBuffer.toString());
                            if (GpsDevice.this.m_listeners != null && !GpsDevice.this.m_listeners.isEmpty()) {
                                for (PositionListener listener : GpsDevice.this.m_listeners) {
                                    listener.newNmeaSentence(readBuffer.toString());
                                }
                            }
                            parseNmeaSentence(readBuffer.toString());
                        }
                    } catch (Exception e) {
                        s_logger.error("Exception in parseNmeaSentence - {}", e);
                    }
                } else {
                    s_logger.debug("GPS InputStream is null");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (Exception e) {
                s_logger.error("Exception in Gps doPollWork");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
            return true;
        }

        private void parseNmeaSentence(String scannedInput) {

            double lon, lat, speed, alt, track;

            // got a message... do a cksum
            if (!NmeaCksum(scannedInput)) {
                s_logger.error("NMEA checksum not valid");
                return;
            }
            // s_logger.info(scannedInput);
            GpsDevice.this.m_lastSentence = scannedInput;
            NMEAParser gpsParser = new NMEAParser();

            gpsParser.parseSentence(scannedInput);
            GpsDevice.this.m_validPosition = gpsParser.is_validPosition();
            // s_logger.debug("Parse : "+scannedInput+" position valid = "+m_validPosition);

            if (!GpsDevice.this.m_validPosition) {
                return;
            }

            if (!scannedInput.startsWith("$G")) {
                // Invalid NMEA String. Return.
                s_logger.warn("Invalid NMEA sentence: " + scannedInput);
                return;
            }
            // Remove the first 3 characters from the input string in order to normalize the commands
            scannedInput = scannedInput.substring(3);

            if (scannedInput.startsWith("TXT")) {
                s_logger.debug("U-Blox init message: {}", scannedInput);
            } else if (scannedInput.startsWith("GGA")) {
                try {
                    lon = gpsParser.get_longNmea();
                    lat = gpsParser.get_latNmea();
                    alt = gpsParser.get_altNmea();
                    GpsDevice.this.m_fixQuality = gpsParser.get_fixQuality();
                    GpsDevice.this.m_latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
                    GpsDevice.this.m_longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
                    GpsDevice.this.m_altitude = new Measurement(alt, Unit.m);
                    GpsDevice.this.m_latitudeNmea = lat;
                    GpsDevice.this.m_longitudeNmea = lon;
                    GpsDevice.this.m_altitudeNmea = alt;
                    GpsDevice.this.m_DOP = gpsParser.get_DOPNmea();
                    GpsDevice.this.m_nrSatellites = gpsParser.get_nrSatellites();
                    GpsDevice.this.m_timeNmea = gpsParser.get_timeNmea();
                } catch (Exception e) {
                    GpsDevice.this.m_latitude = null;
                    GpsDevice.this.m_longitude = null;
                    GpsDevice.this.m_altitude = null;
                    GpsDevice.this.m_latitudeNmea = 0;
                    GpsDevice.this.m_longitudeNmea = 0;
                    GpsDevice.this.m_altitudeNmea = 0;
                }
            } else if (scannedInput.startsWith("GLL")) {
                try {
                    lon = gpsParser.get_longNmea();
                    lat = gpsParser.get_latNmea();
                    GpsDevice.this.m_latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
                    GpsDevice.this.m_longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
                    GpsDevice.this.m_latitudeNmea = lat;
                    GpsDevice.this.m_longitudeNmea = lon;
                } catch (Exception e) {
                    GpsDevice.this.m_latitude = null;
                    GpsDevice.this.m_longitude = null;
                    GpsDevice.this.m_latitudeNmea = 0;
                    GpsDevice.this.m_longitudeNmea = 0;
                }
            } else if (scannedInput.startsWith("GSA")) {
                try {
                    GpsDevice.this.m_PDOP = gpsParser.get_PDOPNmea();
                    GpsDevice.this.m_HDOP = gpsParser.get_HDOPNmea();
                    GpsDevice.this.m_VDOP = gpsParser.get_VDOPNmea();
                    GpsDevice.this.m_3Dfix = gpsParser.get_3DfixNmea();
                    // System.out.println("m_PDOP = "+m_PDOP+" m_HDOP = "+m_HDOP+" m_VDOP = "+m_VDOP+" m_3Dfix =
                    // "+m_3Dfix);
                } catch (Exception e) {
                    GpsDevice.this.m_PDOP = 0;
                    GpsDevice.this.m_HDOP = 0;
                    GpsDevice.this.m_VDOP = 0;
                    GpsDevice.this.m_3Dfix = 0;
                }
            } else if (scannedInput.startsWith("GSV")) {
            } else if (scannedInput.startsWith("RMC")) {
                try {
                    lon = gpsParser.get_longNmea();
                    lat = gpsParser.get_latNmea();
                    speed = gpsParser.get_speedNmea();
                    track = gpsParser.get_trackNmea();
                    GpsDevice.this.m_latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
                    GpsDevice.this.m_longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
                    GpsDevice.this.m_speed = new Measurement(speed, Unit.m_s);
                    GpsDevice.this.m_track = new Measurement(java.lang.Math.toRadians(track), Unit.rad);
                    GpsDevice.this.m_latitudeNmea = lat;
                    GpsDevice.this.m_longitudeNmea = lon;
                    GpsDevice.this.m_speedNmea = speed;
                    GpsDevice.this.m_trackNmea = track;
                    GpsDevice.this.m_dateNmea = gpsParser.get_dateNmea();
                } catch (Exception e) {
                    GpsDevice.this.m_latitude = null;
                    GpsDevice.this.m_longitude = null;
                    GpsDevice.this.m_speed = null;
                    GpsDevice.this.m_latitudeNmea = 0;
                    GpsDevice.this.m_longitudeNmea = 0;
                    GpsDevice.this.m_speedNmea = 0;
                    GpsDevice.this.m_trackNmea = 0;
                }
            } else if (scannedInput.startsWith("VTG")) {
                try {
                    speed = gpsParser.get_speedNmea();
                    GpsDevice.this.m_speed = new Measurement(speed, Unit.m_s);
                    GpsDevice.this.m_speedNmea = speed;
                } catch (Exception e) {
                    GpsDevice.this.m_speed = null;
                    GpsDevice.this.m_speedNmea = 0;
                }
            } else if (scannedInput.indexOf("FOM") != -1) {
                // FOM = scannedInput;
            } else if (scannedInput.indexOf("PPS") != -1) {
                // PPS = scannedInput;
            } else {
                s_logger.warn("Unrecognized NMEA sentence: " + scannedInput);
            }
        }

        private boolean NmeaCksum(String nmeaMessageIn) {
            int starpos = nmeaMessageIn.indexOf('*');
            String s_Cksum = nmeaMessageIn.substring(starpos + 1, nmeaMessageIn.length() - 1);
            int i_Cksum = Integer.parseInt(s_Cksum, 16); // Check sum is coded in hex string

            int i_newCksum = 0;
            for (int i = 1; i < starpos; i++) {
                i_newCksum ^= nmeaMessageIn.charAt(i);
            }

            return i_newCksum == i_Cksum;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" longitude=");
        sb.append(this.m_longitudeNmea);
        sb.append("\n latitude=");
        sb.append(this.m_latitudeNmea);
        sb.append("\n altitude=");
        sb.append(this.m_altitudeNmea);
        sb.append("\n speed=");
        sb.append(this.m_speedNmea);
        sb.append("\n date=");
        sb.append(this.m_dateNmea);
        sb.append("   time=");
        sb.append(this.m_timeNmea);
        sb.append("\n DOP=");
        sb.append(this.m_DOP);
        sb.append("\n 3Dfix=");
        sb.append(this.m_3Dfix);
        sb.append("\n fixQuality=");
        sb.append(this.m_fixQuality);
        return sb.toString();
    }

    public void setListeners(Collection<PositionListener> listeners) {
        this.m_listeners = listeners;
    }
}
