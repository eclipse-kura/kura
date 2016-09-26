/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.component;

import java.util.Map;

public final class Configuration {

    private Configuration() {
    }

    public static String asString(Map<String, ?> properties, final String key) {
        return asString(properties, key, null);
    }

    public static String asString(Map<String, ?> properties, final String key, final String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof String) {
            return (String) value;
        }

        return defaultValue;
    }

    public static String asStringNotEmpty(Map<String, ?> properties, final String key, final String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (!(value instanceof String)) {
            return defaultValue;
        }

        String stringValue = (String) value;
        if (stringValue.isEmpty()) {
            return defaultValue;
        }

        return stringValue;
    }

    public static Integer asInteger(Map<String, ?> properties, String key) {
        return asInteger(properties, key, null);
    }

    public static Integer asInteger(Map<String, ?> properties, String key, Integer defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    public static int asInt(Map<String, ?> properties, String key, int defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    public static Long asLong(Map<String, ?> properties, String key) {
        return asLong(properties, key, null);
    }

    public static Long asLong(Map<String, ?> properties, String key, Long defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return defaultValue;
    }

    public static long asLong(Map<String, ?> properties, String key, long defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return defaultValue;
    }

    public static Double asDouble(Map<String, ?> properties, String key) {
        return asDouble(properties, key, null);
    }

    public static Double asDouble(Map<String, ?> properties, String key, Double defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return defaultValue;
    }

    public static double asDouble(Map<String, ?> properties, String key, double defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return defaultValue;
    }

    /**
     * Get a boolean parameter from the configuration
     *
     * @param properties
     *            the configuration
     * @param key
     *            the key to fetch
     * @return the boolean value from the configuration, or {@code false} if the property set is {@code null}, the property is not set or it is not boolean
     */
    public static boolean asBoolean(Map<String, ?> properties, String key) {
        return asBoolean(properties, key, false);
    }

    public static Boolean asBoolean(Map<String, ?> properties, String key, Boolean defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return defaultValue;
    }

    public static boolean asBoolean(Map<String, ?> properties, String key, boolean defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return defaultValue;
    }
}
