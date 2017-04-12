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
import java.util.List;

import org.eclipse.kura.internal.asset.cloudlet.serialization.SerializationConstants;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class MetadataRequest {

    private List<String> assetNames = new ArrayList<String>();

    public MetadataRequest(JsonArray array) {
        for (JsonValue value : array) {
            JsonObject object = value.asObject();
            assetNames.add(object.get(SerializationConstants.ASSET_NAME_PROPERTY).asString());
        }
    }

    public List<String> getAssetNames() {
        return assetNames;
    }
}
