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

public enum CnsOpTypes {

    /**
     * CnS Operation Type - 'GetRequest'
     */
    OPTYPE_GET((byte) 0x01),

    /**
     * CnS Operation Type - reply to 'GetRequest'
     */
    OPTYPE_GETREP((byte) 0x02),

    /**
     * CnS Operation Type - 'Set' (CnS command)
     */
    OPTYPE_SET((byte) 0x03),

    /**
     * CnS Operation Type - 'SetAck' (acknowledge to Set)
     */
    OPTYPE_SETACK((byte) 0x04),

    /**
     * CnS Operation Type - 'NotificationEnable'
     */
    OPTYPE_NOTIF_ENB((byte) 0x05),

    /**
     * CnS Operation Type - reply to 'NotificationEnable'
     */
    OPTYPE_NOTIF_ENB_REP((byte) 0x06),

    /**
     * CnS Operation Type - 'Notification'
     */
    OPTYPE_NOTIFICATION((byte) 0x07),

    /**
     * CnS Operation Type - 'Notification Disable'
     */
    OPTYPE_NOTIF_DISABLE((byte) 0x08),

    /**
     * CnS Operation Type - acknowledge to 'Notification Disable'
     */
    OPTYPE_NOTIF_DISABLE_ACK((byte) 0x09),

    /**
     * CnS Operation Type - 'Error'
     */
    OPTYPE_ERROR((byte) 0x80);

    private byte m_opType = 0;

    private CnsOpTypes(byte opType) {
        this.m_opType = opType;
    }

    public byte getOpType() {
        return this.m_opType;
    }
}
