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
package org.eclipse.kura.device.internal;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.HashSet;
import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.device.Channel;
import org.eclipse.kura.device.ChannelType;
import org.eclipse.kura.device.util.Devices;
import org.eclipse.kura.type.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class DeviceConfiguration is responsible for retrieving channels from
 * properties. The properties must conform to the following specifications. The
 * properties must have the following.
 *
 * <ul>
 * <li>CHx.</li> where x is any no (eg: CH103432214. / CH1124124215. /
 * CH5654364436. etc) (Note it the format includes a "." at the end)
 * <li>map object containing a channel configuration</li>
 * <li>the value associated with <b><i>driver.id</i></b> key in the map denotes
 * the driver instance name to be consumed by this device</li>
 * <li>A value associated with key <b><i>device.name</i></b> must be present to
 * denote the device name</li>
 * <li>A value associated with <b><i>device.desc</i></b> key denotes the device
 * description</li>
 * </ul>
 *
 * The representation in the provided properties signifies a single channel and
 * it should conform to the following specification.
 *
 * The properties should contain the following keys
 * <ul>
 * <li>name</li>
 * <li>type</li>
 * <li>value_type</li>
 * <li>channel_config</li>
 * </ul>
 *
 * The key <b><i>name</i></b> must be String. The key <b><i>value_type</i></b>
 * must be in one of the following
 *
 * <ul>
 * <li>INTEGER</li>
 * <li>BOOLEAN</li>
 * <li>BYTE</li>
 * <li>DOUBLE</li>
 * <li>LONG</li>
 * <li>SHORT</li>
 * <li>STRING</li>
 * <li>BYTE_ARRAY</li>
 * </ul>
 *
 * The channel {@code type} should be one of the following
 *
 * <ul>
 * <li>READ</li>
 * <li>WRITE</li>
 * <li>READ_WRITE</li>
 * </ul>
 *
 * The {@code channel_config} is a map which provides all the channel specific
 * settings.
 */
public final class DeviceConfiguration {

	/** String denoting a postfix for channel configuration property */
	public static final String CHANNEL_PROPERTY_POSTFIX = ".";

	/** String denoting a prefix for channel configuration property */
	public static final String CHANNEL_PROPERTY_PREFIX = "CH";

	/** Device Description Property to be used in the configuration. */
	public static final String DEVICE_DESC_PROP = "device.desc";

	/** Device Driver Name Property to be used in the configuration. */
	public static final String DEVICE_DRIVER_PROP = "driver.id";

