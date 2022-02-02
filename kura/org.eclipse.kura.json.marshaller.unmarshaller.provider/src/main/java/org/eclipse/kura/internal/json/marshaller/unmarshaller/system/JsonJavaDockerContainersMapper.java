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

import org.eclipse.kura.core.inventory.resources.DockerContainer;
import org.eclipse.kura.core.inventory.resources.DockerContainers;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonJavaDockerContainersMapper {

    // Expected resulting json:
    // {
    // "containers": [
    // {
    // "name" : "rfkill",
    // "version": "nginx:latest",
    // type": "DOCKER",
    // }
    // ]
    // }

    private static final String SYSTEM_CONTAINERS = "containers";
    private static final String SYSTEM_CONTAINERS_CONTAONER_NAME = "name";
    private static final String SYSTEM_CONTAINERS_CONTAONER_VERSION = "version";
    private static final String SYSTEM_CONTAINERS_CONTAONER_TYPE = "type";

    private JsonJavaDockerContainersMapper() {
        // empty constructor
    }

    public static String marshal(DockerContainers dockerContainers) {
        JsonObject json = Json.object();
        JsonArray containers = new JsonArray();
        dockerContainers.getDockerContainers().stream().forEach(p -> containers.add(getJsonPackage(p)));
        json.add(SYSTEM_CONTAINERS, containers);

        return json.toString();
    }

    private static JsonObject getJsonPackage(DockerContainer p) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SYSTEM_CONTAINERS_CONTAONER_NAME, p.getName());
        jsonObject.add(SYSTEM_CONTAINERS_CONTAONER_VERSION, p.getVersion());
        jsonObject.add(SYSTEM_CONTAINERS_CONTAONER_TYPE, p.getTypeString());
        return jsonObject;
    }

}
