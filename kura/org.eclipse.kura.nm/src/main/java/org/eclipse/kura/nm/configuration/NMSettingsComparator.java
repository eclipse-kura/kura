/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm.configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.freedesktop.dbus.types.Variant;

public class NMSettingsComparator {

    private NMSettingsComparator() {
        throw new IllegalStateException("Utility class");
    }

    /* This method compares two NM connection settings maps and determines if they are equal.
     *
     * @param newConnectionSettings The new connection settings to compare.
     * @param oldConnectionSettings The old connection settings to compare against.
     * @return true if the settings are considered equal, false otherwise.
     *
     * @throws IllegalArgumentException if either of the connection settings maps is null.
     */
    public static boolean areSettingsEqual(Map<String, Map<String, Variant<?>>> newConnectionSettings,
            Map<String, Map<String, Variant<?>>> oldConnectionSettings) {

        if (Objects.isNull(newConnectionSettings) || Objects.isNull(oldConnectionSettings)) {
            throw new IllegalArgumentException("Connection settings cannot be null");
        }

        if (!newConnectionSettings.keySet().equals(oldConnectionSettings.keySet())) {
            return false;
        }

        for (String topLevelKey : newConnectionSettings.keySet()) {
            Map<String, Variant<?>> newNested = newConnectionSettings.get(topLevelKey);
            Map<String, Variant<?>> oldNested = oldConnectionSettings.get(topLevelKey);

            if (!areNestedMapsEqual(newNested, oldNested)) {
                return false;
            }
        }

        return true;
    }

    private static boolean areNestedMapsEqual(Map<String, Variant<?>> newNested, Map<String, Variant<?>> oldNested) {
        if (Objects.isNull(newNested) || Objects.isNull(oldNested)) {
            // NM settings should never have null nested maps, they would simply be omitted
            // from the top-level map.
            throw new IllegalArgumentException("Nested maps cannot be null");
        }

        if (!newNested.keySet().equals(oldNested.keySet())) {
            return false;
        }

        for (String nestedKey : newNested.keySet()) {
            Variant<?> newVariant = newNested.get(nestedKey);
            Variant<?> oldVariant = oldNested.get(nestedKey);

            if (!areVariantsEqual(newVariant, oldVariant)) {
                return false;
            }
        }

        return true;
    }

    private static boolean areVariantsEqual(Variant<?> newVariant, Variant<?> oldVariant) {
        if (Objects.isNull(newVariant) || Objects.isNull(oldVariant)) {
            // NM settings should never have null variants, they would simply be omitted
            // from the nested map.
            throw new IllegalArgumentException("Variants cannot be null");
        }

        Object newValue = newVariant.getValue();
        Object oldValue = oldVariant.getValue();

        if (newValue instanceof byte[] && oldValue instanceof byte[]) {
            return Arrays.equals((byte[]) newValue, (byte[]) oldValue);
        }

        return Objects.equals(newValue, oldValue);
    }
}
