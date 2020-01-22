/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnecton.raw.mqtt.util;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class Property<T> {

    protected final String key;
    protected final T defaultValue;
    protected final Class<?> valueType;

    public Property(final String key, final T defaultValue) {
        this(key, defaultValue, defaultValue.getClass());
    }

    public Property(final String key, final Class<T> valueType) {
        this(key, null, valueType);
    }

    private Property(final String key, final T defaultValue, final Class<?> valueType) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
    }

    public String getKey() {
        return this.key;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    @SuppressWarnings("unchecked")
    public T get(final Map<String, Object> properties) throws KuraException {
        try {
            return (T) properties.get(this.key);
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, null, null,
                    "invalid property value for " + this.key, e);
        }
    }

    @SuppressWarnings("unchecked")
    public T getOrDefault(final Map<String, Object> properties) throws KuraException {
        final Object value = properties.get(this.key);

        if (this.valueType.isInstance(value)) {
            return (T) value;
        }
        return this.defaultValue;
    }

    public <U> Property<U> map(final Class<U> valueType, final Function<T, U> mapper) {

        final Property<T> orig = this;

        return new Property<U>(this.key, this.defaultValue != null ? mapper.apply(this.defaultValue) : null,
                valueType) {

            @Override
            public U get(final Map<String, Object> properties) throws KuraException {
                return mapper.apply(orig.get(properties));
            }

            @Override
            public U getOrDefault(final Map<String, Object> properties) {
                try {
                    return mapper.apply(orig.getOrDefault(properties));
                } catch (final Exception e) {
                    return this.defaultValue;
                }
            }
        };
    }

    public Property<T> validate(final Predicate<T> validator) {

        final Property<T> orig = this;

        return new Property<T>(this.key, this.defaultValue, this.valueType) {

            @Override
            public T get(final Map<String, Object> properties) throws KuraException {
                final T value = orig.get(properties);

                if (!validator.test(value)) {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, null, null,
                            "Validation failed for property " + this.key);
                }

                return value;
            }

            @Override
            public T getOrDefault(final Map<String, Object> properties) throws KuraException {
                final T value = orig.getOrDefault(properties);

                if (!validator.test(value)) {
                    return this.defaultValue;
                }

                return value;
            }
        };
    }

}