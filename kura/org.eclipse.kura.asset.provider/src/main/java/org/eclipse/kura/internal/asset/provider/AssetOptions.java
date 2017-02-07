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
import static org.eclipse.kura.asset.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.AssetConstants.DRIVER_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.NAME;
import static org.eclipse.kura.asset.AssetConstants.TYPE;
import static org.eclipse.kura.asset.AssetConstants.VALUE_TYPE;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.type.DataType.BOOLEAN;
import static org.eclipse.kura.type.DataType.BYTE;
import static org.eclipse.kura.type.DataType.BYTE_ARRAY;
import static org.eclipse.kura.type.DataType.DOUBLE;
import static org.eclipse.kura.type.DataType.INTEGER;
import static org.eclipse.kura.type.DataType.LONG;
import static org.eclipse.kura.type.DataType.SHORT;
import static org.eclipse.kura.type.DataType.STRING;

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

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(AssetOptions.class);

    /** Localization Resource */
    private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

    /** The asset description. */
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
     * @throws NullPointerException
     *             if the argument is null
     */
    private void checkChannelAvailability(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
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
     * @throws NullPointerException
     *             if the argument is null
     */
    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
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
     */
    private ChannelType getChannelType(final Map<String, Object> properties, final String channelTypePropertyKey) {
        requireNonNull(properties, message.propertiesNonNull());
        requireNonNull(channelTypePropertyKey, message.channelKeyNonNull());

        if (properties.containsKey(channelTypePropertyKey)) {
            final String channelTypeProp = (String) properties.get(channelTypePropertyKey);
            if ("READ".equalsIgnoreCase(channelTypeProp)) {
                return READ;
            }
            if ("WRITE".equalsIgnoreCase(channelTypeProp)) {
                return WRITE;
            }
            if ("READ_WRITE".equalsIgnoreCase(channelTypeProp)) {
                return READ_WRITE;
            }
        }
        return null;
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
     */
    private DataType getDataType(final Map<String, Object> properties, final String channelValueTypePropertyKey) {
        requireNonNull(properties, message.propertiesNonNull());
        requireNonNull(channelValueTypePropertyKey, message.channelValueTypeNonNull());

        if (properties.containsKey(channelValueTypePropertyKey)) {
            final String dataTypeProp = (String) properties.get(channelValueTypePropertyKey);
            if ("INTEGER".equalsIgnoreCase(dataTypeProp)) {
                return INTEGER;
            }
            if ("DOUBLE".equalsIgnoreCase(dataTypeProp)) {
                return DOUBLE;
            }
            if ("SHORT".equalsIgnoreCase(dataTypeProp)) {
                return SHORT;
            }
            if ("LONG".equalsIgnoreCase(dataTypeProp)) {
                return LONG;
            }
            if ("BYTE".equalsIgnoreCase(dataTypeProp)) {
                return BYTE;
            }
            if ("BYTE_ARRAY".equalsIgnoreCase(dataTypeProp)) {
                return BYTE_ARRAY;
            }
            if ("BOOLEAN".equalsIgnoreCase(dataTypeProp)) {
                return BOOLEAN;
            }
            if ("STRING".equalsIgnoreCase(dataTypeProp)) {
                return STRING;
            }

        }
        return null;
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
     * @throws NullPointerException
     *             if the properties is null
     * @throws IllegalArgumentException
     *             the channel identifier is less
     *             than or equal to zero
     */
    private Channel retrieveChannel(final long channelId, final Map<String, Object> properties) {
        if (channelId <= 0) {
            throw new IllegalArgumentException(message.channelIdNotLessThanZero());
        }
        requireNonNull(properties, message.propertiesNonNull());

        logger.debug(message.retrievingChannel());
        String channelName = null;
        ChannelType channelType = null;
        DataType dataType = null;
        Channel channel = null;
        final Map<String, Object> channelConfig = CollectionUtil.newConcurrentHashMap();

        // All key names present is the properties
        final String channelKeyContainment = CHANNEL_PROPERTY_POSTFIX.value() + CHANNEL_PROPERTY_PREFIX.value()
                + CHANNEL_PROPERTY_POSTFIX.value();
        final String channelKeyFormat = channelId + channelKeyContainment;

        if (properties != null) {
            final String channelNamePropertyKey = channelKeyFormat + NAME.value();
            if (properties.containsKey(channelNamePropertyKey)) {
                channelName = (String) properties.get(channelNamePropertyKey);
            }
            final String channelTypePropertyKey = channelKeyFormat + TYPE.value();
            channelType = getChannelType(properties, channelTypePropertyKey);
            final String channelValueTypePropertyKey = channelKeyFormat + VALUE_TYPE.value();
            dataType = getDataType(properties, channelValueTypePropertyKey);
            for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue().toString();
                final List<String> strings = Arrays.asList(key.split("\\" + CHANNEL_PROPERTY_POSTFIX.value()));
                if (strings.size() > 2 && key.startsWith(String.valueOf(channelId) + CHANNEL_PROPERTY_POSTFIX.value())
                        && DRIVER_PROPERTY_POSTFIX.value().equals(strings.get(2))) {
                    final String driverSpecificPropertyKey = DRIVER_PROPERTY_POSTFIX.value()
                            + CHANNEL_PROPERTY_POSTFIX.value();
                    final String cKey = key
                            .substring(key.indexOf(driverSpecificPropertyKey) + driverSpecificPropertyKey.length());
                    channelConfig.put(cKey, value);
                }
            }
        }
        if (channelType != null && dataType != null) {
            channel = new Channel(channelId, channelName, channelType, dataType, channelConfig);
        }
        logger.debug(message.retrievingChannelDone());
        return channel;
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
            final List<String> strings = Arrays.asList(key.split("\\" + CHANNEL_PROPERTY_POSTFIX.value()));
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
