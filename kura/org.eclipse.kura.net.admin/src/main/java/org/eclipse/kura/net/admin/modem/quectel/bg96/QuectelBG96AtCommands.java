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
package org.eclipse.kura.net.admin.modem.quectel.bg96;

/**
 * Defines AT commands for the Telit HE910 modem.
 *
 * @author ilya.binshtok
 *
 */
public enum QuectelBG96AtCommands {

    getSimStatus("AT+QSIMSTAT?\r\n"),
    getSimPinStatus("at+cpin?\r\n"),
    setAutoSimDetection("at+qsimdet=1,0\r\nat+qsimstat=1"),
    getSmsc("at+csca?\r\n"),
    getMobileStationClass("at+cgclass?\r\n"),
    getRegistrationStatus("at+cgreg?\r\n"),
    getGprsSessionDataVolume("at+QGDCNT?\r\n"),
    getSignalStrength("at+csq\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at+cgsn\r\n"),
    pdpContext("AT+CGDCONT");

    private String m_command;

    private QuectelBG96AtCommands(String atCommand) {
        this.m_command = atCommand;
    }

    public String getCommand() {
        return this.m_command;
    }
}