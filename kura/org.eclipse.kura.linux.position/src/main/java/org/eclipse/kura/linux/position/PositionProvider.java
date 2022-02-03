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

import java.time.LocalDateTime;

import org.eclipse.kura.linux.position.GpsDevice.Listener;
import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.position.Position;

public interface PositionProvider {

    public void start();

    public void stop();

    public Position getPosition();

    public NmeaPosition getNmeaPosition();

    public String getNmeaTime();

    public String getNmeaDate();

    public LocalDateTime getDateTime();

    public boolean isLocked();

    public String getLastSentence();

    public void init(PositionServiceOptions configuration, Listener gpsDeviceListener,
            GpsDeviceAvailabilityListener gpsDeviceAvailabilityListener);

    public PositionProviderType getType();

}
