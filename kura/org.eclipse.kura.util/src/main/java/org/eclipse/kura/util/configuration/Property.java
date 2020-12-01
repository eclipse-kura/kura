/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.util.configuration;

import java.util.Map;
import java.util.Optional;

public final class Property<T> {

    private final String key;
    private final Optional<T> defaultValue;
    private final Class<? extends T> classz;

    @SuppressWarnings("unchecked")
    public Property(final String key, final T defaultValue) {
        this.key = key;
        this.defaultValue = Optional.of(defaultValue);
        this.classz = (Class<? extends T>) defaultValue.getClass();
    }

    public Property(final String key, final Class<T> classz) {
        this.key = key;
        this.classz = classz;
        this.defaultValue = Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public T get(final Map<String, Object> properties) {
        final Object value = properties.get(this.key);

        if (this.classz.isInstance(value)) {
            return (T) value;
        }
        return this.defaultValue.orElseThrow(() -> new IllegalStateException("configuration property + \"" + key
                + "\" has not been provided set and the property does not have a default value"));
    }

    @SuppressWarnings("unchecked")
    public Optional<T> getOptional(final Map<String, Object> properties) {
        final Object value = properties.get(this.key);

        if (this.classz.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
}