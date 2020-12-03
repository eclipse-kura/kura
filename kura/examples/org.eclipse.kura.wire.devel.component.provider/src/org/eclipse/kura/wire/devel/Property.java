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
