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
 ******************************************************************************/
package org.eclipse.kura.rest.configuration.provider.test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public interface JsonProjection {

    public JsonValue apply(final JsonValue element);

    public static JsonProjection self() {
        return new JsonProjection() {

            @Override
            public JsonValue apply(JsonValue element) {
                return element;
            }

            @Override
            public String toString() {
                return "self";
            }
        };
    }

    public default JsonProjection compose(final JsonProjection other) {
        final JsonProjection self = this;

        return new JsonProjection() {

            @Override
            public JsonValue apply(JsonValue element) {
                return other.apply(self.apply(element));
            }

            @Override
            public String toString() {
                return self.toString() + other.toString();
            }
        };
    }

    public default JsonProjection field(final String name) {
        return this.compose(new JsonProjection() {

            @Override
            public JsonValue apply(JsonValue element) {
                final JsonObject asObject = element.asObject();
                return asObject.get(name);
            }

            @Override
            public String toString() {
                return "." + name;
            }
        });
    }

    public default JsonProjection arrayItem(final int index) {
        return this.compose(new JsonProjection() {

            @Override
            public JsonValue apply(JsonValue element) {
                final JsonArray asArray = element.asArray();
                return asArray.get(index);
            }

            @Override
            public String toString() {
                return "[" + index + "]";
            }
        });
    }
}
