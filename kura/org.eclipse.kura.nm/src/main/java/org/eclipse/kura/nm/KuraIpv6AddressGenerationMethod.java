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

public enum KuraIpv6AddressGenerationMethod {

    AUTO,
    DHCP,
    MANUAL,
    UNKNOWN;

    private static final List<KuraIpv6AddressGenerationMethod> ENABLED_STATUS = Arrays.asList(
            KuraIpv6AddressGenerationMethod.AUTO, KuraIpv6AddressGenerationMethod.DHCP,
            KuraIpv6AddressGenerationMethod.MANUAL);

    public static Boolean isEnabled(KuraIpv6AddressGenerationMethod status) {
        return ENABLED_STATUS.contains(status);
    }

    public static KuraIpv6AddressGenerationMethod fromString(String status) {
        switch (status) {
        case "netIPv6MethodAuto":
            return KuraIpv6AddressGenerationMethod.AUTO;
        case "netIPv6MethodDhcp":
            return KuraIpv6AddressGenerationMethod.DHCP;
        case "netIPv6MethodManual":
            return KuraIpv6AddressGenerationMethod.MANUAL;
        default:
            return KuraIpv6AddressGenerationMethod.UNKNOWN;

        }

    }

}