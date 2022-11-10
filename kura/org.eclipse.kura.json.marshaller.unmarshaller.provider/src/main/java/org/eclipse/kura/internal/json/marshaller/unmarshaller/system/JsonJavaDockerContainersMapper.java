/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

import java.util.Optional;

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
    // "type": "DOCKER",
    // "state" : "active",
    // }
    // ]
    // }

    private static final String SYSTEM_CONTAINERS = "containers";
    private static final String SYSTEM_CONTAINERS_CONTAINER_NAME = "name";
    private static final String SYSTEM_CONTAINERS_CONTAINER_VERSION = "version";
    private static final String SYSTEM_CONTAINERS_CONTAINER_TYPE = "type";
    private static final String SYSTEM_CONTAINERS_CONTAINER_STATE = "state";

    private JsonJavaDockerContainersMapper() {
        // empty constructor
    }

    public static DockerContainer unmarshal(final String encoded) {
        final JsonObject object = Json.parse(encoded).asObject();

        final String name = getStringValue(object, SYSTEM_CONTAINERS_CONTAINER_NAME);
        final String version = getStringValue(object, SYSTEM_CONTAINERS_CONTAINER_VERSION);

        return new DockerContainer(name, version);

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
        jsonObject.add(SYSTEM_CONTAINERS_CONTAINER_NAME, p.getName());
        jsonObject.add(SYSTEM_CONTAINERS_CONTAINER_VERSION, p.getVersion());
        jsonObject.add(SYSTEM_CONTAINERS_CONTAINER_TYPE, p.getTypeString());
        jsonObject.add(SYSTEM_CONTAINERS_CONTAINER_STATE, p.getFrameworkContainerState());
        return jsonObject;
    }

    private static Optional<String> getOptionalStringValue(final JsonObject object, final String name) {
        return Optional.ofNullable(object.get(name)).map(s -> {
            if (s.isString()) {
                return s.asString();
            } else {
                throw new IllegalArgumentException(name + " must be a string");
            }
        });
    }

    private static String getStringValue(final JsonObject object, final String name) {
        return getOptionalStringValue(object, name)
                .orElseThrow(() -> new IllegalArgumentException(name + " is required"));
    }

}
