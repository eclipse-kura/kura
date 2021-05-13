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

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents all the information needed to identify and represent a System Resource.
 * A System Resource is every package or component of the system where the framework is running.
 * This information, for example, can be used to create an inventory of the System Resources
 * installed in the host system running the framework.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.0
 */
@Immutable
@ThreadSafe
@ProviderType
public class SystemResourceInfo {

    private final String name;
    private final String version;
    private final SystemResourceType type;

    /**
     * Creates a new {@link SystemResourceInfo} instance.
     *
     * @param name
     *            a string representing the resource name
     */
    public SystemResourceInfo(String name) {
        this(name, "Unknown", SystemResourceType.UNKNOWN);
    }

    /**
     * Creates a new {@link SystemResourceInfo} instance.
     *
     * @param name
     *            a string representing the resource name
     * @param version
     *            a string representing the resource version
     * @param type
     *            a string representing the resource type
     */
    public SystemResourceInfo(String name, String version, String type) {
        this(name, version, SystemResourceType.valueOf(type));
    }

    /**
     * Creates a new {@link SystemResourceInfo} instance.
     *
     * @param name
     *            a string representing the resource name
     * @param version
     *            a string representing the resource version
     * @param type
     *            a {@link SystemResourceType} representing the resource type
     */
    public SystemResourceInfo(String name, String version, SystemResourceType type) {
        this.name = name;
        this.version = version;
        this.type = type;
    }

    /**
     * Returns the resource name
     *
     * @return the resource name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the resource version
     *
     * @return the resource version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Returns the resource type
     *
     * @return the resource type as {@link SystemResourceType}
     */
    public SystemResourceType getType() {
        return this.type;
    }

    /**
     * Returns the resource type
     *
     * @return the resource type as {@link String}
     */
    public String getTypeString() {
        return this.type.toString();
    }
}
