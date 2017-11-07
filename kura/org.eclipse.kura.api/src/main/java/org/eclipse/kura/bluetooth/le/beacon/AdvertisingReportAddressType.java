/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
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
