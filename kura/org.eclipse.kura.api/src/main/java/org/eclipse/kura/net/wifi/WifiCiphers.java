/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.wifi;

/**
 * Wifi Ciphers enum
 */
public enum WifiCiphers {

    CCMP_TKIP(0x00), TKIP(0x01), CCMP(0x02);

    private int m_code;

    private WifiCiphers(int code) {
        this.m_code = code;
    }

    public static WifiCiphers parseCode(int code) {
        for (WifiCiphers cipher : WifiCiphers.values()) {
            if (cipher.m_code == code) {
                return cipher;
            }
        }

        return null;
    }

    public static int getCode(WifiCiphers ciphers) {
        for (WifiCiphers cipher : WifiCiphers.values()) {
            if (cipher == ciphers) {
                return cipher.m_code;
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
