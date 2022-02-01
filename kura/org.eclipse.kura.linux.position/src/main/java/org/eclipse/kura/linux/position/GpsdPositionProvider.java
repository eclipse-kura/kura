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
    private AtomicReference<GpsdInternalState> internalState;
    private PositionServiceOptions configuration;

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
        return new Position(
                toRadiansMeasurement(internalState.get().getLatitude(), internalState.get().getLatitudeError()),
                toRadiansMeasurement(internalState.get().getLongitude(), internalState.get().getLongitudeError()),
                toMetersMeasurement(internalState.get().getAltitude(), internalState.get().getAltitudeError()),
                toMetersPerSecondMeasurement(internalState.get().getSpeed(), internalState.get().getSpeedError()),
                toRadiansMeasurement(internalState.get().getCourse(), internalState.get().getCourseError()));
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        return new NmeaPosition(internalState.get().getLatitude(), internalState.get().getLongitude(),
                internalState.get().getAltitude(), internalState.get().getSpeed(), internalState.get().getCourse());
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
        return LocalDateTime.ofEpochSecond(internalState.get().getTimestamp().longValue(), 0, ZoneOffset.UTC);
    }

    @Override
    public boolean isLocked() {
        if (!this.configuration.isEnabled()) {
            return false;
        }
        if (this.configuration.isStatic()) {
            return true;
        }

        ENMEAMode enmeaMode = internalState.get().getMode();

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
        this.internalState = new AtomicReference<>();
        this.internalState.set(new GpsdInternalState());

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
        internalState.get().setLatitude(tpv.getLatitude());
        internalState.get().setLatitudeError(tpv.getLatitudeError());
        internalState.get().setLongitude(tpv.getLongitude());
        internalState.get().setLongitudeError(tpv.getLongitudeError());
        internalState.get().setAltitude(tpv.getAltitude());
        internalState.get().setAltitudeError(tpv.getAltitudeError());
        internalState.get().setSpeed(tpv.getSpeed());
        internalState.get().setSpeedError(tpv.getSpeedError());
        internalState.get().setCourse(tpv.getCourse());
        internalState.get().setCourseError(tpv.getCourseError());
        internalState.get().setTime(tpv.getTimestamp());
        internalState.get().setMode(tpv.getMode());
    }

    private Measurement toRadiansMeasurement(double value, double error) {
        return new Measurement(toRadians(value), Double.isNaN(error) ? 0.0d : toRadians(error), Unit.rad);
    }

    private Measurement toMetersMeasurement(double value, double error) {
        return new Measurement(value, Double.isNaN(error) ? 0.0d : error, Unit.m);
    }

    private Measurement toMetersPerSecondMeasurement(double value, double error) {
        return new Measurement(value, Double.isNaN(error) ? 0.0d : error, Unit.m_s);
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

    }

}
