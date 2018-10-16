/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.position.NMEAParser.Code;
import org.eclipse.kura.linux.position.NMEAParser.ParseException;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionException;
import org.eclipse.kura.position.PositionListener;
import org.osgi.service.io.ConnectionFactory;
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

    private static final int SERIAL_TIMEOUT_MS = 2000;
    private static final int TERMINATION_TIMEOUT_MS = SERIAL_TIMEOUT_MS + 1000;

    private final CommURI uri;

    private final SerialCommunicate commThread;
    private String lastSentence;

    private Listener listener;

    private final NMEAParser nmeaParser = new NMEAParser();

    public GpsDevice(final ConnectionFactory connFactory, final CommURI commURI, final Listener listener)
            throws PositionException {
        this.uri = commURI;
        this.listener = listener;
        this.commThread = new SerialCommunicate(connFactory, commURI);
    }

    public CommURI getCommURI() {
        return this.uri;
    }

    public synchronized Position getPosition() {
        return this.nmeaParser.getPosition();
    }

    public synchronized NmeaPosition getNmeaPosition() {
        return this.nmeaParser.getNmeaPosition();
    }

    public synchronized boolean isValidPosition() {
        return this.nmeaParser.isValidPosition();
    }

    public synchronized String getDateNmea() {
        return this.nmeaParser.getDateNmea();
    }

    public synchronized String getTimeNmea() {
        return this.nmeaParser.getTimeNmea();
    }

    public void disconnect() {
        this.listener = null;
        this.commThread.disconnect();
    }

    public String getLastSentence() {
        return this.lastSentence;
    }

    public boolean isConnected() {
        return this.commThread.isAlive();
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
    private final class SerialCommunicate extends Thread {

        private InputStream in = null;
        private CommConnection conn = null;
        private boolean run = true;

        public SerialCommunicate(final ConnectionFactory connFactory, final CommURI commURI) throws PositionException {
            try {
                this.conn = (CommConnection) connFactory.createConnection(enableTimeouts(commURI).toString(), 1, false);
                this.in = new BufferedInputStream(requireNonNull(this.conn.openInputStream()));
            } catch (Exception e) {
                closeSerialPort();
                throw new PositionException("Failed to open serial port", e);
            }

            this.start();
        }

        @Override
        public void run() {
            while (true) {
                if (!doPollWork()) {
                    closeSerialPort();
                    return;
                }
            }
        }

        public void disconnect() {
            run = false;
            this.interrupt();
            try {
                this.join(TERMINATION_TIMEOUT_MS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for thread termination");
                Thread.currentThread().interrupt();
            }

            if (this.isAlive()) {
                logger.warn("GPS receiver thread did not terminate after {} milliseconds", TERMINATION_TIMEOUT_MS);
                closeSerialPort();
            }
        }

        private void closeSerialPort() {
            logger.debug("closing serial port...");

            try {
                if (this.in != null) {
                    this.in.close();
                }
            } catch (Exception e) {
                logger.warn("Failed to close serial port InputStream", e);
            }

            try {
                if (this.conn != null) {
                    this.conn.close();
                }
            } catch (Exception e) {
                logger.warn("Failed to close serial port connection", e);
            }

            logger.debug("closing serial port...done");
        }

        private boolean doPollWork() {
            final StringBuilder readBuffer = new StringBuilder();
            int c = -1;
            while (c != 10) {
                if (!run) {
                    logger.debug("Shutdown requested, exiting");
                    return false;
                }
                try {
                    c = this.in.read();
                } catch (Exception e) {
                    logger.error("Exception in gps read - {}", e);
                    return false;
                }
                if (c == -1) {
                    logger.debug("Read timed out");
                } else if (c != 13) {
                    readBuffer.append((char) c);
                }
            }
            if (readBuffer.length() > 0) {
                final String sentence = readBuffer.toString();
                logger.debug("GPS RAW: {}", sentence);
                handleNmeaSentence(sentence);
            }
            return true;
        }

        private void handleNmeaSentence(final String sentence) {

            if (sentence.isEmpty()) {
                logger.debug("Empty NMEA sentence detected");
                return;
            }

            if (GpsDevice.this.listener != null) {
                GpsDevice.this.listener.newNmeaSentence(sentence);
            }

            final boolean isLastPositionValid = nmeaParser.isValidPosition();

            try {
                final boolean isValid;

                synchronized (this) {
                    isValid = nmeaParser.parseSentence(sentence);
                    GpsDevice.this.lastSentence = sentence;
                }

                if (isValid != isLastPositionValid && GpsDevice.this.listener != null) {
                    listener.onLockStatusChanged(isValid);
                    logger.info("{}", GpsDevice.this);
                }

            } catch (ParseException e) {
                final Code code = e.getCode();
                if (code == Code.BAD_CHECKSUM) {
                    logger.warn("NMEA checksum not valid");
                } else if (code == Code.INVALID) {
                    logger.warn("Invalid NMEA sentence: {}", sentence);
                } else {
                    logger.warn("Unrecognized NMEA sentence: {}", sentence);
                }
            } catch (Exception e) {
                logger.warn("Unexpected exception parsing NMEA sentence", e);
            }
        }

        private CommURI enableTimeouts(final CommURI original) {
            return new CommURI.Builder(original.getPort()).withBaudRate(original.getBaudRate())
                    .withDataBits(original.getDataBits()).withStopBits(original.getStopBits())
                    .withFlowControl(original.getFlowControl()).withParity(original.getParity())
                    .withOpenTimeout(SERIAL_TIMEOUT_MS).withReceiveTimeout(SERIAL_TIMEOUT_MS).build();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" longitude=");
        sb.append(this.nmeaParser.getLongNmea());
        sb.append("\n latitude=");
        sb.append(this.nmeaParser.getLatNmea());
        sb.append("\n altitude=");
        sb.append(this.nmeaParser.getAltNmea());
        sb.append("\n speed=");
        sb.append(this.nmeaParser.getSpeedNmea());
        sb.append("\n date=");
        sb.append(this.nmeaParser.getDateNmea());
        sb.append("   time=");
        sb.append(this.nmeaParser.getTimeNmea());
        sb.append("\n DOP=");
        sb.append(this.nmeaParser.getDOPNmea());
        sb.append("\n 3Dfix=");
        sb.append(this.nmeaParser.getFix3DNmea());
        sb.append("\n fixQuality=");
        sb.append(this.nmeaParser.getFixQuality());
        return sb.toString();
    }

    interface Listener extends PositionListener {

        public void onLockStatusChanged(final boolean hasLock);
    }
}
