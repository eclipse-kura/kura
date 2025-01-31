/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package ${package};

import java.util.Map;

class Property<T> {

    protected final String key;
    protected final Class<T> valueType;
    protected final T defaultValue;

    @SuppressWarnings("unchecked")
    public Property(final String key, final T defaultValue) {
        this.key = key;
        this.valueType = (Class<T>) defaultValue.getClass();
        this.defaultValue = defaultValue;
    }

    public T get(final Map<String, Object> properties) {

        return getInternal(properties.get(key));
    }

    public T getOrDefault(final Map<String, Object> properties) {
        try {
            return get(properties);
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private T getInternal(final Object value) {
        if (valueType.isInstance(value)) {
            return (T) value;
        }

        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Parameter " + key + " cannot be retrieved from properties");
        }

        return parse((String) value);
    }

    @SuppressWarnings("unchecked")
    private T parse(final String valueString) {
        final Object result;

        if (valueType == Boolean.class) {
            result = Boolean.parseBoolean(valueString);
        } else if (valueType == Short.class) {
            result = Short.parseShort(valueString);
        } else if (valueType == Integer.class) {
            result = Integer.parseInt(valueString);
        } else if (valueType == Long.class) {
            result = Long.parseLong(valueString);
        } else if (valueType == Float.class) {
            result = Float.parseFloat(valueString);
        } else if (valueType == Double.class) {
            result = Double.parseDouble(valueString);
        } else {
            throw new IllegalArgumentException("Cannot parse " + valueType + " from string");
        }

        return (T) result;
    }
}
