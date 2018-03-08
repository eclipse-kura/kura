/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.asset.cloudlet.serialization.response;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.internal.asset.cloudlet.serialization.SerializationConstants;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class ChannelOperationResponse extends AbstractResponse {

    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    private ChannelStatus getChannelStatus(ChannelRecord record) {
        ChannelStatus status = record.getChannelStatus();

        if (status == null
                || (status.getChannelFlag() == ChannelFlag.FAILURE && status.getExceptionMessage() == null)) {
            status = new ChannelStatus(ChannelFlag.FAILURE, "Unknown error", null);
        }

        return status;
    }

    public void reportAllFailed(String assetName, Iterator<String> channelNames, String errorMessage) {
        JsonObject assetObject = Json.object();
        assetObject.add(SerializationConstants.ASSET_NAME_PROPERTY, assetName);

        final JsonArray channels = new JsonArray();

        channelNames.forEachRemaining((channelName) -> {
            final JsonObject channelObject = Json.object();
            channelObject.add(SerializationConstants.CHANNEL_NAME_PROPERTY, channelName);
            channelObject.add(SerializationConstants.CHANNEL_TIMESTAMP_PROPERTY, System.currentTimeMillis());
            channelObject.add(SerializationConstants.ERROR_PROPERTY, errorMessage);
            channels.add(channelObject);
        });

        assetObject.add(SerializationConstants.CHANNELS_PROPERTY, channels);

        serialized.add(assetObject);
    }

    public void reportResult(String assetName, List<ChannelRecord> list) {
        JsonObject assetObject = Json.object();
        assetObject.add(SerializationConstants.ASSET_NAME_PROPERTY, assetName);

        final JsonArray channels = new JsonArray();

        for (ChannelRecord record : list) {
            final JsonObject channelObject = Json.object();
            channelObject.add(SerializationConstants.CHANNEL_NAME_PROPERTY, record.getChannelName());

            long timestamp = record.getTimestamp();
            if (timestamp == 0) {
                timestamp = System.currentTimeMillis();
            }

            channelObject.add(SerializationConstants.CHANNEL_TIMESTAMP_PROPERTY, timestamp);
            final ChannelStatus status = getChannelStatus(record);

            final TypedValue<?> value = record.getValue();
            if (ChannelFlag.SUCCESS == status.getChannelFlag() && value != null) {
                final DataType type = value.getType();
                String stringValue;
                if (DataType.BYTE_ARRAY == type) {
                    stringValue = BASE64_ENCODER.encodeToString((byte[]) value.getValue());
                } else {
                    stringValue = value.getValue().toString();
                }
                channelObject.add(SerializationConstants.CHANNEL_VALUE_PROPERTY, stringValue);
                channelObject.add(SerializationConstants.CHANNEL_TYPE_PROPERTY, type.toString());
            } else {
                channelObject.add(SerializationConstants.ERROR_PROPERTY, status.getExceptionMessage());
            }
            channels.add(channelObject);
        }

        assetObject.add(SerializationConstants.CHANNELS_PROPERTY, channels);

        serialized.add(assetObject);
    }

}
