/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.position;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The NmeaPosition class is similar to org.osgi.util.position.Position but with different units
 * and more fields.<br>
 * The following fields are equivalent to org.osgi.util.position.Position fields but in more typical
 * units (degrees instead of radians):
 * <ul>
 * <li>Longitude in degrees
 * <li>Latitude in degrees
 * <li>Track in degrees
 * <li>Altitude in meters
 * <li>Speed in km/h
 * <li>Speed in mph
 * </ul>
 * It adds to the OSGI Position class the following fields:<br>
 * <ul>
 * <li>Fix Quality (from GPGGA)
 * <li>Number of Satellites (from GPGGA)
 * <li>DOP : Horizontal dilution of position (from GPGGA)
 * <li>3D fix (from GPGSA)
 * <li>PRNs of sats used for fix (from GPGSA)
 * <li>PDOP : Dilution of precision (from GPGSA)
 * <li>HDOP : Horizontal Dilution of precision (from GPGSA)
 * <li>VDOP : Vertical Dilution of precision (from GPGSA)
 * <li>validFix : indicator of fix validity = A:active or V:void
 * <li>latitudeHemisphere : hemisphere of the latitude = N or S
 * <li>longitudeHemisphere : hemisphere of the longitude = E or W
 * </ul>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NmeaPosition {

    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private double track;
    private int fixQuality;
    private int nrSatellites;
    private double mDOP;
    private double mPDOP;
    private double mHDOP;
    private double mVDOP;
    private int m3Dfix;
    private char validFix;
    private char latitudeHemisphere;
    private char longitudeHemisphere;

    public NmeaPosition(double lat, double lon, double alt, double speed, double track) {
        this(lat, lon, alt, speed, track, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, '0', '0', '0');
    }

    public NmeaPosition(double lat, double lon, double alt, double speed, double track, int fixQuality,
            int nrSatellites, double dop, double pdop, double hdop, double vdop, int fix3D) {
        this(lat, lon, alt, speed, track, fixQuality, nrSatellites, dop, pdop, hdop, vdop, fix3D, '0', '0', '0');
    }

    /**
     * @since 2.0
     */
    public NmeaPosition(double lat, double lon, double alt, double speed, double track, int fixQuality,
            int nrSatellites, double dop, double pdop, double hdop, double vdop, int fix3D, char validF, char hemiLat,
            char hemiLon) {
        this.latitude = lat;
        this.longitude = lon;
        this.altitude = alt;
        this.speed = speed;
        this.track = track;
        this.fixQuality = fixQuality;
        this.nrSatellites = nrSatellites;
        this.mDOP = dop;
        this.mPDOP = pdop;
        this.mHDOP = hdop;
        this.mVDOP = vdop;
        this.m3Dfix = fix3D;
        this.validFix = validF;
        this.latitudeHemisphere = hemiLat;
        this.longitudeHemisphere = hemiLon;
    }

    /**
     * Return the latitude in degrees
     */
    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Return the longitude in degrees
     */
    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Return the altitude in meters
     */
    public double getAltitude() {
        return this.altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    /**
     * Return the speed in km/h
     */
    public double getSpeedKmh() {
        return this.speed * 3.6;
    }

    /**
     * Return the speed in mph
     */
    public double getSpeedMph() {
        return this.speed * 2.24;
    }

    /**
     * Return the speed in m/s
     */
    public double getSpeed() {
        return this.speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    /**
     * Return the track in degrees
     */
    public double getTrack() {
        return this.track;
    }

    public void setTrack(double track) {
        this.track = track;
    }

    public int getFixQuality() {
        return this.fixQuality;
    }

    public void setFixQuality(int fixQuality) {
        this.fixQuality = fixQuality;
    }

    public int getNrSatellites() {
        return this.nrSatellites;
    }

    public void setNrSatellites(int nrSatellites) {
        this.nrSatellites = nrSatellites;
    }

    public double getDOP() {
        return this.mDOP;
    }

    public void setDOP(double dop) {
        this.mDOP = dop;
    }

    public double getPDOP() {
        return this.mPDOP;
    }

    public void setPDOP(double pdop) {
        this.mPDOP = pdop;
    }

    public double getHDOP() {
        return this.mHDOP;
    }

    public void setHDOP(double hdop) {
        this.mHDOP = hdop;
    }

    public double getVDOP() {
        return this.mVDOP;
    }

    public void setVDOP(double vdop) {
        this.mVDOP = vdop;
    }

    public int get3Dfix() {
        return this.m3Dfix;
    }

    public void set3Dfix(int fix3D) {
        this.m3Dfix = fix3D;
    }

    /**
     * @since 2.0
     */
    public char getValidFix() {
        return this.validFix;
    }

    /**
     * @since 2.0
     */
    public void setValidFix(char validFix) {
        this.validFix = validFix;
    }

    /**
     * @since 2.0
     */
    public char getLatitudeHemisphere() {
        return this.latitudeHemisphere;
    }

    /**
     * @since 2.0
     */
    public void setLatitudeHemisphere(char latitudeHemisphere) {
        this.latitudeHemisphere = latitudeHemisphere;
    }

    /**
     * @since 2.0
     */
    public char getLongitudeHemisphere() {
        return this.longitudeHemisphere;
    }

    /**
     * @since 2.0
     */
    public void setLongitudeHemisphere(char longitudeHemisphere) {
        this.longitudeHemisphere = longitudeHemisphere;
    }

}
