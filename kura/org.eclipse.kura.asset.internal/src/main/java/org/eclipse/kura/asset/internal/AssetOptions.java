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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.AssetConfiguration;
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
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class AssetOptions is responsible for retrieving channels from
 * properties. The properties must conform to the following specifications. The
 * properties must have the following.
 *
 * <ul>
 * <li>the value associated with <b><i>driver.id</i></b> key in the map denotes
 * the driver instance name to be consumed by this asset</li>
 * <li>A value associated with key <b><i>asset.name</i></b> must be present to
 * denote the asset name</li>
 * <li>A value associated with <b><i>asset.desc</i></b> key denotes the asset
 * description</li>
 * <li>x.CH.[property]</li> where x is any number denoting the channel's unique
 * ID and the {@code [property]} denotes the protocol specific properties. (Note
 * that the format includes atleast two ".") denotes map object containing a
 * channel configuration</li>
 *
 * For example, 1.CH.name, 1.CH.value.type etc.
 *
 * The representation in the provided properties as prepended by a number
 * signifies a single channel and it should conform to the following
 * specification.
 *
 * The properties should contain the following keys
 * <ul>
 * <li>name</li>
 * <li>type</li>
 * <li>value.type</li>
 * <li>[more configuration]</li> as mentioned by the driver in the format which
 * begins with <b><i>DRIVER.</i></b>
 * </ul>
 *
 * For example, [more configuration] would be 1.CH.DRIVER.modbus.register,
 * 1.CH.DRIVER.modbus.unit.id etc.
 *
 * The key <b><i>name</i></b> must be String. The key <b><i>value.type</i></b>
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
 * @see BaseChannelDescriptor
 * @see AssetConfiguration
 *
 */
public final class AssetOptions {

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

