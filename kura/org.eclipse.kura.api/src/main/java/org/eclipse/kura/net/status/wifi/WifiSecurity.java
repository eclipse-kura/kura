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
 ******************************************************************************/
package org.eclipse.kura.net.status.wifi;

/**
 * Flags describing the security capabilities of an Access Point.
 */
public enum WifiSecurity {
    /** None */
    NONE,
    /** Supports pairwise 40-bit WEP encryption. */
    PAIR_WEP40,
    /** Supports pairwise 104-bit WEP encryption. */
    PAIR_WEP104,
    /** Supports pairwise TKIP encryption. */
    PAIR_TKIP,
    /** Supports pairwise CCMP encryption. */
    PAIR_CCMP,
    /** Supports a group 40-bit WEP cipher. */
    GROUP_WEP40,
    /** Supports a group 104-bit WEP cipher. */
    GROUP_WEP104,
    /** Supports a group TKIP cipher. */
    GROUP_TKIP,
    /** Supports a group CCMP cipher. */
    GROUP_CCMP,
    /** Supports PSK key management. */
    KEY_MGMT_PSK,
    /** Supports 802.1x key management. */
    KEY_MGMT_802_1X,
    /** Supports WPA/RSN Simultaneous Authentication of Equals. */
    KEY_MGMT_SAE,
    /** Supports WPA/RSN Opportunistic Wireless Encryption. */
    KEY_MGMT_OWE,
    /** Supports WPA/RSN Opportunistic Wireless Encryption transition mode. */
    KEY_MGMT_OWE_TM,
    /** Supports WPA3 Enterprise Suite-B 192 bit mode */
    KEY_MGMT_EAP_SUITE_B_192;

}
