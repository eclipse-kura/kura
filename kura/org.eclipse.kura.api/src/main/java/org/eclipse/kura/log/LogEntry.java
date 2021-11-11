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
package org.eclipse.kura.log;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Identifies a device Log message.
 *
 * A {@code LogEntry} object contains all the information associated to a device log message. The log message can be
 * produced by the operating system, the framework, a bundle in the framework or other generic resource.
 *
 * @since 2.3
 */
@Immutable
@ThreadSafe
@ProviderType
public class LogEntry {

    private final Map<String, Object> properties;

    /**
     * Instantiates a new {@link LogEntry}
     *
     * @param readProperties
     *            a Map representing the properties in a key-value format
     */
    public LogEntry(Map<String, Object> readProperties) {
        requireNonNull(readProperties, "Log properties cannot be null.");

        this.properties = Collections.unmodifiableMap(readProperties);
    }

    /**
     * Returns the log properties
     *
     * @return an unmodifiable Map with the properties associated to this LogEntry instance
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public String toString() {
        return this.properties.toString();
    }
}