	/**
	 * String denoting a prefix for driver specific channel configuration
	 * property
	 */
	public static final String DRIVER_PROPERTY_PREFIX = "DRIVER";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetOptions.class);

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The asset description. */
	private String m_assetDescription;

	/** The asset name. */
	private String m_assetName;

	/** The list of channels associated with this asset. */
	private final Map<Long, Channel> m_channels = Maps.newConcurrentMap();

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
	public AssetOptions(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.extractProperties(properties);
	}

	/**
	 * Adds the channel to the map of all the associated channels.
	 *
	 * @param channel
	 *            the channel to be inserted
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private void addChannel(final Channel channel) {
		checkNull(channel, s_message.channelNonNull());
		this.m_channels.put(channel.getId(), channel);
	}

	/**
	 * Checks the availability the channel from the provided properties and add
	 * the channel to the associated map
	 *
	 * @param properties
	 *            the properties to retrieve the channels from
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private void checkChannelAvailability(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());

		final Set<Long> channelIds = this.retrieveChannelIds(properties);
		for (final Long channelName : channelIds) {
			final Channel channel = this.retrieveChannel(channelName, properties);
			this.addChannel(channel);
		}
	}

	/**
	 * Extract the configurations from the provided properties
	 *
	 * @param properties
	 *            the provided properties
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		try {
			if (properties.containsKey(ASSET_DRIVER_PROP)) {
				this.m_driverId = (String) properties.get(ASSET_DRIVER_PROP);
			}
			if (properties.containsKey(ASSET_ID_PROP)) {
				this.m_assetName = (String) properties.get(ASSET_ID_PROP);
			}
			if (properties.containsKey(ASSET_DESC_PROP)) {
				this.m_assetDescription = (String) properties.get(ASSET_DESC_PROP);
			}
			this.checkChannelAvailability(properties);
		} catch (final Exception ex) {
			s_logger.error(s_message.errorRetrievingChannels() + Throwables.getStackTraceAsString(ex));
		}
	}

	/**
	 * Gets the asset configuration.
	 *
	 * @return the asset configuration
	 */
	public AssetConfiguration getAssetConfiguration() {
		return Assets.newAssetConfigruation(this.m_assetName, this.m_assetDescription, this.m_driverId,
				this.m_channels);
	}

	/**
	 * Retrieve channel specific configuration from the provided properties. The
	 * representation in the provided properties signifies a single channel and
	 * it should conform to the mentioned specification.
	 *
	 * @param channelId
	 *            unique channel id (in the format x.CH where x is a channel ID)
	 * @param properties
	 *            the properties to retrieve channel from
	 * @return the specific channel
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private Channel retrieveChannel(final long channelId, final Map<String, Object> properties) {
		checkNull(channelId, s_message.prefixNonNull());
		checkNull(properties, s_message.propertiesNonNull());

		s_logger.debug(s_message.retrievingChannel());
		String channelName = null;
		ChannelType channelType = null;
		DataType dataType = null;
		final Map<String, Object> channelConfig = Maps.newHashMap();

		// All key names present is the properties
		final String channelValueTypeKey = "value.type";
		final String channelTypeKey = "type";
		final String channelNameKey = "name";
		final String channelKeyContainment = CHANNEL_PROPERTY_POSTFIX + CHANNEL_PROPERTY_PREFIX
				+ CHANNEL_PROPERTY_POSTFIX;
		final String channelKeyFormat = channelId + channelKeyContainment;

		if (properties != null) {
			final String channelNamePropertyKey = channelKeyFormat + channelNameKey;
			if (properties.containsKey(channelNamePropertyKey)) {
				channelName = (String) properties.get(channelNamePropertyKey);
			}

			final String channelTypePropertyKey = channelKeyFormat + channelTypeKey;
			if (properties.containsKey(channelTypePropertyKey)) {
				final String type = (String) properties.get(channelTypePropertyKey);
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

			final String channelValueTypePropertyKey = channelKeyFormat + channelValueTypeKey;
			if (properties.containsKey(channelValueTypePropertyKey)) {
				final String valueType = (String) properties.get(channelValueTypePropertyKey);
				if ("INTEGER".equalsIgnoreCase(valueType)) {
					dataType = DataType.INTEGER;
				}
				if ("BOOLEAN".equalsIgnoreCase(valueType)) {
					dataType = DataType.BOOLEAN;
				}
				if ("BYTE".equalsIgnoreCase(valueType)) {
					dataType = DataType.BYTE;
				}
				if ("DOUBLE".equalsIgnoreCase(valueType)) {
					dataType = DataType.DOUBLE;
				}
				if ("LONG".equalsIgnoreCase(valueType)) {
					dataType = DataType.LONG;
				}
				if ("SHORT".equalsIgnoreCase(valueType)) {
					dataType = DataType.SHORT;
				}
				if ("STRING".equalsIgnoreCase(valueType)) {
					dataType = DataType.STRING;
				}
				if ("BYTE_ARRAY".equalsIgnoreCase(valueType)) {
					dataType = DataType.BYTE_ARRAY;
				}
			}
			for (final Map.Entry<String, Object> entry : properties.entrySet()) {
				final String key = entry.getKey();
				final String value = entry.getValue().toString();
				final List<String> strings = Splitter.on(".").splitToList(key);
				if ((strings.size() > 2) && key.startsWith(String.valueOf(channelId))
						&& DRIVER_PROPERTY_PREFIX.equals(strings.get(2))) {
					final String driverSpecificPropertyKey = DRIVER_PROPERTY_PREFIX + CHANNEL_PROPERTY_POSTFIX;
					final String cKey = key
							.substring(key.indexOf(driverSpecificPropertyKey) + driverSpecificPropertyKey.length());
					channelConfig.put(cKey, value);
				}
			}
		}
		final Channel channel = Assets.newChannel(channelId, channelName, channelType, dataType, channelConfig);
		s_logger.debug(s_message.retrievingChannelDone());
		return channel;
	}

	/**
	 * Retrieves the set of id of the channels from the properties
	 *
	 * @param properties
	 *            the properties to parse
	 * @return the list of channel IDs
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private Set<Long> retrieveChannelIds(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		final Set<Long> channelIds = Sets.newHashSet();
		for (final Map.Entry<String, Object> entry : properties.entrySet()) {
			final String key = entry.getKey();
			final List<String> strings = Splitter.on(CHANNEL_PROPERTY_POSTFIX).splitToList(key);
			for (final String string : strings) {
				if (CharMatcher.DIGIT.matchesAnyOf(string)) {
					channelIds.add(Long.parseLong(string));
				}
			}
		}
		return channelIds;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(s_message.name(), this.m_assetName)
				.add(s_message.description(), this.m_assetDescription).add(s_message.driverName(), this.m_driverId)
				.add(s_message.channels(), this.m_channels).toString();
	}

}
