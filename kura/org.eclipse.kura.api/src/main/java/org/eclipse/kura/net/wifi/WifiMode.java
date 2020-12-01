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
 * Modes of operation for wifi interfaces
 */
public enum WifiMode {
    /** Mode is unknown. */
    UNKNOWN(0x00),
    /** Uncoordinated network without central infrastructure. */
    ADHOC(0x01),
    /** Client mode - Coordinated network with one or more central controllers. */
    INFRA(0x02),
    /** Access Point Mode - Coordinated network with one or more central controllers. */
    MASTER(0x03);

    private int code;

    private WifiMode(int code) {
        this.code = code;
    }

    public static WifiMode parseCode(int code) {
        for (WifiMode mode : WifiMode.values()) {
            if (mode.code == code) {
                return mode;
            }
        }

        return null;
    }

    public static int getCode(WifiMode wifiMode) {
        for (WifiMode mode : WifiMode.values()) {
            if (mode == wifiMode) {
                return mode.code;
            }
        }
        return -1;
    }
}
