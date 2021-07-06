/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.clock;

import java.util.HashMap;
import java.util.Map;

public enum ClockProviderType {

    JAVA_NTP("java-ntp"),
    NTPD("ntpd"),
    NTS("nts");

    private static Map<String, ClockProviderType> valuesMap = new HashMap<>();

    static {
        for (ClockProviderType type : ClockProviderType.values()) {
            valuesMap.put(type.getValue(), type);
        }
    }

    private String value;

    public String getValue() {
        return this.value;
    }

    ClockProviderType(String value) {
        this.value = value;
    }

    public static ClockProviderType fromValue(String value) {
        return valuesMap.get(value);
    }

}
