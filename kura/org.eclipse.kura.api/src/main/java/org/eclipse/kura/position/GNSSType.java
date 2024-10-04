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
 ******************************************************************************/
package org.eclipse.kura.position;

import java.util.HashMap;
import java.util.Map;

public enum GNSSType {

    UNKNOWN("Unknown"),
    OTHER("Other"),
    BEIDOU("Beidou"),
    GALILEO("Galileo"),
    GLONASS("Glonass"),
    GPS("Gps"),
    IRNSS("IRNSS"),
    QZSS("QZSS");

    private String value;

    private static Map<String, GNSSType> valuesMap = new HashMap<>();

    static {
        for (GNSSType type : GNSSType.values()) {
            valuesMap.put(type.getValue(), type);
        }
    }

    private GNSSType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static GNSSType fromValue(String value) {
        return valuesMap.get(value);
    }
}
