/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

public enum GwtWifiRadioMode {

    netWifiRadioModeANAC("ac", false, true),
    netWifiRadioModeBGN("n", true, true),
    netWifiRadioModeBG("g", true, false),
    netWifiRadioModeB("b", true, false),
    netWifiRadioModeA("a", true, true);

    private final String radioMode;
    private final boolean twoDotFourGhz;
    private final boolean fiveGhz;

    private GwtWifiRadioMode(String radioMode, boolean twoDotFourGhz, boolean fiveGhz) {
        this.radioMode = radioMode;
        this.twoDotFourGhz = twoDotFourGhz;
        this.fiveGhz = fiveGhz;
    }

    /**
     * Return mode based on given string
     *
     * @param mode
     *            - "a", "b", "g", or "n"
     * @return
     */
    public static GwtWifiRadioMode getRadioMode(String mode) {

        if ("a".equals(mode)) {
            return GwtWifiRadioMode.netWifiRadioModeA;
        } else if ("b".equals(mode)) {
            return GwtWifiRadioMode.netWifiRadioModeB;
        } else if ("g".equals(mode)) {
            return GwtWifiRadioMode.netWifiRadioModeBG;
        } else if ("n".equals(mode)) {
            return GwtWifiRadioMode.netWifiRadioModeBGN;
        } else if ("ac".equals(mode)) {
            return GwtWifiRadioMode.netWifiRadioModeANAC;
        }

        return null;
    }

    public String getRadioMode() {
        return radioMode;
    }

    public boolean isTwoDotFourGhz() {
        return twoDotFourGhz;
    }

    public boolean isFiveGhz() {
        return fiveGhz;
    }
}
