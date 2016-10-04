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
package org.eclipse.kura.net.admin.modem.sierra;

public enum CnsObjectIDs {

    // Radio Information
    OBJID_RSSI(0x1001),
    OBJID_CHANNEL_NUMBER(0x1004),
    OBJID_CHANNEL_STATE(0x1005),
    OBJID_CURRENT_BAND_CLASS(0x4008),

    // Network information
    OBJID_SID_VALUE(0x4001),
    OBJID_NID_VALUE(0x4002),
    OBJID_SRVC_INDICATION(0x1006),
    OBJID_ROAMING_STATUS(0x1007),

    // Hardware information
    OBJID_ESN(0x1000),
    OBJID_PRLVER(0x1008),
    OBJID_FIRMWARE_VERSION(0x0001),
    OBJID_FIRMWARE_DATE(0x0002),
    OBJID_BOOT_VERSION(0x0004),

    // Data summary
    OBJID_CALL_BYTE_CNT(0x3001),

    // Account Reference
    OBJID_ACTIVATION_STATUS(0x1009),
    OBJID_ACTIVATION_DATE(0x1037),
    OBJID_ACTIVE_NAM(0x101f),
    OBJID_PHONE_NO(0x1002),
    OBJID_MDN(0x1038),
    OBJID_MIN(0x1039),

    // Call status
    OBJID_CALL_NOTIFICATION(0x3000),    // Call Notification Status (Get)
    OBJID_CALL_DISCONNECTED(0x300C),   	// call disconnected
    OBJID_CALL_CONNECTING(0x3011),   	// connecting
    OBJID_CALL_CONNECTED(0x300A),   	// call connected
    OBJID_CALL_DORMANT(0x3012),    // call dormant
    OBJID_CALL_ERROR(0x300E),   	// call error

    // OMA-DM
    OBJID_DMCONFIG(0x0E00),    // DM Configuration
    OBJID_DMSTART(0x0E01),    // Start DM Session
    OBJID_DMSTATE(0x0E03),    // DM session state

    OBJID_RADIO_PWR(0x0007); // Radio Power (Set|Get|Notify)

    private int m_objID = 0;

    private CnsObjectIDs(int objID) {
        this.m_objID = objID;
    }

    public int getObjectID() {
        return this.m_objID;
    }
}
