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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionListener;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionLostEvent;
import org.eclipse.kura.position.PositionService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionServiceImpl
        implements PositionService, ConfigurableComponent, GpsDevice.Listener, GpsDeviceAvailabilityListener {

    private static final Logger logger = LoggerFactory.getLogger(PositionServiceImpl.class);

    private ConnectionFactory connectionFactory;
    private EventAdmin eventAdmin;

    private GpsDeviceTracker gpsDeviceTracker;
    private ModemGpsStatusTracker modemGpsStatusTracker;

    private final Map<String, PositionListener> positionListeners = new ConcurrentHashMap<>();

    private PositionServiceOptions options;
    private GpsDevice gpsDevice;

    private boolean hasLock;
    private Position staticPosition;
    private NmeaPosition staticNmeaPosition;

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

    public void setEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    public void setGpsDeviceTracker(final GpsDeviceTracker tracker) {
        this.gpsDeviceTracker = tracker;
        tracker.setListener(this);
    }

    public void unsetGpsDeviceTracker(final GpsDeviceTracker tracker) {
        tracker.setListener(null);
        this.gpsDeviceTracker = null;
    }

    public void setModemGpsStatusTracker(final ModemGpsStatusTracker modemGpsDeviceTracker) {
        this.modemGpsStatusTracker = modemGpsDeviceTracker;
        modemGpsDeviceTracker.setListener(this);
    }

    public void unsetModemGpsStatusTracker(final ModemGpsStatusTracker modemGpsDeviceTracker) {
        modemGpsDeviceTracker.setListener(null);
        this.modemGpsStatusTracker = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(final Map<String, Object> properties) {
        logger.debug("Activating...");

        setStaticPosition(0, 0, 0);
        updated(properties);

        logger.info("Activating... Done.");
    }

    protected void deactivate() {
        logger.debug("Deactivating...");

        stop();

        logger.info("Deactivating... Done.");
    }

    public void updated(final Map<String, Object> properties) {

        logger.debug("Updating...");

        final PositionServiceOptions newOptions = new PositionServiceOptions(properties);

        if (newOptions.equals(this.options)) {
            logger.debug("same configuration, no need ot reconfigure GPS device");
            return;
        }

        this.options = newOptions;

        updateInternal();

        logger.info("Updating... Done.");
    }

    private synchronized void updateInternal() {
        stop();

        if (!this.options.isEnabled()) {
            return;
        }

        if (this.options.isStatic()) {
            setStaticPosition(this.options.getStaticLatitude(), this.options.getStaticLongitude(),
                    this.options.getStaticAltitude());
            setLock(true);
        } else {
            this.gpsDevice = openGpsDevice();
        }
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Position getPosition() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getPosition();
        } else {
            return this.staticPosition;
        }
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getNmeaPosition();
        } else {
            return this.staticNmeaPosition;
        }
    }

    @Override
    public boolean isLocked() {
        if (!this.options.isEnabled()) {
            return false;
        }
        if (this.options.isStatic()) {
            return true;
        }
        return this.gpsDevice != null && this.gpsDevice.isValidPosition();
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
    public void registerListener(String listenerId, PositionListener positionListener) {
        this.positionListeners.put(listenerId, positionListener);
    }

    @Override
    public void unregisterListener(String listenerId) {
        this.positionListeners.remove(listenerId);
    }

    @Override
    public String getLastSentence() {
        if (this.gpsDevice != null) {
            return this.gpsDevice.getLastSentence();
        } else {
            return null;
        }
    }

    protected GpsDevice getGpsDevice() {
        return this.gpsDevice;
    }

    protected PositionServiceOptions getPositionServiceOptions() {
        return this.options;
    }

    private void stop() {
        this.gpsDeviceTracker.reset();

        if (this.gpsDevice != null) {
            this.gpsDevice.disconnect();
            this.gpsDevice = null;
        }

        setStaticPosition(0, 0, 0);
        setLock(false);
    }

    private void setLock(boolean hasLock) {
        if (hasLock && !this.hasLock) {
            logger.debug("posting PositionLockedEvent");
            this.eventAdmin.postEvent(new PositionLockedEvent(Collections.emptyMap()));
        } else if (!hasLock && this.hasLock) {
            logger.debug("posting PositionLostEvent");
            this.eventAdmin.postEvent(new PositionLostEvent(Collections.emptyMap()));
        }
        this.hasLock = hasLock;
    }

    private void setStaticPosition(double latitudeDeg, double longitudeDeg, double altitudeNmea) {

        final double latitudeRad = Math.toRadians(latitudeDeg);
        final double longitudeRad = Math.toRadians(longitudeDeg);

        final Measurement latitude = new Measurement(latitudeRad, Unit.rad);
        final Measurement longitude = new Measurement(longitudeRad, Unit.rad);
        final Measurement altitude = new Measurement(altitudeNmea, Unit.m);
        final Measurement speed = new Measurement(0, Unit.m_s); // conversion speed in knots to m/s : 1 m/s = 1.94384449
        // knots
        final Measurement track = new Measurement(java.lang.Math.toRadians(0), Unit.rad);

        this.staticPosition = new Position(latitude, longitude, altitude, speed, track);
        this.staticNmeaPosition = new NmeaPosition(latitudeDeg, longitudeDeg, altitudeNmea, 0, 0, 0, 0, 0, 0, 0, 0, 0, (char)0, (char)0, (char)0);
    }

    private GpsDevice openGpsDevice(CommURI uri) {

        uri = this.gpsDeviceTracker.track(uri);

        if (uri == null) {
            return null;
        }

        try {
            return new GpsDevice(this.connectionFactory, uri, this);
        } catch (Exception e) {
            logger.warn("Failed to open GPS device: {}", uri, e);
            return null;
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

        device = openGpsDevice(this.options.getGpsDeviceUri());

        if (device != null) {
            logger.info("Opened GPS device from configuration");
        } else {
            logger.info("GPS device not available");
        }

        return device;
    }

    @Override
    public void newNmeaSentence(final String nmeaSentence) {
        for (final PositionListener listener : this.positionListeners.values()) {
            listener.newNmeaSentence(nmeaSentence);
        }
    }

    @Override
    public synchronized void onLockStatusChanged(final boolean hasLock) {
        setLock(hasLock);
    }

    @Override
    public void onGpsDeviceAvailabilityChanged() {
        if (!this.options.isEnabled() || this.options.isStatic()) {
            return;
        }

        updateInternal();
    }

}