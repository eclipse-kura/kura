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
package org.eclipse.kura.internal.asset;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_ID_PROP;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.AssetConstants.DRIVER_PROPERTY_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetHelperService;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class AssetOptions is responsible for retrieving channels from
 * properties.
 *
 * @see AssetConfiguration
 *
 */
final class AssetOptions {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetOptions.class);

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The asset description. */
	private String m_assetDescription;

	/** The Asset Helper Service instance. */
	private final AssetHelperService m_assetHelper;

	/** The asset name. */
	private String m_assetName;

	/** The list of channels associated with this asset. */
	private final Map<Long, Channel> m_channels = CollectionUtil.newConcurrentHashMap();

	/** Name of the driver to be associated with. */
	private String m_driverId;

	/**
	 * Instantiates a new asset configuration.
	 *
	 * @param properties
	 *            the configured properties
	 * @param assetHelperService
	 *            the Asset Helper Service instance
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	AssetOptions(final Map<String, Object> properties, final AssetHelperService assetHelperService) {
		checkNull(properties, s_message.propertiesNonNull());
		checkNull(properties, s_message.assetHelperNonNull());

		this.m_assetHelper = assetHelperService;
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
			s_logger.error(s_message.errorRetrievingChannels() + ThrowableUtil.stackTraceAsString(ex));
		}
	}

	/**
	 * Gets the asset configuration.
	 *
	 * @return the asset configuration
	 */
	AssetConfiguration getAssetConfiguration() {
		return this.m_assetHelper.newAssetConfigruation(this.m_assetName, this.m_assetDescription, this.m_driverId,
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
		final Map<String, Object> channelConfig = CollectionUtil.newConcurrentHashMap();

		// All key names present is the properties
		final String channelValueTypeKey = "value.type";
		final String channelTypeKey = "type";
		final String channelNameKey = "name";
		final String channelKeyContainment = CHANNEL_PROPERTY_POSTFIX.value() + CHANNEL_PROPERTY_PREFIX.value()
				+ CHANNEL_PROPERTY_POSTFIX.value();
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

				final List<String> strings = Arrays.asList(key.split(CHANNEL_PROPERTY_POSTFIX.value()));
				if ((strings.size() > 2) && key.startsWith(String.valueOf(channelId))
						&& DRIVER_PROPERTY_PREFIX.equals(strings.get(2))) {
					final String driverSpecificPropertyKey = DRIVER_PROPERTY_PREFIX.value()
							+ CHANNEL_PROPERTY_POSTFIX.value();
					final String cKey = key
							.substring(key.indexOf(driverSpecificPropertyKey) + driverSpecificPropertyKey.length());
					channelConfig.put(cKey, value);
				}
			}
		}
		final Channel channel = this.m_assetHelper.newChannel(channelId, channelName, channelType, dataType,
				channelConfig);
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
		final Set<Long> channelIds = CollectionUtil.newHashSet();
		for (final Map.Entry<String, Object> entry : properties.entrySet()) {
			final String key = entry.getKey();
			final List<String> strings = Arrays.asList(key.split(CHANNEL_PROPERTY_POSTFIX.value()));
			for (final String string : strings) {
				if (string.matches("\\d+")) {
					channelIds.add(Long.parseLong(string));
				}
			}
		}
		return channelIds;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "AssetOptions [Asset Description=" + this.m_assetDescription + ", Asset Name=" + this.m_assetName
				+ ", Channels=" + this.m_channels + ", Driver ID=" + this.m_driverId + "]";
	}

}
