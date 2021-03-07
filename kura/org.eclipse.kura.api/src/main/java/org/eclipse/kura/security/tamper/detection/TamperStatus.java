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
package org.eclipse.kura.security.tamper.detection;

import java.util.Collections;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents the device tamper status.
 * This includes a boolean flag that indicates if the device has been tampered or not and some properties.
 * The properties indicated in the {@link TamperDetectionProperties} enumeration are well known.
 *
 * @since 2.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public class TamperStatus {

    private final boolean isDeviceTampered;
    private final Map<String, Object> properties;

    /**
     * Creates a new {@link TamperStatus} instance.
     *
     * @param isDeviceTampered
     *            the current tamper status.
     * @param properties
     *            the additional properties, can be <code>null</code>.
     */
    public TamperStatus(boolean isDeviceTampered, Map<String, Object> properties) {
        this.isDeviceTampered = isDeviceTampered;
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : Collections.emptyMap();
    }

    /**
     * Indicates if the device is tampered or not.
     *
     * @return a <code>boolean</code> indicating if the device is tampered or not
     */
    public boolean isDeviceTampered() {
        return this.isDeviceTampered;
    }

    /**
     * Returns additional properties describing the tamper status.
     *
     * @return the additional properties, the result is never <code>null</code> but can be empty.
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

}
