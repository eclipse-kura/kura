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
package org.eclipse.kura.internal.json.marshaller.unmarshaller;

import java.util.Arrays;

import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonJavaSystemDeploymentPackagesMapper {

    // Expected resulting json:
    // {
    // "packages": [
    // {
    // "name" : "org.eclipse.kura.demo.heater",
    // "version": "1.2.0.qualifier",
    // "bundles": [
    // {
    // "name" : "org.eclipse.kura.demo.heater",
    // "version" : "1.0.1"
    // "id" : "1",
    // "state" : "ACTIVE"
    // }
    // ]
    // }
    // ]
    // }

    private static final String DEPLOYMENT_PACKAGES = "deploymentPackages";
    private static final String DP_NAME = "name";
    private static final String DP_VERSION = "version";
    private static final String DP_BUNDLES = "bundles";
    private static final String DP_ID = "id";
    private static final String DP_STATE = "state";

    private JsonJavaSystemDeploymentPackagesMapper() {
        // empty constructor
    }

    public static String marshal(SystemDeploymentPackages systemDPs) {
        JsonObject json = Json.object();
        JsonArray dps = new JsonArray();
        Arrays.asList(systemDPs.getDeploymentPackages()).stream().forEach(dp -> dps.add(getJsonDP(dp)));
        json.add(DEPLOYMENT_PACKAGES, dps);

        return json.toString();
    }

    private static JsonObject getJsonDP(SystemDeploymentPackage dp) {
        JsonArray bundles = new JsonArray();
        Arrays.asList(dp.getBundleInfos()).stream().forEach(b -> {
            JsonObject jsonBundle = new JsonObject();
            jsonBundle.add(DP_NAME, b.getName());
            jsonBundle.add(DP_VERSION, b.getVersion());
            jsonBundle.add(DP_ID, b.getId());
            jsonBundle.add(DP_STATE, b.getState());
            bundles.add(jsonBundle);
        });
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(DP_NAME, dp.getName());
        jsonObject.add(DP_VERSION, dp.getVersion());
        jsonObject.add(DP_BUNDLES, bundles);
        return jsonObject;
    }

}
