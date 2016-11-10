/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.asset;

/**
 * The Enum AssetConstants contains all the necessary constants required for
 * Kura Asset Model
 */
public enum AssetConstants {

    /** Asset Description Property to be used in the configuration. */
    ASSET_DESC_PROP("asset.desc"),

    /** Driver PID Property to be used in the configuration. */
    ASSET_DRIVER_PROP("driver.pid"),

    /** Asset Severity Level to be used in the configuration. */
    ASSET_SEVERITY_LEVEL("severity.level"),

    /** String denoting a postfix for channel configuration property. */
    CHANNEL_PROPERTY_POSTFIX("."),

    /** String denoting a prefix for channel configuration property. */
    CHANNEL_PROPERTY_PREFIX("CH"),

    /**
     * String denoting a postfix for driver specific channel configuration
     * property.
     */
    DRIVER_PROPERTY_POSTFIX("DRIVER"),

    /** Name Property to be used in the configuration. */
    NAME("name"),

    /** Severity Level Property to be used in the configuration. */
    SEVERITY_LEVEL("severity.level"),

    /** Type Property to be used in the configuration. */
    TYPE("type"),

    /** Value type Property to be used in the configuration. */
    VALUE_TYPE("value.type");

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
