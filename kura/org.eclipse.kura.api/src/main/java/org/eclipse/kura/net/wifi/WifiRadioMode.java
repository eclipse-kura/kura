/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.wifi;

/**
 * Types of wifi radio modes
 */
public enum WifiRadioMode {

    RADIO_MODE_80211a(0x00),
    RADIO_MODE_80211b(0x01),
    RADIO_MODE_80211g(0x02),
    RADIO_MODE_80211nHT20(0x03),
    RADIO_MODE_80211nHT40below(0x04),
    RADIO_MODE_80211nHT40above(0x05);

    private int code;

    private WifiRadioMode(int code) {
        this.code = code;
    }

    public static WifiRadioMode parseCode(int code) {
        for (WifiRadioMode mode : WifiRadioMode.values()) {
            if (mode.code == code) {
                return mode;
            }
        }

        return null;
    }

    public static int getCode(WifiRadioMode radioMode) {
        for (WifiRadioMode mode : WifiRadioMode.values()) {
            if (mode == radioMode) {
                return mode.code;
            }
        }

        return -1;
    }
}
