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

package org.eclipse.kura.net.admin.modem.hspa;

public enum HspaModemAtCommands {

    AT("at\r\n"),
    GET_SIM_PIN_STATUS("at+cpin?\r\n"),
    GET_MODEL_NUMBER("at+gmm\r\n"),
    GET_MANUFACTURER("at+gmi\r\n"),
    GET_SERIAL_NUMBER("at+cgsn\r\n"),
    GET_REVISION("at+gmr\r\n"),
    GET_IMSI("at+cimi\r\n"),
    GET_ICCID("at+ccid\r\n"),
    GET_SIGNAL_STRENGTH("at+csq\r\n"),
    GET_MOBILE_STATION_CLASS("at+cgclass?\r\n"),
    GET_REGISTRATION_STATUS("at+cgreg?\r\n"),
    PDP_CONTEXT("at+cgdcont");

    private String command;

    private HspaModemAtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
