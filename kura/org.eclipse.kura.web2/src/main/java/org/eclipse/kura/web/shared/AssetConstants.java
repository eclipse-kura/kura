/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared;

/**
 * This is a copy of {@code org.eclipse.kura.asset.provider.AssetConstants}
 */

public enum AssetConstants {

    /** Separator for channel configuration property. */
    CHANNEL_PROPERTY_SEPARATOR("#"),

    /** Prefix for non driver specific properties. */
    CHANNEL_DEFAULT_PROPERTY_PREFIX("+"),

    /** Prohibited characters for channel name */
    CHANNEL_NAME_PROHIBITED_CHARS(CHANNEL_PROPERTY_SEPARATOR.value() + "/+, "),

    /** Asset Description Property to be used in the configuration. */
    ASSET_DESC_PROP("asset.desc"),

    /** Driver PID Property to be used in the configuration. */
    ASSET_DRIVER_PROP("driver.pid"),

    /** Enabled Property to be used in the configuration. */
    ENABLED(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "enabled"),

    /** Name Property to be used in the configuration. */
    NAME(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "name"),

    /** Type Property to be used in the configuration. */
    TYPE(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "type"),

    /** Value type Property to be used in the configuration. */
    VALUE_TYPE(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "value.type");

    /** The value. */
    private String value;

    /**
     * Instantiates a new asset constants.
     *
     * @param value
     *            the value
     */
    private AssetConstants(final String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of the constant
     *
     * @return the string representation
     */
    public String value() {
        return this.value;
    }

}
