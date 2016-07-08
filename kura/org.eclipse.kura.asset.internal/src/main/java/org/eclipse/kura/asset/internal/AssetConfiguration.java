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
package org.eclipse.kura.asset.internal;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Assets;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.localization.AssetMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.type.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class AssetConfiguration is responsible for retrieving channels from
 * properties. The properties must conform to the following specifications. The
 * properties must have the following.
 *
 * <ul>
 * <li>CHx.</li> where x is any no (eg: CH103432214. / CH1124124215. /
 * CH5654364436. etc) (Note it the format includes a "." at the end)
 * <li>map object containing a channel configuration</li>
 * <li>the value associated with <b><i>driver.id</i></b> key in the map denotes
 * the driver instance name to be consumed by this asset</li>
 * <li>A value associated with key <b><i>asset.name</i></b> must be present to
 * denote the asset name</li>
 * <li>A value associated with <b><i>asset.desc</i></b> key denotes the asset
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
 * must be in one of the following (case-insensitive)
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
 * The channel {@code type} should be one of the following (case-insensitive)
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
public final class AssetConfiguration {

	/** Asset Description Property to be used in the configuration. */
	public static final String ASSET_DESC_PROP = "asset.desc";

	/** Driver Name Property to be used in the configuration. */
	public static final String ASSET_DRIVER_PROP = "driver.id";

	/** Asset Name Property to be used in the configuration. */
	public static final String ASSET_ID_PROP = "asset.name";

	/** String denoting a postfix for channel configuration property */
	public static final String CHANNEL_PROPERTY_POSTFIX = ".";

	/** String denoting a prefix for channel configuration property */
	public static final String CHANNEL_PROPERTY_PREFIX = "CH";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetConfiguration.class);

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The asset description. */
	private String m_assetDescription;

	/** The asset name. */
	private String m_assetName;

	/** The list of channels associated with this asset. */
	private final Map<String, Channel> m_channels = Maps.newConcurrentMap();

	/** Name of the driver to be associated with. */
	private String m_driverId;

	/**
	 * Instantiates a new asset configuration.
	 *
	 * @param properties
	 *            the configured properties
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private AssetConfiguration(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
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
	 * Checks the availability of the provided index in the provided indices and
	 * retrieves the channel from the provided properties and add the channel to
	 * the associated map
	 *
	 * @param properties
	 *            the properties to retrieve the channels from
	 * @param indices
	 *            the list of provided indices
	 * @param index
	 *            the index for which the presence is checked
	 */
	@SuppressWarnings("unchecked")
	private void checkChannelAvailability(final Map<String, Object> properties, final Set<Long> indices,
			final long index) {
		if (!indices.contains(index)) {
			indices.add(index);
			final Object channelProperties = properties.get(CHANNEL_PROPERTY_PREFIX + index + CHANNEL_PROPERTY_POSTFIX);
			// if any key has values of type map, then it is
			// designated for channels
			if (channelProperties instanceof Map<?, ?>) {
				final Channel channel = this.retrieveChannel((Map<String, Object>) channelProperties);
				this.addChannel(channel);
			}
		}
	}

	/**
	 * Extract the configurations from the provided properties
	 *
	 * @param properties
	 *            the provided properties
	 */
	private void extractProperties(final Map<String, Object> properties) {
		final Set<Long> parsedIndexes = Sets.newHashSet();
		for (final String property : properties.keySet()) {
			try {
				final String startStr = CharMatcher.DIGIT.removeFrom(property);
				if ((CHANNEL_PROPERTY_PREFIX + CHANNEL_PROPERTY_POSTFIX).equals(startStr)) {
					final String extractedNo = CharMatcher.DIGIT.retainFrom(property);
					final long index = Long.parseLong(extractedNo);
					this.checkChannelAvailability(properties, parsedIndexes, index);
				}
				if (properties.containsKey(ASSET_DRIVER_PROP)) {
					this.m_driverId = (String) properties.get(ASSET_DRIVER_PROP);
				}
				if (properties.containsKey(ASSET_ID_PROP)) {
					this.m_assetName = (String) properties.get(ASSET_ID_PROP);
				}
				if (properties.containsKey(ASSET_DESC_PROP)) {
					this.m_assetDescription = (String) properties.get(ASSET_DESC_PROP);
				}
			} catch (final Exception ex) {
				s_logger.error(s_message.errorRetrievingChannels() + Throwables.getStackTraceAsString(ex));
			}
		}
	}

	/**
	 * Gets the asset description.
	 *
	 * @return the asset description
	 */
	public String getAssetDescription() {
		return this.m_assetDescription;
	}

	/**
	 * Gets the asset name.
	 *
	 * @return the asset name
	 */
	public String getAssetName() {
		return this.m_assetName;
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
		checkNull(properties, s_message.propertiesNonNull());
		s_logger.debug(s_message.retrievingChannel());

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
				if ("READ".equalsIgnoreCase(type)) {
					channelType = ChannelType.READ;
				}
				if ("WRITE".equalsIgnoreCase(type)) {
					channelType = ChannelType.WRITE;
				}
				if ("READ_WRITE".equalsIgnoreCase(type)) {
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
		s_logger.debug(s_message.retrievingChannelDone());
		return Assets.newChannel(channelName, channelType, dataType, channelConfig);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(s_message.name(), this.m_assetName)
				.add(s_message.description(), this.m_assetDescription).add(s_message.driverName(), this.m_driverId)
				.add(s_message.channels(), this.m_channels).toString();
	}
	
	/**
	 * Static factory to instantiate a new asset configuration
	 *
	 * @param properties
	 *            the configured properties
	 * @return the asset configuration
	 */
	public static AssetConfiguration of(final Map<String, Object> properties) {
		return new AssetConfiguration(properties);
	}

}
