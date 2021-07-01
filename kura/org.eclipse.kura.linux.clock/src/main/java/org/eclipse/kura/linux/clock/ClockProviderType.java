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

public enum ClockProviderType {

    JAVA_NTP("java-ntp"),
    NTPD("ntpd"),
    NTS("nts");

    private String value;

    public String getValue() {
        return this.value;
    }

    ClockProviderType(String value) {
        this.value = value;
    }

}
