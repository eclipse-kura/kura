/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.gainoffset;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GainOffsetEntry {

    private static final Pattern ENTRY_DELIMITER = Pattern.compile("[;\n]");
    private static final Pattern FIELD_DELIMITER = Pattern.compile("[|]");

    private final String propertyName;
    private final double gain;
    private final double offset;

    public GainOffsetEntry(final String propertyName, final double gain, final double offset) {
        this.propertyName = propertyName;
        this.gain = gain;
        this.offset = offset;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public double getGain() {
        return gain;
    }

    public double getOffset() {
        return offset;
    }

    private static void clear(String[] entryArray) {
        entryArray[0] = null;
        entryArray[1] = null;
        entryArray[2] = null;
    }

    private static GainOffsetEntry parse(final String entryString, final String[] tempArray) {
        try {
            clear(tempArray);

            FIELD_DELIMITER.splitAsStream(entryString).map(String::trim).filter(s -> !s.isEmpty())
                    .toArray(i -> tempArray);

            requireNonNull(tempArray[0]);
            final double gain = Double.parseDouble(tempArray[1]);
            final double offset = tempArray[2] == null ? 0 : Double.parseDouble(tempArray[2]);

            return new GainOffsetEntry(tempArray[0], gain, offset);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid entry: " + entryString);
        }
    }

    public static List<GainOffsetEntry> parseAll(String configuration) {
        final String[] tempArray = new String[3];
        return ENTRY_DELIMITER.splitAsStream(configuration).map(String::trim).filter(s -> !s.isEmpty())
                .map(entryString -> parse(entryString, tempArray)).collect(Collectors.toList());
    }
}
