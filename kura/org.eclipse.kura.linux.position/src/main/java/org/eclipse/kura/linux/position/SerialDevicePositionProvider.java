/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.position;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialDevicePositionProvider implements PositionProvider {

    private static final Logger logger = LoggerFactory.getLogger(SerialDevicePositionProvider.class);

    private GpsDeviceTracker gpsDeviceTracker;
    private ModemGpsStatusTracker modemGpsStatusTracker;
    private ConnectionFactory connectionFactory;

    private GpsDevice gpsDevice;

    private PositionServiceOptions configuration;

    private Listener gpsDeviceListener;

    private DateTimeFormatter nmeaDateTimePattern = DateTimeFormatter.ofPattern("ddMMyy hhmmss");

    private static final Position ZERO_POSITION;
    private static final NmeaPosition ZERO_NMEA_POSITION;

    static {
        final double latitudeRad = Math.toRadians(0);
        final double longitudeRad = Math.toRadians(0);

        final Measurement latitude = new Measurement(latitudeRad, Unit.rad);
        final Measurement longitude = new Measurement(longitudeRad, Unit.rad);
        final Measurement altitude = new Measurement(0, Unit.m);
        final Measurement speed = new Measurement(0, Unit.m_s); // conversion speed in knots to m/s : 1 m/s = 1.94384449
        // knots
        final Measurement track = new Measurement(java.lang.Math.toRadians(0), Unit.rad);

        ZERO_POSITION = new Position(latitude, longitude, altitude, speed, track);
        ZERO_NMEA_POSITION = new NmeaPosition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (char) 0, (char) 0, (char) 0);
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void unsetConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = null;
    }

    public void setGpsDeviceTracker(final GpsDeviceTracker tracker) {
        this.gpsDeviceTracker = tracker;
    }

    public void unsetGpsDeviceTracker(final GpsDeviceTracker tracker) {
        tracker.setListener(null);
    }

    public void setModemGpsStatusTracker(final ModemGpsStatusTracker modemGpsDeviceTracker) {
        this.modemGpsStatusTracker = modemGpsDeviceTracker;
    }

    public void unsetModemGpsStatusTracker(final ModemGpsStatusTracker modemGpsDeviceTracker) {
        modemGpsDeviceTracker.setListener(null);
        this.modemGpsStatusTracker = null;
    }

    @Override
    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {

        this.gpsDeviceTracker.setListener(gpsDeviceAvailabilityListener);
        this.modemGpsStatusTracker.setListener(gpsDeviceAvailabilityListener);

        this.gpsDeviceListener = gpsDeviceListener;
        this.configuration = configuration;
    }

    @Override
    public void start() {
        this.gpsDevice = openGpsDevice();
    }

    @Override
    public void stop() {
        this.gpsDeviceTracker.reset();

        if (this.gpsDevice != null) {
            this.gpsDevice.disconnect();
            this.gpsDevice = null;
        }
    }

    private GpsDevice openGpsDevice() {

        logger.info("Opening GPS device...");

        GpsDevice device = openGpsDevice(this.modemGpsStatusTracker.getGpsDeviceUri());

        if (device != null) {
            logger.info("Opened modem GPS device");
            return device;
        } else {
            this.modemGpsStatusTracker.reset();
        }

        device = openGpsDevice(this.configuration.getGpsDeviceUri());

        if (device != null) {
            logger.info("Opened GPS device from configuration");
        } else {
            logger.info("GPS device not available");
        }

        return device;
    }

    private GpsDevice openGpsDevice(CommURI uri) {

        uri = this.gpsDeviceTracker.track(uri);

        if (uri == null) {
            return null;
        }

        try {
            return new GpsDevice(this.connectionFactory, uri, this.gpsDeviceListener);
        } catch (Exception e) {
            logger.warn("Failed to open GPS device: {}", uri, e);
            return null;
        }
    }

    @Override
    public Position getPosition() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getPosition();
        } else {
            return ZERO_POSITION;
        }
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getNmeaPosition();
        } else {
            return ZERO_NMEA_POSITION;
        }
    }

    @Override
    public String getNmeaTime() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getTimeNmea();
        } else {
            return null;
        }
    }

    @Override
    public String getNmeaDate() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getDateNmea();
        } else {
            return null;
        }
    }

    @Override
    public LocalDateTime getDateTime() {
        if (this.gpsDevice != null) {
            String nmeaDateTime = this.getNmeaDate() + " " + this.getNmeaTime();
            return LocalDateTime.parse(nmeaDateTime, nmeaDateTimePattern);
        } else {
            return null;
        }
    }

    @Override
    public boolean isLocked() {
        if (!this.configuration.isEnabled()) {
            return false;
        }
        if (this.configuration.isStatic()) {
            return true;
        }
        return this.gpsDevice != null && this.gpsDevice.isValidPosition();
    }

    @Override
    public String getLastSentence() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getLastSentence();
        } else {
            return null;
        }
    }

    @Override
    public PositionProviderType getType() {
        return PositionProviderType.SERIAL;
    }

    protected GpsDevice getGpsDevice() {
        return this.gpsDevice;
    }

    protected ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    protected GpsDeviceTracker getGpsDeviceTracker() {
        return this.gpsDeviceTracker;
    }

    protected ModemGpsStatusTracker getModemGpsStatusTracker() {
        return this.modemGpsStatusTracker;
    }

}
