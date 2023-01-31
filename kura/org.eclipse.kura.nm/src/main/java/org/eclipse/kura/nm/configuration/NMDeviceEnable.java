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
package org.eclipse.kura.nm.configuration;

public enum NMDeviceEnable {

    DISABLED,
    UNMANAGED,
    ENABLED;

    public static NMDeviceEnable fromKuraInterfaceStatus(KuraInterfaceStatus ip4Status, KuraInterfaceStatus ip6Status) {
        Boolean ip4Enabled = KuraInterfaceStatus.isEnabled(ip4Status);
        Boolean ip4Disabled = ip4Status == KuraInterfaceStatus.DISABLED;
        Boolean ip4Unmanaged = ip4Status == KuraInterfaceStatus.UNMANAGED;
        Boolean ip4Unknown = ip4Status == KuraInterfaceStatus.UNKNOWN;

        Boolean ip6Enabled = KuraInterfaceStatus.isEnabled(ip6Status);
        Boolean ip6Disabled = ip6Status == KuraInterfaceStatus.DISABLED;
        Boolean ip6Unmanaged = ip6Status == KuraInterfaceStatus.UNMANAGED;
        Boolean ip6Unknown = ip6Status == KuraInterfaceStatus.UNKNOWN;

        if ((ip4Enabled && ip6Enabled) || (ip4Enabled && ip6Disabled) || (ip4Disabled && ip6Enabled)) {
            return ENABLED;
        }

        if (ip4Unmanaged && ip6Unmanaged) {
            return UNMANAGED;
        }

        if ((ip4Unmanaged && !ip6Unmanaged) || (!ip4Unmanaged && ip6Unmanaged)) {
            throw new IllegalArgumentException("ip4 and ip6 status should be both UNMANAGED");
        }

        if (ip4Unknown || ip6Unknown) {
            throw new IllegalArgumentException("ip4 and ip6 status should not be UNKNOWN");
        }

        return DISABLED;
    }

}
