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

import java.nio.charset.StandardCharsets;

import org.eclipse.kura.internal.asset.cloudlet.serialization.SerializationConstants;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public abstract class AbstractResponse {

    protected JsonArray serialized = new JsonArray();

    public void reportAssetNotFound(String name) {
        JsonObject assetObject = Json.object();
        assetObject.add(SerializationConstants.ASSET_NAME_PROPERTY, name);
        assetObject.add(SerializationConstants.ERROR_PROPERTY, "Asset not found");
        serialized.add(assetObject);
    }

    public byte[] serialize() {
        return serialized.toString().getBytes(StandardCharsets.UTF_8);
    }
}
