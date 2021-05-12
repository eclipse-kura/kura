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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines some well known keys for the properties returned by {@link TamperStatus#getProperties()}.
 *
 * @since 3.0
 */
@ProviderType
public enum TamperDetectionProperties {

    /**
     * Allows to specify a timestamp for the tamper event, if known.
     * The property value must be a <code>long</code> representing the UNIX timestamp of the event.
     */
    TIMESTAMP_PROPERTY_KEY("timestamp");

    private final String value;

    private TamperDetectionProperties(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
