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
 * AdvertisingReportEventType represents the type of event in to the advertising packet.
 * Possible values are:
 * 0x00 : Connectable undirected advertising
 * 0x01 : Connectable directed advertising
 * 0x02 : Scannable undirected advertising
 * 0x03 : Non connectable undirected advertising
 * 0x04 : Scan Response
 * 0x05 - 0xFF : Reserved for future use
 * 
 * @since 1.3
 */
public enum AdvertisingReportEventType {

    ADV_IND((byte) 0x00),
    ADV_DIRECT_IND((byte) 0x01),
    ADV_SCAN_IND((byte) 0x02),
    ADV_NONCONN_IND((byte) 0x03),
    SCAN_RSP((byte) 0x04);

    private final byte eventType;

    private AdvertisingReportEventType(byte eventType) {
        this.eventType = eventType;
    }

    public byte getEventTypeCode() {
        return this.eventType;
    }

    public static AdvertisingReportEventType valueOf(byte event) {
        AdvertisingReportEventType type;
        if (event == ADV_IND.getEventTypeCode()) {
            type = ADV_IND;
        } else if (event == ADV_DIRECT_IND.getEventTypeCode()) {
            type = ADV_DIRECT_IND;
        } else if (event == ADV_SCAN_IND.getEventTypeCode()) {
            type = ADV_SCAN_IND;
        } else if (event == ADV_NONCONN_IND.getEventTypeCode()) {
            type = ADV_NONCONN_IND;
        } else if (event == SCAN_RSP.getEventTypeCode()) {
            type = SCAN_RSP;
        } else {
            throw new IllegalArgumentException("Report Event type not recognized");
        }
        return type;
    }
}
