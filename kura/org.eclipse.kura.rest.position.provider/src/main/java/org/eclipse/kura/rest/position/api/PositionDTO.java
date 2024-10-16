/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.position.api;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.kura.position.GNSSType;
import org.osgi.util.position.Position;

public class PositionDTO {

    private Double longitude;
    private Double latitude;
    private Double altitude;
    private Double speed;
    private Double track;
    private Set<String> gnssType;

    public PositionDTO(Position position, Set<GNSSType> gnssTypeSet) {
        if (position.getLongitude() != null) {
            this.longitude = Math.toDegrees(position.getLongitude().getValue());
        }

        if (position.getLatitude() != null) {
            this.latitude = Math.toDegrees(position.getLatitude().getValue());
        }

        if (position.getAltitude() != null) {
            this.altitude = position.getAltitude().getValue();
        }

        if (position.getSpeed() != null) {
            this.speed = position.getSpeed().getValue();
        }

        if (position.getTrack() != null) {
            this.track = Math.toDegrees(position.getTrack().getValue());
        }

        if (gnssTypeSet != null) {
            this.gnssType = new HashSet<>();
            for (GNSSType type : gnssTypeSet) {
                this.gnssType.add(type.getValue());
            }
        }
    }

    /**
     * Returns the longitude of this position in degrees.
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Returns the latitude of this position in degrees.
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Returns the altitude of this position in meters.
     */
    public Double getAltitude() {
        return altitude;
    }

    /**
     * Returns the ground speed of this position in meters per second.
     */
    public Double getSpeed() {
        return speed;
    }

    /**
     * Returns the track of this position in degrees as a compass heading.
     */
    public Double getTrack() {
        return track;
    }

    /*
     * Returns the GNSS Type used to retrieve position information
     */
    public Set<String> getGnssType() {
        return gnssType;
    }
}
