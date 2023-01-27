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
package org.eclipse.kura.net2.admin;

import java.util.Arrays;
import java.util.List;

public enum NMDeviceEnable {

    DISABLED,
    UNMANAGED,
    ENABLED;

    private static final List<KuraInterfaceStatus> ENABLED_STATUS = Arrays.asList(KuraInterfaceStatus.ENABLEDLAN,
            KuraInterfaceStatus.ENABLEDWAN, KuraInterfaceStatus.L2ONLY);

    public static NMDeviceEnable fromKuraInterfaceStatus(KuraInterfaceStatus ip4status, KuraInterfaceStatus ip6status) {
        Boolean ip4enabled = ENABLED_STATUS.contains(ip4status);
        Boolean ip6enabled = ENABLED_STATUS.contains(ip6status);

        if ((ip4enabled && ip6enabled) || (ip4enabled && ip6status == KuraInterfaceStatus.DISABLED)
                || (ip4status == KuraInterfaceStatus.DISABLED && ip6enabled)) {
            return ENABLED;
        }

        if (ip4status == KuraInterfaceStatus.UNMANAGED && ip6status == KuraInterfaceStatus.UNMANAGED) {
            return UNMANAGED;
        }

        if ((ip4status == KuraInterfaceStatus.UNMANAGED && ip6status != KuraInterfaceStatus.UNMANAGED)
                || (ip4status != KuraInterfaceStatus.UNMANAGED && ip6status == KuraInterfaceStatus.UNMANAGED)) {
            throw new IllegalArgumentException("ip4 and ip6 status should be both UNMANAGED");
        }

        if (ip4status == KuraInterfaceStatus.UNKNOWN || ip6status == KuraInterfaceStatus.UNKNOWN) {
            throw new IllegalArgumentException("ip4 and ip6 status should not be UNKNOWN");
        }

        return DISABLED;
    }

}
