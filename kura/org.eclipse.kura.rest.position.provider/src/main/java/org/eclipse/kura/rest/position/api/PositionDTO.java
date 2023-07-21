/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import org.osgi.util.position.Position;

public class PositionDTO {

    private Double longitude;
    private Double latitude;
    private Double altitude;
    private Double speed;
    private Double track;

    public PositionDTO(Position position) {
        if (position.getLongitude() != null) {
            this.longitude = Math.toDegrees(position.getLongitude().getValue());
        }

        if (position.getLatitude() != null) {
            this.latitude = Math.toDegrees(position.getLatitude().getValue());
        }

        if (position.getAltitude() != null) {
            this.altitude = position.getAltitude().getValue();
        }
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public Double getTrack() {
        return track;
    }
}
