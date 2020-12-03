/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.net.admin.modem.telefonica;

public enum TelefonicaModemAtCommands {
    
    getICCID("at+iccid\r\n"),
    getSimType("at^cardmode\r\n"),
    disableURC("at^dsci=0\r\n");

    private String command;

    TelefonicaModemAtCommands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return this.command;
    }

}
