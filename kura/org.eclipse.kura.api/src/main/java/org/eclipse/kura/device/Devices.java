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
package org.eclipse.kura.device;

import java.util.Map;

import org.eclipse.kura.type.DataType;

/**
 * The Class Devices is an utility class to provide useful static factory
 * methods for devices and drivers
 */
public final class Devices {
	
	/** Constructor */
	private Devices() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Creates a new channel with the provided values
	 *
	 * @param name
	 *            the name of the channel
	 * @param type
	 *            the type of the channel
	 * @param valueType
	 *            the value type of the channel
	 * @param configuration
	 *            the configuration to be read
	 * @return the channel
	 */
	public static Channel newChannel(final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration) {
		return new Channel(name, type, valueType, configuration);
	}

	/**
	 * Prepares new device record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the device record
	 */
	public static DeviceRecord newDeviceRecord(final String channelName) {
		return new DeviceRecord(channelName);
	}

	/**
	 * Prepares new driver record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the driver record
	 */
	public static DriverRecord newDriverRecord(final String channelName) {
		return new DriverRecord(channelName);
	}

}
