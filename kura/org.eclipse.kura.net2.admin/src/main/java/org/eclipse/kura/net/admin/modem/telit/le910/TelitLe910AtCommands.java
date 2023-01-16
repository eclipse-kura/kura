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

package org.eclipse.kura.net.admin.modem.telit.le910;

public enum TelitLe910AtCommands {

    gpsStartLocationServiceRequest("AT$GPSSLSR=2,3\r\n"),
    PDP_CONTEXT("at+cgdcont");

    private String command;

    private TelitLe910AtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
