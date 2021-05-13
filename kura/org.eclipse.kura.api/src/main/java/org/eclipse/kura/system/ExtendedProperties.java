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
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a set of properties that can be returned by custom {@link SystemService} implementations to provide
 * additional
 * informations about the system that are not specified by the {@link SystemService} interface.
 * The extended properties are organized in named groups, as specified by the {@link ExtendedPropertyGroup}
 * interface.
 * The number of returned groups, their names and contained property keys and values are not specified by Kura APIs.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.0
 */
@ProviderType
public class ExtendedProperties {

    private final String version;
    private final List<ExtendedPropertyGroup> groups;

    /**
     * Creates a new {@link ExtendedProperties} instance.
     * 
     * @param version
     *            the version
     * @param groups
     *            the property groups
     */
    public ExtendedProperties(final String version, final List<ExtendedPropertyGroup> groups) {
        if (requireNonNull(version, "version cannot be null").trim().isEmpty()) {
            throw new IllegalArgumentException("version cannot be empty");
        }

        this.version = version;
        this.groups = Collections.unmodifiableList(requireNonNull(groups, "groups cannot be null"));
    }

    /**
     * Returns the version
     * 
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the property group list
     * 
     * @return the property group list
     */
    public List<ExtendedPropertyGroup> getPropertyGroups() {
        return groups;
    }
}
