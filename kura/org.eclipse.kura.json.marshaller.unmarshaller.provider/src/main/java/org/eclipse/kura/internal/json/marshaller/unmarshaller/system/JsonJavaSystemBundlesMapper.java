/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.json.marshaller.unmarshaller.system;

import java.util.Arrays;

import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonJavaSystemBundlesMapper {

    // Expected resulting json:
    // {
    // "bundles": [
    // {
    // "name" : "org.eclipse.osgi",
    // "version": "3.8.1.v20120830-144521",
    // "id": "0",
    // "state" : "ACTIVE"
    // }
    // ]
    // }

    private static final String SYSTEM_BUNDLES = "bundles";
    private static final String SYSTEM_BUNDLES_NAME = "name";
    private static final String SYSTEM_BUNDLES_VERSION = "version";
    private static final String SYSTEM_BUNDLES_ID = "id";
    private static final String SYSTEM_BUNDLES_STATE = "state";

    private JsonJavaSystemBundlesMapper() {
        // empty constructor
    }

    public static String marshal(SystemBundles systemBundles) {
        JsonObject json = Json.object();
        JsonArray bundles = new JsonArray();
        Arrays.asList(systemBundles.getBundles()).stream().forEach(sb -> bundles.add(getJsonBundle(sb)));
        json.add(SYSTEM_BUNDLES, bundles);

        return json.toString();
    }

    private static JsonObject getJsonBundle(SystemBundle sb) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SYSTEM_BUNDLES_NAME, sb.getName());
        jsonObject.add(SYSTEM_BUNDLES_VERSION, sb.getVersion());
        jsonObject.add(SYSTEM_BUNDLES_ID, sb.getId());
        jsonObject.add(SYSTEM_BUNDLES_STATE, sb.getState());
        return jsonObject;
    }

}
