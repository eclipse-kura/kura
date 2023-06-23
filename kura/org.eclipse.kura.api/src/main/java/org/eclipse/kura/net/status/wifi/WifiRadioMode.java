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
 * Types of WiFi radio modes
 */
public enum WifiRadioMode {

    UNKNOWN,
    RADIO_MODE_80211A,
    RADIO_MODE_80211B,
    RADIO_MODE_80211G,
    RADIO_MODE_80211NHT20,
    RADIO_MODE_80211NHT40_BELOW,
    RADIO_MODE_80211NHT40_ABOVE,
    RADIO_MODE_80211_AC;

}
