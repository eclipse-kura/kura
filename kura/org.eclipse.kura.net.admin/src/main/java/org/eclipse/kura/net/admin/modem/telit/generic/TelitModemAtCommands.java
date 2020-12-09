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

package org.eclipse.kura.net.admin.modem.telit.generic;

public enum TelitModemAtCommands {

    at("at\r\n"),
    getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at#cgsn\r\n"),
    getIMSI("at#cimi\r\n"),
    getICCID("at#ccid\r\n"),
    getRevision("at+gmr\r\n"),
    getSignalStrength("at+csq\r\n"),
    isGpsPowered("at$GPSP?\r\n"),
    gpsPowerUp("at$GPSP=1\r\n"),
    gpsPowerDown("at$GPSP=0\r\n"),
    // gpsEnableNMEA("AT$GPSNMUN=3,1,1,1,1,1,1\r\n"),
    gpsEnableNMEA("AT$GPSNMUN="),
    escapeSequence("+++");

    private String command;

    private TelitModemAtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
