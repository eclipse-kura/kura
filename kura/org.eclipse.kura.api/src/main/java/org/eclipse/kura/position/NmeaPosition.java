/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
 * </ul>
 */
public class NmeaPosition {

    private double m_latitude;
    private double m_longitude;
    private double m_altitude;
    private double m_speed;
    private double m_track;
    private int m_fixQuality;
    private int m_nrSatellites;
    private double m_DOP;
    private double m_PDOP;
    private double m_HDOP;
    private double m_VDOP;
    private int m_3Dfix;

    public NmeaPosition(double lat, double lon, double alt, double speed, double track) {
        this.m_latitude = lat;
        this.m_longitude = lon;
        this.m_altitude = alt;
        this.m_speed = speed;
        this.m_track = track;
    }

    public NmeaPosition(double lat, double lon, double alt, double speed, double track, int fixQuality,
            int nrSatellites, double DOP, double PDOP, double HDOP, double VDOP, int fix3D) {
        this.m_latitude = lat;
        this.m_longitude = lon;
        this.m_altitude = alt;
        this.m_speed = speed;
        this.m_track = track;
        this.m_fixQuality = fixQuality;
        this.m_nrSatellites = nrSatellites;
        this.m_DOP = DOP;
        this.m_PDOP = PDOP;
        this.m_HDOP = HDOP;
        this.m_VDOP = VDOP;
        this.m_3Dfix = fix3D;
    }

    /**
     * Return the latitude in degrees
     */
    public double getLatitude() {
        return this.m_latitude;
    }

    public void setLatitude(double l_latitude) {
        this.m_latitude = l_latitude;
    }

    /**
     * Return the longitude in degrees
     */
    public double getLongitude() {
        return this.m_longitude;
    }

    public void setLongitude(double l_longitude) {
        this.m_longitude = l_longitude;
    }

    /**
     * Return the altitude in meters
     */
    public double getAltitude() {
        return this.m_altitude;
    }

    public void setAltitude(double l_altitude) {
        this.m_altitude = l_altitude;
    }

    /**
     * Return the speed in km/h
     */
    public double getSpeedKmh() {
        return this.m_speed * 3.6;
    }

    /**
     * Return the speed in mph
     */
    public double getSpeedMph() {
        return this.m_speed * 2.24;
    }

    /**
     * Return the speed in m/s
     */
    public double getSpeed() {
        return this.m_speed;
    }

    public void setSpeed(double l_speed) {
        this.m_speed = l_speed;
    }

    /**
     * Return the track in degrees
     */
    public double getTrack() {
        return this.m_track;
    }

    public void setTrack(double l_track) {
        this.m_track = l_track;
    }

    public int getFixQuality() {
        return this.m_fixQuality;
    }

    public void setFixQuality(int m_fixQuality) {
        this.m_fixQuality = m_fixQuality;
    }

    public int getNrSatellites() {
        return this.m_nrSatellites;
    }

    public void setNrSatellites(int m_nrSatellites) {
        this.m_nrSatellites = m_nrSatellites;
    }

    public double getDOP() {
        return this.m_DOP;
    }

    public void setDOP(double m_DOP) {
        this.m_DOP = m_DOP;
    }

    public double getPDOP() {
        return this.m_PDOP;
    }

    public void setPDOP(double m_PDOP) {
        this.m_PDOP = m_PDOP;
    }

    public double getHDOP() {
        return this.m_HDOP;
    }

    public void setHDOP(double m_HDOP) {
        this.m_HDOP = m_HDOP;
    }

    public double getVDOP() {
        return this.m_VDOP;
    }

    public void setVDOP(double m_VDOP) {
        this.m_VDOP = m_VDOP;
    }

    public int get3Dfix() {
        return this.m_3Dfix;
    }

    public void set3Dfix(int m_3Dfix) {
        this.m_3Dfix = m_3Dfix;
    }

}
