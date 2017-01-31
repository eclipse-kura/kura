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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    private static final String PROPERTY_DELIMITER = "_";
    private static final String ERROR_SUFFIX = "_error";
    private static final String VALUE_SUFFIX = "_value";
    private final String name;
    private final TypedValue<?> value;
    private final Map<String, TypedValue<?>> properties = new HashMap<>();

    public WireField(String name, TypedValue<?> value) {
        this.name = name;
        this.value = value;
    }

    public static List<WireField> unflatten(Map<String, TypedValue<?>> properties) {
        final List<WireField> wireFieldList = Collections.emptyList();
        for (final Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final String name = key.substring(0, key.lastIndexOf(PROPERTY_DELIMITER));
            final WireField wireField;
            if (key.endsWith(VALUE_SUFFIX) || key.endsWith(ERROR_SUFFIX)) {
                wireField = new WireField(name, entry.getValue());

                for (final Entry<String, TypedValue<?>> entryAgain : properties.entrySet()) {
                    final String tempKey = entryAgain.getKey();
                    final String tempName = tempKey.substring(0, tempKey.lastIndexOf(PROPERTY_DELIMITER));
                    final String tempSuffix = tempKey.substring(tempKey.lastIndexOf(PROPERTY_DELIMITER) + 1,
                            tempKey.length());
                    if (tempName.equals(name) && !tempKey.endsWith(VALUE_SUFFIX) && !tempKey.endsWith(ERROR_SUFFIX)) {
                        wireField.addProperty(tempSuffix, entryAgain.getValue());
                    }
                }
                wireFieldList.add(wireField);
            }
        }

        return wireFieldList;
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
            typeSuffix = ERROR_SUFFIX;
        } else {
            typeSuffix = VALUE_SUFFIX;
        }
        result.put(this.name + typeSuffix, getValue());

        for (Entry<String, TypedValue<?>> entry : this.properties.entrySet()) {
            result.put(this.name + PROPERTY_DELIMITER + entry.getKey(), entry.getValue());
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
