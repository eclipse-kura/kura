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
package org.eclipse.kura.type;

/**
 * This contains all the required data type constants required for representing
 * Java data types as {@link TypedValue}
 * @since 1.2
 */
public enum DataType {

    BOOLEAN,

    BYTE_ARRAY,

    DOUBLE,

    INTEGER,

    LONG,

    FLOAT,

    STRING;

    /**
     * Converts {@code stringDataType}, if possible, to the related {@link DataType}.
     *
     * @param stringDataType
     *            String that we want to use to get the respective {@link DataType}.
     * @return a DataType that corresponds to the String passed as argument.
     * @throws IllegalArgumentException
     *             if the passed string does not correspond to an existing {@link DataType}.
     */
    public static DataType getDataType(String stringDataType) {
        if (INTEGER.name().equalsIgnoreCase(stringDataType)) {
            return INTEGER;
        }
        if (FLOAT.name().equalsIgnoreCase(stringDataType)) {
            return FLOAT;
        }
        if (DOUBLE.name().equalsIgnoreCase(stringDataType)) {
            return DOUBLE;
        }
        if (LONG.name().equalsIgnoreCase(stringDataType)) {
            return LONG;
        }
        if (BYTE_ARRAY.name().equalsIgnoreCase(stringDataType)) {
            return BYTE_ARRAY;
        }
        if (BOOLEAN.name().equalsIgnoreCase(stringDataType)) {
            return BOOLEAN;
        }
        if (STRING.name().equalsIgnoreCase(stringDataType)) {
            return STRING;
        }

        throw new IllegalArgumentException("Cannot convert to DataType");
    }
}
