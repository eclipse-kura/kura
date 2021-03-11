/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.system;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents an extended property group.
 * An extended property group has a name and some properties.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.2
 */
@ProviderType
public class ExtendedPropertyGroup {

    private final String name;
    private final Map<String, String> properties;

    /**
     * Creates a new {@link ExtendedPropertyGroup} instance.
     * 
     * @param name
     *            the group name, must not be null or empty
     * @param properties
     *            the property map, must not be null
     */
    public ExtendedPropertyGroup(final String name, final Map<String, String> properties) {
        if (requireNonNull(name, "name cannot be null").trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        this.name = name;
        this.properties = Collections.unmodifiableMap(requireNonNull(properties, "properties cannot be null"));
    }

    /**
     * Returns the group name.
     * 
     * @return the group name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the group properties.
     * 
     * @return the group properties.
     */
    public Map<String, String> getProperties() {
        return properties;
    }
}
