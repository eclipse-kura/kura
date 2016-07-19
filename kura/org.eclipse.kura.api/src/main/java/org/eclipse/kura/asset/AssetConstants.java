/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.asset;

/**
 * The Enum AssetConstants contains all the necessary constants required for
 * Kura Asset Model
 */
public enum AssetConstants {

	/** Asset Description Property to be used in the configuration. */
	ASSET_DESC_PROP("asset.desc"),

	/** Driver Name Property to be used in the configuration. */
	ASSET_DRIVER_PROP("driver.id"),

	/** Asset Name Property to be used in the configuration. */
	ASSET_ID_PROP("asset.name"),

	/** String denoting a postfix for channel configuration property. */
	CHANNEL_PROPERTY_POSTFIX("."),

	/** String denoting a prefix for channel configuration property. */
	CHANNEL_PROPERTY_PREFIX("CH"),

	/**
	 * String denoting a prefix for driver specific channel configuration
	 * property.
	 */
	DRIVER_PROPERTY_PREFIX("DRIVER"),

	/** Name Property to be used in the configuration. */
	NAME("name"),

	/** The timer event. */
	TIMER_EVENT("TimerEvent"),

	/** Type Property to be used in the configuration. */
	TYPE("type"),

	/** Value type Property to be used in the configuration. */
	VALUE_TYPE("value.type");

	/** The value. */
	String value;

	/**
	 * Instantiates a new asset constants.
	 *
	 * @param value
	 *            the value
	 */
	AssetConstants(final String value) {
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
