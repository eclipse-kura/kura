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

import java.util.Arrays;
import java.util.List;

public enum KuraIp6ConfigurationMethod {

    AUTO,
    DHCP,
    MANUAL,
    UNKNOWN;

    private static final List<KuraIp6ConfigurationMethod> ENABLED_STATUS = Arrays.asList(
            KuraIp6ConfigurationMethod.AUTO, KuraIp6ConfigurationMethod.DHCP, KuraIp6ConfigurationMethod.MANUAL);

    public static Boolean isEnabled(KuraIp6ConfigurationMethod status) {
        return ENABLED_STATUS.contains(status);
    }

    public static KuraIp6ConfigurationMethod fromString(String status) {
        switch (status) {
        case "netIPv6MethodAuto":
            return KuraIp6ConfigurationMethod.AUTO;
        case "netIPv6MethodDhcp":
            return KuraIp6ConfigurationMethod.DHCP;
        case "netIPv6MethodManual":
            return KuraIp6ConfigurationMethod.MANUAL;
        default:
            return KuraIp6ConfigurationMethod.UNKNOWN;

        }

    }

}