/**
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.s7plc;

import Moka7.S7;

public enum S7PlcArea {

    AREA_CT(S7.S7AreaCT) {

        @Override
        public String toString() {
            return "Counters";
        }
    },
    AREA_PE(S7.S7AreaPE) {

        @Override
        public String toString() {
            return "Inputs";
        }
    },
    AREA_PA(S7.S7AreaPA) {

        @Override
        public String toString() {
            return "Outputs";
        }
    },
    AREA_MK(S7.S7AreaMK) {

        @Override
        public String toString() {
            return "Merkers";
        }
    },
    AREA_TM(S7.S7AreaTM) {

        @Override
        public String toString() {
            return "Timers";
        }
    },
    AREA_DB(S7.S7AreaDB) {

        @Override
        public String toString() {
            return "Data Blocks";
        }
    };

    private int value;

    S7PlcArea(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
