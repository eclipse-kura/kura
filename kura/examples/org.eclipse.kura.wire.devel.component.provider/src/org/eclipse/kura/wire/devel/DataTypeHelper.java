/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.devel;

import java.util.Base64;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public final class DataTypeHelper {

    private DataTypeHelper() {
        throw new IllegalAccessError();
    }

    public static TypedValue<?> parseTypedValue(final DataType type, final String valueString) {
        switch (type) {
        case BOOLEAN:
            return TypedValues.newBooleanValue(Boolean.parseBoolean(valueString));
        case LONG:
            return TypedValues.newLongValue(Long.parseLong(valueString));
        case FLOAT:
            return TypedValues.newFloatValue(Float.parseFloat(valueString));
        case BYTE_ARRAY:
            return TypedValues.newByteArrayValue(Base64.getDecoder().decode(valueString));
        case DOUBLE:
            return TypedValues.newDoubleValue(Double.parseDouble(valueString));
        case STRING:
            return TypedValues.newStringValue(valueString);
        case INTEGER:
            return TypedValues.newIntegerValue(Integer.parseInt(valueString));
        default:
            throw new IllegalArgumentException("Unsupported type");
        }
    }
}
