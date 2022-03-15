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

import static java.lang.Math.toRadians;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.taimos.gpsd4java.api.IObjectListener;
import de.taimos.gpsd4java.backend.GPSdEndpoint;
import de.taimos.gpsd4java.types.ATTObject;
import de.taimos.gpsd4java.types.DeviceObject;
import de.taimos.gpsd4java.types.DevicesObject;
import de.taimos.gpsd4java.types.ENMEAMode;
import de.taimos.gpsd4java.types.SKYObject;
import de.taimos.gpsd4java.types.TPVObject;
import de.taimos.gpsd4java.types.subframes.SUBFRAMEObject;

public class GpsdPositionProvider implements PositionProvider, IObjectListener {

    private static final Logger logger = LoggerFactory.getLogger(GpsdPositionProvider.class);
    private final AtomicReference<GpsdInternalState> internalStateReference = new AtomicReference<>(
            new GpsdInternalState());

    private GPSdEndpoint gpsEndpoint;
    private PositionServiceOptions configuration;

    private Listener gpsDeviceListener;
    private ScheduledExecutorService executor;
    private Future<?> checkFuture = CompletableFuture.completedFuture(null);

    @Override
    public void start() {
        if (this.gpsEndpoint != null) {
            this.gpsEndpoint.start();
        }

        final OptionalInt validityInterval = this.configuration.getGpsdMaxValidityInterval();

        if (validityInterval.isPresent() && executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    @Override
    public void stop() {
        if (this.gpsEndpoint != null) {
            this.gpsEndpoint.stop();
        }

        if (executor != null) {
            executor.shutdown();

            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting fix validity check executor shutdown", e);
                Thread.currentThread().interrupt();
            }

            executor = null;
        }
    }

    @Override
    public Position getPosition() {
        GpsdInternalState internalState = internalStateReference.get();
        return new Position(toRadiansMeasurement(internalState.getLatitude(), internalState.getLatitudeError()),
                toRadiansMeasurement(internalState.getLongitude(), internalState.getLongitudeError()),
                toMetersMeasurement(internalState.getAltitude(), internalState.getAltitudeError()),
                toMetersPerSecondMeasurement(internalState.getSpeed(), internalState.getSpeedError()),
                toRadiansMeasurement(internalState.getCourse(), internalState.getCourseError()));
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        GpsdInternalState internalState = internalStateReference.get();
        return new NmeaPosition(internalState.getLatitude(), internalState.getLongitude(), internalState.getAltitude(),
                internalState.getSpeed(), internalState.getCourse());
    }

    @Override
    public String getNmeaTime() {
        throw new UnsupportedOperationException("GpsdPositionProvider doesn't return NMEA time.");
    }

    @Override
    public String getNmeaDate() {
        throw new UnsupportedOperationException("GpsdPositionProvider doesn't return NMEA time.");
    }

    @Override
    public LocalDateTime getDateTime() {
        return LocalDateTime.ofEpochSecond(internalStateReference.get().getTimestamp().longValue(), 0, ZoneOffset.UTC);
    }

    @Override
    public boolean isLocked() {
        if (!this.configuration.isEnabled()) {
            return false;
        }
        if (this.configuration.isStatic()) {
            return true;
        }

        ENMEAMode enmeaMode = internalStateReference.get().getMode();

        return (enmeaMode == ENMEAMode.TwoDimensional || enmeaMode == ENMEAMode.ThreeDimensional);
    }

    @Override
    public String getLastSentence() {
        throw new UnsupportedOperationException("GpsdPositionProvider doesn't return NMEA sentences.");
    }

    @Override
    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {
        stop();

        this.configuration = configuration;
        this.internalStateReference.set(new GpsdInternalState());
        this.gpsDeviceListener = gpsDeviceListener;

        this.gpsEndpoint = new GPSdEndpoint(configuration.getGpsdHost(), configuration.getGpsdPort());
        this.gpsEndpoint.addListener(this);
        try {
            this.gpsEndpoint.watch(true, true);
        } catch (IOException e) {
            logger.info("Unable to start the Gpsd watch mode", e);
        }
    }

    @Override
    public PositionProviderType getType() {
        return PositionProviderType.GPSD;
    }

    @Override
    public void handleATT(ATTObject att) {
        // Noting to do.
    }

    @Override
    public void handleDevice(DeviceObject device) {
        // Noting to do.
    }

    @Override
    public void handleDevices(DevicesObject devices) {
        // Noting to do.
    }

    @Override
    public void handleSKY(SKYObject sky) {
        // Noting to do.
    }

    @Override
    public void handleSUBFRAME(SUBFRAMEObject subframe) {
        // Noting to do.
    }

    @Override
    public synchronized void handleTPV(TPVObject tpv) {

        final GpsdInternalState oldInternalState = internalStateReference.get();

        final GpsdInternalState tpvState = new GpsdInternalState(tpv);

        final GpsdInternalState newState;

        if (!tpvState.isValid()) {
            if (oldInternalState.isValid()) {
                newState = new GpsdInternalState(oldInternalState);
                newState.setMode(tpvState.getMode());
            } else {
                newState = oldInternalState;
            }
        } else {
            newState = tpvState;

            restartCheckTask();
        }

        internalStateReference.set(newState);

        boolean isLastPositionValid = oldInternalState.isValid();

        boolean isNewPositionValid = newState.isValid();

        if (this.gpsDeviceListener != null && isNewPositionValid != isLastPositionValid) {
            this.gpsDeviceListener.onLockStatusChanged(isNewPositionValid);
            logger.info("Lock Status changed: {}", newState);
        }

    }

    private void restartCheckTask() {
        final OptionalInt validityInterval = this.configuration.getGpsdMaxValidityInterval();

        if (validityInterval.isPresent()) {

            checkFuture.cancel(false);

            final long intervalNanos = Duration.ofSeconds(validityInterval.getAsInt()).toNanos();
            checkFuture = executor.schedule(() -> this.checkFixValidity(intervalNanos),
                    validityInterval.getAsInt() + 1L, TimeUnit.SECONDS);
        }
    }

    private synchronized void checkFixValidity(final long validityIntervalNanos) {
        final GpsdInternalState currentState = this.internalStateReference.get();

        final long now = System.nanoTime();

        if (now - currentState.getCreationInstantNanos() > validityIntervalNanos) {
            final TPVObject obj = new TPVObject();
            obj.setMode(ENMEAMode.NoFix);
            handleTPV(obj);
        }
    }

    private Measurement toRadiansMeasurement(double value, double error) {
        return new Measurement(toRadians(Double.isNaN(value) ? 0.0d : value),
                Double.isNaN(error) ? 0.0d : toRadians(error), Unit.rad);
    }

    private Measurement toMetersMeasurement(double value, double error) {
        return new Measurement(Double.isNaN(value) ? 0.0d : value, Double.isNaN(error) ? 0.0d : error, Unit.m);
    }

    private Measurement toMetersPerSecondMeasurement(double value, double error) {
        return new Measurement(Double.isNaN(value) ? 0.0d : value, Double.isNaN(error) ? 0.0d : error, Unit.m_s);
    }

    private class GpsdInternalState {

        private final long creationInstantNanos;

        private double latitude;
        private double latitudeError;
        private double longitude;
        private double longitudeError;
        private double altitude;
        private double altitudeError;
        private double speed;
        private double speedError;
        private double course;
        private double courseError;
        private double timestamp;
        private ENMEAMode mode = ENMEAMode.NotSeen;

        public GpsdInternalState() {
            this.creationInstantNanos = System.nanoTime();
        }

        public GpsdInternalState(final TPVObject tpv) {
            this.setLatitude(tpv.getLatitude());
            this.setLatitudeError(tpv.getLatitudeError());
            this.setLongitude(tpv.getLongitude());
            this.setLongitudeError(tpv.getLongitudeError());
            this.setAltitude(tpv.getAltitude());
            this.setAltitudeError(tpv.getAltitudeError());
            this.setSpeed(tpv.getSpeed());
            this.setSpeedError(tpv.getSpeedError());
            this.setCourse(tpv.getCourse());
            this.setCourseError(tpv.getCourseError());
            this.setTime(tpv.getTimestamp());
            this.setMode(tpv.getMode());
            this.creationInstantNanos = System.nanoTime();
        }

        public GpsdInternalState(final GpsdInternalState other) {
            this.latitude = other.latitude;
            this.latitudeError = other.latitudeError;
            this.longitude = other.longitude;
            this.longitudeError = other.longitudeError;
            this.altitude = other.altitude;
            this.altitudeError = other.altitudeError;
            this.speed = other.speed;
            this.speedError = other.speedError;
            this.course = other.course;
            this.courseError = other.courseError;
            this.timestamp = other.timestamp;
            this.mode = other.mode;
            this.creationInstantNanos = other.creationInstantNanos;
        }

        public void setCourseError(double value) {
            this.courseError = value;
        }

        public void setCourse(double value) {
            this.course = value;
        }

        public void setSpeedError(double value) {
            this.speedError = value;
        }

        public void setSpeed(double value) {
            this.speed = value;
        }

        public void setAltitudeError(double value) {
            this.altitudeError = value;
        }

        public void setAltitude(double value) {
            this.altitude = value;
        }

        public void setLongitudeError(double value) {
            this.longitudeError = value;
        }

        public void setLongitude(double value) {
            this.longitude = value;
        }

        public void setLatitudeError(double value) {
            this.latitudeError = value;
        }

        public void setLatitude(double value) {
            this.latitude = value;
        }

        public void setTime(double value) {
            this.timestamp = value;
        }

        public void setMode(ENMEAMode value) {
            this.mode = value;
        }

        public double getCourseError() {
            return this.courseError;
        }

        public double getCourse() {
            return this.course;
        }

        public double getSpeedError() {
            return this.speedError;
        }

        public double getSpeed() {
            return this.speed;
        }

        public double getAltitudeError() {
            return this.altitudeError;
        }

        public double getAltitude() {
            return this.altitude;
        }

        public double getLongitudeError() {
            return this.longitudeError;
        }

        public double getLongitude() {
            return this.longitude;
        }

        public double getLatitudeError() {
            return this.latitudeError;
        }

        public double getLatitude() {
            return this.latitude;
        }

        public Double getTimestamp() {
            return this.timestamp;
        }

        public ENMEAMode getMode() {
            return this.mode;
        }

        public boolean isValid() {
            return (this.mode == ENMEAMode.TwoDimensional || this.mode == ENMEAMode.ThreeDimensional);
        }

        public long getCreationInstantNanos() {
            return creationInstantNanos;
        }

        @Override
        public String toString() {
            return "GpsdInternalState [latitude=" + latitude + ", latitudeError=" + latitudeError + ", longitude="
                    + longitude + ", longitudeError=" + longitudeError + ", altitude=" + altitude + ", altitudeError="
                    + altitudeError + ", speed=" + speed + ", speedError=" + speedError + ", course=" + course
                    + ", courseError=" + courseError + ", timestamp=" + timestamp + ", mode=" + mode + "]";
        }

    }

}
