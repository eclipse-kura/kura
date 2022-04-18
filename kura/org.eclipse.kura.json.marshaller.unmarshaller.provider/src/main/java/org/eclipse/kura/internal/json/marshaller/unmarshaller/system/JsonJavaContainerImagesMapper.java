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

import org.eclipse.kura.core.inventory.resources.ContainerImage;
import org.eclipse.kura.core.inventory.resources.ContainerImages;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonJavaContainerImagesMapper {

    // Expected resulting json:
    // {
    // "images": [
    // {
    // "name" : "rfkill",
    // "version": "latest",
    // type": "CONTAINER_IMAGE",
    // }
    // ]
    // }

    private static final String SYSTEM_IMAGES = "images";
    private static final String SYSTEM_IMAGES_IMAGE_NAME = "name";
    private static final String SYSTEM_IMAGES_IMAGE_VERSION = "version";
    private static final String SYSTEM_IMAGES_IMAGE_TYPE = "type";

    private JsonJavaContainerImagesMapper() {
        // empty constructor
    }

    public static ContainerImage unmarshal(final String encoded) {
        final JsonObject object = Json.parse(encoded).asObject();

        final String name = getStringValue(object, SYSTEM_IMAGES_IMAGE_NAME);
        final String version = getStringValue(object, SYSTEM_IMAGES_IMAGE_VERSION);

        return new ContainerImage(name, version);
    }

    public static String marshal(ContainerImages contianerImages) {
        JsonObject json = Json.object();
        JsonArray images = new JsonArray();
        contianerImages.getContainerImages().stream().forEach(p -> images.add(getJsonPackage(p)));
        json.add(SYSTEM_IMAGES, images);

        return json.toString();
    }

    private static JsonObject getJsonPackage(ContainerImage p) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SYSTEM_IMAGES_IMAGE_NAME, p.getName());
        jsonObject.add(SYSTEM_IMAGES_IMAGE_VERSION, p.getVersion());
        jsonObject.add(SYSTEM_IMAGES_IMAGE_TYPE, p.getTypeString());
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
