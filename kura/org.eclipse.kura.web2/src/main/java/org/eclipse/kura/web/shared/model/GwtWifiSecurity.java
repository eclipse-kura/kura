/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

public enum GwtWifiSecurity {

    netWifiSecurityNONE("NONE"),
    netWifiSecurityWEP("WEP"),
    netWifiSecurityWPA("WPA"),
    netWifiSecurityWPA2("WPA2"),
    netWifiSecurityWPA2WPA3Enterprise("WPA2/WPA3-Enterprise"),
    netWifiSecurityWPA_WPA2("WPA/WPA2");

    String value;

    private GwtWifiSecurity(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
