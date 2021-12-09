/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.asset.provider;

/**
 * This class contains constants related to the {@link BaseAsset} configuration persistence.
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
    VALUE_TYPE(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "value.type"),

    /** Scale Property to be used in the configuration. */
    VALUE_SCALE(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "scale"),

    /** Gain Property to be used in the configuration. */
    VALUE_OFFSET(CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "offset");

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
