/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.linux.position;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.comm.CommURI;

public class PositionServiceOptions {

    private static final Property<Boolean> IS_ENABLED = new Property<>("enabled", false);
    private static final Property<Boolean> IS_STATIC = new Property<>("static", false);
    private static final Property<Double> STATIC_LATITUDE = new Property<>("latitude", 0.0d);
    private static final Property<Double> STATIC_LONGITUDE = new Property<>("longitude", 0.0d);
    private static final Property<Double> STATIC_ALTITUDE = new Property<>("altitude", 0.0d);
    private static final Property<String> PORT = new Property<>("port", "");
    private static final Property<Integer> BAUD_RATE = new Property<>("baudRate", 115200);
    private static final Property<Integer> BITS_PER_WORD = new Property<>("bitsPerWord", 8);
    private static final Property<Integer> STOP_BITS = new Property<>("stopBits", 1);
    private static final Property<Integer> PARITY = new Property<>("parity", 0);

    private final Map<String, Object> properties;

    public PositionServiceOptions(final Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    public boolean isEnabled() {
        return IS_ENABLED.get(properties);
    }

    public boolean isStatic() {
        return IS_STATIC.get(properties);
    }

    public double getStaticLatitude() {
        return STATIC_LATITUDE.get(properties);
    }

    public double getStaticLongitude() {
        return STATIC_LONGITUDE.get(properties);
    }

    public double getStaticAltitude() {
        return STATIC_ALTITUDE.get(properties);
    }

    public int getBaudRate() {
        return BAUD_RATE.get(properties);
    }

    public int getBitsPerWord() {
        return BITS_PER_WORD.get(properties);
    }

    public int getStopBits() {
        return STOP_BITS.get(properties);
    }

    public int getParity() {
        return PARITY.get(properties);
    }

    public String getPort() {
        return PORT.get(properties);
    }

    public CommURI getGpsDeviceUri() {
        if (getPort().isEmpty()) {
            return null;
        }

        return new CommURI.Builder(getPort()).withBaudRate(getBaudRate()).withDataBits(getBitsPerWord())
                .withStopBits(getStopBits()).withParity(getParity()).build();
    }

    @Override
    public int hashCode() {
        // just to stop SonarLint complaining about missing hashCode()
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PositionServiceOptions)) {
            return false;
        }
        final PositionServiceOptions other = (PositionServiceOptions) obj;

        return isEnabled() == other.isEnabled() && isStatic() == other.isStatic()
                && getStaticLatitude() == other.getStaticLatitude()
                && getStaticLongitude() == other.getStaticLongitude()
                && getStaticAltitude() == other.getStaticAltitude() && getPort().equals(other.getPort())
                && getBaudRate() == other.getBaudRate() && getBitsPerWord() == other.getBitsPerWord()
                && getStopBits() == other.getStopBits() && getParity() == other.getParity();

    }

    private static final class Property<T> {

        private final String key;
        private final T defaultValue;

        Property(final String key, final T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        T get(final Map<String, Object> properties) {
            final Object value = properties.get(key);

            if (defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }
}
