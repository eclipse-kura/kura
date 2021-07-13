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

import java.util.Optional;

import org.eclipse.kura.core.inventory.resources.SystemBundleRef;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class JsonJavaSystemBundleRefMapper {

    private JsonJavaSystemBundleRefMapper() {
    }

    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";

    public static SystemBundleRef unmarshal(final String encoded) {
        final JsonObject object = Json.parse(encoded).asObject();

        final String name = getStringValue(object, NAME_KEY);
        final Optional<String> version = getOptionalStringValue(object, VERSION_KEY);

        return new SystemBundleRef(name, version);
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
