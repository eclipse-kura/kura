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

import org.eclipse.kura.nm.enums.NMSettingIP6ConfigPrivacy;

public enum KuraIp6Privacy {

    DISABLED,
    ENABLED_PUBLIC_ADD,
    ENABLED_TEMP_ADD;

    public static KuraIp6Privacy fromString(String status) {
        switch (status) {
        case "netIPv6PrivacyDisabled":
            return KuraIp6Privacy.DISABLED;
        case "netIPv6PrivacyEnabledPubAdd":
            return KuraIp6Privacy.ENABLED_PUBLIC_ADD;
        case "netIPv6PrivacyEnabledTempAdd":
            return KuraIp6Privacy.ENABLED_TEMP_ADD;
        default:
            throw new IllegalArgumentException(String.format("Unsupported IPv6 privacy value: \"%s\"", status));
        }
    }

    public static NMSettingIP6ConfigPrivacy toNMSettingIP6ConfigPrivacy(KuraIp6Privacy privacyValue) {
        switch (privacyValue) {
        case DISABLED:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_DISABLED;
        case ENABLED_PUBLIC_ADD:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_PUBLIC_ADDR;
        case ENABLED_TEMP_ADD:
            return NMSettingIP6ConfigPrivacy.NM_SETTING_IP6_CONFIG_PRIVACY_PREFER_TEMP_ADDR;
        default:
            throw new IllegalArgumentException(String.format("Unsupported IPv6 privacy value: \"%s\"", privacyValue));
        }
    }
}
