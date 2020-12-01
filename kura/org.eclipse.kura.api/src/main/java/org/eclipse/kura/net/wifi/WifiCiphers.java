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
 * Wifi Ciphers enum
 */
public enum WifiCiphers {

    CCMP_TKIP(0x00),
    TKIP(0x01),
    CCMP(0x02);

    private int code;

    private WifiCiphers(int code) {
        this.code = code;
    }

    public static WifiCiphers parseCode(int code) {
        for (WifiCiphers cipher : WifiCiphers.values()) {
            if (cipher.code == code) {
                return cipher;
            }
        }

        return null;
    }

    public static int getCode(WifiCiphers ciphers) {
        for (WifiCiphers cipher : WifiCiphers.values()) {
            if (cipher == ciphers) {
                return cipher.code;
            }
        }

        return -1;
    }

    public static String toString(WifiCiphers ciphers) {

        String ret = null;
        for (WifiCiphers cipher : WifiCiphers.values()) {
            if (cipher == ciphers) {
                if (cipher == WifiCiphers.CCMP_TKIP) {
                    ret = "CCMP TKIP";
                } else if (cipher == WifiCiphers.TKIP) {
                    ret = "TKIP";
                } else if (cipher == WifiCiphers.CCMP) {
                    ret = "CCMP";
                }
            }
        }

        return ret;
    }
}
