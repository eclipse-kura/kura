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

public enum KuraIpv6Method {

    AUTO,
    DHCP,
    MANUAL,
    UNKNOWN;

    private static final List<KuraIpv6Method> ENABLED_STATUS = Arrays.asList(KuraIpv6Method.AUTO, KuraIpv6Method.DHCP,
            KuraIpv6Method.MANUAL);

    public static Boolean isEnabled(KuraIpv6Method status) {
        return ENABLED_STATUS.contains(status);
    }

    public static KuraIpv6Method fromString(String status) {
        switch (status) {
        case "netIPv6MethodAuto":
            return KuraIpv6Method.AUTO;
        case "netIPv6MethodDhcp":
            return KuraIpv6Method.DHCP;
        case "netIPv6MethodManual":
            return KuraIpv6Method.MANUAL;
        default:
            return KuraIpv6Method.UNKNOWN;

        }

    }

}