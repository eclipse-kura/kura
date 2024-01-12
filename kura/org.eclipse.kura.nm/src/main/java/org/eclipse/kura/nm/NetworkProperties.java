/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.kura.configuration.Password;

public class NetworkProperties {

    private final Map<String, Object> properties;

    public NetworkProperties(Map<String, Object> rawProperties) {
        this.properties = Objects.requireNonNull(rawProperties);
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public <T> T get(Class<T> clazz, String key, Object... args) {
        String formattedKey = String.format(key, args);

        if (!this.properties.containsKey(formattedKey)) {
            throw new NoSuchElementException(String.format("The \"%s\" key is missing.", formattedKey));
        }

        Object rawValue = this.properties.get(formattedKey);
        if (Objects.isNull(rawValue)) {
            throw new NoSuchElementException(String.format("The \"%s\" key contains a null value.", formattedKey));
        }

        if (!clazz.isAssignableFrom(rawValue.getClass())) {
            // Criteria: there's no such element in the map that matches the requested type (clazz)
            throw new NoSuchElementException(
                    String.format("The \"%s\" key contains a value of type \"%s\" (requested type: \"%s\").",
                            formattedKey, rawValue.getClass().getName(), clazz.getName()));
        }

        if (clazz == String.class || clazz == Password.class) {
            String value = "";

            if (clazz == String.class) {
                value = String.class.cast(rawValue);
            } else {
                Password pwValue = Password.class.cast(rawValue);
                value = pwValue.toString();
            }

            if (value.isEmpty()) {
                throw new NoSuchElementException(
                        String.format("The \"%s\" key contains an empty string value.", formattedKey));
            }
        }

        return clazz.cast(this.properties.get(formattedKey));
    }

    public <T> Optional<T> getOpt(Class<T> clazz, String key, Object... args) {
        String formattedKey = String.format(key, args);

        if (!this.properties.containsKey(formattedKey)) {
            return Optional.empty();
        }

        Object rawValue = this.properties.get(formattedKey);
        if (Objects.isNull(rawValue)) {
            return Optional.empty();
        }

        if (!clazz.isAssignableFrom(rawValue.getClass())) {
            // Criteria: there's no such element in the map that matches the requested type (clazz)
            return Optional.empty();
        }

        if (clazz == String.class || clazz == Password.class) {
            String value = "";

            if (clazz == String.class) {
                value = String.class.cast(rawValue);
            } else {
                Password pwValue = Password.class.cast(rawValue);
                value = pwValue.toString();
            }

            if (value.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(clazz.cast(rawValue));
    }

    public List<String> getStringList(String key, Object... args) {
        String commaSeparatedString = get(String.class, key, args);

        List<String> stringList = new ArrayList<>();
        Pattern comma = Pattern.compile(",");
        if (Objects.nonNull(commaSeparatedString) && !commaSeparatedString.isEmpty()) {
            comma.splitAsStream(commaSeparatedString).filter(s -> !s.trim().isEmpty()).map(String::trim)
                    .forEach(stringList::add);
        }

        return stringList;
    }

    public Optional<List<String>> getOptStringList(String key, Object... args) {
        Optional<String> commaSeparatedString = getOpt(String.class, key, args);
        if (!commaSeparatedString.isPresent() || commaSeparatedString.get().isEmpty()) {
            return Optional.empty();
        }

        List<String> stringList = new ArrayList<>();
        Pattern comma = Pattern.compile(",");
        comma.splitAsStream(commaSeparatedString.get()).filter(s -> !s.trim().isEmpty()).map(String::trim)
                .forEach(stringList::add);

        return Optional.of(stringList);
    }
}
