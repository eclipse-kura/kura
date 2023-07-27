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

public enum KuraIp6ConfigurationMethod {

    AUTO,
    DHCP,
    MANUAL;

    public static KuraIp6ConfigurationMethod fromString(String status) {
        switch (status) {
        case "netIPv6MethodAuto":
            return KuraIp6ConfigurationMethod.AUTO;
        case "netIPv6MethodDhcp":
            return KuraIp6ConfigurationMethod.DHCP;
        case "netIPv6MethodManual":
            return KuraIp6ConfigurationMethod.MANUAL;
        default:
            throw new IllegalArgumentException(String.format("Unsupported IPv6 configuration method: \"%s\"", status));

        }

    }

}