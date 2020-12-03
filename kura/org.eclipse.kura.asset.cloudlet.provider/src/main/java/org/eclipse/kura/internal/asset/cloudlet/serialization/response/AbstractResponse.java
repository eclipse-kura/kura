/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
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
