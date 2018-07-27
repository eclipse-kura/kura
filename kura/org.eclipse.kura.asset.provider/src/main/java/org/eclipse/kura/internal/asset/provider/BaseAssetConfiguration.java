/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;
import static org.eclipse.kura.asset.provider.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.provider.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.provider.AssetConstants.CHANNEL_NAME_PROHIBITED_CHARS;
import static org.eclipse.kura.asset.provider.AssetConstants.CHANNEL_PROPERTY_SEPARATOR;
import static org.eclipse.kura.asset.provider.AssetConstants.ENABLED;
import static org.eclipse.kura.asset.provider.AssetConstants.TYPE;
import static org.eclipse.kura.asset.provider.AssetConstants.VALUE_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.provider.AssetConstants;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BaseAssetConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BaseAssetConfiguration.class);

    private Map<String, Object> properties;
    private Tocd ocd;
    private AssetConfiguration assetConfiguration;
    private boolean hasReadChannels;
    private String kuraServicePid;

    public BaseAssetConfiguration(final Map<String, Object> properties) {
        this.properties = properties;
        this.assetConfiguration = new AssetConfiguration(getDescription(properties), getDriverPid(properties),
                retreiveChannelList(properties));
        this.hasReadChannels = !getAllReadRecords().isEmpty();
        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Tocd getDefinition() {
        return ocd;
    }

    public AssetConfiguration getAssetConfiguration() {
        return assetConfiguration;
    }

    public boolean hasReadChannels() {
        return hasReadChannels;
    }

    public String getKuraServicePid() {
        return kuraServicePid;
    }

    public List<ChannelRecord> getAllReadRecords() {
        final Map<String, Channel> channels = getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> readRecords = new ArrayList<>();

        for (Entry<String, Channel> e : channels.entrySet()) {
            final Channel channel = e.getValue();
            if (channel.isEnabled()
                    && (channel.getType() == ChannelType.READ || channel.getType() == ChannelType.READ_WRITE)) {
                readRecords.add(channel.createReadRecord());
            }
        }

        return Collections.unmodifiableList(readRecords);
    }

    @SuppressWarnings("unchecked")
    public void complete(final Tocd baseOcd, final ComponentContext context, final List<Tad> assetDescriptor,
            final Driver driver) {
        Object opaqueDriverDescriptor = null;
        try {
            final ChannelDescriptor channelDescriptor = driver.getChannelDescriptor();
            if (channelDescriptor == null) {
                return;
            }
            opaqueDriverDescriptor = channelDescriptor.getDescriptor();
            if (!(opaqueDriverDescriptor instanceof List<?>)) {
                return;
            }
        } catch (Exception e) {
            logger.warn("Failed to get channel descriptor", e);
            return;
        }
        final List<Tad> driverDescriptor = (List<Tad>) opaqueDriverDescriptor;

        this.ocd = buildDefinition(baseOcd, assetDescriptor, driverDescriptor);
        this.properties = buildNewConfiguration(context, assetDescriptor, properties, driverDescriptor);
        this.assetConfiguration = new AssetConfiguration(getDescription(properties), getDriverPid(properties),
                retreiveChannelList(properties));
    }

    private Tocd buildDefinition(final Tocd baseOcd, final List<Tad> assetDescriptor,
            final List<Tad> driverDescriptor) {
        Stream.concat(assetDescriptor.stream(), driverDescriptor.stream()).forEach(attribute -> {
            for (final Entry<String, Channel> entry : assetConfiguration.getAssetChannels().entrySet()) {
                final String channelName = entry.getKey();
                if (!(attribute instanceof Tad)) {
                    return;
                }
                final Tad newAttribute = cloneAd((Tad) attribute, channelName);
                baseOcd.addAD(newAttribute);
            }
        });
        return baseOcd;
    }

    private Map<String, Object> buildNewConfiguration(final ComponentContext context, final List<Tad> assetDescriptor,
            Map<String, Object> newConfiguration, final List<Tad> driverDescriptor) {
        final Tocd tempOcd = new Tocd();
        assetDescriptor.forEach(tempOcd::addAD);
        driverDescriptor.forEach(tempOcd::addAD);
        final Map<String, Object> defaultValues = ComponentUtil.getDefaultProperties(tempOcd, context);
        final Map<String, Channel> channels = assetConfiguration.getAssetChannels();
        for (AD tad : tempOcd.getAD()) {
            if (!tad.isRequired()) {
                continue;
            }
            final String id = tad.getId();
            for (final Channel channel : channels.values()) {
                final Map<String, Object> config = channel.getConfiguration();
                if (config.get(id) == null) {
                    if (newConfiguration == null) {
                        newConfiguration = CollectionUtil.newHashMap();
                        newConfiguration.putAll(this.properties);
                    }
                    final Object defaultValue = defaultValues.get(id);
                    newConfiguration.put(channel.getName() + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + id,
                            defaultValue);
                }
            }
        }
        return newConfiguration;
    }

    private static Tad cloneAd(final Tad oldAd, final String channelName) {

        final String oldAdId = oldAd.getId();
        String prefix = channelName + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value();

        final Tad result = new Tad();
        result.setId(prefix + oldAdId);
        result.setName(prefix + oldAd.getName());
        result.setCardinality(oldAd.getCardinality());
        result.setType(Tscalar.fromValue(oldAd.getType().value()));
        result.setDescription(oldAd.getDescription());
        result.setDefault(oldAd.getDefault());
        result.setMax(oldAd.getMax());
        result.setMin(oldAd.getMin());
        result.setRequired(oldAd.isRequired());
        for (final Option option : oldAd.getOption()) {
            final Toption newOption = new Toption();
            newOption.setLabel(option.getLabel());
            newOption.setValue(option.getValue());
            result.getOption().add(newOption);
        }
        return result;
    }

    private static String getDescription(final Map<String, Object> properties) {
        return (String) properties.getOrDefault(ASSET_DESC_PROP.value(), "");
    }

    private static String getDriverPid(final Map<String, Object> properties) {
        return (String) properties.get(ASSET_DRIVER_PROP.value());
    }

    private static boolean isValidChannelName(String channelName) {
        if (isNull(channelName))
            return false;

        final String prohibitedChars = CHANNEL_NAME_PROHIBITED_CHARS.value();

        for (int i = 0; i < channelName.length(); i++) {
            if (prohibitedChars.indexOf(channelName.charAt(i)) != -1) {
                return false;
            }
        }

        return true;
    }

    private static ChannelType getChannelType(final Map<String, Object> properties) {
        final String channelTypeProp = (String) properties.get(TYPE.value());

        if (channelTypeProp == null) {
            return null;
        }
        return ChannelType.getChannelType(channelTypeProp);
    }

    private static DataType getDataType(final Map<String, Object> properties) {
        final String valueTypeProp = (String) properties.get(VALUE_TYPE.value());

        if (valueTypeProp == null) {
            return null;
        }
        return DataType.getDataType(valueTypeProp);
    }

    private static boolean isEnabled(final Map<String, Object> properties) {
        try {
            return Boolean.parseBoolean(properties.get(ENABLED.value()).toString());
        } catch (Exception e) {
            logger.debug("Failed to retrieve enabled channel property");
            return true;
        }
    }

    private static Channel extractChannel(final String channelName, final Map<String, Object> properties) {
        logger.debug("Retrieving single channel information from the properties...");
        Channel channel = null;

        Map<String, Object> channelConfig = retrieveChannelConfig(channelName, properties);
        if (channelConfig == null) {
            return null;
        }

        final ChannelType channelType = getChannelType(channelConfig);
        final DataType dataType = getDataType(channelConfig);
        final boolean isEnabled = isEnabled(channelConfig);

        if (channelType != null && dataType != null) {
            channel = new Channel(channelName, channelType, dataType, channelConfig);
            channel.setEnabled(isEnabled);
        }
        logger.debug("Retrieving single channel information from the properties...Done");
        return channel;
    }

    private static Map<String, Object> retrieveChannelConfig(final String targetChannelName,
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
            final Object value = entry.getValue();
            if (value != null) {
                channelConfig.put(key.substring(propertyBeginIndex), value.toString());
            }
        }
        return channelConfig;
    }

    private static String extractChannelName(String propertyKey) {
        int pos = propertyKey.indexOf(CHANNEL_PROPERTY_SEPARATOR.value());
        if (pos <= 0) {
            return null;
        }
        return propertyKey.substring(0, pos);
    }

    private static Map<String, Channel> retreiveChannelList(Map<String, Object> properties) {
        Set<String> alreadyProcessedChannelNames = new HashSet<>();
        Map<String, Channel> result = new HashMap<>();

        for (Entry<String, Object> e : properties.entrySet()) {
            String channelName = extractChannelName(e.getKey());
            if (channelName == null || alreadyProcessedChannelNames.contains(channelName)) {
                continue;
            }
            alreadyProcessedChannelNames.add(channelName);
            if (!isValidChannelName(channelName)) {
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
