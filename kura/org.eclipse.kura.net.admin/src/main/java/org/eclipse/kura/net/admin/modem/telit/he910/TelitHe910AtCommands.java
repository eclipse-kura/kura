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
package org.eclipse.kura.net.admin.modem.telit.he910;

/**
 * Defines AT commands for the Telit HE910 modem.
 */
public enum TelitHe910AtCommands {

    GET_SIM_STATUS("at#qss?\r\n"),
    GET_SIM_PIN_STATUS("at+cpin?\r\n"),
    SET_AUTO_SIM_DETECTION("at#simdet=2\r\n"),
    SIMULATE_SIM_NOT_INSERTED("at#simdet=0\r\n"),
    SIMULATE_SIM_INSERTED("at#simdet=1\r\n"),
    GET_SMSC("at+csca?\r\n"),
    GET_MOBILE_STATION_CLASS("at+cgclass?\r\n"),
    GET_REGISTRATION_STATUS("at+cgreg?\r\n"),
    GET_GPRS_SESSION_DATA_VOLUME("at#gdatavol=1\r\n"),
    PDP_CONTEXT("at+cgdcont");

    private String command;

    private TelitHe910AtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}