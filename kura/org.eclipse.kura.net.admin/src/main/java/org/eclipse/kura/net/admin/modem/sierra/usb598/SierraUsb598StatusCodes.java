/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra.usb598;

public enum SierraUsb598StatusCodes {

    NOT_AVAILABLE(-1),

    // channel state return codes
    CHANSTATE_NOT_ACQUIRED(0x0000),
    CHANSTATE_ACQUIRED(0x0001),
    CHANSTATE_SCANNING(0x0005),

    // current band class return codes
    BANDCLASS_CELLULAR(0x0000),
    BANDCLASS_PCS(0x0001),

    // activation status return codes
    ACTSTAT_NOT_ACTIVATED(0x0000),
    ACTSTAT_ACTIVATED(0x0001),

    // roaming status return codes
    ROAMSTAT_NOT_ROAMING(0x0000),
    ROAMSTAT_W_SID(0x0001),
    ROAMSTAT_WO_SID(0x0002),

    // service indication return codes
    SRVCIND_NO(0x0000),
    SRVCIND_CDMA(0x0002),
    SRVCIND_GPS(0x0003),

    // call status return codes
    CALLSTAT_DISCONNECTED(0),
    CALLSTAT_CONNECTING(1),
    CALLSTAT_CONNECTED(2),
    CALLSTAT_DORMANT(3),

    // power mode return codes
    PMODE_LPM(0x0000),    // Low Power Mode
    PMODE_ONLINE(0x0001); // Online

    private int statusCode = 0;

    private SierraUsb598StatusCodes(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
