/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

    at("at\r\n"),
    getSimPinStatus("at+cpin?\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at+cgsn\r\n"),
    getRevision("at+gmr\r\n"),
    getIMSI("at+cimi\r\n"),
    getICCID("at+ccid\r\n"),
    getSignalStrength("at+csq\r\n"),
    getMobileStationClass("at+cgclass?\r\n"),
    getRegistrationStatus("at+cgreg?\r\n"),
    pdpContext("at+cgdcont");

    private String command;

    private HspaModemAtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
