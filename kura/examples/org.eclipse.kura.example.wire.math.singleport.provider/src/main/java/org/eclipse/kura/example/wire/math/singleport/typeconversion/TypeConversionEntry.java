/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.example.wire.math.singleport.typeconversion;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.type.DataType;

public class TypeConversionEntry {

    private static final Pattern ENTRY_DELIMITER = Pattern.compile("[;\n]");
    private static final Pattern FIELD_DELIMITER = Pattern.compile("[|]");

    private final String propertyName;
    private final DataType type;

    public TypeConversionEntry(final String propertyName, final DataType type) {
        this.propertyName = propertyName;
        this.type = type;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public DataType getType() {
        return type;
    }

    private static void clear(String[] entryArray) {
        entryArray[0] = null;
        entryArray[1] = null;
    }

    private static TypeConversionEntry parse(final String entryString, final String[] tempArray) {
        try {
            clear(tempArray);

            FIELD_DELIMITER.splitAsStream(entryString).map(String::trim).filter(s -> !s.isEmpty())
                    .toArray(i -> tempArray);

            requireNonNull(tempArray[0]);
            requireNonNull(tempArray[1]);
            String typeTemp = tempArray[1];
            DataType dataType;
            
            if ("double".equalsIgnoreCase(typeTemp)) {
                dataType = DataType.DOUBLE;
            } else if ("float".equalsIgnoreCase(typeTemp)) {
                dataType = DataType.FLOAT;
            } else if ("integer".equalsIgnoreCase(typeTemp)) {
                dataType = DataType.INTEGER;
            } else if ("long".equalsIgnoreCase(typeTemp)) {
                dataType = DataType.LONG;
            } else {
                throw new IllegalArgumentException("Unsupported type");
            }

            return new TypeConversionEntry(tempArray[0], dataType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid entry: " + entryString);
        }
    }

    public static List<TypeConversionEntry> parseAll(String configuration) {
        final String[] tempArray = new String[3];
        return ENTRY_DELIMITER.splitAsStream(configuration).map(String::trim).filter(s -> !s.isEmpty())
                .map(entryString -> parse(entryString, tempArray)).collect(Collectors.toList());
    }
}
