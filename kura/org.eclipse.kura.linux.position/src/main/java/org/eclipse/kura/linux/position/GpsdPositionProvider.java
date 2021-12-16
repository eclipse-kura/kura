/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.json.JSONException;
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
import de.taimos.gpsd4java.types.SKYObject;
import de.taimos.gpsd4java.types.TPVObject;
import de.taimos.gpsd4java.types.subframes.SUBFRAMEObject;

public class GpsdPositionProvider implements PositionProvider, IObjectListener {

    private static final Logger logger = LoggerFactory.getLogger(GpsdPositionProvider.class);

    private GPSdEndpoint gpsEndpoint;

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

    @Override
    public void start() {
        this.gpsEndpoint.start();
    }

    @Override
    public void stop() {
        this.gpsEndpoint.stop();
    }

    @Override
    public Position getPosition() {
        return new Position(toRadiansMeasurement(getLatitude(), getLatitudeError()),
                toRadiansMeasurement(getLongitude(), getLongitudeError()),
                toRadiansMeasurement(getAltitude(), getAltitudeError()),
                toRadiansMeasurement(getSpeed(), getSpeedError()), toRadiansMeasurement(getCourse(), getCourseError()));
    }

    @Override
    public NmeaPosition getNmeaPosition() {
       return new NmeaPosition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    @Override
    public String getNmeaTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNmeaDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getLastSentence() {
        throw new UnsupportedOperationException("GpsdPositionProvider doesn't return NMEA sentences.");
    }

    @Override
    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener) {

        this.gpsEndpoint = new GPSdEndpoint(configuration.getGpsdHost(), configuration.getGpsdPort());
        this.gpsEndpoint.addListener(this);
        try {
            this.gpsEndpoint.watch(true, true);
        } catch (JSONException | IOException e) {
            logger.info("Unable to start the Gpsd watch mode", e);
        }
    }

    @Override
    public PositionProviderType getType() {
        return PositionProviderType.GPSD;
    }

    @Override
    public void handleATT(ATTObject att) {

    }

    @Override
    public void handleDevice(DeviceObject device) {

    }

    @Override
    public void handleDevices(DevicesObject devices) {

    }

    @Override
    public void handleSKY(SKYObject sky) {

    }

    @Override
    public void handleSUBFRAME(SUBFRAMEObject subframe) {

    }

    @Override
    public void handleTPV(TPVObject tpv) {
        this.lastPosition = new Position(toRadiansMeasurement(tpv.getLatitude(), tpv.getLatitudeError()),
                toRadiansMeasurement(tpv.getLongitude(), tpv.getLongitudeError()),
                toRadiansMeasurement(tpv.getAltitude(), tpv.getAltitudeError()),
                toRadiansMeasurement(tpv.getSpeed(), tpv.getSpeedError()),
                toRadiansMeasurement(tpv.getCourse(), tpv.getCourseError()));
    }

    private Measurement toRadiansMeasurement(double value, double error) {
        return new Measurement(toRadians(value), toRadians(error), Unit.rad);
    }

    private double getCourseError() {
        return this.courseError;
    }

    private double getCourse() {
        return this.course;
    }

    private double getSpeedError() {
        return this.speedError;
    }

    private double getSpeed() {
        return this.speed;
    }

    private double getAltitudeError() {
        return this.altitudeError;
    }

    private double getAltitude() {
        return this.altitude;
    }

    private double getLongitudeError() {
        return this.longitudeError;
    }

    private double getLongitude() {
        return this.longitude;
    }

    private double getLatitudeError() {
        return this.latitudeError;
    }

    private double getLatitude() {
        return this.latitude;
    }

}
