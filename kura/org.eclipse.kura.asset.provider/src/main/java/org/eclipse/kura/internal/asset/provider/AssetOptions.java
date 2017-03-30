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
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_SEPARATOR;
import static org.eclipse.kura.asset.AssetConstants.TYPE;
import static org.eclipse.kura.asset.AssetConstants.VALUE_TYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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

    private static final Logger logger = LoggerFactory.getLogger(AssetOptions.class);

    private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

    private String assetDescription;

    /** The list of channels associated with this asset. */
    private Map<String, Channel> channels;

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
     * Extract the configurations from the provided properties
     *
     * @param properties
     *            the provided properties
     */
    private void extractProperties(final Map<String, Object> properties) {
        try {
            this.driverPid = (String) properties.get(ASSET_DRIVER_PROP.value());
            this.assetDescription = (String) properties.get(ASSET_DESC_PROP.value());
            this.channels = retreiveChannelList(properties);
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

    private ChannelType getChannelType(final Map<String, Object> properties) {
        final String channelTypeProp = (String) properties.get(TYPE.value());

        if (channelTypeProp == null) {
            return null;
        }
        return ChannelType.getChannelType(channelTypeProp);
    }

    private DataType getDataType(final Map<String, Object> properties) {
        final String valueTypeProp = (String) properties.get(VALUE_TYPE.value());

        if (valueTypeProp == null) {
            return null;
        }
        return DataType.getDataType(valueTypeProp.toString());
    }

    private Channel extractChannel(final String channelName, final Map<String, Object> properties) {
        logger.debug(message.retrievingChannel());
        Channel channel = null;

        Map<String, Object> channelConfig = retrieveChannelConfig(channelName, properties);
        if (channelConfig == null) {
            return null;
        }

        final ChannelType channelType = getChannelType(channelConfig);
        final DataType dataType = getDataType(channelConfig);

        if (channelType != null && dataType != null) {
            channel = new Channel(channelName, channelType, dataType, channelConfig);
        }
        logger.debug(message.retrievingChannelDone());
        return channel;
    }

    private Map<String, Object> retrieveChannelConfig(final String targetChannelName,
            final Map<String, Object> properties) {
        final Map<String, Object> channelConfig = CollectionUtil.newConcurrentHashMap();
        final int propertyBeginIndex = targetChannelName.length() + 1;

        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final String channelName = extractChannelName(key);
            if (channelName == null) {
                continue;
            }
            if (!targetChannelName.equals(channelName)) {
                continue;
            }
            if (key.length() <= propertyBeginIndex) {
                return null;
            }
            channelConfig.put(key.substring(propertyBeginIndex), entry.getValue().toString());
        }
        return channelConfig;
    }

    private String extractChannelName(String propertyKey) {
        int pos = propertyKey.indexOf(CHANNEL_PROPERTY_SEPARATOR.value());
        if (pos <= 0) {
            return null;
        }
        return propertyKey.substring(0, pos);
    }

    private Map<String, Channel> retreiveChannelList(Map<String, Object> properties) {
        Set<String> alreadyProcessedChannelNames = new HashSet<>();
        Map<String, Channel> result = new HashMap<>();

        for (Entry<String, Object> e : properties.entrySet()) {
            String channelName = extractChannelName(e.getKey());
            if (channelName == null || alreadyProcessedChannelNames.contains(channelName)) {
                continue;
            }
            alreadyProcessedChannelNames.add(channelName);
            if (!Channel.isValidChannelName(channelName)) {
                continue;
            }
            Channel channel = extractChannel(channelName, properties);
            if (channel != null) {
                result.put(channelName, channel);
            }
        }
        return result;
    }
}
