/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra.usb598;

public enum SierraUsb598AtCommands {

    at("at\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at+gsn\r\n"),
    getRevision("at+gmr\r\n"),
    reset("at!reset\r\n");

    private String m_command;

    private SierraUsb598AtCommands(String atCommand) {
        this.m_command = atCommand;
    }

    public String getCommand() {
        return this.m_command;
    }
}
