/*******************************************************************************
 * Copyright (c) 2019, 2020 3 PORT d.o.o. and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  3 PORT d.o.o.
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.zte.me3630;

public enum ZteMe3630AtCommands {

    getSimPinStatus("at+cpin?\r\n"),
    getSysInfo("at^sysinfo\r\n"),
    getModuleStatus("at+zpas?\r\n"),
    getRegistrationStatus("at+creg?\r\n"),
    getSignalStrength("at+csq\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at+cgsn\r\n"),
    getICCID("at+zgeticcid\r\n"),
    pdpContext("AT+CGDCONT");

    private String atCommand;

    private ZteMe3630AtCommands(String atCommand) {
        this.atCommand = atCommand;
    }

    public String getCommand() {
        return this.atCommand;
    }
}