/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.asset.cloudlet.serialization.response;

import java.util.Map.Entry;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.internal.asset.cloudlet.serialization.SerializationConstants;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class MetadataResponse extends AbstractResponse {

    public void addAssetMetadata(String name, Asset asset) {
        JsonObject assetObject = Json.object();
        assetObject.add(SerializationConstants.ASSET_NAME_PROPERTY, name);

        JsonArray channels = new JsonArray();

        for (Entry<String, Channel> channelEntry : asset.getAssetConfiguration().getAssetChannels().entrySet()) {
            final Channel channel = channelEntry.getValue();
            final JsonObject channelObject = Json.object();
            channelObject.add(SerializationConstants.CHANNEL_NAME_PROPERTY, channel.getName());
            channelObject.add(SerializationConstants.CHANNEL_TYPE_PROPERTY, channel.getValueType().toString());
            channelObject.add(SerializationConstants.CHANNEL_MODE_PROPERTY, channel.getType().toString());
            channels.add(channelObject);
        }

        assetObject.add(SerializationConstants.CHANNELS_PROPERTY, channels);
        serialized.add(assetObject);
    }
}
