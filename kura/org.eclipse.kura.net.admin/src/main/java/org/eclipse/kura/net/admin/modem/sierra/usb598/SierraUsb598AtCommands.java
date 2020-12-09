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
package org.eclipse.kura.net.admin.modem.sierra.usb598;

public enum SierraUsb598AtCommands {

    at("at\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at+gsn\r\n"),
    getRevision("at+gmr\r\n"),
    reset("at!reset\r\n");

    private String command;

    private SierraUsb598AtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
