/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.linux.position.api;

import org.osgi.util.position.Position;

public class PositionDTO {

    private final double longitude;
    private final double latitude;
    private final double speed;

    public PositionDTO(Position position) {
        this.longitude = position.getLongitude().getValue();
        this.latitude = position.getLatitude().getValue();
        this.speed = position.getSpeed().getValue();
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getSpeed() {
        return speed;
    }
    
}
