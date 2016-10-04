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

/**
 * A few helper methods for consuming configuration from a properties map
 */
public final class Configuration {

    private Configuration() {
    }

    /**
     * Get a string value, defaulting to {@code null}
     *
     * @param properties
     *            the properties to read from, may be {@code null}
     * @param key
     *            the key to read, may be {@code null}
     * @return the string value or {@code null}
     */
    public static String asString(final Map<String, ?> properties, final String key) {
        return asString(properties, key, null);
    }

    /**
     * Get a string value
     *
     * @param properties
     *            the properties to read from, may be {@code null}
     * @param key
     *            the key to read, may be {@code null}
     * @param defaultValue
     *            the default value, may be {@code null}
     * @return the string value or the default value
     */
    public static String asString(final Map<String, ?> properties, final String key, final String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof String) {
            return (String) value;
        }

        return defaultValue;
    }

    /**
     * Get a string value, unless it is empty
     * <p>
     * If the properties map contains the string, but the string is empty by {@link String#isEmpty()}, then
     * also the default value will be returned.
     * </p>
     *
     * @param properties
     *            the properties to read from, may be {@code null}
     * @param key
     *            the key to read, may be {@code null}
     * @param defaultValue
     *            the default value, may be {@code null}
     * @return the string value or the default value
     */
    public static String asStringNotEmpty(final Map<String, ?> properties, final String key, final String defaultValue) {
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

    public static Integer asInteger(final Map<String, ?> properties, final String key) {
        return asInteger(properties, key, null);
    }

    public static Integer asInteger(final Map<String, ?> properties, final String key, final Integer defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    public static int asInt(final Map<String, ?> properties, final String key, final int defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    public static Long asLong(final Map<String, ?> properties, final String key) {
        return asLong(properties, key, null);
    }

    public static Long asLong(final Map<String, ?> properties, final String key, final Long defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return defaultValue;
    }

    public static long asLong(final Map<String, ?> properties, final String key, final long defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return defaultValue;
    }

    public static Double asDouble(final Map<String, ?> properties, final String key) {
        return asDouble(properties, key, null);
    }

    public static Double asDouble(final Map<String, ?> properties, final String key, final Double defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return defaultValue;
    }

    public static double asDouble(final Map<String, ?> properties, final String key, final double defaultValue) {
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
     * @return the boolean value from the configuration, or {@code false} if the property set is {@code null}, the
     *         property is not set or it is not boolean
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
