/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.internal.asset.provider;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.isNull;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.AssetConstants.DRIVER_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.NAME;
import static org.eclipse.kura.asset.AssetConstants.TYPE;
import static org.eclipse.kura.asset.AssetConstants.VALUE_TYPE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class AssetOptions is responsible for retrieving channels from
 * properties.
 *
 * @see AssetConfiguration
 */
public final class AssetOptions {

    private static final String DRIVER_SPECIFIC_PROPERTY_KEY = DRIVER_PROPERTY_POSTFIX.value()
            + CHANNEL_PROPERTY_POSTFIX.value();

    private static final String CHANNEL_PROPERTY_SPLIT_REGEX = "\\" + CHANNEL_PROPERTY_POSTFIX.value();

    private static final Logger logger = LoggerFactory.getLogger(AssetOptions.class);

    private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

    private String assetDescription;

    /** The list of channels associated with this asset. */
    private final Map<Long, Channel> channels = CollectionUtil.newConcurrentHashMap();

    /** Name of the driver to be associated with. */
    private String driverPid;

    /**
     * Instantiates a new asset configuration.
     *
     * @param properties
     *            the configured properties
     * @throws NullPointerException
     *             if the argument is null
     */
    public AssetOptions(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        extractProperties(properties);
    }

    /**
     * Adds the channel to the map of all the associated channels.
     *
     * @param channel
     *            the channel to be inserted
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void addChannel(final Channel channel) {
        requireNonNull(channel, message.channelNonNull());
        this.channels.put(channel.getId(), channel);
    }

    /**
     * Checks the availability the channel from the provided properties and add
     * the channel to the associated map
     *
     * @param properties
     *            the properties to retrieve the channels from
     */
    private void checkChannelAvailability(final Map<String, Object> properties) {
        final Set<Long> channelIds = retrieveChannelIds(properties);
        for (final long channelId : channelIds) {
            final Channel channel = retrieveChannel(channelId, properties);
            addChannel(channel);
        }
    }

    /**
     * Extract the configurations from the provided properties
     *
     * @param properties
     *            the provided properties
     */
    private void extractProperties(final Map<String, Object> properties) {
        try {
            if (properties.containsKey(ASSET_DRIVER_PROP.value())) {
                this.driverPid = (String) properties.get(ASSET_DRIVER_PROP.value());
            }
            if (properties.containsKey(ASSET_DESC_PROP.value())) {
                this.assetDescription = (String) properties.get(ASSET_DESC_PROP.value());
            }
            checkChannelAvailability(properties);
        } catch (final Exception ex) {
            logger.error(message.errorRetrievingChannels(), ex);
        }
    }

    /**
     * Gets the asset configuration.
     *
     * @return the asset configuration
     */
    public AssetConfiguration getAssetConfiguration() {
        return new AssetConfiguration(this.assetDescription, this.driverPid, this.channels);
    }

    /**
     * Returns the Channel Type
     *
     * @param properties
     *            the properties to read
     * @param channelTypePropertyKey
     *            the key to read from the provided properties
     * @return the Channel Type
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws IllegalArgumentException
     *             if it is not possible to get a ChannelType from the provided properties.
     */
    private ChannelType getChannelType(final Map<String, Object> properties, final String channelKeyFormat) {
        requireNonNull(channelKeyFormat, message.channelKeyNonNull());

        final String channelTypePropertyKey = channelKeyFormat + TYPE.value();

        ChannelType result = null;
        if (properties.containsKey(channelTypePropertyKey)) {
            final String channelTypeProp = (String) properties.get(channelTypePropertyKey);
            result = ChannelType.getChannelType(channelTypeProp);
        }
        return result;
    }

    /**
     * Returns the Value Type
     *
     * @param properties
     *            the properties to read
     * @param channelValueTypePropertyKey
     *            the key to read from the provided properties
     * @return the Channel Type
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws IllegalArgumentException
     *             if the {@link DataType} is not found
     */
    private DataType getDataType(final Map<String, Object> properties, final String channelKeyFormat) {
        requireNonNull(properties, message.propertiesNonNull());
        requireNonNull(channelKeyFormat, message.channelValueTypeNonNull());

        DataType result = null;
        final String channelValueTypePropertyKey = channelKeyFormat + VALUE_TYPE.value();
        if (properties.containsKey(channelValueTypePropertyKey)) {
            final String dataTypeProp = (String) properties.get(channelValueTypePropertyKey);
            result = DataType.getDataType(dataTypeProp);
        }
        return result;
    }

