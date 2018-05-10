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
