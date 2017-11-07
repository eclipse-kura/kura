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

    private static final Logger logger = LoggerFactory.getLogger(GpsDevice.class);

    private static Object lock = new Object();
    static final String PROTOCOL_NAME = "position";

    // private String unitName = PROTOCOL_NAME;
    private SerialCommunicate comm;
    private boolean connConfigd = false;
    private boolean validPosition = false;
    private String lastSentence;

    private Measurement latitude = null;
    private Measurement longitude = null;
    private Measurement altitude = null;
    private Measurement speed = null;
    private Measurement track = null;
    private double latitudeNmea = 0;
    private double longitudeNmea = 0;
    private double altitudeNmea = 0;
    private double speedNmea = 0;
    private double trackNmea = 0;
    private int fixQuality = 0;
    private int nrSatellites = 0;
    private double dop = 0;
    private double pdop = 0;
    private double hdop = 0;
    private double vdop = 0;
    private int fix3D = 0;
    private String dateNmea = "";
    private String timeNmea = "";
    private Collection<PositionListener> listeners;

    public GpsDevice() {
        this.latitude = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
        this.longitude = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
        this.altitude = new Measurement(0, Unit.m);
        this.speed = new Measurement(0, Unit.m_s);
        this.track = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
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
        return new Position(this.latitude, this.longitude, this.altitude, this.speed, this.track);
    }

    public NmeaPosition getNmeaPosition() {
        return new NmeaPosition(this.latitudeNmea, this.longitudeNmea, this.altitudeNmea, this.speedNmea,
                this.trackNmea, this.fixQuality, this.nrSatellites, this.dop, this.pdop, this.hdop, this.vdop,
                this.fix3D);
    }

    public boolean isValidPosition() {
        return this.validPosition;
    }

    public String getDateNmea() {
        return this.dateNmea;
    }

    public String getTimeNmea() {
        return this.timeNmea;
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
        return this.lastSentence;
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

        private ScheduledExecutorService executor;
        private ScheduledFuture<?> task;

        InputStream in;
        CommConnection conn = null;
        Properties connConfig = null;

        public SerialCommunicate(ConnectionFactory connFactory, Properties connectionConfig) throws PositionException {
            logger.debug("Configure serial connection");

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
            if (this.task != null && !this.task.isDone()) {
                logger.debug("SerialCommunicate() :: Cancelling GpsSerialCommunicate task ...");
                this.task.cancel(true);
                logger.info("SerialCommunicate() :: GpsSerialCommunicate task cancelled? = {}", this.task.isDone());
                this.task = null;
            }

            this.executor = Executors.newSingleThreadScheduledExecutor();

            this.task = this.executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("GpsSerialCommunicate");
                    if (!doPollWork()) {
                        logger.info("The doPollWork() method returned 'false' - disconnecting ...");
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
            synchronized (lock) {
                if (this.task != null && !this.task.isDone()) {
                    logger.debug("disconnect() :: Cancelling GpsSerialCommunicate task ...");
                    this.task.cancel(true);
                    logger.info("disconnect() :: GpsSerialCommunicate task cancelled? = {}", this.task.isDone());
                    this.task = null;
                }

                if (this.executor != null) {
                    logger.debug("disconnect() :: Terminating GpsSerialCommunicate Thread ...");
                    this.executor.shutdownNow();
                    try {
                        this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("Interrupted - {}", e);
                    }
                    logger.info("disconnect() :: GpsSerialCommunicate Thread terminated? - {}",
                            this.executor.isTerminated());
                    this.executor = null;
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
                            logger.error("Exception in gps read - {}", e);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e1) {
                                logger.warn("Interrupted - {}", e1);
                            }
                            return false;
                        }
                        if (c != 13 && c != -1) {
                            readBuffer.append((char) c);
                        }
                    }
                    try {
                        if (readBuffer.length() > 0) {
                            logger.debug("GPS RAW: {}", readBuffer.toString());
                            if (GpsDevice.this.listeners != null && !GpsDevice.this.listeners.isEmpty()) {
                                for (PositionListener listener : GpsDevice.this.listeners) {
                                    listener.newNmeaSentence(readBuffer.toString());
                                }
                            }
                            parseNmeaSentence(readBuffer.toString());
                        }
                    } catch (Exception e) {
                        logger.error("Exception in parseNmeaSentence - {}", e);
                    }
                } else {
                    logger.debug("GPS InputStream is null");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (Exception e) {
                logger.error("Exception in Gps doPollWork");
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
                logger.error("NMEA checksum not valid");
                return;
            }
            // s_logger.info(scannedInput);
            GpsDevice.this.lastSentence = scannedInput;
            NMEAParser gpsParser = new NMEAParser();

            gpsParser.parseSentence(scannedInput);
            GpsDevice.this.validPosition = gpsParser.isValidPosition();
            // s_logger.debug("Parse : "+scannedInput+" position valid = "+m_validPosition);

            if (!GpsDevice.this.validPosition) {
                return;
            }

            if (!scannedInput.startsWith("$G")) {
                // Invalid NMEA String. Return.
                logger.warn("Invalid NMEA sentence: " + scannedInput);
                return;
            }
            // Remove the first 3 characters from the input string in order to normalize the commands
            scannedInput = scannedInput.substring(3);

            if (scannedInput.startsWith("TXT")) {
                logger.debug("U-Blox init message: {}", scannedInput);
            } else if (scannedInput.startsWith("GGA")) {
                try {
                    lon = gpsParser.getLongNmea();
                    lat = gpsParser.getLatNmea();
                    alt = gpsParser.getAltNmea();
                    GpsDevice.this.fixQuality = gpsParser.getFixQuality();
                    GpsDevice.this.latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
                    GpsDevice.this.longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
                    GpsDevice.this.altitude = new Measurement(alt, Unit.m);
                    GpsDevice.this.latitudeNmea = lat;
                    GpsDevice.this.longitudeNmea = lon;
                    GpsDevice.this.altitudeNmea = alt;
                    GpsDevice.this.dop = gpsParser.getDOPNmea();
                    GpsDevice.this.nrSatellites = gpsParser.getNrSatellites();
                    GpsDevice.this.timeNmea = gpsParser.getTimeNmea();
                } catch (Exception e) {
                    GpsDevice.this.latitude = null;
                    GpsDevice.this.longitude = null;
                    GpsDevice.this.altitude = null;
                    GpsDevice.this.latitudeNmea = 0;
                    GpsDevice.this.longitudeNmea = 0;
                    GpsDevice.this.altitudeNmea = 0;
                }
            } else if (scannedInput.startsWith("GLL")) {
                try {
                    lon = gpsParser.getLongNmea();
                    lat = gpsParser.getLatNmea();
                    GpsDevice.this.latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
                    GpsDevice.this.longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
                    GpsDevice.this.latitudeNmea = lat;
                    GpsDevice.this.longitudeNmea = lon;
                } catch (Exception e) {
                    GpsDevice.this.latitude = null;
                    GpsDevice.this.longitude = null;
                    GpsDevice.this.latitudeNmea = 0;
                    GpsDevice.this.longitudeNmea = 0;
                }
            } else if (scannedInput.startsWith("GSA")) {
                try {
                    GpsDevice.this.pdop = gpsParser.getPDOPNmea();
                    GpsDevice.this.hdop = gpsParser.getHDOPNmea();
                    GpsDevice.this.vdop = gpsParser.getVDOPNmea();
                    GpsDevice.this.fix3D = gpsParser.getFix3DNmea();
                    // System.out.println("PDOP = "+pdop+" HDOP = "+hdop+" VDOP = "+vdop+" fix3D = "+fix3D);
                } catch (Exception e) {
                    GpsDevice.this.pdop = 0;
                    GpsDevice.this.hdop = 0;
                    GpsDevice.this.vdop = 0;
                    GpsDevice.this.fix3D = 0;
                }
            } else if (scannedInput.startsWith("GSV")) {
            } else if (scannedInput.startsWith("RMC")) {
                try {
                    lon = gpsParser.getLongNmea();
                    lat = gpsParser.getLatNmea();
                    speed = gpsParser.getSpeedNmea();
                    track = gpsParser.getTrackNmea();
                    GpsDevice.this.latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
                    GpsDevice.this.longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
                    GpsDevice.this.speed = new Measurement(speed, Unit.m_s);
                    GpsDevice.this.track = new Measurement(java.lang.Math.toRadians(track), Unit.rad);
                    GpsDevice.this.latitudeNmea = lat;
                    GpsDevice.this.longitudeNmea = lon;
                    GpsDevice.this.speedNmea = speed;
                    GpsDevice.this.trackNmea = track;
                    GpsDevice.this.dateNmea = gpsParser.getDateNmea();
                } catch (Exception e) {
                    GpsDevice.this.latitude = null;
                    GpsDevice.this.longitude = null;
                    GpsDevice.this.speed = null;
                    GpsDevice.this.latitudeNmea = 0;
                    GpsDevice.this.longitudeNmea = 0;
                    GpsDevice.this.speedNmea = 0;
                    GpsDevice.this.trackNmea = 0;
                }
            } else if (scannedInput.startsWith("VTG")) {
                try {
                    speed = gpsParser.getSpeedNmea();
                    GpsDevice.this.speed = new Measurement(speed, Unit.m_s);
                    GpsDevice.this.speedNmea = speed;
                } catch (Exception e) {
                    GpsDevice.this.speed = null;
                    GpsDevice.this.speedNmea = 0;
                }
            } else if (scannedInput.indexOf("FOM") != -1) {
                // FOM = scannedInput;
            } else if (scannedInput.indexOf("PPS") != -1) {
                // PPS = scannedInput;
            } else {
                logger.warn("Unrecognized NMEA sentence: " + scannedInput);
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
        sb.append(this.longitudeNmea);
        sb.append("\n latitude=");
        sb.append(this.latitudeNmea);
        sb.append("\n altitude=");
        sb.append(this.altitudeNmea);
        sb.append("\n speed=");
        sb.append(this.speedNmea);
        sb.append("\n date=");
        sb.append(this.dateNmea);
        sb.append("   time=");
        sb.append(this.timeNmea);
        sb.append("\n DOP=");
        sb.append(this.dop);
        sb.append("\n 3Dfix=");
        sb.append(this.fix3D);
        sb.append("\n fixQuality=");
        sb.append(this.fixQuality);
        return sb.toString();
    }

    public void setListeners(Collection<PositionListener> listeners) {
        this.listeners = listeners;
    }
}
