/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.bluetooth.le.beacon;

/**
 * AdvertisingReportAddressType represents the type of address in to the advertising packet.
 * Possible values are:
 * 0x00 : Public Device Address
 * 0x01 : Random Device Address
 * 0x05 - 0xFF : Reserved for future use
 *
 * @since 1.3
 */
public enum AdvertisingReportAddressType {

    PUBLIC((byte) 0x00),
    RANDOM((byte) 0x01);

    private final byte addressType;

    private AdvertisingReportAddressType(byte addressType) {
        this.addressType = addressType;
    }

    public byte getEventTypeCode() {
        return this.addressType;
    }

    public static AdvertisingReportAddressType valueOf(byte address) {
        AdvertisingReportAddressType type;
        if (address == PUBLIC.getEventTypeCode()) {
            type = PUBLIC;
        } else if (address == RANDOM.getEventTypeCode()) {
            type = RANDOM;
        } else {
            throw new IllegalArgumentException("Address type not recognized");
        }
        return type;
    }

}
