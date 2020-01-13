/*******************************************************************************
 * Copyright (c) 2020 3 PORT d.o.o. and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     3 PORT d.o.o.
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.simtech.sim7000;

public enum SimTechSim7000AtCommands {

    getSimPinStatus("at+cpin?\r\n"),
    getModuleStatus("at+cnsmod?\r\n"),
    getSignalStrength("at+csq\r\n"),
    pdpContext("AT+CGDCONT");

    private String atCommand;

    private SimTechSim7000AtCommands(String atCommand) {
        this.atCommand = atCommand;
    }

    public String getCommand() {
        return this.atCommand;
    }
}
