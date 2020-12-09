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
package org.eclipse.kura.position;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.position.Position;

/**
 * This interface provides methods getting a geographic position.
 * The OSGI Position class represents a geographic location, based on the WGS84 System
 * (World Geodetic System 1984).
 * <p>
 * Position(Measurement lat, Measurement lon, Measurement alt, Measurement speed, Measurement track)
 * <p>
 * The interface also return a NmeaPosition, subclass of Position
 *
 * @see org.eclipse.kura.position.NmeaPosition NmeaPosition
 * @see org.osgi.util.position.Position Position
 * @see org.osgi.util.measurement.Measurement Measurement
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface PositionService {

    /**
     * Returns the current geographic position.<br>
     * The org.osgi.util.measurement.Measurement class is used to represent the values that make up a position:
     * <ul>
     * <li>getLongitude() : returns the longitude of this position in radians.
     * <li>getLatitude() : returns the latitude of this position in radians.
     * <li>getSpeed() : returns the ground speed of this position in meters per second.
     * <li>getTrack() : Returns the track of this position in radians as a compass heading. The track is the
     * extrapolation of
     * previous previously measured positions to a future position.
     * </ul>
     *
     * @see org.osgi.util.position.Position Position
     */
    public Position getPosition();

    /**
     * Returns the current NMEA geographic position.
     * <ul>
     * <li>getLongitude() : returns the longitude of this position in degrees.
     * <li>getLatitude() : returns the latitude of this position in degrees.
     * <li>getSpeedKmh() : returns the ground speed of this position in km/h.
     * <li>getSpeedMph() : returns the ground speed of this position in mph.
     * <li>getTrack() : Returns the track of this position in degrees as a compass heading.
     * </ul>
     *
     * @see org.eclipse.kura.position.NmeaPosition NmeaPosition
     */
    public NmeaPosition getNmeaPosition();

    /**
     * Returns the current NMEA time from GGA or ZDA sentence
     */
    public String getNmeaTime();

    /**
     * Returns the current NMEA date from ZDA sentence
     */
    public String getNmeaDate();

    /**
     * Returns true if a valid geographic position has been received.
     */
    public boolean isLocked();

    /**
     * Returns the last sentence received from the gps.
     */
    public String getLastSentence();

    /**
     * Registers position listener
     *
     * @param listenerId
     *            - listener ID as {@link String}
     * @param positionListener
     *            - position listener as {@link PositionListener}
     */
    public void registerListener(String listenerId, PositionListener positionListener);

    /**
     * Unregisters position listener
     *
     * @param listenerId
     *            - listener ID as {@link String}
     */
    public void unregisterListener(String listenerId);
}
