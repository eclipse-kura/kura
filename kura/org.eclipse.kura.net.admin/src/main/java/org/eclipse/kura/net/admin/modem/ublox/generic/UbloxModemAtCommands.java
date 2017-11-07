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

package org.eclipse.kura.net.admin.modem.ublox.generic;

public enum UbloxModemAtCommands {

    getGprsSessionDataVolume("at+ugcntrd\r\n"),
    getAuthentificationRequest("at+uauthreq");

    private String m_command;

    private UbloxModemAtCommands(String atCommand) {
        this.m_command = atCommand;
    }

    public String getCommand() {
        return this.m_command;
    }
}
