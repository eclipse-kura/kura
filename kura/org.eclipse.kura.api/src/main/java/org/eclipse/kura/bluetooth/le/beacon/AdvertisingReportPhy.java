/*******************************************************************************
 * Copyright (c) 2020, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Scott Ware
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.bluetooth.le.beacon;

/**
 * AdvertisingReportPhy represents the Bluetooth physical layer (PHY) variant used for an advertising report.
 * Possible values are:
 * 0x00 : None
 * 0x01 : LE 1M
 * 0x02 : LE 2M
 * 0x03 : LE Coded
 *
 * @since 2.2
 */
public enum AdvertisingReportPhy {

    NONE((byte) 0x00),
    LE_1M((byte) 0x01),
    LE_2M((byte) 0x02),
    LE_CODED((byte) 0x03);

    private final byte phy;

    private AdvertisingReportPhy(byte phy) {
        this.phy = phy;
    }

    public byte getPhyCode() {
        return this.phy;
    }

    public static AdvertisingReportPhy valueOf(byte phy) {
        AdvertisingReportPhy value;

        if (phy == NONE.getPhyCode()) {
            value = NONE;
        } else if (phy == LE_1M.getPhyCode()) {
            value = LE_1M;
        } else if (phy == LE_2M.getPhyCode()) {
            value = LE_2M;
        } else if (phy == LE_CODED.getPhyCode()) {
            value = LE_CODED;
        } else {
            throw new IllegalArgumentException("PHY variant not recognized");
        }
        return value;
    }

}
