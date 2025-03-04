/*******************************************************************************
 * Copyright (c) 2021, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller.system;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.system.SystemResourceInfo;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonJavaSystemResourcesMapper {

    // Expected resulting json:
    // {
    // "inventory": [
    // {
    // "name" : "rfkill",
    // "version": "2.33.1-0.1",
    // type": "DEB",
    // }
    // ]
    // }

    private static final String INVENTORY = "inventory";
    private static final String RESOURCE_NAME = "name";
    private static final String RESOURCE_VERSION = "version";
    private static final String RESOURCE_TYPE = "type";

    private JsonJavaSystemResourcesMapper() {
        // empty constructor
    }

    public static void marshal(OutputStream outStream, SystemResourcesInfo systemResourcesInfo) throws IOException {
        JsonObject json = Json.object();
        JsonArray resources = new JsonArray();
        systemResourcesInfo.getSystemResources().stream().forEach(sri -> resources.add(getJsonSystemResource(sri)));
        json.add(INVENTORY, resources);

        outStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject getJsonSystemResource(SystemResourceInfo sri) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(RESOURCE_NAME, sri.getName());
        jsonObject.add(RESOURCE_VERSION, sri.getVersion());
        jsonObject.add(RESOURCE_TYPE, sri.getTypeString());
        return jsonObject;
    }

}