	/** Device Name Property to be used in the configuration. */
	public static final String DEVICE_ID_PROP = "device.name";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DeviceConfiguration.class);

	/**
	 * Static factory to instantiate a new device configuration
	 *
	 * @param properties
	 *            the configured properties
	 * @return the device configuration
	 */
	public static DeviceConfiguration of(final Map<String, Object> properties) {
		return new DeviceConfiguration(properties);
	}

	/** The list of channels associated with this device. */
	private final Map<String, Channel> m_channels = Maps.newConcurrentMap();

	/** The device description. */
	private String m_deviceDescription;

	/** The device name. */
	private String m_deviceName;

	/** Name of the driver to be associated with. */
	private String m_driverId;

	/**
	 * Instantiates a new device configuration.
	 *
	 * @param properties
	 *            the configured properties
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private DeviceConfiguration(final Map<String, Object> properties) {
		checkNull(properties, "Properties cannot be null");
		this.extractProperties(properties);
	}

	/**
	 * Adds the channel to the map of all the associated channels.
	 *
	 * @param channel
	 *            the channel to be inserted
	 */
	private void addChannel(final Channel channel) {
		this.m_channels.put(channel.getName(), channel);
	}

	/**
	 * Extract the configurations from the provided properties
	 *
	 * @param properties
	 *            the provided properties
	 */
	@SuppressWarnings("unchecked")
	private void extractProperties(final Map<String, Object> properties) {
		final HashSet<Long> parsedIndexes = Sets.newHashSet();
		for (final String property : properties.keySet()) {
			try {
				final String startStr = CharMatcher.DIGIT.removeFrom(property);
				if ((CHANNEL_PROPERTY_PREFIX + CHANNEL_PROPERTY_POSTFIX).equals(startStr)) {
					final String extractedNo = CharMatcher.DIGIT.retainFrom(property);
					final long index = Long.parseLong(extractedNo);
					if (!parsedIndexes.contains(index)) {
						parsedIndexes.add(index);
						final Object channelProperties = properties
								.get(CHANNEL_PROPERTY_PREFIX + index + CHANNEL_PROPERTY_POSTFIX);
						// if any key has values of type map, then it is
						// designated for channels
						if (channelProperties instanceof Map<?, ?>) {
							final Channel channel = this.retrieveChannel((Map<String, Object>) channelProperties);
							this.addChannel(channel);
						}
					}
				}
				if (properties.containsKey(DEVICE_DRIVER_PROP)) {
					this.m_driverId = (String) properties.get(DEVICE_DRIVER_PROP);
				}
				if (properties.containsKey(DEVICE_ID_PROP)) {
					this.m_deviceName = (String) properties.get(DEVICE_ID_PROP);
				}
				if (properties.containsKey(DEVICE_DESC_PROP)) {
					this.m_deviceDescription = (String) properties.get(DEVICE_DESC_PROP);
				}
			} catch (final Exception ex) {
				s_logger.error("Error while retrieving channels from the provided configurable properties..."
						+ Throwables.getStackTraceAsString(ex));
			}
		}
	}

	/**
	 * Gets the channels.
	 *
	 * @return the channels
	 */
	public Map<String, Channel> getChannels() {
		return this.m_channels;
	}

	/**
	 * Gets the device description.
	 *
	 * @return the device description
	 */
	public String getDeviceDescription() {
		return this.m_deviceDescription;
	}

	/**
	 * Gets the device name.
	 *
	 * @return the device name
	 */
	public String getDeviceName() {
		return this.m_deviceName;
	}

	/**
	 * Gets the driver id.
	 *
	 * @return the driver id
	 */
	public String getDriverId() {
		return this.m_driverId;
	}

	/**
	 * Retrieve channel specific configuration from the provided properties. The
	 * representation in the provided properties signifies a single channel and
	 * it should conform to the mentioned specification.
	 *
	 * @param properties
	 *            the properties to retrieve channel from
	 * @return the specific channel
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	@SuppressWarnings("unchecked")
	private Channel retrieveChannel(final Map<String, Object> properties) {
		checkNull(properties, "Properties cannot be null");
		s_logger.debug("Retrieving single channel information from the properties...");

		String channelName = null;
		ChannelType channelType = null;
		DataType dataType = null;
		Map<String, Object> channelConfig = null;

		// All key names present is the properties
		final String channelConfigKey = "channel_config";
		final String channelValueTypeKey = "value_type";
		final String channelTypeKey = "type";
		final String channelNameKey = "name";

		if (properties != null) {
			if (properties.containsKey(channelNameKey)) {
				channelName = (String) properties.get(channelNameKey);
			}
			if (properties.containsKey(channelTypeKey)) {
				final String type = (String) properties.get(channelTypeKey);
				if ("READ".equals(type)) {
					channelType = ChannelType.READ;
				}
				if ("WRITE".equals(type)) {
					channelType = ChannelType.WRITE;
				}
				if ("READ_WRITE".equals(type)) {
					channelType = ChannelType.READ_WRITE;
				}
			}
			if (properties.containsKey(channelValueTypeKey)) {
				final String type = (String) properties.get(channelValueTypeKey);

				if ("INTEGER".equalsIgnoreCase(type)) {
					dataType = DataType.INTEGER;
				}
				if ("BOOLEAN".equalsIgnoreCase(type)) {
					dataType = DataType.BOOLEAN;
				}
				if ("BYTE".equalsIgnoreCase(type)) {
					dataType = DataType.BYTE;
				}
				if ("DOUBLE".equalsIgnoreCase(type)) {
					dataType = DataType.DOUBLE;
				}
				if ("LONG".equalsIgnoreCase(type)) {
					dataType = DataType.LONG;
				}
				if ("SHORT".equalsIgnoreCase(type)) {
					dataType = DataType.SHORT;
				}
				if ("STRING".equalsIgnoreCase(type)) {
					dataType = DataType.STRING;
				}
				if ("BYTE_ARRAY".equalsIgnoreCase(type)) {
					dataType = DataType.BYTE_ARRAY;
				}
			}
			if (properties.containsKey(channelConfigKey)) {
				final Object value = properties.get(channelConfigKey);
				if (value instanceof Map<?, ?>) {
					channelConfig = (Map<String, Object>) properties.get(channelConfigKey);
				}
			}
		}
		s_logger.debug("Retrieving single channel information from the properties...Done");
		return Devices.newChannel(channelName, channelType, dataType, channelConfig);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("device_name", this.m_deviceName)
				.add("device_description", this.m_deviceDescription).add("driver_name", this.m_driverId)
				.add("associated_channels", this.m_channels).toString();
	}

}
