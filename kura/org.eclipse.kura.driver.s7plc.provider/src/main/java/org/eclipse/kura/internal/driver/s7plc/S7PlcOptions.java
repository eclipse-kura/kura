/**
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 *   Eurotech
 */

package org.eclipse.kura.internal.driver.s7plc;

import java.util.Map;

final class S7PlcOptions {

    private static final Property<String> IP_PROP = new Property<>("host.ip", "");
    private static final Property<Boolean> AUTENTICATE_PROP = new Property<>("authenticate", false);
    private static final Property<String> PASSWORD_PROP = new Property<>("password", "");
    private static final Property<Integer> RACK_PROP = new Property<>("rack", 0);
    private static final Property<Integer> SLOT_PROP = new Property<>("slot", 2);
    private static final Property<Integer> MINIMUM_GAP_SIZE_PROP = new Property<>("read.minimum.gap.size", 0);

    private final String ip;
    private final boolean authenticate;
    private final String password;
    private final int rack;
    private final int slot;
    private final int minimumGapSize;

    S7PlcOptions(final Map<String, Object> properties) {
        this.ip = IP_PROP.get(properties);
        this.authenticate = AUTENTICATE_PROP.get(properties);
        this.password = PASSWORD_PROP.get(properties);
        this.rack = RACK_PROP.get(properties);
        this.slot = SLOT_PROP.get(properties);
        this.minimumGapSize = MINIMUM_GAP_SIZE_PROP.get(properties);
    }

    String getIp() {
        return ip;
    }

    boolean shouldAuthenticate() {
        return authenticate;
    }

    String getPassword() {
        return password;
    }

    int getRack() {
        return rack;
    }

    int getSlot() {
        return slot;
    }

    int getMinimumGapSize() {
        return minimumGapSize;
    }

    private static class Property<T> {

        private final String key;
        private final T defaultValue;

        public Property(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(Map<String, Object> properties) {
            final Object value = properties.get(this.key);
            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }

}
