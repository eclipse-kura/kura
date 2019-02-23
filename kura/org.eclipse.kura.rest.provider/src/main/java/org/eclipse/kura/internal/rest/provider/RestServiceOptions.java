/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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

    private ConfigurationProperty<String[]> PROPERTY_USER_NAMES = new ConfigurationProperty<>("user.name",
            new String[] {});
    private ConfigurationProperty<String[]> PROPERTY_PASSWORDS = new ConfigurationProperty<>("password",
            new String[] {});
    private ConfigurationProperty<String[]> PROPERTY_ROLES = new ConfigurationProperty<String[]>("roles",
            new String[] {});

    private final Map<String, Object> properties;

    public RestServiceOptions(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String[] getUserNames() {
        return PROPERTY_USER_NAMES.get(properties);
    }

    public String[] getPasswords() {
        return PROPERTY_PASSWORDS.get(properties);
    }

    public String[] getRoles() {
        return PROPERTY_ROLES.get(properties);
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
            return defaultValue;
        }
    }
}
