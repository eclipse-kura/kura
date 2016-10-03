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
 * Modes of operation for wifi interfaces
 */
public enum WifiMode {
    /** Mode is unknown. */
    UNKNOWN(0x00), /** Uncoordinated network without central infrastructure. */
    ADHOC(0x01), /** Client mode - Coordinated network with one or more central controllers. */
    INFRA(0x02), /** Access Point Mode - Coordinated network with one or more central controllers. */
    MASTER(0x03);

    private int m_code;

    private WifiMode(int code) {
        this.m_code = code;
    }

    public static WifiMode parseCode(int code) {
        for (WifiMode mode : WifiMode.values()) {
            if (mode.m_code == code) {
                return mode;
            }
        }

        return null;
    }

    public static int getCode(WifiMode wifiMode) {
        for (WifiMode mode : WifiMode.values()) {
            if (mode == wifiMode) {
                return mode.m_code;
            }
        }
        return -1;
    }
}
