/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm;

public enum KuraWifiSecurityType {

    SECURITY_NONE,
    SECURITY_WEP,
    SECURITY_WPA,
    SECURITY_WPA2,
    SECURITY_WPA2_WPA3_ENTERPRISE,
    SECURITY_WPA_WPA2;

    public static KuraWifiSecurityType fromString(String securityType) {
        if (securityType == null || securityType.isEmpty()) {
            throw new IllegalArgumentException("Invalid security type: null or empty string are not supported");
        }

        switch (securityType) {
        case "NONE":
            return KuraWifiSecurityType.SECURITY_NONE;
        case "SECURITY_WEP":
            return KuraWifiSecurityType.SECURITY_WEP;
        case "SECURITY_WPA":
            return KuraWifiSecurityType.SECURITY_WPA;
        case "SECURITY_WPA2":
            return KuraWifiSecurityType.SECURITY_WPA2;
        case "SECURITY_WPA2_WPA3_ENTERPRISE":
            return KuraWifiSecurityType.SECURITY_WPA2_WPA3_ENTERPRISE;
        case "SECURITY_WPA_WPA2":
            return KuraWifiSecurityType.SECURITY_WPA_WPA2;
        default:
            throw new IllegalArgumentException("Invalid security type: " + securityType);
        }
    }
}
