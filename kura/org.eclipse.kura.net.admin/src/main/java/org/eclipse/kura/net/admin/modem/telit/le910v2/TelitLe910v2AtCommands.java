/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
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