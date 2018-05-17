/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel;

import java.util.Map;

public final class Property<T> {

    private final String key;
    private final T defaultValue;

    public Property(final String key, final T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public T get(final Map<String, Object> properties) {
        final Object value = properties.get(key);

        if (defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
}
