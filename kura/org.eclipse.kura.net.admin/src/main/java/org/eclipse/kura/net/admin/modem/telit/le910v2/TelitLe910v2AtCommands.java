/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.modem.telit.le910v2;

/**
 * Defines AT commands for the Telit LE910 v2 modem.
 */
public enum TelitLe910v2AtCommands {

    PDP_AUTH("at#pdpauth"),
    ENABLE_CELL_DIV("at#rxdiv=1,1\r\n"),
    DISABLE_CELL_DIV("at#rxdiv=0,1\r\n");

    private String command;

    private TelitLe910v2AtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}