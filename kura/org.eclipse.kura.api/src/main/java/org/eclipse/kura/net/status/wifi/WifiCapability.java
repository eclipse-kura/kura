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
 * The capability of a WiFi interface.
 *
 */
public enum WifiCapability {
    /** The device has no encryption/authentication capabilities */
    NONE,
    /** The device supports 40/64-bit WEP encryption. */
    CIPHER_WEP40,
    /** The device supports 104/128-bit WEP encryption. */
    CIPHER_WEP104,
    /** The device supports the TKIP encryption. */
    CIPHER_TKIP,
    /** The device supports the AES/CCMP encryption. */
    CIPHER_CCMP,
    /** The device supports the WPA1 encryption/authentication protocol. */
    WPA,
    /** The device supports the WPA2/RSN encryption/authentication protocol. */
    RSN,
    /** The device supports Access Point mode. */
    AP,
    /** The device supports Ad-Hoc mode. */
    ADHOC,
    /** The device reports frequency capabilities. */
    FREQ_VALID,
    /** The device supports 2.4GHz frequencies. */
    FREQ_2GHZ,
    /** The device supports 5GHz frequencies. */
    FREQ_5GHZ,
    /** The device supports mesh points. */
    MESH,
    /** The device supports WPA2 in IBSS networks */
    IBSS_RSN;

}
