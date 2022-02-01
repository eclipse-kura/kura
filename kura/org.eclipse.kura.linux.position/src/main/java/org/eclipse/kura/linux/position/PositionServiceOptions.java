/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

    private static final Property<String> PROVIDER = new Property<>("provider", PositionProviderType.SERIAL.getValue());
    private static final Property<String> GPSD_HOST = new Property<>("gpsd.host", "localhost");
    private static final Property<Integer> GPSD_PORT = new Property<>("gpsd.port", 2947);

    private final Map<String, Object> properties;

    public PositionServiceOptions(final Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    public boolean isEnabled() {
        return IS_ENABLED.get(this.properties);
    }

    public boolean isStatic() {
        return IS_STATIC.get(this.properties);
    }

    public double getStaticLatitude() {
        return STATIC_LATITUDE.get(this.properties);
    }

    public double getStaticLongitude() {
        return STATIC_LONGITUDE.get(this.properties);
    }

    public double getStaticAltitude() {
        return STATIC_ALTITUDE.get(this.properties);
    }

    public int getBaudRate() {
        return BAUD_RATE.get(this.properties);
    }

    public int getBitsPerWord() {
        return BITS_PER_WORD.get(this.properties);
    }

    public int getStopBits() {
        return STOP_BITS.get(this.properties);
    }

    public int getParity() {
        return PARITY.get(this.properties);
    }

    public String getPort() {
        return PORT.get(this.properties);
    }

    public PositionProviderType getPositionProvider() {
        return PositionProviderType.fromValue(PROVIDER.get(this.properties));
    }

    public String getGpsdHost() {
        return GPSD_HOST.get(this.properties);
    }

    public int getGpsdPort() {
        return GPSD_PORT.get(this.properties);
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
                && getStopBits() == other.getStopBits() && getParity() == other.getParity()
                && getPositionProvider().equals(other.getPositionProvider())
                && getGpsdHost().equals(other.getGpsdHost()) && getGpsdPort() == other.getGpsdPort();
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
            final Object value = properties.get(this.key);

            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return this.defaultValue;
        }
    }
}
