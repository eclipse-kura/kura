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
 ******************************************************************************/
package org.eclipse.kura.gpio;

import java.util.Map;
import java.util.Objects;

/**
 * GPIO description as a set of properties.
 *
 * The properties map contains the attributes needed to identify and configure a GPIO pin.
 * The mandatory DISPLAY_NAME_PROPERTY property is used to get a human readable name for the GPIO pin.
 *
 */
public class KuraGPIODescription {

    public static final String DISPLAY_NAME_PROPERTY = "display.name";

    private final Map<String, String> properties;

    public KuraGPIODescription(Map<String, String> properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Properties map cannot be null");
        }
        if (!properties.containsKey(DISPLAY_NAME_PROPERTY) || properties.get(DISPLAY_NAME_PROPERTY).isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory property: " + DISPLAY_NAME_PROPERTY);
        }

        this.properties = properties;
    }
    
    public Map<String, String> getProperties() {
        return this.properties;
    }

    public String getDisplayName() {
        return this.properties.get(DISPLAY_NAME_PROPERTY);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.properties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KuraGPIODescription)) {
            return false;
        }
        KuraGPIODescription other = (KuraGPIODescription) obj;
        return Objects.equals(this.properties, other.properties);
    }

    @Override
    public String toString() {
        return "KuraGPIODescription [properties=" + this.properties + "]";
    }

}