    /**
     * Retrieve channel specific configuration from the provided properties. The
     * representation in the provided properties signifies a single channel and
     * it should conform to the mentioned specification.
     *
     * @param channelId
     *            unique channel ID (in the format x.CH where x is a channel ID)
     * @param properties
     *            the properties to retrieve channel from
     * @return the specific channel
     * @throws IllegalArgumentException
     *             the channel identifier is less
     *             than or equal to zero
     */
    private Channel retrieveChannel(final long channelId, final Map<String, Object> properties) {
        if (channelId <= 0) {
            throw new IllegalArgumentException(message.channelIdNotLessThanZero());
        }

        logger.debug(message.retrievingChannel());
        Channel channel = null;

        // All key names present is the properties
        final StringBuilder channelPropertyKeyBuilder = new StringBuilder();
        channelPropertyKeyBuilder.append(channelId);
        channelPropertyKeyBuilder.append(CHANNEL_PROPERTY_POSTFIX.value());
        channelPropertyKeyBuilder.append(CHANNEL_PROPERTY_PREFIX.value());
        channelPropertyKeyBuilder.append(CHANNEL_PROPERTY_POSTFIX.value());
        final String channelKeyFormat = channelPropertyKeyBuilder.toString();

        final String channelName = retrieveChannelName(properties, channelKeyFormat);
        final ChannelType channelType = getChannelType(properties, channelKeyFormat);
        final DataType dataType = getDataType(properties, channelKeyFormat);
        final Map<String, Object> channelConfig = retrieveChannelConfig(channelId, properties);

        if (channelType != null && dataType != null) {
            channel = new Channel(channelId, channelName, channelType, dataType, channelConfig);
        }
        logger.debug(message.retrievingChannelDone());
        return channel;
    }

    private Map<String, Object> retrieveChannelConfig(final long channelId, final Map<String, Object> properties) {
        final Map<String, Object> channelConfig = CollectionUtil.newConcurrentHashMap();
        final String channelIdPropertyPrefix = String.valueOf(channelId) + CHANNEL_PROPERTY_POSTFIX.value();

        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue().toString();
            final List<String> strings = Arrays.asList(key.split(CHANNEL_PROPERTY_SPLIT_REGEX));
            if (strings.size() > 2 && key.startsWith(channelIdPropertyPrefix)
                    && DRIVER_PROPERTY_POSTFIX.value().equals(strings.get(2))) {
                final String cKey = key
                        .substring(key.indexOf(DRIVER_SPECIFIC_PROPERTY_KEY) + DRIVER_SPECIFIC_PROPERTY_KEY.length());
                channelConfig.put(cKey, value);
            }
        }
        return channelConfig;
    }

    private String retrieveChannelName(final Map<String, Object> properties, final String channelKeyFormat) {
        String result = null;
        if (isNull(channelKeyFormat)) {
            return result;
        }

        final String channelNamePropertyKey = channelKeyFormat + NAME.value();
        if (properties.containsKey(channelNamePropertyKey)) {
            result = (String) properties.get(channelNamePropertyKey);
        }
        return result;
    }

    /**
     * Retrieves the set of id of the channels from the properties
     *
     * @param properties
     *            the properties to parse
     * @return the list of channel IDs
     * @throws NullPointerException
     *             if the argument is null
     */
    private Set<Long> retrieveChannelIds(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        final Set<Long> channelIds = CollectionUtil.newHashSet();
        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final List<String> strings = Arrays.asList(key.split(CHANNEL_PROPERTY_SPLIT_REGEX));
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
        return "AssetOptions [Asset Description=" + this.assetDescription + ", Channels=" + this.channels
                + ", Driver ID=" + this.driverPid + "]";
    }

    /**
     * Updates with new properties
     *
     * @param properties
     *            the new properties
     * @throws NullPointerException
     *             if the argument is null
     */
    public void update(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        extractProperties(properties);
    }
}
