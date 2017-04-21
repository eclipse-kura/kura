/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.asset.cloudlet.serialization.request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.internal.asset.cloudlet.serialization.SerializationConstants;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class ReadRequest {

    private String assetName;
    private Set<String> channelNames = new HashSet<>();

    public ReadRequest(JsonObject object) {
        this.assetName = object.get(SerializationConstants.ASSET_NAME_PROPERTY).asString();
        Optional.ofNullable(object.get(SerializationConstants.CHANNELS_PROPERTY)).ifPresent((channels) -> {
            channels.asArray().forEach((value) -> {
                channelNames.add(value.asObject().get(SerializationConstants.CHANNEL_NAME_PROPERTY).asString());
            });
        });
    }

    public static List<ReadRequest> parseAll(JsonArray array) {
        List<ReadRequest> result = new ArrayList<>();
        for (JsonValue value : array) {
            result.add(new ReadRequest(value.asObject()));
        }
        return result;
    }

    public String getAssetName() {
        return assetName;
    }

    public Set<String> getChannelNames() {
        return channelNames;
    }
}
