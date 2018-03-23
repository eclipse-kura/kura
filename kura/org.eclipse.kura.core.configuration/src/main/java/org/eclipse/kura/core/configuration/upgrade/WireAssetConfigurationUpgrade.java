/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.configuration.upgrade;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class WireAssetConfigurationUpgrade {

    static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    private static final String CHANNEL_PROPERTY_SEPARATOR = "#";
    private static final String CHANNEL_NAME_PROPERTY_SUFFIX = CHANNEL_PROPERTY_SEPARATOR + "+name";
    private static final String EMIT_ALL_CHANNELS_PROP_NAME = "emit.all.channels";

    private WireAssetConfigurationUpgrade() {
    }

    static void upgrade(final Map<String, Object> properties) {
        if (properties.containsKey(EMIT_ALL_CHANNELS_PROP_NAME)) {
            return;
        }

        properties.put(EMIT_ALL_CHANNELS_PROP_NAME, false);
        final Set<String> channelNames = getChannelNames(properties);

        for (final String channelName : channelNames) {
            properties.put(channelName + CHANNEL_NAME_PROPERTY_SUFFIX, channelName);
        }
    }

    static Set<String> getChannelNames(final Map<String, Object> properties) {

        final Set<String> channelNames = new HashSet<>();

        for (final Entry<String, Object> e : properties.entrySet()) {
            final String key = e.getKey();
            final int index = key.indexOf(CHANNEL_PROPERTY_SEPARATOR);
            if (index == -1) {
                continue;
            }
            final String channelName = key.substring(0, index);
            channelNames.add(channelName);
        }

        return channelNames;
    }
}
