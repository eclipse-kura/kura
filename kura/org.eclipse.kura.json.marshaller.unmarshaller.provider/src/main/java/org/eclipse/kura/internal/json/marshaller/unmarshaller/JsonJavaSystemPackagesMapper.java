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

import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonJavaSystemPackagesMapper {

    // Expected resulting json:
    // {
    // "systemPackages": [
    // {
    // "name" : "rfkill",
    // "version": "2.33.1-0.1",
    // type": "DEB",
    // }
    // ]
    // }

    private static final String SYSTEM_PACKAGES = "systemPackages";
    private static final String SYSTEM_PACKAGES_PACKAGE_NAME = "name";
    private static final String SYSTEM_PACKAGES_PACKAGE_VERSION = "version";
    private static final String SYSTEM_PACKAGES_PACKAGE_TYPE = "type";

    private JsonJavaSystemPackagesMapper() {
        // empty constructor
    }

    public static String marshal(SystemPackages systemPackages) {
        JsonObject json = Json.object();
        JsonArray packages = new JsonArray();
        systemPackages.getSystemPackages().stream().forEach(p -> packages.add(getJsonPackage(p)));
        json.add(SYSTEM_PACKAGES, packages);

        return json.toString();
    }

    private static JsonObject getJsonPackage(SystemPackage p) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SYSTEM_PACKAGES_PACKAGE_NAME, p.getName());
        jsonObject.add(SYSTEM_PACKAGES_PACKAGE_VERSION, p.getVersion());
        jsonObject.add(SYSTEM_PACKAGES_PACKAGE_TYPE, p.getTypeString());
        return jsonObject;
    }

}
