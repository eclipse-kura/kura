/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel;

import java.util.Base64;
import java.util.Base64.Decoder;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public final class DataTypeHelper {

    private static final Decoder decoder = Base64.getDecoder();

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
            return TypedValues.newByteArrayValue(decoder.decode(valueString));
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
