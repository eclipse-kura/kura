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
package org.eclipse.kura.net.admin.modem.sierra.mc87xx;

public enum SierraMc87xxAtCommands {

    at("at\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at+gsn\r\n"),
    getFirmwareVersion("at!gver?\r\n"),
    getSystemInfo("at^sysinfo\r\n"),
    getSignalStrength("at+csq\r\n"),
    getMobileStationClass("at+cgclass?\r\n"),
    pdpContext("AT+CGDCONT"),
    softReset("at!reset\r\n");

    private String command;

    private SierraMc87xxAtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
