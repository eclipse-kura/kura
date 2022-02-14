/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.linux.position;

import java.util.HashMap;
import java.util.Map;

public enum PositionProviderType {

    SERIAL("serial"),
    GPSD("gpsd");

    private String value;

    private static Map<String, PositionProviderType> valuesMap = new HashMap<>();

    static {
        for (PositionProviderType type : PositionProviderType.values()) {
            valuesMap.put(type.getValue(), type);
        }
    }

    private PositionProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PositionProviderType fromValue(String value) {
        return valuesMap.get(value);
    }

}
