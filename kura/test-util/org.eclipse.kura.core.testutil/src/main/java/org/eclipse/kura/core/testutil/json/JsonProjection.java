/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.testutil.json;

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
                return "";
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
                try {
                    return asArray.get(index);
                } catch (final Exception e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return "[" + index + "]";
            }
        });
    }

    public default JsonProjection anyArrayItem(final JsonProjection other) {
        return this.compose(new JsonProjection() {

            @Override
            public JsonValue apply(JsonValue element) {
                final JsonArray asArray = element.asArray();

                for (final JsonValue member : asArray) {
                    try {
                        final JsonValue value = other.apply(member);

                        if (value != null) {
                            return value;
                        }
                    } catch (final Exception e) {
                        // continue
                    }
                }

                return null;
            }

            @Override
            public String toString() {
                return "[*]" + other.toString();
            }
        });
    }

    public default JsonProjection matching(final JsonValue value) {
        return this.compose(new JsonProjection() {

            @Override
            public JsonValue apply(JsonValue element) {
                if (value.equals(element)) {
                    return element;
                } else {
                    return null;
                }
            }

            @Override
            public String toString() {
                return "(=" + value + ")";
            }
        });
    }
}
