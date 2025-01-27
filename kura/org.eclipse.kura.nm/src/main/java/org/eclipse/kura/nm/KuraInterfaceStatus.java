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

public enum KuraInterfaceStatus {

    DISABLED,
    UNMANAGED,
    ENABLED;

    public static KuraInterfaceStatus fromKuraIpStatus(KuraIpStatus ip4Status, KuraIpStatus ip6Status) {
        Boolean ip4Enabled = KuraIpStatus.isEnabled(ip4Status);
        boolean ip4Disabled = ip4Status == KuraIpStatus.DISABLED;
        boolean ip4Unmanaged = ip4Status == KuraIpStatus.UNMANAGED;
        boolean ip4Unknown = ip4Status == KuraIpStatus.UNKNOWN;

        Boolean ip6Enabled = KuraIpStatus.isEnabled(ip6Status);
        boolean ip6Disabled = ip6Status == KuraIpStatus.DISABLED;
        boolean ip6Unmanaged = ip6Status == KuraIpStatus.UNMANAGED;
        boolean ip6Unknown = ip6Status == KuraIpStatus.UNKNOWN;

        if (ip4Enabled && ip6Enabled || ip4Enabled && ip6Disabled || ip4Disabled && ip6Enabled) {
            return ENABLED;
        }

        if (ip4Unmanaged && ip6Unmanaged) {
            return UNMANAGED;
        }

        if (ip4Unmanaged || ip6Unmanaged) { // && (ip4Status != ip6Status)
            throw new IllegalArgumentException("ip4 and ip6 status should be both UNMANAGED");
        }

        if (ip4Unknown || ip6Unknown) {
            throw new IllegalArgumentException("ip4 and ip6 status should not be UNKNOWN");
        }

        return DISABLED;
    }

}
