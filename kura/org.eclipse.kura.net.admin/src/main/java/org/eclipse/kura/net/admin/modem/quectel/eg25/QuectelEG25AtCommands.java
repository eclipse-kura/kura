/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.eg25;

public enum QuectelEG25AtCommands {

    GET_SIM_STATUS("at+qsimstat?\r\n"),
    GET_SIM_PIN_STATUS("at+cpin?\r\n"),
    GET_MOBILESTATION_CLASS("at+cgclass?\r\n"),
    GET_REGISTRATION_STATUS("at+cgreg?\r\n"),
    GET_GPRS_SESSION_DATA_VOLUME("at+qgdcnt?\r\n"),
    PDP_CONTEXT("at+cgdcont\r\n");

    private String command;

    private QuectelEG25AtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}