/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.type.ErrorValue;
import org.eclipse.kura.type.TypedValue;

/**
 * The WireField represents a data type to be contained in {@link WireRecord}
 *
 * @noextend This class is not intended to be extended by clients.
 */
public class WireField {

    private final String name;
    private final TypedValue<?> value;
    private final Map<String, TypedValue<?>> properties = new HashMap<>();

    public WireField(String name, TypedValue<?> value) {
        this.name = name;
        this.value = value;
    }

    public WireField(Map<String, TypedValue<?>> properties) {

    }

    public String getName() {
        return this.name;
    }

    public TypedValue<?> getValue() {
        return this.value;
    }

    public Map<String, TypedValue<?>> getProperties() {
        return this.properties;
    }

    public void addProperty(String key, TypedValue<?> value) {
        if ("value".equals(key) || "error".equals(key)) {
            throw new IllegalArgumentException();
        }
        this.properties.put(key, value);
    }

    public Map<String, TypedValue<?>> flatten() {
        Map<String, TypedValue<?>> result = new HashMap<>();
        String typeSuffix;
        if (this.value instanceof ErrorValue) {
            typeSuffix = "_error";
        } else {
            typeSuffix = "_value";
        }
        result.put(this.name + typeSuffix, getValue());

        for (Entry<String, TypedValue<?>> entry : this.properties.entrySet()) {
            result.put(this.name + "_" + entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WireField)) {
            return false;
        }
        WireField other = (WireField) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    // TODO: unflatten

}
