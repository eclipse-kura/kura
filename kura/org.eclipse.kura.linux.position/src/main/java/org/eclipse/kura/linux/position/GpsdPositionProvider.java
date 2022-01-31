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

    private AtomicReference<Double> latitude = new AtomicReference<>();
    private AtomicReference<Double> latitudeError = new AtomicReference<>();
    private AtomicReference<Double> longitude = new AtomicReference<>();
    private AtomicReference<Double> longitudeError = new AtomicReference<>();
    private AtomicReference<Double> altitude = new AtomicReference<>();
    private AtomicReference<Double> altitudeError = new AtomicReference<>();
    private AtomicReference<Double> speed = new AtomicReference<>();
    private AtomicReference<Double> speedError = new AtomicReference<>();
    private AtomicReference<Double> course = new AtomicReference<>();
    private AtomicReference<Double> courseError = new AtomicReference<>();
    private AtomicReference<Double> timestamp = new AtomicReference<>();
    private AtomicReference<ENMEAMode> mode = new AtomicReference<>();

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
        return new Position(toRadiansMeasurement(getLatitude(), getLatitudeError()),
                toRadiansMeasurement(getLongitude(), getLongitudeError()),
                toMetersMeasurement(getAltitude(), getAltitudeError()),
                toMetersPerSecondMeasurement(getSpeed(), getSpeedError()),
                toRadiansMeasurement(getCourse(), getCourseError()));
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        return new NmeaPosition(getLatitude(), getLongitude(), getAltitude(), getSpeed(), getCourse());
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
        return LocalDateTime.ofEpochSecond(getTimestamp().longValue(), 0, ZoneOffset.UTC);
    }

    @Override
    public boolean isLocked() {
        if (!this.configuration.isEnabled()) {
            return false;
        }
        if (this.configuration.isStatic()) {
            return true;
        }

        return (getMode() == ENMEAMode.TwoDimensional || getMode() == ENMEAMode.ThreeDimensional);
    }

    @Override
    public String getLastSentence() {
        throw new UnsupportedOperationException("GpsdPositionProvider doesn't return NMEA sentences.");
    }

    @Override
    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {

        this.configuration = configuration;

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
        setLatitude(tpv.getLatitude());
        setLatitudeError(tpv.getLatitudeError());
        setLongitude(tpv.getLongitude());
        setLongitudeError(tpv.getLongitudeError());
        setAltitude(tpv.getAltitude());
        setAltitudeError(tpv.getAltitudeError());
        setSpeed(tpv.getSpeed());
        setSpeedError(tpv.getSpeedError());
        setCourse(tpv.getCourse());
        setCourseError(tpv.getCourseError());
        setTime(tpv.getTimestamp());
        setMode(tpv.getMode());
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

    private void setCourseError(double value) {
        this.courseError.set(value);
    }

    private void setCourse(double value) {
        this.course.set(value);
    }

    private void setSpeedError(double value) {
        this.speedError.set(value);
    }

    private void setSpeed(double value) {
        this.speed.set(value);
    }

    private void setAltitudeError(double value) {
        this.altitudeError.set(value);
    }

    private void setAltitude(double value) {
        this.altitude.set(value);
    }

    private void setLongitudeError(double value) {
        this.longitudeError.set(value);
    }

    private void setLongitude(double value) {
        this.longitude.set(value);
    }

    private void setLatitudeError(double value) {
        this.latitudeError.set(value);
    }

    private void setLatitude(double value) {
        this.latitude.set(value);
    }

    private void setTime(double value) {
        this.timestamp.set(value);
    }

    private void setMode(ENMEAMode value) {
        this.mode.set(value);
    }

    private double getCourseError() {
        return this.courseError.get();
    }

    private double getCourse() {
        return this.course.get();
    }

    private double getSpeedError() {
        return this.speedError.get();
    }

    private double getSpeed() {
        return this.speed.get();
    }

    private double getAltitudeError() {
        return this.altitudeError.get();
    }

    private double getAltitude() {
        return this.altitude.get();
    }

    private double getLongitudeError() {
        return this.longitudeError.get();
    }

    private double getLongitude() {
        return this.longitude.get();
    }

    private double getLatitudeError() {
        return this.latitudeError.get();
    }

    private double getLatitude() {
        return this.latitude.get();
    }

    private Double getTimestamp() {
        return this.timestamp.get();
    }

    private ENMEAMode getMode() {
        return this.mode.get();
    }

}
