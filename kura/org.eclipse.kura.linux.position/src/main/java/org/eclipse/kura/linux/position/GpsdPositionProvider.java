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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    private GPSdEndpoint gpsEndpoint;
    private AtomicReference<GpsdInternalState> internalStateReference;
    private PositionServiceOptions configuration;

    private Listener gpsDeviceListener;

    @Override
    public void start() {
        if (this.gpsEndpoint != null) {
            this.gpsEndpoint.start();
        }
    }

    @Override
    public void stop() {
        if (this.gpsEndpoint != null) {
            this.gpsEndpoint.stop();
        }
    }

    @Override
    public Position getPosition() {
        GpsdInternalState internalState = internalStateReference.get();
        if (internalState == null) {
            return new Position(toRadiansMeasurement(0, 0), toRadiansMeasurement(0, 0), toMetersMeasurement(0, 0),
                    toMetersPerSecondMeasurement(0, 0), toRadiansMeasurement(0, 0));
        }
        return new Position(toRadiansMeasurement(internalState.getLatitude(), internalState.getLatitudeError()),
                toRadiansMeasurement(internalState.getLongitude(), internalState.getLongitudeError()),
                toMetersMeasurement(internalState.getAltitude(), internalState.getAltitudeError()),
                toMetersPerSecondMeasurement(internalState.getSpeed(), internalState.getSpeedError()),
                toRadiansMeasurement(internalState.getCourse(), internalState.getCourseError()));
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        GpsdInternalState internalState = internalStateReference.get();
        if (internalState == null) {
            return null;
        }
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

        this.configuration = configuration;
        this.internalStateReference = new AtomicReference<>();
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
    public void handleTPV(TPVObject tpv) {

        GpsdInternalState internalState = new GpsdInternalState();

        internalState.setLatitude(tpv.getLatitude());
        internalState.setLatitudeError(tpv.getLatitudeError());
        internalState.setLongitude(tpv.getLongitude());
        internalState.setLongitudeError(tpv.getLongitudeError());
        internalState.setAltitude(tpv.getAltitude());
        internalState.setAltitudeError(tpv.getAltitudeError());
        internalState.setSpeed(tpv.getSpeed());
        internalState.setSpeedError(tpv.getSpeedError());
        internalState.setCourse(tpv.getCourse());
        internalState.setCourseError(tpv.getCourseError());
        internalState.setTime(tpv.getTimestamp());
        internalState.setMode(tpv.getMode());

        boolean isLastPositionValid = this.internalStateReference.get() != null
                && this.internalStateReference.get().isValid();
        
        boolean isNewPositionValid = internalState.isValid();

        if (this.gpsDeviceListener != null && isNewPositionValid != isLastPositionValid) {
            this.gpsDeviceListener.onLockStatusChanged(isNewPositionValid);
            logger.info("Lock Status changed: {}", internalState);
        }

        internalStateReference.set(internalState);
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

        @Override
        public String toString() {
            return "GpsdInternalState [latitude=" + latitude + ", latitudeError=" + latitudeError + ", longitude="
                    + longitude + ", longitudeError=" + longitudeError + ", altitude=" + altitude + ", altitudeError="
                    + altitudeError + ", speed=" + speed + ", speedError=" + speedError + ", course=" + course
                    + ", courseError=" + courseError + ", timestamp=" + timestamp + ", mode=" + mode + "]";
        }

    }

}
