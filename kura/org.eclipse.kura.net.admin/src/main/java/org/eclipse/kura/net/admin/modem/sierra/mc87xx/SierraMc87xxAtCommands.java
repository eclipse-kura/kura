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
