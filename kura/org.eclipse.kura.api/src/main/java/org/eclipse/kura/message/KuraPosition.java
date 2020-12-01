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
 ******************************************************************************/
package org.eclipse.kura.message;

import java.util.Date;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraPosition is a data structure to capture a geo location. It can be
 * associated to a KuraPayload to geotag a KuraMessage before sending to a
 * remote cloud platform. Refer to the description of each of the fields for more
 * information on the model of KuraPosition.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraPosition {

    /**
     * Longitude of this position in degrees. This is a mandatory field.
     */
    private Double longitude;

    /**
     * Latitude of this position in degrees. This is a mandatory field.
     */
    private Double latitude;

    /**
     * Altitude of the position in meters.
     */
    private Double altitude;

    /**
     * Dilution of the precision (DOP) of the current GPS fix.
     */
    private Double precision;

    /**
     * Heading (direction) of the position in degrees
     */
    private Double heading;

    /**
     * Speed for this position in meter/sec.
     */
    private Double speed;

    /**
     * Timestamp extracted from the GPS system
     */
    private Date timestamp;

    /**
     * Number of satellites seen by the systems
     */
    private Integer satellites;

    /**
     * Status of GPS system: 1 = no GPS response, 2 = error in response, 4 =
     * valid.
     */
    private Integer status;

    public KuraPosition() {
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Double getAltitude() {
        return this.altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public Double getPrecision() {
        return this.precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public Double getHeading() {
        return this.heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public Double getSpeed() {
        return this.speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getSatellites() {
        return this.satellites;
    }

    public void setSatellites(int satellites) {
        this.satellites = satellites;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
