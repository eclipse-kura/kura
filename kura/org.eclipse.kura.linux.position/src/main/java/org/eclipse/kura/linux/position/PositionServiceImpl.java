/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionListener;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionLostEvent;
import org.eclipse.kura.position.PositionService;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionServiceImpl
        implements PositionService, ConfigurableComponent, GpsDeviceAvailabilityListener, GpsDevice.Listener {

    private static final Logger logger = LoggerFactory.getLogger(PositionServiceImpl.class);

    private EventAdmin eventAdmin;
    private List<PositionProvider> positionProviders = new ArrayList<>();

    private PositionProvider currentProvider;

    private final Map<String, PositionListener> positionListeners = new ConcurrentHashMap<>();

    private PositionServiceOptions options;

    private boolean hasLock;
    private Position staticPosition;
    private NmeaPosition staticNmeaPosition;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    public void setPositionProviders(PositionProvider positionProvider) {
        this.positionProviders.add(positionProvider); // ADD NAME TO PROVIDERS
    }

    public void unsetPositionProviders(PositionProvider positionProvider) {
        this.positionProviders.remove(positionProvider);
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

        stopPositionProvider();

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

        stopPositionProvider();

        if (!this.options.isEnabled()) {
            return;
        }

        if (this.options.isStatic()) {
            setStaticPosition(this.options.getStaticLatitude(), this.options.getStaticLongitude(),
                    this.options.getStaticAltitude());
            setLock(true);
        } else {
            try {
                startPositionProvider();
            } catch (KuraException e) {
                logger.error("Unable to start the chosen Position Provider", e);
            }
        }
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Position getPosition() {
        if (this.options.isEnabled() && !this.options.isStatic()) {
            return this.currentProvider.getPosition();
        } else {
            return this.staticPosition;
        }

    }

    @Override
    public NmeaPosition getNmeaPosition() {
        if (this.options.isEnabled() && !this.options.isStatic()) {
            return this.currentProvider.getNmeaPosition();
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
        return this.currentProvider.isLocked();
    }

    @Override
    public String getNmeaTime() {
        if (this.currentProvider != null) {
            return this.currentProvider.getNmeaTime();
        } else {
            return null;
        }
    }

    @Override
    public String getNmeaDate() {
        if (this.currentProvider != null) {
            return this.currentProvider.getNmeaDate();
        } else {
            return null;
        }
    }

    @Override
    public LocalDateTime getDateTime() {
        if (this.currentProvider != null) {
            return this.currentProvider.getDateTime();
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
        if (this.currentProvider != null) {
            return this.currentProvider.getLastSentence();
        } else {
            return null;
        }
    }

    protected PositionServiceOptions getPositionServiceOptions() {
        return this.options;
    }

    private void startPositionProvider() throws KuraException {
        stopPositionProvider();

        this.currentProvider = this.positionProviders.stream()
                .filter(pp -> pp.getType() == this.options.getPositionProvider()).findAny()
                .orElseThrow(() -> new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, " provider",
                        this.options.getPositionProvider()));

        this.currentProvider.init(options, this, this);
        this.currentProvider.start();

    }

    private void stopPositionProvider() {
        if (this.currentProvider != null) {
            this.currentProvider.stop();
            this.currentProvider = null;
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
        this.staticNmeaPosition = new NmeaPosition(latitudeDeg, longitudeDeg, altitudeNmea, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                (char) 0, (char) 0, (char) 0);
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