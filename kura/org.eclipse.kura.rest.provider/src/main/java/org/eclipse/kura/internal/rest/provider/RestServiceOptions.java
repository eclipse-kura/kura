/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.provider;

import java.util.Map;

public class RestServiceOptions {

    private final ConfigurationProperty<String[]> propertyUserNames = new ConfigurationProperty<>("user.name",
            new String[] {});
    private final ConfigurationProperty<String[]> propertyPasswords = new ConfigurationProperty<>("password",
            new String[] {});
    private final ConfigurationProperty<String[]> propertyRoles = new ConfigurationProperty<>("roles", new String[] {});

    private final Map<String, Object> properties;

    public RestServiceOptions(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String[] getUserNames() {
        return this.propertyUserNames.get(this.properties);
    }

    public String[] getPasswords() {
        return this.propertyPasswords.get(this.properties);
    }

    public String[] getRoles() {
        return this.propertyRoles.get(this.properties);
    }

    private static class ConfigurationProperty<T> {

        private final String key;
        private final T defaultValue;

        public ConfigurationProperty(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(Map<String, Object> properties) {
            final Object value = properties.get(this.key);
            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return this.defaultValue;
        }
    }
}
