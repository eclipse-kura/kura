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
import java.util.Optional;

public enum KuraIpStatus {

    DISABLED,
    UNMANAGED,
    L2ONLY,
    ENABLEDLAN,
    ENABLEDWAN,
    UNKNOWN;

    private static final List<KuraIpStatus> ENABLED_STATUS = Arrays.asList(KuraIpStatus.ENABLEDLAN,
            KuraIpStatus.ENABLEDWAN, KuraIpStatus.L2ONLY);

    public static Boolean isEnabled(KuraIpStatus status) {
        return ENABLED_STATUS.contains(status);
    }

    public static KuraIpStatus fromString(String status) {
        switch (status) {
        case "netIPv4StatusDisabled":
        case "netIPv6StatusDisabled":
            return KuraIpStatus.DISABLED;
        case "netIPv4StatusUnmanaged":
        case "netIPv6StatusUnmanaged":
            return KuraIpStatus.UNMANAGED;
        case "netIPv4StatusL2Only":
        case "netIPv6StatusL2Only":
            return KuraIpStatus.L2ONLY;
        case "netIPv4StatusEnabledLAN":
        case "netIPv6StatusEnabledLAN":
            return KuraIpStatus.ENABLEDLAN;
        case "netIPv4StatusEnabledWAN":
        case "netIPv6StatusEnabledWAN":
            return KuraIpStatus.ENABLEDWAN;
        default:
            return KuraIpStatus.UNKNOWN;

        }

    }

    public static Optional<KuraIpStatus> fromString(Optional<String> status) {

        if (status.isPresent()) {
            switch (status.get()) {
            case "netIPv4StatusDisabled":
            case "netIPv6StatusDisabled":
                return Optional.of(KuraIpStatus.DISABLED);
            case "netIPv4StatusUnmanaged":
            case "netIPv6StatusUnmanaged":
                return Optional.of(KuraIpStatus.UNMANAGED);
            case "netIPv4StatusL2Only":
            case "netIPv6StatusL2Only":
                return Optional.of(KuraIpStatus.L2ONLY);
            case "netIPv4StatusEnabledLAN":
            case "netIPv6StatusEnabledLAN":
                return Optional.of(KuraIpStatus.ENABLEDLAN);
            case "netIPv4StatusEnabledWAN":
            case "netIPv6StatusEnabledWAN":
                return Optional.of(KuraIpStatus.ENABLEDWAN);
            default:
                return Optional.of(KuraIpStatus.UNKNOWN);

            }

        } else {
            return Optional.empty();
        }

    }

}
