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

package org.eclipse.kura.net.admin.modem.telit.generic;

public enum TelitModemAtCommands {

    AT("at\r\n"),
    GET_MODEL_NUMBER("at+gmm\r\n"),
    GET_MANUFACTURER("at+gmi\r\n"),
    GET_SERIAL_NUMBER("at#cgsn\r\n"),
    GET_IMSI("at#cimi\r\n"),
    GET_ICCID("at#ccid\r\n"),
    GET_REVISION("at+gmr\r\n"),
    GET_SIGNAL_STRENGTH("at+csq\r\n"),
    IS_GPS_POWERED("at$GPSP?\r\n"),
    GPS_POWER_UP("at$GPSP=1\r\n"),
    GPS_POWER_DOWN("at$GPSP=0\r\n"),
    // GPS_ENABLE_NMEA("AT$GPSNMUN=3,1,1,1,1,1,1\r\n"),
    GPS_ENABLE_NMEA("AT$GPSNMUN="),
    ESCAPE_SEQUENCE("+++");

    private String command;

    private TelitModemAtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
